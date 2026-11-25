package com.unholymc.entity.slop;

import com.unholymc.config.ConfigManager;
import com.unholymc.horror.Jumpscares;
import com.unholymc.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/** Neutral slop: the fastest one; sneaks up, screams at a player (a small jumpscare) and runs away. Never attacks. */
public class NeutralSlop extends AbstractSlop {
    private int scareCooldown;

    public NeutralSlop(EntityType<? extends NeutralSlop> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        var cfg = ConfigManager.get().slops;
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, cfg.neutralHealth)
                .add(Attributes.MOVEMENT_SPEED, cfg.neutralSpeed)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    public SlopKind kind() {
        return SlopKind.NEUTRAL;
    }

    @Override
    protected double despawnDistance() {
        return ConfigManager.get().slops.neutralDespawnDistance;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RepelGoal(this, 1.4, this::currentFleeSource));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(3, new ScareGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (scareCooldown > 0) scareCooldown--;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_SLOP_NEUTRAL_AMBIENT.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_SLOP_NEUTRAL_HURT.value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_SLOP_NEUTRAL_DEATH.value();
    }

    /** Approach a player, scream when close, then flee. */
    private class ScareGoal extends Goal {
        private @Nullable ServerPlayer victim;

        ScareGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (scareCooldown > 0 || currentFleeSource() != null) return false;
            var cfg = ConfigManager.get().slops;
            Player p = level().getNearestPlayer(NeutralSlop.this, cfg.neutralScareNoticeRange);
            if (p instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator()) {
                victim = sp;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return victim != null && victim.isAlive() && scareCooldown <= 0 && currentFleeSource() == null
                    && distanceToSqr(victim) < 24 * 24;
        }

        @Override
        public void tick() {
            if (victim == null) return;
            getLookControl().setLookAt(victim);
            var cfg = ConfigManager.get().slops;
            if (distanceToSqr(victim) <= cfg.neutralScareTriggerRange * cfg.neutralScareTriggerRange) {
                playSound(ModSounds.ENTITY_SLOP_NEUTRAL_SCARE.value(), 2f, 1f);
                Jumpscares.slopScare(victim);
                scareCooldown = cfg.neutralScareCooldownTicks;
                repelFrom(victim.position(), cfg.neutralFleeTicks);
                playSound(ModSounds.ENTITY_SLOP_NEUTRAL_FLEE.value(), 1f, 1f);
                victim = null;
            } else if (getNavigation().isDone() || tickCount % 10 == 0) {
                getNavigation().moveTo(victim, 1.0);
            }
        }

        @Override
        public void stop() {
            victim = null;
            getNavigation().stop();
        }
    }
}
