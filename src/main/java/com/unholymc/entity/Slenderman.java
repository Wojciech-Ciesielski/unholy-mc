package com.unholymc.entity;

import com.unholymc.config.ConfigManager;
import com.unholymc.horror.HallucinationSpawner;
import com.unholymc.registry.ModEntities;
import com.unholymc.registry.ModSounds;
import com.unholymc.registry.ModTags;
import com.unholymc.world.BloodMoon;
import com.unholymc.world.SlenderForests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Slenderman, the miniboss (GDD 3.1, DECISIONS #13/#20): 150 HP, iron-armour protection, 16 damage.
 * Glitch-teleports next to his target, summons 2 hallucinations per nearby player every minute, ignores
 * holy items, vanishes at dawn. During a Blood Moon he becomes Deadly (x3 HP, 24 damage).
 * Drops the Unholy Sword, ender pearls and 10 levels of experience; a defeated forest stays empty.
 */
public class Slenderman extends Monster {
    private static final EntityDataAccessor<Integer> DATA_GLITCH = SynchedEntityData.defineId(Slenderman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DEADLY = SynchedEntityData.defineId(Slenderman.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier DEADLY_HEALTH = com.unholymc.UnholyMC.id("deadly_health");
    private static final Identifier DEADLY_DAMAGE = com.unholymc.UnholyMC.id("deadly_damage");
    public static final int GLITCH_OUT = 5, GLITCH_IN = 4;

    private final ServerBossEvent bossEvent = new ServerBossEvent(java.util.UUID.randomUUID(), Component.translatable("entity.unholymc.slenderman"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private @Nullable Vec3 pendingTeleport;
    private int teleportCooldown;
    private int summonTimer;
    /** Forest this Slenderman belongs to (Long.MIN_VALUE = summoned by command). */
    private long forestAnchor = Long.MIN_VALUE;
    /** /thm summon deadly_slenderman: stays deadly regardless of the moon. */
    private boolean forcedDeadly;

    public void forceDeadly() {
        this.forcedDeadly = true;
        setDeadly(true);
    }

    public Slenderman(EntityType<? extends Slenderman> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        var cfg = ConfigManager.get().slenderman;
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, cfg.health)
                .add(Attributes.ARMOR, cfg.armor)
                .add(Attributes.ATTACK_DAMAGE, cfg.damage)
                .add(Attributes.MOVEMENT_SPEED, cfg.speed)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GLITCH, 0);
        builder.define(DATA_DEADLY, false);
    }

    public int glitchTicks() {
        return entityData.get(DATA_GLITCH);
    }

    public boolean isDeadly() {
        return entityData.get(DATA_DEADLY);
    }

    public void setForestAnchor(long anchor) {
        this.forestAnchor = anchor;
    }

    public long forestAnchor() {
        return forestAnchor;
    }

    /** Turns into (or back from) the Deadly Slenderman, keeping the health ratio. */
    public void setDeadly(boolean deadly) {
        if (deadly == isDeadly()) return;
        var cfg = ConfigManager.get().slenderman;
        float ratio = getHealth() / getMaxHealth();
        var health = getAttribute(Attributes.MAX_HEALTH);
        var damage = getAttribute(Attributes.ATTACK_DAMAGE);
        health.removeModifier(DEADLY_HEALTH);
        damage.removeModifier(DEADLY_DAMAGE);
        if (deadly) {
            health.addTransientModifier(new AttributeModifier(DEADLY_HEALTH, cfg.deadlyHealthMultiplier - 1f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            damage.addTransientModifier(new AttributeModifier(DEADLY_DAMAGE, cfg.deadlyDamage - cfg.damage, AttributeModifier.Operation.ADD_VALUE));
            playSound(ModSounds.ENTITY_SLENDERMAN_DEADLY_ROAR.value(), 3f, 1f);
        }
        setHealth(getMaxHealth() * ratio);
        entityData.set(DATA_DEADLY, deadly);
        bossEvent.setName(Component.translatable(deadly ? "entity.unholymc.deadly_slenderman" : "entity.unholymc.slenderman"));
        bossEvent.setColor(deadly ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.PURPLE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 32f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) return;
        var cfg = ConfigManager.get().slenderman;

        // glitch teleport state machine: out at the old place, jump, in at the new place
        int glitch = glitchTicks();
        if (glitch > 0) {
            glitch--;
            if (glitch == GLITCH_IN && pendingTeleport != null) {
                level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.5, getZ(), 20, 0.3, 1.0, 0.3, 0.02);
                teleportTo(pendingTeleport.x, pendingTeleport.y, pendingTeleport.z);
                playSound(ModSounds.ENTITY_SLENDERMAN_TELEPORT.value(), 1.5f, 1f);
                pendingTeleport = null;
            }
            entityData.set(DATA_GLITCH, glitch);
        }
        if (teleportCooldown > 0) teleportCooldown--;
        LivingEntity target = getTarget();
        if (target != null && glitch == 0 && teleportCooldown == 0
                && (distanceTo(target) > cfg.teleportWhenFartherThan || random.nextInt(300) == 0)) {
            startTeleportNear(target);
        }

        // summon hallucinations every minute for every player around
        if (++summonTimer >= cfg.summonIntervalTicks) {
            summonTimer = 0;
            boolean any = false;
            for (ServerPlayer p : level.getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                    && p.distanceToSqr(this) <= cfg.summonRadius * cfg.summonRadius)) {
                for (int i = 0; i < cfg.hallucinationsPerPlayer; i++) {
                    BlockPos spot = HallucinationSpawner.findSpot(level, p, 3, 8);
                    if (spot == null) continue;
                    SlenderHallucination h = ModEntities.SLENDERMAN_HALLUCINATION.create(level, EntitySpawnReason.MOB_SUMMONED);
                    if (h == null) continue;
                    h.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360f, 0f);
                    h.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.MOB_SUMMONED, null);
                    h.setTarget(p);
                    level.addFreshEntity(h);
                    any = true;
                }
            }
            if (any) playSound(ModSounds.ENTITY_SLENDERMAN_SUMMON.value(), 3f, 1f);
        }

        // Blood Moon -> Deadly; dawn -> gone
        if (tickCount % 20 == 0) {
            setDeadly(forcedDeadly || BloodMoon.isActive(level));
            if (level.dimension() == Level.OVERWORLD && level.isBrightOutside() && forestAnchor != Long.MIN_VALUE) {
                vanish();
                return;
            }
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    public void startTeleportNear(LivingEntity target) {
        if (!(level() instanceof ServerLevel level) || !(target instanceof ServerPlayer p)) return;
        BlockPos spot = HallucinationSpawner.findSpot(level, p, 2.5, 6);
        if (spot == null) return;
        pendingTeleport = new Vec3(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        entityData.set(DATA_GLITCH, GLITCH_OUT + GLITCH_IN);
        teleportCooldown = ConfigManager.get().slenderman.teleportCooldownTicks;
        playSound(ModSounds.ENTITY_SLENDERMAN_TELEPORT.value(), 1.5f, 0.8f);
    }

    public void vanish() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.5, getZ(), 40, 0.4, 1.2, 0.4, 0.02);
            playSound(ModSounds.ENTITY_SLENDERMAN_VANISH.value(), 2f, 1f);
        }
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(ModTags.IS_HOLY)) return false; // completely immune to holy items
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && glitchTicks() == 0 && random.nextInt(4) == 0) {
            startTeleportNear(attacker);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level && forestAnchor != Long.MIN_VALUE) {
            SlenderForests.markDefeated(level, forestAnchor);
        }
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return ConfigManager.get().slenderman.experience;
    }

    @Override
    public void checkDespawn() {
        if (level().getDifficulty() == Difficulty.PEACEFUL) discard();
        setNoActionTime(0); // vanilla does this when a player is near; without it wandering AI stops after ~5 s
    }

    @Override
    public boolean shouldBeSaved() {
        return forestAnchor == Long.MIN_VALUE && super.shouldBeSaved(); // forest Slendermen re-appear at night
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_SLENDERMAN_AMBIENT.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_SLENDERMAN_HURT.value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_SLENDERMAN_DEATH.value();
    }
}
