package com.unholymc.entity.slop;

import com.unholymc.config.ConfigManager;
import com.unholymc.registry.ModSounds;
import com.unholymc.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Unholy slop: faster than aggressive ones, inflicts Darkness, and is immune to everything except holy
 * damage and commands (DECISIONS #12). Not calmed by the dark flashlight core (DECISIONS #3).
 */
public class UnholySlop extends AbstractSlop {
    public UnholySlop(EntityType<? extends UnholySlop> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        var cfg = ConfigManager.get().slops;
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, cfg.unholyHealth)
                .add(Attributes.ATTACK_DAMAGE, cfg.unholyDamage)
                .add(Attributes.MOVEMENT_SPEED, cfg.unholySpeed)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public SlopKind kind() {
        return SlopKind.UNHOLY;
    }

    @Override
    protected double despawnDistance() {
        return ConfigManager.get().slops.unholyDespawnDistance;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RepelGoal(this, 1.3, this::currentFleeSource));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(5, new SlopWander(this, 0.8, com.unholymc.config.ConfigManager.get().slops.wanderIntervalTicks));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!source.is(ModTags.IS_HOLY) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (source.getEntity() instanceof Player) playSound(ModSounds.ENTITY_SLOP_UNHOLY_IMMUNE.value(), 1f, 1f);
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return (!source.is(ModTags.IS_HOLY) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) || super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ConfigManager.get().slops.unholyDarknessTicks, 0), this);
            playSound(ModSounds.ENTITY_SLOP_UNHOLY_ATTACK.value(), 1f, 1f);
        }
        return hit;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return currentFleeSource() == null && super.canAttack(target);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_SLOP_UNHOLY_AMBIENT.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_SLOP_UNHOLY_HURT.value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_SLOP_UNHOLY_DEATH.value();
    }
}
