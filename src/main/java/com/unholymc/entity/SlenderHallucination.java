package com.unholymc.entity;

import com.unholymc.config.ConfigManager;
import com.unholymc.entity.slop.RepelGoal;
import com.unholymc.entity.slop.SlopKind;
import com.unholymc.entity.slop.SlopVariant;
import com.unholymc.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Hallucination summoned by Slenderman (GDD 3.2, DECISIONS #23): a real entity visible to everybody that
 * attacks any player regardless of sanity; vulnerable to holy items. Lives for a limited time.
 */
public class SlenderHallucination extends Monster implements BillboardEntity, HolyRepellable {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(SlenderHallucination.class, EntityDataSerializers.INT);
    private int age;
    private @Nullable Vec3 fleeFrom;
    private int fleeUntil;

    public SlenderHallucination(EntityType<? extends SlenderHallucination> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        var cfg = ConfigManager.get().hallucinations;
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, cfg.slenderHealth)
                .add(Attributes.ATTACK_DAMAGE, cfg.slenderDamage)
                .add(Attributes.MOVEMENT_SPEED, cfg.slenderSpeed)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, SlopVariant.ofKind(SlopKind.AGGRESSIVE).getFirst().ordinal());
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        this.entityData.set(DATA_VARIANT, SlopVariant.random(SlopKind.AGGRESSIVE, level.getRandom()).ordinal());
        return super.finalizeSpawn(level, difficulty, reason, data);
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
    public boolean isSlenderMinion() {
        return true;
    }

    @Override
    public boolean isChasing() {
        return true;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_VARIANT.equals(accessor)) refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        SlopVariant v = variant();
        return EntityDimensions.scalable(v.scaledWidth(), v.scaledHeight());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RepelGoal(this, 1.3, this::fleeSource));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    private @Nullable Vec3 fleeSource() {
        if (fleeFrom != null && tickCount > fleeUntil) fleeFrom = null;
        return fleeFrom;
    }

    @Override
    public void repelFrom(Vec3 source, int ticks) {
        fleeFrom = source;
        fleeUntil = tickCount + ticks;
        setTarget(null);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && ++age > ConfigManager.get().hallucinations.slenderLifetimeTicks) {
            playSound(ModSounds.ENTITY_HALLUCINATION_DISSOLVE.value(), 1f, 1f);
            discard();
        }
    }

    @Override
    public void checkDespawn() {
        if (level().getDifficulty() == Difficulty.PEACEFUL) discard();
        setNoActionTime(0); // vanilla does this when a player is near; without it wandering AI stops after ~5 s
    }

    @Override
    public boolean shouldBeSaved() {
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

    /** GDD 5.2: killing a hallucination restores +2 sanity - Slenderman's ones included. */
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer p && com.unholymc.survival.StatsHelper.isActive(p)) {
            com.unholymc.survival.StatsHelper.addSanity(p, ConfigManager.get().hallucinations.killSanity);
        }
    }
}
