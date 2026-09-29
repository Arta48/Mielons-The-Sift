package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class EchoGolemEntity extends AbstractGolem implements GeoEntity {
   private static final Map SOUL_RESERVATIONS = new HashMap();
   private static final Map SOUL_FAILURE_MEMORY = new HashMap();
   private static final int MAX_NEW_PATHS_PER_SERVER_TICK = 1;
   private static final int MAX_CRITICAL_EXIT_PATHS_PER_SERVER_TICK = 1;
   private static long pathBudgetTick = Long.MIN_VALUE;
   private static int pathsUsedThisTick;
   private static int criticalExitPathsUsedThisTick;
   private static final EntityDataAccessor DATA_MISSION_STATE;
   private static final EntityDataAccessor DATA_HAS_SOUL;
   private static final EntityDataAccessor DATA_BONDED;
   private static final RawAnimation IDLE;
   private static final RawAnimation WALK;
   private static final RawAnimation PICKUP;
   private static final RawAnimation CARRY_IDLE;
   private static final RawAnimation CARRY_WALK;
   private static final RawAnimation BOW;
   private static final RawAnimation SIT;
   private static final RawAnimation SOUL_VISIBLE;
   private static final RawAnimation SOUL_HIDDEN;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private BlockPos soulTarget;
   private BlockPos soulApproach;
   private BlockPos canyonEntrance;
   private BlockPos canyonMidpoint;
   private BlockPos canyonCluster;
   private BlockPos ignoredCanyon;
   private UUID singerTarget;
   private UUID bondedPlayer;
   private String bondedPlayerName;
   private BlockPos deliveryAnchor;
   private BlockPos patrolTarget;
   private int patrolCooldown;
   private RoutePhase routePhase;
   private int stateTicks;
   private int routeTicks;
   private int routeFailures;
   private int targetCooldown;
   private int nearbySoulScanCooldown;
   private long lastSoulScanTick;
   private long nextPhysicalSoulScanTick;
   private long nextAllowedPathfindTick;
   private int repathCooldown;
   private int emptyCanyonChecks;
   private int canyonEntryAttempts;
   private int canyonDepthAttempts;
   private int ignoredCanyonTicks;
   private int landmarkCooldown;
   private int explorationTurnTicks;
   private int explorationTargetTicks;
   private double explorationHeading;
   private BlockPos explorationTarget;
   private boolean enteredCanyon;
   private boolean exitMidpointPassed;
   private BlockPos navigationGoal;
   private Path preparedExitPath;
   private BlockPos preparedExitPathTarget;
   private int stalledSamples;
   private double progressSampleX;
   private double progressSampleY;
   private double progressSampleZ;
   private boolean progressSampleReady;
   private BlockPos distantCanyonHint;
   private double carrySampleX;
   private double carrySampleY;
   private double carrySampleZ;
   private int carryWatchdogTicks;
   private int carryStallSamples;
   private int carryRecoveryStage;
   private int livenessTicks;
   private int livenessStallSamples;
   private double livenessSampleX;
   private double livenessSampleY;
   private double livenessSampleZ;
   private final Map unavailableCanyons;
   private final Map unreachableSoulBlocks;

   public EchoGolemEntity(EntityType type, Level level) {
      super(type, level);
      this.routePhase = EchoGolemEntity.RoutePhase.EXPLORE;
      this.lastSoulScanTick = Long.MIN_VALUE;
      this.nextPhysicalSoulScanTick = Long.MIN_VALUE;
      this.nextAllowedPathfindTick = Long.MIN_VALUE;
      this.unavailableCanyons = new HashMap();
      this.unreachableSoulBlocks = new HashMap();
      this.explorationHeading = this.random.nextDouble() * Math.PI * (double)2.0F;
      this.explorationTurnTicks = 900 + this.random.nextInt(901);
      this.setPathfindingMalus(PathType.LEAVES, 0.0F);
      this.setPathfindingMalus(PathType.COCOA, 0.0F);
      this.setPathfindingMalus(PathType.DAMAGE_CAUTIOUS, 0.0F);
      this.getNavigation().setCanFloat(true);
      this.getNavigation().setMaxVisitedNodesMultiplier(2.0F);
      this.nearbySoulScanCooldown = 1 + this.random.nextInt(20);
      this.landmarkCooldown = 1 + this.random.nextInt(45);
      this.nextPhysicalSoulScanTick = level.getGameTime() + (long)this.random.nextInt(80);
      this.nextAllowedPathfindTick = level.getGameTime() + (long)this.random.nextInt(4);
      this.livenessSampleX = this.getX();
      this.livenessSampleY = this.getY();
      this.livenessSampleZ = this.getZ();
      PathNavigation var4 = this.getNavigation();
      if (var4 instanceof GroundPathNavigation groundNavigation) {
         groundNavigation.setCanPathToTargetsBelowSurface(true);
      }

   }

   public static AttributeSupplier.Builder createAttributes() {
      return AbstractGolem.createMobAttributes().add(Attributes.MAX_HEALTH, (double)34.0F).add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.FOLLOW_RANGE, (double)128.0F).add(Attributes.STEP_HEIGHT, (double)1.5F).add(Attributes.MOVEMENT_EFFICIENCY, (double)1.0F).add(Attributes.KNOCKBACK_RESISTANCE, (double)0.25F);
   }

   public static void clearTransientState() {
      SOUL_RESERVATIONS.clear();
      SOUL_FAILURE_MEMORY.clear();
      pathBudgetTick = Long.MIN_VALUE;
      pathsUsedThisTick = 0;
      criticalExitPathsUsedThisTick = 0;
   }

   public static boolean checkEchoGolemSpawnRules(EntityType type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
      if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
         return false;
      } else {
         AABB exclusion = (new AABB(pos)).inflate((double)160.0F, (double)72.0F, (double)160.0F);
         return level.getLevel().getEntitiesOfClass(EchoGolemEntity.class, exclusion).isEmpty();
      }
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_MISSION_STATE, EchoGolemEntity.MissionState.SEARCH.ordinal());
      builder.define(DATA_HAS_SOUL, false);
      builder.define(DATA_BONDED, false);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
   }

   public boolean hasSoulBlock() {
      return (Boolean)this.entityData.get(DATA_HAS_SOUL);
   }

   public boolean canInteractWithSinger() {
      return this.bondedPlayer == null && this.bondedPlayerName == null && !(Boolean)this.entityData.get(DATA_BONDED);
   }

   public MissionState missionState() {
      int index = Mth.clamp((Integer)this.entityData.get(DATA_MISSION_STATE), 0, EchoGolemEntity.MissionState.values().length - 1);
      return EchoGolemEntity.MissionState.values()[index];
   }

   public boolean isWalkingForAnimation() {
      return this.getDeltaMovement().horizontalDistanceSqr() > 0.0012;
   }

   public void beginBow() {
      if (this.hasSoulBlock()) {
         this.getNavigation().stop();
         this.setMissionState(EchoGolemEntity.MissionState.BOW);
      }

   }

   public void faceEntity(Entity other) {
      double dx = other.getX() - this.getX();
      double dz = other.getZ() - this.getZ();
      float yaw = (float)(Mth.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
      this.setYRot(yaw);
      this.setYHeadRot(yaw);
      this.setYBodyRot(yaw);
   }

   public boolean transferSoulToSinger() {
      if (this.canInteractWithSinger() && this.hasSoulBlock()) {
         Level var2 = this.level();
         if (var2 instanceof ServerLevel) {
            ServerLevel level = (ServerLevel)var2;
            ModAdvancements.awardNearby(level, this.position(), (double)16.0F, "the_sift/like_father_and_son");
         }

         this.entityData.set(DATA_HAS_SOUL, false);
         this.level().playSound((Entity)null, this.blockPosition(), ModSounds.ECHO_GOLEM_ITEM_DROP, this.getSoundSource(), 1.0F, 0.92F);
         this.singerTarget = null;
         this.deliveryAnchor = null;
         this.patrolTarget = null;
         this.forgetCurrentCanyon(600);
         this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
         if (this.missionState() != EchoGolemEntity.MissionState.BOW) {
            this.setMissionState(EchoGolemEntity.MissionState.SEARCH);
         }

         return true;
      } else {
         return false;
      }
   }

   public void finishBow() {
      if (this.missionState() == EchoGolemEntity.MissionState.BOW) {
         if (!this.hasSoulBlock()) {
            this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
         }

         this.setMissionState(EchoGolemEntity.MissionState.SEARCH);
      }

   }

   public void sitAfterLosingSoul() {
      this.getNavigation().stop();
      this.singerTarget = null;
      this.deliveryAnchor = null;
      this.patrolTarget = null;
      this.forgetCurrentCanyon(600);
      this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
      this.setMissionState(EchoGolemEntity.MissionState.SIT);
   }

   private void setMissionState(MissionState state) {
      this.entityData.set(DATA_MISSION_STATE, state.ordinal());
      this.stateTicks = 0;
      this.repathCooldown = 0;
      this.resetRouteProgressSample();
      this.resetCarryWatchdog();
   }

   private void setRoutePhase(RoutePhase phase) {
      this.routePhase = phase;
      this.routeTicks = 0;
      this.routeFailures = 0;
      this.repathCooldown = 0;
      this.navigationGoal = null;
      this.clearPreparedExitPath();
      this.explorationTarget = null;
      this.explorationTargetTicks = 0;
      this.resetRouteProgressSample();
      this.getNavigation().stop();
   }

   public void tick() {
      super.tick();
      Level var2 = this.level();
      if (var2 instanceof ServerLevel serverLevel) {
         if (this.isAlive()) {
            if (this.ignoredCanyonTicks > 0 && --this.ignoredCanyonTicks == 0) {
               this.ignoredCanyon = null;
            }

            ++this.stateTicks;
            ++this.routeTicks;
            switch (this.missionState().ordinal()) {
               case 0 -> this.tickSearch(serverLevel);
               case 1 -> this.tickPickup(serverLevel);
               case 2 -> this.tickCarry(serverLevel);
               case 3 -> this.tickBow(serverLevel);
               case 4 -> this.tickSit();
            }

            if (this.missionState() == EchoGolemEntity.MissionState.CARRY && this.hasSoulBlock()) {
               this.tickCarryWatchdog(serverLevel);
            }

            if (this.missionState() == EchoGolemEntity.MissionState.SEARCH) {
               this.tickLivenessWatchdog(serverLevel);
            }

            this.assistLocalStep();
            return;
         }
      }

   }

   private void tickSearch(ServerLevel level) {
      if (this.hasSoulBlock()) {
         this.setRoutePhase(this.hasReachedSurface() ? EchoGolemEntity.RoutePhase.DELIVER : EchoGolemEntity.RoutePhase.EXIT_CANYON);
         this.setMissionState(EchoGolemEntity.MissionState.CARRY);
      } else {
         if (this.soulTarget != null && !level.getBlockState(this.soulTarget).is(ModBlocks.SOUL_BLOCK)) {
            this.forgetSoulFailure(this.soulTarget);
            this.clearSoulTargetReservation();
            this.targetCooldown = 0;
            this.nearbySoulScanCooldown = 2 + this.random.nextInt(5);
            this.repathCooldown = 0;
            this.navigationGoal = null;
            this.getNavigation().stop();
         }

         if (--this.nearbySoulScanCooldown <= 0) {
            this.nearbySoulScanCooldown = 12;
            this.lastSoulScanTick = level.getGameTime();
            SoulCandidate nearby = this.findReachableSoulBlock(level, this.blockPosition(), this.soulTarget == null ? 36 : 16, this.soulTarget == null ? 26 : 12);
            double currentDistance = this.soulTarget == null ? Double.POSITIVE_INFINITY : this.soulTarget.distSqr(this.blockPosition());
            double nearbyDistance = nearby == null ? Double.POSITIVE_INFINITY : nearby.soul().distSqr(this.blockPosition());
            if (nearby != null && (this.soulTarget == null || !nearby.soul().equals(this.soulTarget) && nearbyDistance + (double)1.0F < currentDistance)) {
               this.claimSoulTarget(nearby);
               this.canyonDepthAttempts = 0;
               this.emptyCanyonChecks = 0;
               this.setRoutePhase(EchoGolemEntity.RoutePhase.SEEK_SOUL);
               return;
            }
         }

         if (this.soulTarget != null && this.routePhase != EchoGolemEntity.RoutePhase.SEEK_SOUL) {
            this.setRoutePhase(EchoGolemEntity.RoutePhase.SEEK_SOUL);
         }

         switch (this.routePhase.ordinal()) {
            case 0:
               this.tickExploreRoute(level);
               break;
            case 1:
               this.tickCanyonApproach(level);
               break;
            case 2:
               this.tickCanyonEntry(level);
               break;
            case 3:
               this.tickSoulSearch(level);
               break;
            case 4:
               this.tickLeaveEmptyCanyon(level);
               break;
            case 5:
            case 6:
            default:
               this.setRoutePhase(this.enteredCanyon && this.canyonCluster != null ? EchoGolemEntity.RoutePhase.SEEK_SOUL : EchoGolemEntity.RoutePhase.EXPLORE);
               break;
            case 7:
               this.tickCanyonDescent(level);
         }

      }
   }

   private void tickExploreRoute(ServerLevel level) {
      this.purgeExpiredRejections(level.getGameTime());
      if (--this.landmarkCooldown <= 0) {
         this.landmarkCooldown = 45;
         if (this.acquireCanyonRoute(level)) {
            this.setRoutePhase(EchoGolemEntity.RoutePhase.APPROACH_CANYON);
            return;
         }
      }

      this.tickExploration(level);
      this.monitorRouteProgress(true);
   }

   private void tickCanyonApproach(ServerLevel level) {
      if (!this.hasValidCanyonRoute()) {
         this.clearCanyonRoute();
         this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
      } else {
         BlockPos approach = this.canyonApproach();
         if (approach == null) {
            this.abandonBlockedCanyon(level, 2400L);
         } else if (this.isNear(approach, (double)3.5F, (double)5.0F)) {
            this.setRoutePhase(EchoGolemEntity.RoutePhase.ENTER_CANYON);
         } else {
            this.navigateToward(approach, 0.88);
            this.monitorRouteProgress(false);
            if (this.routeFailures >= 8) {
               this.abandonBlockedCanyon(level, 2400L);
            }

         }
      }
   }

   private void tickCanyonEntry(ServerLevel level) {
      if (!this.hasValidCanyonRoute()) {
         this.clearCanyonRoute();
         this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
      } else if (this.isNear(this.canyonEntrance, (double)3.5F, (double)4.0F)) {
         this.enteredCanyon = true;
         this.canyonEntryAttempts = 0;
         this.setRoutePhase(this.canyonMidpoint == null ? EchoGolemEntity.RoutePhase.SEEK_SOUL : EchoGolemEntity.RoutePhase.DESCEND_CANYON);
      } else {
         this.navigateToward(this.canyonEntrance, 0.84);
         this.monitorRouteProgress(false);
         if (this.routeFailures >= 12) {
            if (++this.canyonEntryAttempts >= 2) {
               this.abandonBlockedCanyon(level, 2400L);
            } else {
               this.setRoutePhase(EchoGolemEntity.RoutePhase.APPROACH_CANYON);
            }
         }

      }
   }

   private void tickCanyonDescent(ServerLevel level) {
      if (!this.hasValidCanyonRoute()) {
         this.clearCanyonRoute();
         this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
      } else if (this.canyonMidpoint != null && !this.isNear(this.canyonMidpoint, (double)3.0F, (double)4.0F)) {
         this.navigateToward(this.canyonMidpoint, 0.82);
         this.monitorRouteProgress(false);
         if (this.routeFailures >= 12) {
            if (++this.canyonEntryAttempts >= 2) {
               this.markCurrentCanyonUnavailable(level, 2400L);
               this.setRoutePhase(EchoGolemEntity.RoutePhase.LEAVE_EMPTY_CANYON);
            } else {
               this.enteredCanyon = false;
               this.setRoutePhase(EchoGolemEntity.RoutePhase.APPROACH_CANYON);
            }
         }

      } else {
         this.setRoutePhase(EchoGolemEntity.RoutePhase.SEEK_SOUL);
      }
   }

   private void tickSoulSearch(ServerLevel level) {
      if (this.soulTarget != null) {
         if (this.soulApproach == null) {
            if (!this.tryAcquirePathBudget(level, 4)) {
               return;
            }

            this.soulApproach = this.findReachableSoulApproach(level, this.soulTarget);
            if (this.soulApproach == null) {
               if (++this.routeFailures >= 3) {
                  this.rejectCurrentSoulTarget(level, 100L);
                  this.nearbySoulScanCooldown = 2 + this.random.nextInt(5);
                  this.routeFailures = 0;
               }

               return;
            }

            this.routeFailures = 0;
         }

         if (this.canReachSoulWithHands(level, this.soulTarget)) {
            this.getNavigation().stop();
            this.setMissionState(EchoGolemEntity.MissionState.PICKUP);
         } else {
            this.navigateToward(this.soulApproach, 0.9);
            this.monitorRouteProgress(false);
            if (this.routeFailures >= 8) {
               this.rejectCurrentSoulTarget(level, 200L);
               this.targetCooldown = 0;
               this.nearbySoulScanCooldown = 2 + this.random.nextInt(5);
               this.routeFailures = 0;
            }

         }
      } else if (this.lastSoulScanTick == level.getGameTime() && this.canyonCluster != null && this.isNear(this.canyonCluster.above(), (double)11.0F, (double)10.0F) && ++this.emptyCanyonChecks >= 3) {
         this.markCurrentCanyonUnavailable(level, 6000L);
         this.setRoutePhase(EchoGolemEntity.RoutePhase.LEAVE_EMPTY_CANYON);
      } else if (this.canyonCluster == null) {
         this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
      } else if (!this.enteredCanyon) {
         this.setRoutePhase(EchoGolemEntity.RoutePhase.APPROACH_CANYON);
      } else if (!this.isNear(this.canyonCluster.above(), (double)11.0F, (double)10.0F)) {
         this.navigateToward(this.canyonCluster.above(), 0.84);
         this.monitorRouteProgress(false);
         if (this.routeFailures >= 12) {
            if (++this.canyonDepthAttempts >= 3) {
               this.markCurrentCanyonUnavailable(level, 6000L);
               this.setRoutePhase(EchoGolemEntity.RoutePhase.LEAVE_EMPTY_CANYON);
            } else {
               this.enteredCanyon = false;
               this.setRoutePhase(EchoGolemEntity.RoutePhase.APPROACH_CANYON);
            }
         }

      }
   }

   private void tickLeaveEmptyCanyon(ServerLevel level) {
      if (this.hasReachedSurface()) {
         this.clearCanyonRoute();
         this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
      } else {
         BlockPos exit = this.ensureReachableExit(level);
         if (exit != null) {
            this.navigateExitToward(exit, 0.86);
            this.monitorRouteProgress(false);
         } else {
            this.tickEmergencyAscent(level);
         }

      }
   }

   private void tickExploration(ServerLevel level) {
      if (--this.explorationTurnTicks <= 0) {
         this.explorationHeading += (this.random.nextDouble() - (double)0.5F) * 0.38;
         this.explorationTurnTicks = 900 + this.random.nextInt(901);
         this.explorationTarget = null;
      }

      ++this.explorationTargetTicks;
      boolean reached = this.explorationTarget != null && this.isNear(this.explorationTarget, (double)4.0F, (double)7.0F);
      boolean failed = this.routeFailures >= 6 || this.explorationTargetTicks >= 1200;
      if (this.explorationTarget == null || reached || failed) {
         if (failed) {
            this.explorationHeading += (this.random.nextBoolean() ? (double)1.0F : (double)-1.0F) * (0.58 + this.random.nextDouble() * 0.38);
         }

         this.explorationTarget = this.chooseExplorationTarget(level);
         this.explorationTargetTicks = 0;
         this.routeFailures = 0;
         this.repathCooldown = 0;
         this.navigationGoal = null;
         this.getNavigation().stop();
      }

      if (this.explorationTarget != null) {
         this.navigateToward(this.explorationTarget, 0.78);
      }

   }

   private BlockPos chooseExplorationTarget(ServerLevel level) {
      double[] offsets = new double[]{(double)0.0F, 0.18, -0.18, 0.38, -0.38, 0.62, -0.62};
      int[] distances = new int[]{42, 34, 26};

      for (int distance : distances) {
         for (double offset : offsets) {
            double angle = this.explorationHeading + offset;
            int x = Mth.floor(this.getX() + Math.cos(angle) * (double)distance);
            int z = Mth.floor(this.getZ() + Math.sin(angle) * (double)distance);
            if (level.getChunkSource().getChunkNow(Math.floorDiv(x, 16), Math.floorDiv(z, 16)) != null) {
               int y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
               BlockPos candidate = new BlockPos(x, y, z);
               return candidate;
            }
         }
      }

      Vec3 desired = new Vec3(this.getX() + Math.cos(this.explorationHeading) * (double)28.0F, this.getY(), this.getZ() + Math.sin(this.explorationHeading) * (double)28.0F);
      Vec3 fallback = DefaultRandomPos.getPosTowards(this, 24, 12, desired, (Math.PI / 3D));
      return fallback == null ? null : BlockPos.containing(fallback);
   }

   private void tickPickup(ServerLevel level) {
      this.getNavigation().stop();
      if (this.stateTicks == 18) {
         if (this.soulTarget == null || !level.getBlockState(this.soulTarget).is(ModBlocks.SOUL_BLOCK)) {
            this.forgetSoulFailure(this.soulTarget);
            this.clearSoulTargetReservation();
            this.targetCooldown = 0;
            this.setRoutePhase(this.enteredCanyon && this.canyonCluster != null ? EchoGolemEntity.RoutePhase.SEEK_SOUL : EchoGolemEntity.RoutePhase.EXPLORE);
            this.setMissionState(EchoGolemEntity.MissionState.SEARCH);
            return;
         }

         this.forgetSoulFailure(this.soulTarget);
         level.setBlock(this.soulTarget, Blocks.AIR.defaultBlockState(), 3);
         this.clearSoulTargetReservation();
         this.entityData.set(DATA_HAS_SOUL, true);
         this.deliveryAnchor = this.canyonEntrance != null ? this.canyonEntrance.immutable() : this.blockPosition();
         this.setPersistenceRequired();
         this.level().playSound((Entity)null, this.blockPosition(), ModSounds.ECHO_GOLEM_ITEM_GET, this.getSoundSource(), 1.0F, 0.96F);
      }

      if (this.stateTicks >= 34) {
         this.clearSoulTargetReservation();
         this.exitMidpointPassed = !this.needsCanyonMidpointForExit();
         this.setRoutePhase(this.hasReachedSurface() ? EchoGolemEntity.RoutePhase.DELIVER : EchoGolemEntity.RoutePhase.EXIT_CANYON);
         this.setMissionState(EchoGolemEntity.MissionState.CARRY);
      }

   }

   private void tickCarry(ServerLevel level) {
      if (!this.hasSoulBlock()) {
         this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
         this.setMissionState(EchoGolemEntity.MissionState.SEARCH);
      } else {
         if (this.routePhase != EchoGolemEntity.RoutePhase.EXIT_CANYON && this.routePhase != EchoGolemEntity.RoutePhase.DELIVER) {
            this.setRoutePhase(this.hasReachedSurface() ? EchoGolemEntity.RoutePhase.DELIVER : EchoGolemEntity.RoutePhase.EXIT_CANYON);
         }

         if (this.routePhase == EchoGolemEntity.RoutePhase.EXIT_CANYON) {
            if (this.hasReachedSurface()) {
               this.enteredCanyon = false;
               this.setRoutePhase(EchoGolemEntity.RoutePhase.DELIVER);
            } else {
               if (!this.exitMidpointPassed && this.canyonMidpoint != null) {
                  if (this.isNear(this.canyonMidpoint, (double)3.0F, (double)4.0F)) {
                     this.exitMidpointPassed = true;
                     this.repathCooldown = 0;
                     this.navigationGoal = null;
                     this.getNavigation().stop();
                  } else {
                     this.navigateExitToward(this.canyonMidpoint, 0.84);
                     this.monitorRouteProgress(false);
                     if (this.routeFailures < 12) {
                        return;
                     }

                     this.exitMidpointPassed = true;
                     this.routeFailures = 0;
                  }
               }

               BlockPos exit = this.ensureReachableExit(level);
               if (exit != null) {
                  this.navigateExitToward(exit, 0.88);
                  this.monitorRouteProgress(false);
               } else {
                  this.tickEmergencyAscent(level);
               }

            }
         } else if (!this.canInteractWithSinger()) {
            this.tickBondedDelivery(level);
         } else {
            SingerEntity singer = this.resolveSinger(level);
            if (singer == null && --this.targetCooldown <= 0) {
               this.targetCooldown = 30;
               singer = SoulExchangeManager.requestSinger(level, this);
               if (singer != null) {
                  this.singerTarget = singer.getUUID();
                  this.navigationGoal = null;
                  this.repathCooldown = 0;
               }
            }

            if (singer != null) {
               singer.offerSoulGolem(this);
               this.navigateToward(singer.blockPosition(), 0.86);
               this.monitorRouteProgress(false);
            } else {
               this.tickExploration(level);
               this.monitorRouteProgress(true);
            }

         }
      }
   }

   private void tickBondedDelivery(ServerLevel level) {
      Player owner = this.resolveBondedPlayer(level);
      if (owner != null && owner.level() == level && this.distanceToSqr(owner) <= (double)4096.0F) {
         this.patrolTarget = null;
         this.patrolCooldown = 0;
         this.faceEntity(owner);
         if (this.distanceToSqr(owner) <= (double)9.0F) {
            this.setMissionState(EchoGolemEntity.MissionState.BOW);
         } else {
            this.navigateToward(owner.blockPosition(), 0.82);
            this.monitorRouteProgress(false);
         }
      } else {
         this.tickPatrolAroundCanyon(level);
         this.monitorRouteProgress(true);
      }
   }

   private void tickPatrolAroundCanyon(ServerLevel level) {
      BlockPos anchor = this.deliveryAnchor != null ? this.deliveryAnchor : (this.canyonEntrance != null ? this.canyonEntrance : this.blockPosition());
      if (this.horizontalDistanceSqr(anchor) > (double)4096.0F) {
         this.patrolTarget = anchor;
         this.navigateToward(anchor, 0.76);
      } else if (--this.patrolCooldown <= 0 || this.patrolTarget == null || this.getNavigation().isDone()) {
         this.patrolCooldown = 50 + this.random.nextInt(51);
         double angle = this.random.nextDouble() * Math.PI * (double)2.0F;
         int radius = 14 + this.random.nextInt(47);
         int x = anchor.getX() + (int)Math.round(Math.cos(angle) * (double)radius);
         int z = anchor.getZ() + (int)Math.round(Math.sin(angle) * (double)radius);
         BlockPos probe = new BlockPos(x, anchor.getY(), z);
         if (!level.isLoaded(probe)) {
            this.patrolTarget = anchor;
         } else {
            int y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            this.patrolTarget = new BlockPos(x, y, z);
         }

         this.navigateToward(this.patrolTarget, 0.74);
      }
   }

   private void tickBow(ServerLevel level) {
      this.getNavigation().stop();
      if (this.canInteractWithSinger()) {
         if (this.stateTicks >= 100) {
            if (this.hasSoulBlock()) {
               this.setRoutePhase(this.hasReachedSurface() ? EchoGolemEntity.RoutePhase.DELIVER : EchoGolemEntity.RoutePhase.EXIT_CANYON);
               this.setMissionState(EchoGolemEntity.MissionState.CARRY);
            } else {
               this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
               this.setMissionState(EchoGolemEntity.MissionState.SEARCH);
            }
         }

      } else {
         Player owner = this.resolveBondedPlayer(level);
         if (this.hasSoulBlock()) {
            if (owner == null || owner.level() != level || this.distanceToSqr(owner) > (double)20.25F) {
               this.setRoutePhase(this.hasReachedSurface() ? EchoGolemEntity.RoutePhase.DELIVER : EchoGolemEntity.RoutePhase.EXIT_CANYON);
               this.setMissionState(EchoGolemEntity.MissionState.CARRY);
               return;
            }

            this.faceEntity(owner);
            if (this.stateTicks == 26) {
               this.completePlayerSoulDelivery(level, owner);
            }
         } else if (owner != null && owner.level() == level) {
            this.faceEntity(owner);
         }

         if (this.stateTicks >= 48) {
            this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
            this.setMissionState(EchoGolemEntity.MissionState.SEARCH);
         }

      }
   }

   private void completePlayerSoulDelivery(ServerLevel level, Player player) {
      if (this.hasSoulBlock()) {
         this.entityData.set(DATA_HAS_SOUL, false);
         ItemStack soul = new ItemStack(ModBlocks.SOUL_BLOCK_ITEM);
         if (!player.addItem(soul)) {
            player.drop(soul, false, Prediction.SERVER_ONLY);
         }

         if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            ModAdvancements.award(serverPlayer, "the_sift/friend_of_the_sift");
         }

         level.playSound((Entity)null, this.blockPosition(), ModSounds.ECHO_GOLEM_ITEM_DROP, this.getSoundSource(), 1.0F, 1.04F);
         this.forgetCurrentCanyon(600);
         this.deliveryAnchor = null;
         this.patrolTarget = null;
      }
   }

   private boolean hasReachedSurface() {
      if (this.canyonEntrance == null) {
         return this.level().canSeeSky(this.blockPosition().above());
      } else {
         return this.getY() >= (double)this.canyonEntrance.getY() - (double)2.0F && (this.level().canSeeSky(this.blockPosition().above()) || this.horizontalDistanceSqr(this.canyonEntrance) <= (double)144.0F);
      }
   }

   private void tickCarryWatchdog(ServerLevel level) {
      if (++this.carryWatchdogTicks >= 20) {
         this.carryWatchdogTicks = 0;
         double dx = this.getX() - this.carrySampleX;
         double dy = this.getY() - this.carrySampleY;
         double dz = this.getZ() - this.carrySampleZ;
         double movedSqr = dx * dx + dy * dy + dz * dz;
         this.carrySampleX = this.getX();
         this.carrySampleY = this.getY();
         this.carrySampleZ = this.getZ();
         SingerEntity singer = this.resolveSinger(level);
         Player owner = this.resolveBondedPlayer(level);
         boolean waitingForHandshake = this.routePhase == EchoGolemEntity.RoutePhase.DELIVER && (singer != null && this.distanceToSqr(singer) <= (double)9.0F || owner != null && this.distanceToSqr(owner) <= (double)9.0F);
         if (!(movedSqr >= 0.16) && !waitingForHandshake) {
            int toleratedSamples = this.routePhase == EchoGolemEntity.RoutePhase.EXIT_CANYON ? 2 : 3;
            if (++this.carryStallSamples >= toleratedSamples) {
               this.carryStallSamples = 0;
               this.recoverCarryRoute(level);
            }
         } else {
            this.carryStallSamples = 0;
            if (movedSqr >= (double)1.0F) {
               this.carryRecoveryStage = Math.max(0, this.carryRecoveryStage - 1);
            }

         }
      }
   }

   private void recoverCarryRoute(ServerLevel level) {
      ++this.carryRecoveryStage;
      this.getNavigation().stop();
      this.navigationGoal = null;
      this.repathCooldown = 0;
      this.routeFailures = Math.max(this.routeFailures, 7);
      this.nextAllowedPathfindTick = level.getGameTime();
      this.clearPreparedExitPath();
      if (this.routePhase == EchoGolemEntity.RoutePhase.EXIT_CANYON) {
         if (!this.exitMidpointPassed) {
            this.exitMidpointPassed = true;
         }

         BlockPos emergency = this.findReachableSurfaceExit(level);
         if (emergency != null) {
            this.canyonEntrance = emergency;
            this.routeFailures = 0;
            this.navigateExitToward(emergency, 0.86);
         } else {
            this.explorationHeading += (this.random.nextBoolean() ? (double)1.0F : (double)-1.0F) * (0.48 + Math.min(0.72, (double)this.carryRecoveryStage * 0.12));
            this.tickEmergencyAscent(level);
         }
      } else {
         if (this.routePhase == EchoGolemEntity.RoutePhase.DELIVER) {
            this.singerTarget = null;
            this.targetCooldown = 0;
            this.patrolTarget = null;
            this.patrolCooldown = 0;
            this.explorationTarget = null;
            this.explorationTargetTicks = 0;
            this.explorationHeading += (this.random.nextDouble() - (double)0.5F) * 0.72;
            this.tickExploration(level);
         }

      }
   }

   private void resetCarryWatchdog() {
      this.carrySampleX = this.getX();
      this.carrySampleY = this.getY();
      this.carrySampleZ = this.getZ();
      this.carryWatchdogTicks = 0;
      this.carryStallSamples = 0;
      this.carryRecoveryStage = 0;
   }

   private SingerEntity resolveSinger(ServerLevel level) {
      if (this.canInteractWithSinger() && this.singerTarget != null) {
         Entity entity = level.getEntityInAnyDimension(this.singerTarget);
         if (entity instanceof SingerEntity) {
            SingerEntity singer = (SingerEntity)entity;
            if (singer.isAlive() && singer.isSoulEvent()) {
               return singer;
            }
         }

         this.singerTarget = null;
         return null;
      } else {
         this.singerTarget = null;
         return null;
      }
   }

   private Player resolveBondedPlayer(ServerLevel level) {
      if (this.bondedPlayer != null) {
         Player byUuid = level.getServer().getPlayerList().getPlayer(this.bondedPlayer);
         if (byUuid != null) {
            this.bondedPlayerName = byUuid.getName().getString();
            return byUuid;
         }
      }

      if (this.bondedPlayerName != null && !this.bondedPlayerName.isBlank()) {
         for (ServerPlayer onlinePlayer : level.getServer().getPlayerList().getPlayers()) {
            if (onlinePlayer.getName().getString().equalsIgnoreCase(this.bondedPlayerName)) {
               this.bondedPlayer = onlinePlayer.getUUID();
               this.bondedPlayerName = onlinePlayer.getName().getString();
               this.entityData.set(DATA_BONDED, true);
               this.setPersistenceRequired();
               return onlinePlayer;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private boolean isBondedTo(Player player) {
      if (!(Boolean)this.entityData.get(DATA_BONDED)) {
         return true;
      } else if (this.bondedPlayer != null && this.bondedPlayer.equals(player.getUUID())) {
         return true;
      } else if (this.bondedPlayerName != null && this.bondedPlayerName.equalsIgnoreCase(player.getName().getString())) {
         this.bondedPlayer = player.getUUID();
         this.bondedPlayerName = player.getName().getString();
         return true;
      } else {
         return false;
      }
   }

   private boolean acquireCanyonRoute(ServerLevel level) {
      Set<Long> excluded = new HashSet(this.unavailableCanyons.keySet());
      if (this.ignoredCanyon != null && this.ignoredCanyonTicks > 0) {
         excluded.add(this.ignoredCanyon.asLong());
      }

      BlockPos cluster = (BlockPos)SiftLandmarkTracker.nearestActiveSoulCanyon(level, this.blockPosition(), (double)1024.0F, excluded).orElse(null);
      if (cluster == null) {
         return false;
      } else if (!this.hasUnquarantinedSoulAtCanyon(level, cluster)) {
         this.rejectCanyon(level, cluster, 6000L);
         return false;
      } else {
         BlockPos entrance = (BlockPos)SiftLandmarkTracker.nearestOther(SiftLandmarkTracker.Kind.SOUL_CANYON_ENTRANCE, cluster, (double)96.0F, (BlockPos)null).orElse(null);
         if (entrance == null) {
            return false;
         } else {
            BlockPos midpoint = (BlockPos)SiftLandmarkTracker.nearestOther(SiftLandmarkTracker.Kind.SOUL_CANYON_MIDPOINT, cluster, (double)72.0F, (BlockPos)null).orElse(null);
            BlockPos approach = this.canyonApproach(level, entrance, cluster);
            if (approach == null) {
               this.rejectCanyon(level, cluster, 2400L);
               return false;
            } else if (this.horizontalDistanceSqr(approach) > (double)9216.0F) {
               if (!cluster.equals(this.distantCanyonHint)) {
                  this.distantCanyonHint = cluster;
                  this.explorationHeading = Mth.atan2((double)approach.getZ() + (double)0.5F - this.getZ(), (double)approach.getX() + (double)0.5F - this.getX());
                  this.explorationTarget = null;
                  this.explorationTargetTicks = 0;
                  this.navigationGoal = null;
                  this.repathCooldown = 0;
                  this.getNavigation().stop();
               }

               return false;
            } else {
               this.distantCanyonHint = null;
               this.canyonCluster = cluster;
               this.canyonEntrance = entrance;
               this.canyonMidpoint = midpoint;
               this.enteredCanyon = false;
               this.exitMidpointPassed = false;
               this.emptyCanyonChecks = 0;
               this.canyonEntryAttempts = 0;
               this.canyonDepthAttempts = 0;
               return true;
            }
         }
      }
   }

   private boolean hasUnquarantinedSoulAtCanyon(ServerLevel level, BlockPos cluster) {
      long gameTime = level.getGameTime();

      for (BlockPos soul : (Iterable<BlockPos>) SiftLandmarkTracker.activeSoulBlocksNear(level, cluster, 38, 28)) {
         if (this.canReachSoulWithHands(level, soul) || !this.isSoulGloballyRejected(soul, gameTime)) {
            return true;
         }
      }

      return false;
   }

   private boolean hasValidCanyonRoute() {
      return this.canyonCluster != null && this.canyonEntrance != null;
   }

   private void clearCanyonRoute() {
      this.clearSoulTargetReservation();
      this.canyonEntrance = null;
      this.canyonMidpoint = null;
      this.canyonCluster = null;
      this.enteredCanyon = false;
      this.exitMidpointPassed = false;
      this.emptyCanyonChecks = 0;
      this.canyonEntryAttempts = 0;
      this.canyonDepthAttempts = 0;
      this.landmarkCooldown = 0;
      this.targetCooldown = 0;
   }

   private void forgetCurrentCanyon(int ignoreTicks) {
      if (this.canyonCluster != null && ignoreTicks > 0) {
         this.ignoredCanyon = this.canyonCluster;
         this.ignoredCanyonTicks = ignoreTicks;
      }

      this.clearCanyonRoute();
   }

   private void abandonBlockedCanyon(ServerLevel level, long duration) {
      BlockPos blocked = this.canyonCluster;
      if (blocked != null) {
         this.rejectCanyon(level, blocked, duration);
         this.ignoredCanyon = blocked;
         this.ignoredCanyonTicks = (int)Math.min(2147483647L, duration);
      }

      this.clearCanyonRoute();
      this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
      this.forceExplorationTurn(level, false);
   }

   private void markCurrentCanyonUnavailable(ServerLevel level, long duration) {
      if (this.canyonCluster != null) {
         long until = level.getGameTime() + duration;
         this.unavailableCanyons.put(this.canyonCluster.asLong(), until);
         this.ignoredCanyon = this.canyonCluster;
         this.ignoredCanyonTicks = (int)Math.min(2147483647L, duration);
         this.clearSoulTargetReservation();
      }
   }

   private void purgeExpiredRejections(long gameTime) {
      this.unavailableCanyons.entrySet().removeIf(entry -> ((Long)((Map.Entry<?, ?>) entry).getValue()) <= gameTime);
      this.unreachableSoulBlocks.entrySet().removeIf(entry -> ((Long)((Map.Entry<?, ?>) entry).getValue()) <= gameTime);
   }

   private void rejectCanyon(ServerLevel level, BlockPos canyon, long duration) {
      if (canyon != null) {
         this.unavailableCanyons.put(canyon.asLong(), level.getGameTime() + duration);
         if (canyon.equals(this.distantCanyonHint)) {
            this.distantCanyonHint = null;
         }

      }
   }

   private void forceExplorationTurn(ServerLevel level, boolean rejectHint) {
      if (rejectHint && this.distantCanyonHint != null) {
         this.rejectCanyon(level, this.distantCanyonHint, 2400L);
      }

      this.getNavigation().stop();
      this.navigationGoal = null;
      this.repathCooldown = 0;
      this.routeFailures = 0;
      this.explorationTarget = null;
      this.explorationTargetTicks = 0;
      double turn = (double)1.75F + this.random.nextDouble() * 0.7;
      this.explorationHeading += this.random.nextBoolean() ? turn : -turn;
      this.explorationTurnTicks = 600 + this.random.nextInt(601);
      this.explorationTarget = this.chooseExplorationTarget(level);
      this.resetRouteProgressSample();
   }

   private BlockPos ensureReachableExit(ServerLevel level) {
      if (this.canyonEntrance == null) {
         BlockPos origin = this.canyonCluster == null ? this.blockPosition() : this.canyonCluster;
         this.canyonEntrance = (BlockPos)SiftLandmarkTracker.nearestOther(SiftLandmarkTracker.Kind.SOUL_CANYON_ENTRANCE, origin, (double)112.0F, (BlockPos)null).orElse(null);
         this.navigationGoal = null;
         this.repathCooldown = 0;
      }

      if (this.canyonEntrance != null && this.routeFailures < 6) {
         return this.canyonEntrance;
      } else {
         BlockPos emergency = this.findReachableSurfaceExit(level);
         if (emergency != null) {
            this.canyonEntrance = emergency;
            this.routeFailures = 0;
            this.navigationGoal = null;
            this.repathCooldown = 0;
            this.getNavigation().stop();
            return emergency;
         } else {
            return this.canyonEntrance;
         }
      }
   }

   private BlockPos findReachableSurfaceExit(ServerLevel level) {
      if (!this.tryAcquireCriticalExitPathBudget(level, 5)) {
         return null;
      } else {
         Set<BlockPos> candidates = new HashSet();
         double phase = this.explorationHeading;
         int minimumExitY = this.canyonEntrance == null ? this.blockPosition().getY() + 2 : this.canyonEntrance.getY() - 2;

         for (int radius : new int[]{8, 14, 20, 28}) {
            for (int direction = 0; direction < 8; ++direction) {
               double angle = phase + (double)direction * Math.PI * (double)2.0F / (double)8.0F;
               int x = Mth.floor(this.getX() + Math.cos(angle) * (double)radius);
               int z = Mth.floor(this.getZ() + Math.sin(angle) * (double)radius);
               int y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
               BlockPos feet = new BlockPos(x, y, z);
               if (y >= minimumExitY && level.isLoaded(feet) && level.canSeeSky(feet.above())) {
                  candidates.add(feet);
               }
            }
         }

         if (candidates.isEmpty()) {
            return null;
         } else {
            Path path = this.getNavigation().createPath(candidates, 2);
            if (path != null && path.getEndNode() != null && path.canReach()) {
               BlockPos target = path.getEndNode().asBlockPos().immutable();
               this.preparedExitPath = path;
               this.preparedExitPathTarget = target;
               return target;
            } else {
               return null;
            }
         }
      }
   }

   private void tickEmergencyAscent(ServerLevel level) {
      if (this.getNavigation().isInProgress() && !this.getNavigation().isStuck()) {
         this.monitorRouteProgress(false);
      } else if (--this.repathCooldown <= 0) {
         this.repathCooldown = 5;
         if (!this.tryAcquireCriticalExitPathBudget(level, 5)) {
            this.repathCooldown = 1 + (this.getId() & 1);
         } else {
            int surfaceY = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, this.blockPosition().getX(), this.blockPosition().getZ());
            Vec3 upward = new Vec3(this.getX() + Math.cos(this.explorationHeading) * (double)18.0F, Math.max(this.getY() + (double)8.0F, (double)surfaceY), this.getZ() + Math.sin(this.explorationHeading) * (double)18.0F);
            Vec3 waypoint = DefaultRandomPos.getPosTowards(this, 22, 18, upward, (Math.PI / 2D));
            if (waypoint == null || !this.getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, 0.84)) {
               this.explorationHeading += (this.random.nextBoolean() ? (double)1.0F : (double)-1.0F) * (0.65 + this.random.nextDouble() * 0.55);
               ++this.routeFailures;
            }

            this.monitorRouteProgress(false);
         }
      }
   }

   private BlockPos canyonApproach() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel level) {
         return this.canyonApproach(level, this.canyonEntrance, this.canyonCluster);
      } else {
         return this.canyonEntrance;
      }
   }

   private BlockPos canyonApproach(ServerLevel level, BlockPos entrance, BlockPos cluster) {
      if (entrance != null && cluster != null) {
         double dx = (double)(entrance.getX() - cluster.getX());
         double dz = (double)(entrance.getZ() - cluster.getZ());
         double length = Math.sqrt(dx * dx + dz * dz);
         if (length < (double)1.0F) {
            return entrance;
         } else {
            int x = entrance.getX() + (int)Math.round(dx / length * (double)7.0F);
            int z = entrance.getZ() + (int)Math.round(dz / length * (double)7.0F);
            BlockPos heightProbe = new BlockPos(x, entrance.getY(), z);
            int y = level.isLoaded(heightProbe) ? level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z) : entrance.getY();
            BlockPos approach = new BlockPos(x, y, z);
            return this.horizontalDistanceSqr(approach) <= (double)16.0F ? entrance : approach;
         }
      } else {
         return entrance;
      }
   }

   private boolean navigateToward(BlockPos target, double speed) {
      return this.navigateToward(target, speed, false);
   }

   private boolean navigateExitToward(BlockPos target, double speed) {
      return this.navigateToward(target, speed, true);
   }

   private boolean navigateToward(BlockPos target, double speed, boolean criticalExit) {
      if (target == null) {
         return false;
      } else {
         if (!target.equals(this.navigationGoal)) {
            this.navigationGoal = target.immutable();
            this.repathCooldown = 0;
            this.getNavigation().stop();
            if (!target.equals(this.preparedExitPathTarget)) {
               this.clearPreparedExitPath();
            }
         }

         if (this.getNavigation().isInProgress() && !this.getNavigation().isStuck()) {
            return true;
         } else if (criticalExit && target.equals(this.preparedExitPathTarget) && this.preparedExitPath != null) {
            Path prepared = this.preparedExitPath;
            this.clearPreparedExitPath();
            if (this.getNavigation().moveTo(prepared, speed)) {
               this.repathCooldown = 0;
               this.routeFailures = Math.max(0, this.routeFailures - 1);
               return true;
            } else {
               ++this.routeFailures;
               this.repathCooldown = 2;
               return false;
            }
         } else if (--this.repathCooldown > 0) {
            return false;
         } else {
            ServerLevel serverLevel = (ServerLevel)this.level();
            boolean acquired = criticalExit ? this.tryAcquireCriticalExitPathBudget(serverLevel, 4) : this.tryAcquirePathBudget(serverLevel, 4);
            if (!acquired) {
               this.repathCooldown = 1 + (this.getId() & 1);
               return false;
            } else {
               this.repathCooldown = 10 + this.random.nextInt(7);
               Vec3 exact = Vec3.atBottomCenterOf(target);
               Vec3 destination = exact;
               double exactDx = exact.x - this.getX();
               double exactDz = exact.z - this.getZ();
               if (!criticalExit && exactDx * exactDx + exactDz * exactDz > (double)1600.0F) {
                  Vec3 waypoint = this.forwardWaypoint(exact);
                  if (waypoint == null) {
                     ++this.routeFailures;
                     this.repathCooldown = 4;
                     return false;
                  }

                  destination = waypoint;
               }

               BlockPos pathTarget = BlockPos.containing(destination);
               Path path;
               if (criticalExit) {
                  Set<BlockPos> rampWidth = new HashSet();

                  for (int dx = -1; dx <= 1; ++dx) {
                     for (int dz = -1; dz <= 1; ++dz) {
                        rampWidth.add(pathTarget.offset(dx, 0, dz));
                     }
                  }

                  path = this.getNavigation().createPath(rampWidth, 1);
               } else {
                  path = this.getNavigation().createPath(pathTarget, 1);
               }

               float acceptedDistance = criticalExit ? 10.0F : 4.0F;
               if (path != null && path.getEndNode() != null && (path.canReach() || !(path.getEndNode().distanceToSqr(pathTarget) > acceptedDistance))) {
                  if (!this.getNavigation().moveTo(path, speed)) {
                     ++this.routeFailures;
                     this.repathCooldown = 4;
                     return false;
                  } else {
                     this.repathCooldown = 0;
                     if (this.routeFailures > 0) {
                        --this.routeFailures;
                     }

                     return true;
                  }
               } else {
                  ++this.routeFailures;
                  this.repathCooldown = 4;
                  return false;
               }
            }
         }
      }
   }

   boolean tryAcquirePathBudget(ServerLevel level, int personalInterval) {
      long now = level.getGameTime();
      if (now < this.nextAllowedPathfindTick) {
         return false;
      } else {
         resetPathBudgetTick(now);
         if (pathsUsedThisTick >= 1) {
            this.nextAllowedPathfindTick = now + 1L + (long)(this.getId() & 1);
            return false;
         } else {
            ++pathsUsedThisTick;
            this.nextAllowedPathfindTick = now + (long)Math.max(1, personalInterval);
            return true;
         }
      }
   }

   private boolean tryAcquireCriticalExitPathBudget(ServerLevel level, int personalInterval) {
      long now = level.getGameTime();
      if (now < this.nextAllowedPathfindTick) {
         return false;
      } else {
         resetPathBudgetTick(now);
         if (criticalExitPathsUsedThisTick >= 1) {
            this.nextAllowedPathfindTick = now + 1L + (long)(this.getId() & 1);
            return false;
         } else {
            ++criticalExitPathsUsedThisTick;
            this.nextAllowedPathfindTick = now + (long)Math.max(1, personalInterval);
            return true;
         }
      }
   }

   private static void resetPathBudgetTick(long now) {
      if (pathBudgetTick != now) {
         pathBudgetTick = now;
         pathsUsedThisTick = 0;
         criticalExitPathsUsedThisTick = 0;
      }
   }

   private void clearPreparedExitPath() {
      this.preparedExitPath = null;
      this.preparedExitPathTarget = null;
   }

   private Vec3 forwardWaypoint(Vec3 exact) {
      double dx = exact.x - this.getX();
      double dz = exact.z - this.getZ();
      double length = Math.sqrt(dx * dx + dz * dz);
      if (length < (double)1.0F) {
         return exact;
      } else {
         double forwardX = dx / length;
         double forwardZ = dz / length;
         double sideX = -forwardZ;
         double sideZ = forwardX;
         double[] lateral = new double[]{(double)0.0F, (double)4.0F, (double)-4.0F, (double)8.0F, (double)-8.0F};
         int first = Math.min(this.routeFailures, lateral.length - 1);

         for (int distance : new int[]{24, 18, 12}) {
            for (int index = 0; index < lateral.length; ++index) {
               double side = lateral[(first + index) % lateral.length];
               int x = Mth.floor(this.getX() + forwardX * (double)distance + sideX * side);
               int z = Mth.floor(this.getZ() + forwardZ * (double)distance + sideZ * side);
               if (this.level().isLoaded(new BlockPos(x, this.blockPosition().getY(), z))) {
                  int y = this.level().getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                  return new Vec3((double)x + (double)0.5F, (double)y, (double)z + (double)0.5F);
               }
            }
         }

         return null;
      }
   }

   private void monitorRouteProgress(boolean exploring) {
      if (this.routeTicks % 20 == 0) {
         if (!this.progressSampleReady) {
            this.resetRouteProgressSample();
         } else {
            double dx = this.getX() - this.progressSampleX;
            double dy = this.getY() - this.progressSampleY;
            double dz = this.getZ() - this.progressSampleZ;
            double movedSqr = dx * dx + dy * dy + dz * dz;
            this.progressSampleX = this.getX();
            this.progressSampleY = this.getY();
            this.progressSampleZ = this.getZ();
            if (movedSqr >= 0.16) {
               this.stalledSamples = 0;
            } else if (++this.stalledSamples >= 2) {
               this.stalledSamples = 0;
               this.getNavigation().stop();
               this.navigationGoal = null;
               this.repathCooldown = 0;
               ServerLevel serverLevel = (ServerLevel)this.level();
               if (exploring) {
                  this.forceExplorationTurn(serverLevel, true);
               } else if (!this.hasSoulBlock() && this.missionState() != EchoGolemEntity.MissionState.CARRY) {
                  switch (this.routePhase.ordinal()) {
                     case 1:
                     case 2:
                        this.abandonBlockedCanyon(serverLevel, 2400L);
                        break;
                     case 3:
                        if (this.soulTarget != null) {
                           this.soulApproach = null;
                           this.targetCooldown = 0;
                           this.nextAllowedPathfindTick = serverLevel.getGameTime();
                           this.routeFailures = Math.min(2, this.routeFailures + 1);
                        } else {
                           this.markCurrentCanyonUnavailable(serverLevel, 2400L);
                           this.setRoutePhase(EchoGolemEntity.RoutePhase.LEAVE_EMPTY_CANYON);
                        }
                        break;
                     case 4:
                        this.routeFailures += 4;
                        break;
                     case 5:
                     case 6:
                     default:
                        this.forceExplorationTurn(serverLevel, false);
                        break;
                     case 7:
                        this.markCurrentCanyonUnavailable(serverLevel, 2400L);
                        this.setRoutePhase(EchoGolemEntity.RoutePhase.LEAVE_EMPTY_CANYON);
                  }

               } else {
                  this.routeFailures += 4;
               }
            }
         }
      }
   }

   private void resetRouteProgressSample() {
      this.progressSampleX = this.getX();
      this.progressSampleY = this.getY();
      this.progressSampleZ = this.getZ();
      this.progressSampleReady = true;
      this.stalledSamples = 0;
   }

   private void tickLivenessWatchdog(ServerLevel level) {
      if (++this.livenessTicks >= 20) {
         this.livenessTicks = 0;
         double dx = this.getX() - this.livenessSampleX;
         double dy = this.getY() - this.livenessSampleY;
         double dz = this.getZ() - this.livenessSampleZ;
         double movedSqr = dx * dx + dy * dy + dz * dz;
         this.livenessSampleX = this.getX();
         this.livenessSampleY = this.getY();
         this.livenessSampleZ = this.getZ();
         boolean expectsMovement = this.soulTarget != null || this.navigationGoal != null || this.explorationTarget != null || this.routePhase == EchoGolemEntity.RoutePhase.APPROACH_CANYON || this.routePhase == EchoGolemEntity.RoutePhase.ENTER_CANYON || this.routePhase == EchoGolemEntity.RoutePhase.DESCEND_CANYON || this.routePhase == EchoGolemEntity.RoutePhase.LEAVE_EMPTY_CANYON;
         if (expectsMovement && !(movedSqr >= 0.09)) {
            if (++this.livenessStallSamples >= 3) {
               this.livenessStallSamples = 0;
               this.getNavigation().stop();
               this.navigationGoal = null;
               this.repathCooldown = 0;
               this.nextAllowedPathfindTick = level.getGameTime();
               if (this.soulTarget != null) {
                  this.soulApproach = null;
                  this.routeFailures = Math.min(2, this.routeFailures + 1);
               } else {
                  if (this.routePhase == EchoGolemEntity.RoutePhase.EXPLORE) {
                     this.forceExplorationTurn(level, true);
                  } else {
                     this.routeFailures += 3;
                  }

               }
            }
         } else {
            this.livenessStallSamples = 0;
         }
      }
   }

   private void assistLocalStep() {
      if (this.horizontalCollision && this.onGround() && this.getNavigation().isInProgress() && (this.tickCount + this.getId()) % 6 == 0) {
         Vec3 velocity = this.getDeltaMovement();
         this.setDeltaMovement(velocity.x, Math.max(velocity.y, 0.34), velocity.z);
      }
   }

   private void tickSit() {
      this.getNavigation().stop();
      if (this.stateTicks >= 600) {
         this.targetCooldown = 0;
         this.setRoutePhase(EchoGolemEntity.RoutePhase.EXPLORE);
         this.setMissionState(EchoGolemEntity.MissionState.SEARCH);
      }

   }

   private SoulCandidate findReachableSoulBlock(ServerLevel level, BlockPos origin, int horizontal, int vertical) {
      this.purgeExpiredRejections(level.getGameTime());
      List<BlockPos> candidates = new ArrayList(SiftLandmarkTracker.activeSoulBlocksNear(level, origin, horizontal, vertical));
      if (level.getGameTime() >= this.nextPhysicalSoulScanTick) {
         this.nextPhysicalSoulScanTick = level.getGameTime() + 40L;
         this.collectPhysicalSoulBlocks(level, this.blockPosition(), Math.min(horizontal, 12), Math.min(vertical, 8), candidates, 20);
      }

      candidates.sort(Comparator.comparingDouble((candidatex) -> candidatex.distSqr(this.blockPosition())));

      for (BlockPos candidate : candidates) {
         if (level.isLoaded(candidate) && level.getBlockState(candidate).is(ModBlocks.SOUL_BLOCK)) {
            if (!this.isSoulReservedByOther(level, candidate)) {
               if (this.canReachSoulWithHands(level, candidate)) {
                  this.forgetSoulFailure(candidate);
                  return new SoulCandidate(candidate, this.blockPosition());
               }

               Long rejectedUntil = (Long)this.unreachableSoulBlocks.get(candidate.asLong());
               if ((rejectedUntil == null || rejectedUntil <= level.getGameTime()) && !this.isSoulGloballyRejected(candidate, level.getGameTime())) {
                  return new SoulCandidate(candidate, (BlockPos)null);
               }
            }
         } else {
            this.forgetSoulFailure(candidate);
         }
      }

      return null;
   }

   private void collectPhysicalSoulBlocks(ServerLevel level, BlockPos center, int horizontal, int vertical, List output, int maximumAdded) {
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
      int added = 0;

      for (int ring = 0; ring <= horizontal; ++ring) {
         for (int dx = -ring; dx <= ring; ++dx) {
            for (int dz = -ring; dz <= ring; ++dz) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == ring && dx * dx + dz * dz <= horizontal * horizontal) {
                  cursor.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                  if (level.isLoaded(cursor)) {
                     for (int index = 0; index <= vertical * 2; ++index) {
                        int magnitude = (index + 1) / 2;
                        int dy = index == 0 ? 0 : (index % 2 == 1 ? -magnitude : magnitude);
                        cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        if (level.getBlockState(cursor).is(ModBlocks.SOUL_BLOCK)) {
                           BlockPos found = cursor.immutable();
                           if (!output.contains(found)) {
                              output.add(found);
                              ++added;
                              if (added >= maximumAdded) {
                                 return;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private BlockPos findReachableSoulApproach(ServerLevel level, BlockPos soul) {
      if (this.canReachSoulWithHands(level, soul)) {
         return this.blockPosition();
      } else {
         Set<BlockPos> approaches = new HashSet();

         for (int yOffset = -1; yOffset <= 2; ++yOffset) {
            for (int dx = -1; dx <= 1; ++dx) {
               for (int dz = -1; dz <= 1; ++dz) {
                  approaches.add(soul.offset(dx, yOffset, dz));
               }
            }
         }

         Path path = this.getNavigation().createPath(approaches, 1);
         if (path != null && path.getEndNode() != null && !(path.getEndNode().distanceToSqr(soul.above()) > 3.4F)) {
            return path.getEndNode().asBlockPos().immutable();
         } else {
            return null;
         }
      }
   }

   private boolean canReachSoulWithHands(ServerLevel level, BlockPos soul) {
      if (level.isLoaded(soul) && level.getBlockState(soul).is(ModBlocks.SOUL_BLOCK)) {
         AABB body = this.getBoundingBox();
         AABB block = new AABB(soul);
         double gapX = Math.max((double)0.0F, Math.max(block.minX - body.maxX, body.minX - block.maxX));
         double gapY = Math.max((double)0.0F, Math.max(block.minY - body.maxY, body.minY - block.maxY));
         double gapZ = Math.max((double)0.0F, Math.max(block.minZ - body.maxZ, body.minZ - block.maxZ));
         if (gapX * gapX + gapY * gapY + gapZ * gapZ > (double)3.0625F) {
            return false;
         } else {
            Vec3 target = new Vec3((double)soul.getX() + (double)0.5F, (double)soul.getY() + 0.85, (double)soul.getZ() + (double)0.5F);
            Vec3 handOrigin = new Vec3(Math.max(body.minX + 0.01, Math.min(body.maxX - 0.01, target.x)), Math.max(body.minY + 0.1, Math.min(body.maxY - 0.1, target.y)), Math.max(body.minZ + 0.01, Math.min(body.maxZ - 0.01, target.z)));
            BlockHitResult hit = level.clip(new ClipContext(handOrigin, target, Block.COLLIDER, Fluid.NONE, this));
            return hit.getType() == Type.MISS || soul.equals(hit.getBlockPos());
         }
      } else {
         return false;
      }
   }

   private boolean isSoulReservedByOther(ServerLevel level, BlockPos soul) {
      UUID owner = (UUID)SOUL_RESERVATIONS.get(soul.asLong());
      if (owner != null && !owner.equals(this.getUUID())) {
         Entity entity = level.getEntityInAnyDimension(owner);
         if (entity instanceof EchoGolemEntity) {
            EchoGolemEntity other = (EchoGolemEntity)entity;
            if (other.isAlive() && soul.equals(other.soulTarget)) {
               return true;
            }
         }

         SOUL_RESERVATIONS.remove(soul.asLong(), owner);
         return false;
      } else {
         return false;
      }
   }

   private void claimSoulTarget(SoulCandidate candidate) {
      this.clearSoulTargetReservation();
      this.soulTarget = candidate.soul();
      this.soulApproach = candidate.approach();
      SOUL_RESERVATIONS.put(this.soulTarget.asLong(), this.getUUID());
   }

   private void rejectCurrentSoulTarget(ServerLevel level, long duration) {
      if (this.soulTarget != null) {
         this.rememberSoulFailure(level, this.soulTarget, duration);
      }

      this.clearSoulTargetReservation();
   }

   private void rememberSoulFailure(ServerLevel level, BlockPos soul, long minimumDuration) {
      long now = level.getGameTime();
      long packed = soul.asLong();
      SoulFailureMemory previous = (SoulFailureMemory)SOUL_FAILURE_MEMORY.get(packed);
      int failures = previous != null && previous.forgetAfter() > now ? Math.min(4, previous.failures() + 1) : 1;
      long var10000;
      switch (failures) {
         case 1 -> var10000 = 100L;
         case 2 -> var10000 = 400L;
         default -> var10000 = 2400L;
      }

      long penalty = var10000;
      penalty = Math.max(penalty, minimumDuration);
      long retryAfter = now + penalty;
      SOUL_FAILURE_MEMORY.put(packed, new SoulFailureMemory(failures, retryAfter, retryAfter + 12000L));
      this.unreachableSoulBlocks.put(packed, retryAfter);
   }

   private boolean isSoulGloballyRejected(BlockPos soul, long gameTime) {
      SoulFailureMemory memory = (SoulFailureMemory)SOUL_FAILURE_MEMORY.get(soul.asLong());
      if (memory == null) {
         return false;
      } else if (memory.forgetAfter() <= gameTime) {
         SOUL_FAILURE_MEMORY.remove(soul.asLong(), memory);
         return false;
      } else {
         return memory.retryAfter() > gameTime;
      }
   }

   private void forgetSoulFailure(BlockPos soul) {
      if (soul != null) {
         long packed = soul.asLong();
         this.unreachableSoulBlocks.remove(packed);
         SOUL_FAILURE_MEMORY.remove(packed);
      }
   }

   private void clearSoulTargetReservation() {
      if (this.soulTarget != null) {
         SOUL_RESERVATIONS.remove(this.soulTarget.asLong(), this.getUUID());
      }

      this.soulTarget = null;
      this.soulApproach = null;
   }

   private double horizontalDistanceSqr(BlockPos pos) {
      double dx = (double)pos.getX() + (double)0.5F - this.getX();
      double dz = (double)pos.getZ() + (double)0.5F - this.getZ();
      return dx * dx + dz * dz;
   }

   private boolean isNear(BlockPos pos, double horizontal, double vertical) {
      return pos != null && this.horizontalDistanceSqr(pos) <= horizontal * horizontal && Math.abs(this.getY() - (double)pos.getY()) <= vertical;
   }

   private boolean needsCanyonMidpointForExit() {
      if (this.canyonMidpoint != null && this.canyonEntrance != null) {
         double midDx = (double)(this.canyonMidpoint.getX() - this.canyonEntrance.getX());
         double midDz = (double)(this.canyonMidpoint.getZ() - this.canyonEntrance.getZ());
         double midpointFromEntrance = midDx * midDx + midDz * midDz;
         return this.horizontalDistanceSqr(this.canyonEntrance) > midpointFromEntrance + (double)16.0F;
      } else {
         return false;
      }
   }

   private boolean releaseCarriedSoul(ServerLevel level) {
      if (!this.hasSoulBlock()) {
         return false;
      } else {
         this.entityData.set(DATA_HAS_SOUL, false);
         this.spawnAtLocation(level, new ItemStack(ModBlocks.SOUL_BLOCK_ITEM), 0.8F);
         level.playSound((Entity)null, this.blockPosition(), ModSounds.ECHO_GOLEM_ITEM_DROP, this.getSoundSource(), 1.0F, 1.0F);
         this.sitAfterLosingSoul();
         return true;
      }
   }

   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (held.getItem() == Items.ECHO_SHARD) {
         if (this.bondedPlayer == null && this.bondedPlayerName == null && !(Boolean)this.entityData.get(DATA_BONDED)) {
            Level var7 = this.level();
            if (var7 instanceof ServerLevel) {
               ServerLevel level = (ServerLevel)var7;
               this.bondedPlayer = player.getUUID();
               this.bondedPlayerName = player.getName().getString();
               this.entityData.set(DATA_BONDED, true);
               this.singerTarget = null;
               this.patrolTarget = null;
               this.patrolCooldown = 0;
               this.setPersistenceRequired();
               if (!player.hasInfiniteMaterials()) {
                  held.shrink(1);
               }

               level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + (double)this.getBbHeight() * 0.7, this.getZ(), 7, 0.45, 0.55, 0.45, 0.08);
               if (this.missionState() == EchoGolemEntity.MissionState.BOW) {
                  this.setRoutePhase(this.hasSoulBlock() ? (this.hasReachedSurface() ? EchoGolemEntity.RoutePhase.DELIVER : EchoGolemEntity.RoutePhase.EXIT_CANYON) : EchoGolemEntity.RoutePhase.EXPLORE);
                  this.setMissionState(this.hasSoulBlock() ? EchoGolemEntity.MissionState.CARRY : EchoGolemEntity.MissionState.SEARCH);
               }

               return InteractionResult.SUCCESS_SERVER;
            } else {
               return InteractionResult.SUCCESS;
            }
         } else {
            return InteractionResult.PASS;
         }
      } else if (this.hasSoulBlock()) {
         if (!this.isBondedTo(player)) {
            return InteractionResult.PASS;
         } else {
            Level var5 = this.level();
            if (var5 instanceof ServerLevel) {
               ServerLevel level = (ServerLevel)var5;
               this.releaseCarriedSoul(level);
               return InteractionResult.SUCCESS_SERVER;
            } else {
               return InteractionResult.SUCCESS;
            }
         }
      } else {
         return super.mobInteract(player, hand);
      }
   }

   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      this.releaseCarriedSoul(level);
      boolean hurt = super.hurtServer(level, source, amount);
      if (hurt) {
         this.getNavigation().stop();
         this.navigationGoal = null;
         this.repathCooldown = 0;
         this.livenessTicks = 0;
         this.livenessStallSamples = 0;
         this.livenessSampleX = this.getX();
         this.livenessSampleY = this.getY();
         this.livenessSampleZ = this.getZ();
         if (this.missionState() == EchoGolemEntity.MissionState.SEARCH && this.routePhase == EchoGolemEntity.RoutePhase.SEEK_SOUL) {
            this.soulApproach = null;
         }
      }

      return hurt;
   }

   protected int getBaseExperienceReward(ServerLevel level) {
      return 4 + this.random.nextInt(4);
   }

   public int getMaxHeadXRot() {
      return 0;
   }

   public int getMaxHeadYRot() {
      return 55;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.ECHO_GOLEM_HURT;
   }

   protected SoundEvent getDeathSound() {
      return ModSounds.ECHO_GOLEM_DEATH;
   }

   protected void playStepSound(BlockPos pos, BlockState state) {
      this.playSound(ModSounds.ECHO_GOLEM_STEP, 0.62F, 0.94F + this.random.nextFloat() * 0.12F);
   }

   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putInt("mission_state", this.missionState().ordinal());
      output.putBoolean("has_soul", this.hasSoulBlock());
      output.putInt("state_ticks", this.stateTicks);
      output.putInt("route_phase", this.routePhase.ordinal());
      output.putInt("route_ticks", this.routeTicks);
      if (this.soulTarget != null) {
         output.putLong("soul_target", this.soulTarget.asLong());
      }

      if (this.soulApproach != null) {
         output.putLong("soul_approach", this.soulApproach.asLong());
      }

      if (this.canyonEntrance != null) {
         output.putLong("canyon_entrance", this.canyonEntrance.asLong());
      }

      if (this.canyonMidpoint != null) {
         output.putLong("canyon_midpoint", this.canyonMidpoint.asLong());
      }

      if (this.canyonCluster != null) {
         output.putLong("canyon_cluster", this.canyonCluster.asLong());
      }

      if (this.ignoredCanyon != null) {
         output.putLong("ignored_canyon", this.ignoredCanyon.asLong());
      }

      output.putInt("ignored_canyon_ticks", this.ignoredCanyonTicks);
      output.putBoolean("entered_canyon", this.enteredCanyon);
      output.putBoolean("exit_midpoint_passed", this.exitMidpointPassed);
      output.putDouble("exploration_heading", this.explorationHeading);
      if (this.singerTarget != null) {
         output.putString("singer_target", this.singerTarget.toString());
      }

      if (this.bondedPlayer != null) {
         output.putString("bonded_player", this.bondedPlayer.toString());
      }

      if (this.bondedPlayerName != null && !this.bondedPlayerName.isBlank()) {
         output.putString("bonded_player_name", this.bondedPlayerName);
      }

      if (this.deliveryAnchor != null) {
         output.putLong("delivery_anchor", this.deliveryAnchor.asLong());
      }

   }

   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      int state = Mth.clamp(input.getIntOr("mission_state", 0), 0, EchoGolemEntity.MissionState.values().length - 1);
      this.entityData.set(DATA_MISSION_STATE, state);
      this.entityData.set(DATA_HAS_SOUL, input.getBooleanOr("has_soul", false));
      this.stateTicks = Math.max(0, input.getIntOr("state_ticks", 0));
      int savedRoute = input.getIntOr("route_phase", -1);
      if (savedRoute >= 0 && savedRoute < EchoGolemEntity.RoutePhase.values().length) {
         this.routePhase = EchoGolemEntity.RoutePhase.values()[savedRoute];
      } else if (this.hasSoulBlock()) {
         this.routePhase = EchoGolemEntity.RoutePhase.EXIT_CANYON;
      } else {
         this.routePhase = EchoGolemEntity.RoutePhase.EXPLORE;
      }

      this.routeTicks = Math.max(0, input.getIntOr("route_ticks", 0));
      this.soulTarget = (BlockPos)input.getLong("soul_target").map(BlockPos::of).orElse(null);
      this.soulApproach = (BlockPos)input.getLong("soul_approach").map(BlockPos::of).orElse(null);
      this.canyonEntrance = (BlockPos)input.getLong("canyon_entrance").or(() -> input.getLong("exit_target")).map(BlockPos::of).orElse(null);
      this.canyonMidpoint = (BlockPos)input.getLong("canyon_midpoint").map(BlockPos::of).orElse(null);
      this.canyonCluster = (BlockPos)input.getLong("canyon_cluster").map(BlockPos::of).orElse(null);
      this.ignoredCanyon = (BlockPos)input.getLong("ignored_canyon").map(BlockPos::of).orElse(null);
      this.ignoredCanyonTicks = Math.max(0, input.getIntOr("ignored_canyon_ticks", 0));
      this.enteredCanyon = input.getBooleanOr("entered_canyon", false);
      this.exitMidpointPassed = input.getBooleanOr("exit_midpoint_passed", !this.needsCanyonMidpointForExit());
      this.explorationHeading = input.getDoubleOr("exploration_heading", this.explorationHeading);
      this.singerTarget = (UUID)input.getString("singer_target").flatMap((value) -> {
         try {
            return Optional.of(UUID.fromString(value));
         } catch (IllegalArgumentException var2) {
            return Optional.empty();
         }
      }).orElse(null);
      this.bondedPlayer = (UUID)input.getString("bonded_player").flatMap((value) -> {
         try {
            return Optional.of(UUID.fromString(value));
         } catch (IllegalArgumentException var2) {
            return Optional.empty();
         }
      }).orElse(null);
      String savedBondedPlayerName = input.getStringOr("bonded_player_name", "");
      this.bondedPlayerName = savedBondedPlayerName.isBlank() ? null : savedBondedPlayerName;
      boolean bonded = this.bondedPlayer != null || this.bondedPlayerName != null;
      this.entityData.set(DATA_BONDED, bonded);
      this.deliveryAnchor = (BlockPos)input.getLong("delivery_anchor").map(BlockPos::of).orElse(null);
      if (bonded) {
         this.setPersistenceRequired();
         this.singerTarget = null;
         if (this.hasSoulBlock()) {
            this.entityData.set(DATA_MISSION_STATE, EchoGolemEntity.MissionState.CARRY.ordinal());
            this.stateTicks = 0;
            if (this.routePhase != EchoGolemEntity.RoutePhase.EXIT_CANYON && this.routePhase != EchoGolemEntity.RoutePhase.DELIVER) {
               this.routePhase = this.canyonEntrance == null ? EchoGolemEntity.RoutePhase.DELIVER : EchoGolemEntity.RoutePhase.EXIT_CANYON;
            }
         }
      }

      this.navigationGoal = null;
      this.repathCooldown = 0;
      this.routeFailures = 0;
      this.resetRouteProgressSample();
      if (savedRoute < 0 && !this.hasSoulBlock() && this.canyonCluster != null && this.canyonEntrance != null) {
         this.routePhase = this.enteredCanyon ? EchoGolemEntity.RoutePhase.SEEK_SOUL : EchoGolemEntity.RoutePhase.APPROACH_CANYON;
      }

   }

   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController("mission", 4, (state) -> {
         EchoGolemEntity golem = (EchoGolemEntity)state.animatable();
         RawAnimation var10000;
         switch (golem.missionState().ordinal()) {
            case 0 -> var10000 = golem.isWalkingForAnimation() ? WALK : IDLE;
            case 1 -> var10000 = PICKUP;
            case 2 -> var10000 = golem.isWalkingForAnimation() ? CARRY_WALK : CARRY_IDLE;
            case 3 -> var10000 = BOW;
            case 4 -> var10000 = SIT;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         RawAnimation animation = var10000;
         state.controller().setAnimation(animation);
         return PlayState.CONTINUE;
      }));
      controllers.add(new AnimationController("soul_visibility", 0, (state) -> {
         EchoGolemEntity golem = (EchoGolemEntity)state.animatable();
         state.controller().setAnimation(golem.hasSoulBlock() ? SOUL_VISIBLE : SOUL_HIDDEN);
         return PlayState.CONTINUE;
      }));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   static {
      DATA_MISSION_STATE = SynchedEntityData.defineId(EchoGolemEntity.class, EntityDataSerializers.INT);
      DATA_HAS_SOUL = SynchedEntityData.defineId(EchoGolemEntity.class, EntityDataSerializers.BOOLEAN);
      DATA_BONDED = SynchedEntityData.defineId(EchoGolemEntity.class, EntityDataSerializers.BOOLEAN);
      IDLE = RawAnimation.begin().thenLoop("idle");
      WALK = RawAnimation.begin().thenLoop("walk");
      PICKUP = RawAnimation.begin().thenPlay("pickup");
      CARRY_IDLE = RawAnimation.begin().thenLoop("carry_idle");
      CARRY_WALK = RawAnimation.begin().thenLoop("carry_walk");
      BOW = RawAnimation.begin().thenPlay("bow");
      SIT = RawAnimation.begin().thenLoop("sit");
      SOUL_VISIBLE = RawAnimation.begin().thenLoop("soul_visible");
      SOUL_HIDDEN = RawAnimation.begin().thenLoop("soul_hidden");
   }

   public static enum MissionState {
      SEARCH,
      PICKUP,
      CARRY,
      BOW,
      SIT;

      // $FF: synthetic method
      private static MissionState[] $values() {
         return new MissionState[]{SEARCH, PICKUP, CARRY, BOW, SIT};
      }
   }

   private static enum RoutePhase {
      EXPLORE,
      APPROACH_CANYON,
      ENTER_CANYON,
      SEEK_SOUL,
      LEAVE_EMPTY_CANYON,
      EXIT_CANYON,
      DELIVER,
      DESCEND_CANYON;

      // $FF: synthetic method
      private static RoutePhase[] $values() {
         return new RoutePhase[]{EXPLORE, APPROACH_CANYON, ENTER_CANYON, SEEK_SOUL, LEAVE_EMPTY_CANYON, EXIT_CANYON, DELIVER, DESCEND_CANYON};
      }
   }

   private static record SoulCandidate(BlockPos soul, BlockPos approach) {
   }

   private static record SoulFailureMemory(int failures, long retryAfter, long forgetAfter) {
   }
}
