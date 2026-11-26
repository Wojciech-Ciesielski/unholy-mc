package com.unholymc.entity.slop;

import com.unholymc.config.ConfigManager;
import com.unholymc.item.FlashlightItem;
import com.unholymc.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

/** Aggressive slop: chases and attacks players; neutralised inside a Nightmare Gem flashlight cone (DECISIONS #3). */
public class AggressiveSlop extends AbstractSlop {
    public AggressiveSlop(EntityType<? extends AggressiveSlop> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        var cfg = ConfigManager.get().slops;
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, cfg.aggressiveHealth)
                .add(Attributes.ATTACK_DAMAGE, cfg.aggressiveDamage)
                .add(Attributes.MOVEMENT_SPEED, cfg.aggressiveSpeed)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    public SlopKind kind() {
        return SlopKind.AGGRESSIVE;
    }

    @Override
    protected double despawnDistance() {
        return ConfigManager.get().slops.aggressiveDespawnDistance;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RepelGoal(this, 1.3, this::currentFleeSource));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(5, new SlopWander(this, 0.8, com.unholymc.config.ConfigManager.get().slops.wanderIntervalTicks));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                (target, level) -> !(target instanceof Player p && FlashlightItem.isInDarkCone(p, this))));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && getTarget() instanceof Player p && FlashlightItem.isInDarkCone(p, this)) {
            setTarget(null); // calmed by the dark light
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return currentFleeSource() == null && super.canAttack(target);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_SLOP_AGGRESSIVE_AMBIENT.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_SLOP_AGGRESSIVE_HURT.value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_SLOP_AGGRESSIVE_DEATH.value();
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target != null && getTarget() == null && !level().isClientSide()) {
            playSound(ModSounds.ENTITY_SLOP_AGGRESSIVE_CHASE.value(), 1f, 1f);
        }
        super.setTarget(target);
    }

    public static boolean isNight(ServerLevel level) {
        return level.isDarkOutside();
    }
}
