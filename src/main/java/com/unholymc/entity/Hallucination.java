package com.unholymc.entity;

import com.unholymc.config.ConfigManager;
import com.unholymc.entity.slop.SlopKind;
import com.unholymc.entity.slop.SlopVariant;
import com.unholymc.item.CrucifixItem;
import com.unholymc.registry.ModSounds;
import com.unholymc.survival.StatsHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * A personal hallucination (GDD 5.3, DECISIONS #22): only its owner receives it from the server, it hunts
 * and really hurts only the owner, and the owner's blows also land on whatever real entity stands inside
 * its hitbox (e.g. another player). Never saved; vanishes when the owner leaves, recovers or grabs a crucifix.
 */
public class Hallucination extends PathfinderMob implements BillboardEntity, HolyRepellable, AuraFilter {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(Hallucination.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CHASING = SynchedEntityData.defineId(Hallucination.class, EntityDataSerializers.BOOLEAN);

    private @Nullable UUID owner;
    /** Summoned by /thm summon: ignores the owner's sanity and game mode (test command). */
    private boolean forced;

    public void setForced() {
        this.forced = true;
    }
    private int age;

    public Hallucination(EntityType<? extends Hallucination> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        var cfg = ConfigManager.get().hallucinations;
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, cfg.health)
                .add(Attributes.ATTACK_DAMAGE, cfg.damage)
                .add(Attributes.MOVEMENT_SPEED, cfg.speed)
                .add(Attributes.ARMOR, 0)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    public void setup(ServerPlayer owner, SlopVariant variant) {
        this.owner = owner.getUUID();
        this.entityData.set(DATA_VARIANT, variant.ordinal());
        this.setTarget(owner);
    }

    public @Nullable UUID owner() {
        return owner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, SlopVariant.ofKind(SlopKind.AGGRESSIVE).getFirst().ordinal());
        builder.define(DATA_CHASING, true);
    }

    @Override
    public SlopVariant variant() {
        return SlopVariant.byId(this.entityData.get(DATA_VARIANT));
    }

    @Override
    public boolean isHallucinationLook() {
        return true;
    }

    @Override
    public boolean isChasing() {
        return this.entityData.get(DATA_CHASING);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_VARIANT.equals(accessor)) refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        SlopVariant v = variant();
        return EntityDimensions.scalable(v.width, v.height);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
    }

    // --- visibility: only the owner gets this entity ---

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return owner != null && owner.equals(player.getUUID());
    }

    @Override
    public boolean affects(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    public @Nullable ServerPlayer ownerPlayer() {
        return owner != null && level() instanceof ServerLevel sl ? sl.getServer().getPlayerList().getPlayer(owner) : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        age++;
        ServerPlayer p = ownerPlayer();
        var cfg = ConfigManager.get().hallucinations;
        if (p == null || !p.isAlive() || p.level() != level || p.distanceToSqr(this) > cfg.vanishDistance * cfg.vanishDistance
                || age > cfg.lifetimeTicks || CrucifixItem.isHeldForPassive(p)
                || (!forced && (!StatsHelper.isActive(p) || com.unholymc.attachment.ModAttachments.hud(p).sanity() > ConfigManager.get().sanity.lowSanityThreshold))) {
            vanish();
            return;
        }
        if (getTarget() != p) setTarget(p);
    }

    /** Only the owner sees the hallucination, so only the owner may hear it. */
    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (isSilent() || !(level() instanceof ServerLevel)) return;
        ServerPlayer p = ownerPlayer();
        if (p != null) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), getSoundSource(),
                    getX(), getY(), getZ(), volume, pitch, random.nextLong()));
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target instanceof Player p && affects(p) && super.canAttack(target);
    }

    // --- the owner's blows go through into the real world ---

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player p && affects(p) && source.isDirect()) {
            for (Entity e : level.getEntities(this, getBoundingBox(), e -> e != attacker && e instanceof LivingEntity && !(e instanceof Hallucination) && e.isAlive())) {
                e.hurtServer(level, source, amount);
            }
        } else if (attacker != null && !(attacker instanceof Player p2 && affects(p2))) {
            return false; // nothing but its owner (and holy items) can touch it
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (source.getEntity() instanceof ServerPlayer p && affects(p)) {
            StatsHelper.addSanity(p, ConfigManager.get().hallucinations.killSanity);
        }
    }

    public void vanish() {
        if (level() instanceof ServerLevel level) {
            ServerPlayer p = ownerPlayer();
            if (p != null) level.sendParticles(p, ParticleTypes.SMOKE, false, false, getX(), getY() + getBbHeight() / 2, getZ(), 12, 0.3, 0.5, 0.3, 0.01);
            playSound(ModSounds.ENTITY_HALLUCINATION_DISSOLVE.value(), 0.8f, 1f);
        }
        discard();
    }

    @Override
    public void repelFrom(net.minecraft.world.phys.Vec3 source, int ticks) {
        vanish(); // a personal illusion breaks at the touch of holy power
    }

    // --- no physical presence for anybody else ---

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_HALLUCINATION_AMBIENT.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_HALLUCINATION_HURT.value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_HALLUCINATION_DISSOLVE.value();
    }
}
