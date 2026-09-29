package mielon.thesift.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SonorousDeepslateBlock;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.TheSiftDimension;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class SingerEntity extends PathfinderMob implements GeoEntity {
   private static final RawAnimation SEQUENCE = RawAnimation.begin().thenPlay("appear").thenPlay("sing").thenPlay("disappear");
   private static final String CONTROLLER = "singer_sequence";
   private static final EntityDataAccessor DATA_SEQUENCE_STARTED;
   private static final EntityDataAccessor DATA_SEQUENCE_TICK;
   private static final EntityDataAccessor DATA_SOUL_EVENT;
   private static final EntityDataAccessor DATA_SOUL_EVENT_PHASE;
   private static final EntityDataAccessor DATA_SOUL_EVENT_TICK;
   private static final RawAnimation EVENT_APPEAR;
   private static final RawAnimation EVENT_WALK;
   private static final RawAnimation EVENT_IDLE;
   private static final RawAnimation EVENT_RECEIVE;
   private static final RawAnimation EVENT_DISAPPEAR;
   private static final int SOUL_TRANSFER_TICK = 22;
   private static final int WARDEN_DIGGING_PARTICLES_PER_TICK = 30;
   private static final int WARDEN_DIGGING_PARTICLE_DURATION_TICKS = 90;
   private static final float WARDEN_DIGGING_PARTICLE_OFFSET = 0.7F;
   private static final int HORN_SEARCH_RADIUS = 20;
   private static final int MAX_HORN_TARGETS = 8;
   private static final double SOUND_WAVE_SPEED = 0.6;
   private static final double SINGER_HEAD_Y_OFFSET = (double)3.0F;
   private static final int[] SOUND_WAVE_TICKS;
   private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
   private SingerAnimationTimings timings;
   private int sequenceTick;
   private boolean started;
   private boolean singSoundPlayed;
   private boolean hornTargetsPrepared;
   private boolean disappearSoundPlayed;
   private boolean restoreLegacyHornTargets;
   private List hornTargets = List.of();
   private final List pendingHornActivations = new ArrayList();
   private BlockPos portalCenter;
   private UUID soulEventGolem;
   private int soulEventTick;
   private int roamMoveCooldown;
   private int canyonAwarenessCooldown;
   private BlockPos avoidedSoulCanyon;
   private boolean permanentSoulEvent;

   public SingerEntity(EntityType type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.setNoAi(true);
      this.setPermanentlyInvulnerable(true);
      this.timings = SingerAnimationTimings.load();
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_SEQUENCE_STARTED, false);
      builder.define(DATA_SEQUENCE_TICK, 0);
      builder.define(DATA_SOUL_EVENT, false);
      builder.define(DATA_SOUL_EVENT_PHASE, SingerEntity.SoulEventPhase.APPEAR.ordinal());
      builder.define(DATA_SOUL_EVENT_TICK, 0);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, (double)20.0F).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.KNOCKBACK_RESISTANCE, (double)0.0F).add(Attributes.SCALE, 1.2);
   }

   public void setPortalCenter(BlockPos portalCenter) {
      this.portalCenter = portalCenter == null ? null : portalCenter.immutable();
   }

   public BlockPos getPortalCenter() {
      return this.portalCenter;
   }

   public void beginSequence() {
      if (!this.started) {
         this.started = true;
         this.entityData.set(DATA_SEQUENCE_STARTED, true);
         this.entityData.set(DATA_SEQUENCE_TICK, 0);
         this.timings = SingerAnimationTimings.load();
         this.sequenceTick = 0;
         this.singSoundPlayed = false;
         this.hornTargetsPrepared = false;
         this.disappearSoundPlayed = false;
         this.hornTargets = List.of();
         this.pendingHornActivations.clear();
         if (!this.level().isClientSide()) {
            this.playAppearEffects();
         }

      }
   }

   public void beginSoulEvent(UUID golemId) {
      this.permanentSoulEvent = false;
      this.entityData.set(DATA_SOUL_EVENT, true);
      this.started = false;
      this.soulEventGolem = golemId;
      this.soulEventTick = 0;
      this.roamMoveCooldown = 0;
      this.setSoulEventPhase(SingerEntity.SoulEventPhase.APPEAR);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.setNoAi(true);
      this.setPermanentlyInvulnerable(false);
      this.setHealth(this.getMaxHealth());
   }

   private void beginPermanentSoulEvent() {
      this.beginSoulEvent((UUID)null);
      this.permanentSoulEvent = true;
      this.setPersistenceRequired();
   }

   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData spawnData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
      if ((reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.COMMAND) && !this.isSoulEvent() && this.portalCenter == null) {
         this.beginPermanentSoulEvent();
      }

      return result;
   }

   public boolean isSoulEvent() {
      return (Boolean)this.entityData.get(DATA_SOUL_EVENT);
   }

   public void offerSoulGolem(EchoGolemEntity golem) {
      if (this.isSoulEvent() && golem.hasSoulBlock() && golem.canInteractWithSinger()) {
         SoulEventPhase phase = this.soulEventPhase();
         if (phase != SingerEntity.SoulEventPhase.ROAMING) {
            if (this.soulEventGolem == null) {
               return;
            }

            Level var4 = this.level();
            if (!(var4 instanceof ServerLevel)) {
               return;
            }

            ServerLevel level = (ServerLevel)var4;
            if (level.getEntityInAnyDimension(this.soulEventGolem) != null) {
               return;
            }
         }

         this.soulEventGolem = golem.getUUID();
      }
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         this.tickClientDiggingParticles();
      } else {
         if (!this.started && !this.isSoulEvent() && this.portalCenter == null) {
            this.beginPermanentSoulEvent();
         }

         if (this.isSoulEvent()) {
            this.tickSoulEvent((ServerLevel)this.level());
         } else {
            this.tickPendingHornActivations();
            if (this.started) {
               if (this.sequenceTick >= this.timings.appearTicks() && !this.singSoundPlayed) {
                  this.singSoundPlayed = true;
                  this.prepareHornTargets();
                  this.playSingSoundFromSinger();
               }

               if (this.restoreLegacyHornTargets) {
                  this.restoreLegacyHornTargets = false;
                  this.hornTargetsPrepared = false;
                  this.prepareHornTargets(true);
               }

               this.tickSoundWaveLaunches();
               int disappearStart = this.timings.appearTicks() + this.timings.singTicks();
               if (this.sequenceTick >= disappearStart && !this.disappearSoundPlayed) {
                  this.disappearSoundPlayed = true;
                  this.playDisappearEffects();
               }

               ++this.sequenceTick;
               this.entityData.set(DATA_SEQUENCE_TICK, this.sequenceTick);
               int total = this.timings.appearTicks() + this.timings.singTicks() + this.timings.disappearTicks();
               if (this.sequenceTick >= total - 1) {
                  this.discard();
               }

            }
         }
      }
   }

   private void tickSoulEvent(ServerLevel level) {
      SoulEventPhase phase = this.soulEventPhase();
      ++this.soulEventTick;
      this.entityData.set(DATA_SOUL_EVENT_TICK, this.soulEventTick);
      switch (phase.ordinal()) {
         case 0:
            if (this.soulEventTick == 1) {
               this.playAppearEffects();
            }

            if (this.soulEventTick >= this.timings.appearTicks()) {
               this.noPhysics = false;
               this.setNoGravity(false);
               this.setNoAi(false);
               this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
            }
            break;
         case 1:
            this.tickSoulApproach(level);
            break;
         case 2:
            this.tickSoulExchange(level);
            break;
         case 3:
            this.tickSoulRoaming(level);
            break;
         case 4:
            this.getNavigation().stop();
            this.noPhysics = true;
            this.setNoGravity(true);
            this.setNoAi(true);
            if (this.soulEventTick == 1) {
               this.playDisappearEffects();
            }

            if (this.soulEventTick >= this.timings.disappearTicks()) {
               this.discard();
            }
      }

   }

   private void tickSoulApproach(ServerLevel level) {
      EchoGolemEntity golem = this.resolveSoulGolem(level);
      if (golem != null && golem.hasSoulBlock() && golem.canInteractWithSinger()) {
         this.faceSoulPartner(golem);
         if (this.soulEventTick % 15 == 1 || this.getNavigation().isDone()) {
            this.getNavigation().moveTo(golem, 0.92);
         }

         if (this.distanceToSqr(golem) > (double)144.0F) {
            this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
         } else {
            if (this.distanceToSqr(golem) <= (double)6.25F) {
               this.getNavigation().stop();
               golem.beginBow();
               this.setSoulEventPhase(SingerEntity.SoulEventPhase.EXCHANGE);
            }

         }
      } else {
         this.soulEventGolem = null;
         this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
      }
   }

   private void tickSoulExchange(ServerLevel level) {
      EchoGolemEntity golem = this.resolveSoulGolem(level);
      this.getNavigation().stop();
      if (golem == null) {
         this.soulEventGolem = null;
         this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
      } else {
         this.faceSoulPartner(golem);
         golem.faceEntity(this);
         if (this.soulEventTick < 22 && !golem.hasSoulBlock()) {
            golem.finishBow();
            this.soulEventGolem = null;
            this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
         } else {
            this.getLookControl().setLookAt(golem, 12.0F, 8.0F);
            if (this.soulEventTick == 22) {
               if (!golem.transferSoulToSinger()) {
                  golem.finishBow();
                  this.soulEventGolem = null;
                  this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
                  return;
               }

               this.playSound(ModSounds.SINGER_HAPPY, 1.0F, 0.96F + this.random.nextFloat() * 0.08F);
            }

            if (this.soulEventTick >= 48) {
               golem.finishBow();
               this.soulEventGolem = null;
               this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
            }

         }
      }
   }

   private void tickSoulRoaming(ServerLevel level) {
      if (!this.gentlyAvoidActiveSoulCanyon(level)) {
         EchoGolemEntity assigned = this.resolveSoulGolem(level);
         if (assigned != null && assigned.hasSoulBlock() && assigned.canInteractWithSinger() && this.distanceToSqr(assigned) <= (double)100.0F) {
            this.setSoulEventPhase(SingerEntity.SoulEventPhase.APPROACH);
         } else {
            if (this.soulEventTick % 40 == 1) {
               EchoGolemEntity nearby = (EchoGolemEntity)level.getEntitiesOfClass(EchoGolemEntity.class, this.getBoundingBox().inflate((double)48.0F, (double)28.0F, (double)48.0F), (golem) -> golem.hasSoulBlock() && golem.canInteractWithSinger()).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
               if (nearby != null) {
                  this.soulEventGolem = nearby.getUUID();
                  if (this.distanceToSqr(nearby) <= (double)100.0F) {
                     this.setSoulEventPhase(SingerEntity.SoulEventPhase.APPROACH);
                     return;
                  }
               }
            }

            if (--this.roamMoveCooldown <= 0 || this.getNavigation().isDone()) {
               this.roamMoveCooldown = 60 + this.random.nextInt(61);
               Vec3 destination = this.chooseSoulRoamDestination(level);
               if (destination != null) {
                  this.getNavigation().moveTo(destination.x, destination.y, destination.z, 0.78);
               }
            }

            if (!this.permanentSoulEvent && this.soulEventTick >= 600) {
               this.setSoulEventPhase(SingerEntity.SoulEventPhase.DISAPPEAR);
            }

         }
      }
   }

   private boolean gentlyAvoidActiveSoulCanyon(ServerLevel level) {
      if (--this.canyonAwarenessCooldown <= 0) {
         this.canyonAwarenessCooldown = 40;
         this.avoidedSoulCanyon = (BlockPos)SiftLandmarkTracker.nearestActiveSoulCanyon(level, this.blockPosition(), (double)36.0F).orElse(null);
      }

      if (this.avoidedSoulCanyon == null) {
         return false;
      } else {
         double horizontal = horizontalDistanceSqr(this.blockPosition(), this.avoidedSoulCanyon);
         boolean belowRim = this.getY() <= (double)this.avoidedSoulCanyon.getY() + (double)10.0F;
         if (belowRim && !(horizontal > (double)289.0F)) {
            if (this.getNavigation().isDone() || this.soulEventTick % 40 == 1) {
               Vec3 away = DefaultRandomPos.getPosAway(this, 15, 8, Vec3.atCenterOf(this.avoidedSoulCanyon));
               if (away != null) {
                  this.getNavigation().moveTo(away.x, away.y, away.z, 0.66);
               } else if (this.getNavigation().isDone()) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private Vec3 chooseSoulRoamDestination(ServerLevel level) {
      for (int attempt = 0; attempt < 5; ++attempt) {
         Vec3 destination = DefaultRandomPos.getPos(this, 12, 6);
         if (destination != null) {
            BlockPos candidate = BlockPos.containing(destination);
            BlockPos canyon = this.avoidedSoulCanyon;
            if (canyon == null || horizontalDistanceSqr(candidate, canyon) >= (double)324.0F) {
               return destination;
            }
         }
      }

      return null;
   }

   private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
      double dx = (double)(first.getX() - second.getX());
      double dz = (double)(first.getZ() - second.getZ());
      return dx * dx + dz * dz;
   }

   private void faceSoulPartner(EchoGolemEntity golem) {
      double dx = golem.getX() - this.getX();
      double dz = golem.getZ() - this.getZ();
      float yaw = (float)(Mth.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
      this.setYRot(yaw);
      this.setYHeadRot(yaw);
      this.setYBodyRot(yaw);
   }

   private EchoGolemEntity resolveSoulGolem(ServerLevel level) {
      if (this.soulEventGolem == null) {
         return null;
      } else {
         Entity entity = level.getEntityInAnyDimension(this.soulEventGolem);
         EchoGolemEntity var10000;
         if (entity instanceof EchoGolemEntity) {
            EchoGolemEntity golem = (EchoGolemEntity)entity;
            if (golem.isAlive() && golem.canInteractWithSinger()) {
               var10000 = golem;
               return var10000;
            }
         }

         var10000 = null;
         return var10000;
      }
   }

   private SoulEventPhase soulEventPhase() {
      int index = Mth.clamp((Integer)this.entityData.get(DATA_SOUL_EVENT_PHASE), 0, SingerEntity.SoulEventPhase.values().length - 1);
      return SingerEntity.SoulEventPhase.values()[index];
   }

   private boolean isSoulEventWalkingForAnimation() {
      return this.getDeltaMovement().horizontalDistanceSqr() > 9.0E-4 || this.getNavigation().isInProgress();
   }

   private void setSoulEventPhase(SoulEventPhase phase) {
      this.entityData.set(DATA_SOUL_EVENT_PHASE, phase.ordinal());
      this.soulEventTick = 0;
      this.entityData.set(DATA_SOUL_EVENT_TICK, 0);
   }

   private void playSingSoundFromSinger() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel serverLevel) {
         serverLevel.playSound((Entity)null, this.getX(), this.getY(), this.getZ(), ModSounds.SINGER_SING, SoundSource.NEUTRAL, 1000.0F, 1.0F);
      }
   }

   private static int secondsToTicks(double seconds) {
      return Math.max(0, (int)Math.round(seconds * (double)20.0F));
   }

   private void prepareHornTargets() {
      this.prepareHornTargets(false);
   }

   private void prepareHornTargets(boolean includeNoteModeForMigration) {
      if (!this.hornTargetsPrepared) {
         this.hornTargetsPrepared = true;
         Level var3 = this.level();
         if (var3 instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)var3;
            Vec3 var19 = this.position();
            Vec3 forward = this.getLookAngle();
            forward = new Vec3(forward.x, (double)0.0F, forward.z);
            if (forward.lengthSqr() < 1.0E-6) {
               forward = new Vec3((double)0.0F, (double)0.0F, (double)1.0F);
            } else {
               forward = forward.normalize();
            }

            Vec3 right = new Vec3(-forward.z, (double)0.0F, forward.x);
            if (right.lengthSqr() < 1.0E-6) {
               right = new Vec3((double)1.0F, (double)0.0F, (double)0.0F);
            } else {
               right = right.normalize();
            }

            List<BlockPos> candidates = new ArrayList();
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            int originX = this.blockPosition().getX();
            int originY = this.blockPosition().getY();
            int originZ = this.blockPosition().getZ();

            for (int dx = -20; dx <= 20; ++dx) {
               for (int dy = -20; dy <= 20; ++dy) {
                  for (int dz = -20; dz <= 20; ++dz) {
                     double distanceSquared = (double)dx * (double)dx + (double)dy * (double)dy + (double)dz * (double)dz;
                     if (!(distanceSquared > (double)400.0F)) {
                        cursor.set(originX + dx, originY + dy, originZ + dz);
                        BlockState state = serverLevel.getBlockState(cursor);
                        if (state.is(ModBlocks.SONOROUS_DEEPSLATE)) {
                           SonorousDeepslateBlock.Mode mode = (SonorousDeepslateBlock.Mode)state.getValue(SonorousDeepslateBlock.MODE);
                           if (mode == SonorousDeepslateBlock.Mode.HORN || includeNoteModeForMigration && mode == SonorousDeepslateBlock.Mode.NOTE) {
                              candidates.add(cursor.immutable());
                           }
                        }
                     }
                  }
               }
            }

            Vec3 finalRight = right;
            candidates.sort((a, b) -> {
               Vec3 aOffset = Vec3.atCenterOf(a).subtract(var19);
               Vec3 bOffset = Vec3.atCenterOf(b).subtract(var19);
               double aRight = aOffset.dot(finalRight);
               double bRight = bOffset.dot(finalRight);
               return Double.compare(bRight, aRight);
            });
            if (candidates.size() > 8) {
               candidates = new ArrayList(candidates.subList(0, 8));
            }

            int splitIndex = (candidates.size() + 1) / 2;
            List<BlockPos> firstHalf = new ArrayList(candidates.subList(0, splitIndex));
            List<BlockPos> secondHalf = new ArrayList(candidates.subList(splitIndex, candidates.size()));
            firstHalf.sort(Comparator.comparingDouble((pos) -> Vec3.atCenterOf((BlockPos) pos).distanceToSqr(var19)).reversed());
            secondHalf.sort(Comparator.comparingDouble((pos) -> Vec3.atCenterOf(pos).distanceToSqr(var19)));
            List<BlockPos> result = new ArrayList(candidates.size());
            result.addAll(firstHalf);
            result.addAll(secondHalf);
            this.hornTargets = List.copyOf(result);
         }
      }
   }

   private void tickSoundWaveLaunches() {
      if (this.singSoundPlayed && !this.hornTargets.isEmpty()) {
         int singStartTick = this.timings.appearTicks();
         int singRelativeTick = this.sequenceTick - singStartTick;

         for (int i = 0; i < SOUND_WAVE_TICKS.length && i < this.hornTargets.size(); ++i) {
            if (singRelativeTick == SOUND_WAVE_TICKS[i]) {
               this.launchSoundWave((BlockPos)this.hornTargets.get(i));
            }
         }

      }
   }

   private void launchSoundWave(BlockPos target) {
      Level var3 = this.level();
      if (var3 instanceof ServerLevel serverLevel) {
         BlockState targetState = serverLevel.getBlockState(target);
         if (targetState.is(ModBlocks.SONOROUS_DEEPSLATE)) {
            if (targetState.getValue(SonorousDeepslateBlock.MODE) == SonorousDeepslateBlock.Mode.HORN) {
               Vec3 start = new Vec3(this.getX(), this.getY() + (double)3.0F, this.getZ());
               Vec3 end = Vec3.atCenterOf(target);
               Vec3 delta = end.subtract(start);
               double distance = delta.length();
               if (distance < 0.001) {
                  this.convertHornToNote(serverLevel, target);
               } else {
                  Vec3 direction = delta.scale((double)1.0F / distance);
                  int travelTicks = Math.max(1, (int)Math.ceil(distance / 0.6));
                  double encodedMagnitude = (double)1.0F + (double)travelTicks / (double)100.0F;
                  serverLevel.sendParticles(ModParticles.SINGER_SOUND_WAVE, start.x, start.y, start.z, 0, direction.x * encodedMagnitude, direction.y * encodedMagnitude, direction.z * encodedMagnitude, (double)1.0F);
                  this.pendingHornActivations.add(new PendingHornActivation(target.immutable(), travelTicks));
               }
            }
         }
      }
   }

   private void tickPendingHornActivations() {
      if (!this.pendingHornActivations.isEmpty()) {
         Iterator<PendingHornActivation> iterator = this.pendingHornActivations.iterator();

         while(iterator.hasNext()) {
            PendingHornActivation pending = (PendingHornActivation)iterator.next();
            --pending.ticksLeft;
            if (pending.ticksLeft <= 0) {
               Level var4 = this.level();
               if (var4 instanceof ServerLevel) {
                  ServerLevel serverLevel = (ServerLevel)var4;
                  this.convertHornToNote(serverLevel, pending.pos);
               }

               iterator.remove();
            }
         }

      }
   }

   private void convertHornToNote(ServerLevel level, BlockPos pos) {
      BlockState hornState = level.getBlockState(pos);
      if (hornState.is(ModBlocks.SONOROUS_DEEPSLATE)) {
         if (hornState.getValue(SonorousDeepslateBlock.MODE) == SonorousDeepslateBlock.Mode.HORN) {
            level.setBlock(pos, (BlockState)hornState.setValue(SonorousDeepslateBlock.MODE, SonorousDeepslateBlock.Mode.NOTE), 3);
            level.levelEvent(2001, pos, Block.getId(hornState));
         }
      }
   }

   private void playAppearEffects() {
      if (this.portalCenter == null) {
         this.level().playSound((Entity)null, this.getX(), this.getY(), this.getZ(), ModSounds.THE_SIFT_PORTAL_OPEN, SoundSource.BLOCKS, 4.0F, 1.0F);
      }

      this.level().playSound((Entity)null, this.getX(), this.getY(), this.getZ(), ModSounds.SINGER_EMERGE, SoundSource.HOSTILE, 4.0F, 1.0F);
   }

   private void playDisappearEffects() {
      this.level().playSound((Entity)null, this.getX(), this.getY(), this.getZ(), ModSounds.SINGER_DIG, SoundSource.HOSTILE, 4.0F, 1.0F);
      if (this.portalCenter == null) {
         this.level().playSound((Entity)null, this.getX(), this.getY(), this.getZ(), ModSounds.THE_SIFT_PORTAL_CLOSE, SoundSource.BLOCKS, 4.0F, 1.0F);
      }

   }

   private void tickClientDiggingParticles() {
      if (this.isClientAppearOrDisappearPhase()) {
         RandomSource random = this.getRandom();
         boolean inSift = this.level().dimension().equals(TheSiftDimension.LEVEL_KEY);
         BlockParticleOption blockParticle = null;
         if (inSift) {
            BlockState stateBelow = this.getBlockStateOn();
            if (stateBelow.getRenderShape() == RenderShape.INVISIBLE) {
               return;
            }

            blockParticle = new BlockParticleOption(ParticleTypes.BLOCK, stateBelow);
         }

         for (int i = 0; i < 30; ++i) {
            double x = this.getX() + (double)Mth.randomBetween(random, -0.7F, 0.7F);
            double y = this.getY();
            double z = this.getZ() + (double)Mth.randomBetween(random, -0.7F, 0.7F);
            if (inSift) {
               this.level().addParticle(blockParticle, x, y, z, (double)0.0F, (double)0.0F, (double)0.0F);
            } else {
               this.level().addParticle(ModParticles.SIFT_PARALLAX, x, y, z, (double)0.0F, (double)0.0F, (double)0.0F);
            }
         }

      }
   }

   private boolean isClientAppearOrDisappearPhase() {
      if ((Boolean)this.entityData.get(DATA_SOUL_EVENT)) {
         SoulEventPhase phase = this.soulEventPhase();
         int phaseTick = (Integer)this.entityData.get(DATA_SOUL_EVENT_TICK);
         return (phase == SingerEntity.SoulEventPhase.APPEAR || phase == SingerEntity.SoulEventPhase.DISAPPEAR) && phaseTick < 90;
      } else if (!(Boolean)this.entityData.get(DATA_SEQUENCE_STARTED)) {
         return false;
      } else {
         int tick = (Integer)this.entityData.get(DATA_SEQUENCE_TICK);
         int disappearStart = this.timings.appearTicks() + this.timings.singTicks();
         int total = disappearStart + this.timings.disappearTicks();
         boolean emerging = tick < this.timings.appearTicks() && tick < 90;
         boolean digging = tick >= disappearStart && tick < total && tick - disappearStart < 90;
         return emerging || digging;
      }
   }

   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("sequence_started", this.started);
      output.putInt("sequence_tick", this.sequenceTick);
      output.putBoolean("sing_sound_played", this.singSoundPlayed);
      output.putBoolean("horn_targets_prepared", this.hornTargetsPrepared);
      output.putBoolean("disappear_sound_played", this.disappearSoundPlayed);
      output.putBoolean("soul_event", this.isSoulEvent());
      output.putInt("soul_event_phase", this.soulEventPhase().ordinal());
      output.putInt("soul_event_tick", this.soulEventTick);
      output.putBoolean("permanent_soul_event", this.permanentSoulEvent);
      if (this.soulEventGolem != null) {
         output.putString("soul_event_golem", this.soulEventGolem.toString());
      }

      output.putInt("horn_targets_version", 1);
      output.putInt("horn_target_count", this.hornTargets.size());

      for (int i = 0; i < this.hornTargets.size(); ++i) {
         output.putLong("horn_target_" + i, ((BlockPos)this.hornTargets.get(i)).asLong());
      }

      if (this.portalCenter != null) {
         output.putLong("portal_center", this.portalCenter.asLong());
      }

      output.putInt("pending_horn_count", this.pendingHornActivations.size());

      for (int i = 0; i < this.pendingHornActivations.size(); ++i) {
         PendingHornActivation pending = (PendingHornActivation)this.pendingHornActivations.get(i);
         output.putLong("pending_horn_pos_" + i, pending.pos.asLong());
         output.putInt("pending_horn_ticks_" + i, pending.ticksLeft);
      }

   }

   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      this.started = input.getBooleanOr("sequence_started", false);
      this.sequenceTick = input.getIntOr("sequence_tick", 0);
      this.entityData.set(DATA_SEQUENCE_STARTED, this.started);
      this.entityData.set(DATA_SEQUENCE_TICK, this.sequenceTick);
      this.singSoundPlayed = input.getBooleanOr("sing_sound_played", false);
      this.hornTargetsPrepared = input.getBooleanOr("horn_targets_prepared", false);
      this.disappearSoundPlayed = input.getBooleanOr("disappear_sound_played", false);
      boolean soulEvent = input.getBooleanOr("soul_event", false);
      this.entityData.set(DATA_SOUL_EVENT, soulEvent);
      int eventPhase = Mth.clamp(input.getIntOr("soul_event_phase", 0), 0, SingerEntity.SoulEventPhase.values().length - 1);
      this.entityData.set(DATA_SOUL_EVENT_PHASE, eventPhase);
      this.soulEventTick = Math.max(0, input.getIntOr("soul_event_tick", 0));
      this.permanentSoulEvent = input.getBooleanOr("permanent_soul_event", false);
      this.entityData.set(DATA_SOUL_EVENT_TICK, this.soulEventTick);
      String golemId = input.getStringOr("soul_event_golem", "");

      try {
         this.soulEventGolem = golemId.isEmpty() ? null : UUID.fromString(golemId);
      } catch (IllegalArgumentException var15) {
         this.soulEventGolem = null;
      }

      if (soulEvent) {
         SoulEventPhase loadedPhase = SingerEntity.SoulEventPhase.values()[eventPhase];
         boolean immaterial = loadedPhase == SingerEntity.SoulEventPhase.APPEAR || loadedPhase == SingerEntity.SoulEventPhase.DISAPPEAR;
         this.noPhysics = immaterial;
         this.setNoGravity(immaterial);
         this.setNoAi(immaterial);
         this.setPermanentlyInvulnerable(false);
      }

      int hornTargetsVersion = input.getIntOr("horn_targets_version", 0);
      int hornTargetCount = Math.max(0, Math.min(8, input.getIntOr("horn_target_count", 0)));
      List<BlockPos> loadedHornTargets = new ArrayList(hornTargetCount);

      for (int i = 0; i < hornTargetCount; ++i) {
         long packed = (Long)input.getLong("horn_target_" + i).orElse(0L);
         loadedHornTargets.add(BlockPos.of(packed));
      }

      this.hornTargets = List.copyOf(loadedHornTargets);
      this.restoreLegacyHornTargets = hornTargetsVersion == 0 && this.started && this.singSoundPlayed && this.sequenceTick < this.timings.appearTicks() + this.timings.singTicks();
      long packedPortalCenter = (Long)input.getLong("portal_center").orElse(Long.MIN_VALUE);
      this.portalCenter = packedPortalCenter == Long.MIN_VALUE ? null : BlockPos.of(packedPortalCenter);
      this.pendingHornActivations.clear();
      int pendingCount = Math.max(0, input.getIntOr("pending_horn_count", 0));

      for (int i = 0; i < pendingCount; ++i) {
         long packedPos = (Long)input.getLong("pending_horn_pos_" + i).orElse(0L);
         int ticksLeft = Math.max(0, input.getIntOr("pending_horn_ticks_" + i, 0));
         this.pendingHornActivations.add(new PendingHornActivation(BlockPos.of(packedPos), ticksLeft));
      }

      this.timings = SingerAnimationTimings.load();
   }

   protected void registerGoals() {
   }

   public boolean isPickable() {
      return this.isSoulEvent();
   }

   public boolean isPushable() {
      return this.isSoulEvent();
   }

   public boolean canBeCollidedWith(Entity entity) {
      return this.isSoulEvent() && super.canBeCollidedWith(entity);
   }

   public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
      if (this.isSoulEvent()) {
         return super.isInvulnerableTo(level, source);
      } else {
         return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
      }
   }

   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      if (this.isSoulEvent()) {
         return super.hurtServer(level, source, amount);
      } else {
         return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ? super.hurtServer(level, source, amount) : false;
      }
   }

   protected int getBaseExperienceReward(ServerLevel level) {
      return this.isSoulEvent() ? 8 + this.random.nextInt(5) : 0;
   }

   protected SoundEvent getAmbientSound() {
      return this.portalCenter != null ? null : ModSounds.SINGER_IDLE;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return this.portalCenter != null ? null : ModSounds.SINGER_HURT;
   }

   protected SoundEvent getDeathSound() {
      return this.portalCenter != null ? null : ModSounds.SINGER_DEATH;
   }

   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController("singer_sequence", 0, (state) -> {
         SingerEntity singer = (SingerEntity)state.animatable();
         if ((Boolean)singer.entityData.get(DATA_SOUL_EVENT)) {
            SoulEventPhase phase = singer.soulEventPhase();
            RawAnimation var10000;
            switch (phase.ordinal()) {
               case 0 -> var10000 = EVENT_APPEAR;
               case 1 -> var10000 = singer.isSoulEventWalkingForAnimation() ? EVENT_WALK : EVENT_IDLE;
               case 2 -> var10000 = EVENT_RECEIVE;
               case 3 -> var10000 = singer.isSoulEventWalkingForAnimation() ? EVENT_WALK : EVENT_IDLE;
               case 4 -> var10000 = EVENT_DISAPPEAR;
               default -> throw new MatchException((String)null, (Throwable)null);
            }

            RawAnimation eventAnimation = var10000;
            state.controller().setAnimation(eventAnimation);
            return PlayState.CONTINUE;
         } else if (!(Boolean)singer.entityData.get(DATA_SEQUENCE_STARTED)) {
            return PlayState.STOP;
         } else {
            AnimationController<GeoAnimatable> controller = state.controller();
            controller.setAnimation(SEQUENCE);
            double targetTime = (double)(Integer)singer.entityData.get(DATA_SEQUENCE_TICK) / (double)20.0F;
            double currentTime = controller.getCurrentTimelineTime();
            if (currentTime < (double)0.0F || Math.abs(currentTime - targetTime) > 0.2) {
               controller.setTimelineTime(targetTime);
            }

            return PlayState.CONTINUE;
         }
      }));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.animatableInstanceCache;
   }

   static {
      DATA_SEQUENCE_STARTED = SynchedEntityData.defineId(SingerEntity.class, EntityDataSerializers.BOOLEAN);
      DATA_SEQUENCE_TICK = SynchedEntityData.defineId(SingerEntity.class, EntityDataSerializers.INT);
      DATA_SOUL_EVENT = SynchedEntityData.defineId(SingerEntity.class, EntityDataSerializers.BOOLEAN);
      DATA_SOUL_EVENT_PHASE = SynchedEntityData.defineId(SingerEntity.class, EntityDataSerializers.INT);
      DATA_SOUL_EVENT_TICK = SynchedEntityData.defineId(SingerEntity.class, EntityDataSerializers.INT);
      EVENT_APPEAR = RawAnimation.begin().thenPlay("appear");
      EVENT_WALK = RawAnimation.begin().thenLoop("walk");
      EVENT_IDLE = RawAnimation.begin().thenLoop("idle");
      EVENT_RECEIVE = RawAnimation.begin().thenPlay("receive_soul");
      EVENT_DISAPPEAR = RawAnimation.begin().thenPlay("disappear");
      SOUND_WAVE_TICKS = new int[]{secondsToTicks(0.16666666666666666), secondsToTicks((double)1.25F), secondsToTicks(2.0833333333333335), secondsToTicks(2.8333333333333335), secondsToTicks((double)3.75F), secondsToTicks(4.583333333333333), secondsToTicks(4.916666666666667), secondsToTicks(5.833333333333333)};
   }

   private static final class PendingHornActivation {
      private final BlockPos pos;
      private int ticksLeft;

      private PendingHornActivation(BlockPos pos, int ticksLeft) {
         this.pos = pos;
         this.ticksLeft = ticksLeft;
      }
   }

   private static enum SoulEventPhase {
      APPEAR,
      APPROACH,
      EXCHANGE,
      ROAMING,
      DISAPPEAR;

      // $FF: synthetic method
      private static SoulEventPhase[] $values() {
         return new SoulEventPhase[]{APPEAR, APPROACH, EXCHANGE, ROAMING, DISAPPEAR};
      }
   }
}
