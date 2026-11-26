package com.unholymc.entity.slop;

import com.unholymc.entity.BillboardEntity;
import com.unholymc.entity.HolyRepellable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Common base of the three PNG slop categories (GDD chapter 4). */
public abstract class AbstractSlop extends Monster implements HolyRepellable, BillboardEntity {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(AbstractSlop.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CHASING = SynchedEntityData.defineId(AbstractSlop.class, EntityDataSerializers.BOOLEAN);

    protected @Nullable Vec3 fleeFrom;
    protected int fleeUntil;

    protected AbstractSlop(EntityType<? extends AbstractSlop> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public abstract SlopKind kind();

    protected abstract double despawnDistance();

    /** Aggressive and Unholy slops vanish in daylight (GDD 4). */
    protected boolean despawnsByDay() {
        return kind() != SlopKind.NEUTRAL;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, SlopVariant.ofKind(kind()).getFirst().ordinal());
        builder.define(DATA_CHASING, false);
    }

    @Override
    public SlopVariant variant() {
        return SlopVariant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(SlopVariant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
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
        return EntityDimensions.scalable(v.scaledWidth(), v.scaledHeight());
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        setVariant(SlopVariant.random(kind(), level.getRandom()));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", variant().ordinal());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(SlopVariant.byId(input.getIntOr("Variant", variant().ordinal())));
    }

    // --- fleeing (holy items) ---

    @Override
    public void repelFrom(Vec3 source, int ticks) {
        this.fleeFrom = source;
        this.fleeUntil = this.tickCount + ticks;
        this.setTarget(null);
    }

    protected @Nullable Vec3 currentFleeSource() {
        if (fleeFrom != null && this.tickCount > fleeUntil) fleeFrom = null;
        return fleeFrom;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_CHASING, this.getTarget() != null);
        }
    }

    // --- despawning (GDD 4: day / distance, DECISIONS #31: none in peaceful) ---

    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }
        if (this.isPersistenceRequired()) return;
        if (despawnsByDay() && this.level() instanceof ServerLevel sl && sl.dimension() == Level.OVERWORLD && !com.unholymc.world.Night.isNight(sl)) {
            this.discard();
            return;
        }
        Player nearest = this.level().getNearestPlayer(this, -1.0);
        double max = despawnDistance();
        if (nearest == null || nearest.distanceToSqr(this) > max * max) {
            this.discard();
        } else if (nearest.distanceToSqr(this) < 32 * 32) {
            // vanilla Mob.checkDespawn does this too: a mob with a player nearby is "active". Without it the counter
            // grows past 100 and RandomStrollGoal refuses to start, so idle slops stood still for ever (B5).
            this.setNoActionTime(0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false; // handled in checkDespawn
    }
}
