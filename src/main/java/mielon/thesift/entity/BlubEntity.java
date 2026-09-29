package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.Optional;
import java.util.UUID;
import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BlubEntity extends TamableAnimal implements GeoEntity {
   private static final double PACK_ALERT_RANGE = (double)16.0F;
   private static final int PACK_ANGER_TICKS = 600;
   private static final int FLAPPING_TICKS = 20;
   private static final String BODY_CONTROLLER = "body";
   private static final String HAPPY_TRIGGER = "happy";
   private static final EntityDataAccessor DATA_FLAPPING;
   private static final EntityDataAccessor DATA_SITTING;
   private static final RawAnimation HOPPING;
   private static final RawAnimation HAPPY;
   private static final RawAnimation FLAPPING;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private int angerTicks;
   private UUID angerTargetId;
   private int flappingTicks;
   private int nextFlappingTick;
   private int jumpTicks;
   private int jumpDuration;
   private int jumpDelayTicks;
   private int nextIdleSoundTick;
   private boolean wasOnGround;

   public BlubEntity(EntityType type, Level level) {
      super(type, level);
      this.jumpControl = new BlubJumpControl(this);
      this.moveControl = new BlubMoveControl(this);
      this.setSpeedModifier((double)0.0F);
      this.nextFlappingTick = 100 + this.random.nextInt(301);
      this.nextIdleSoundTick = 160 + this.random.nextInt(321);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, (double)20.0F).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, (double)3.0F).add(Attributes.FOLLOW_RANGE, (double)24.0F).add(Attributes.TEMPT_RANGE, (double)10.0F).add(Attributes.STEP_HEIGHT, (double)1.0F);
   }

   public static boolean checkBlubSpawnRules(EntityType type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
      return Mob.checkMobSpawnRules(type, level, reason, pos, random) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_FLAPPING, false);
      builder.define(DATA_SITTING, false);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, (stack) -> stack.getItem() == ModBlocks.SOUL_BLOCK_ITEM, false));
      this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1, 10.0F, 2.0F));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, (double)1.0F));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 7.0F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
      this.targetSelector.addGoal(3, new HurtByTargetGoal(this, new Class[0]));
   }

   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel) {
         if (this.flappingTicks > 0 && --this.flappingTicks == 0) {
            this.entityData.set(DATA_FLAPPING, false);
         }

         LivingEntity target = this.getTarget();
         boolean orderedSitting = this.isOrderedToSit();
         if ((Boolean)this.entityData.get(DATA_SITTING) != orderedSitting) {
            this.entityData.set(DATA_SITTING, orderedSitting);
         }

         if (this.isInSittingPose() != orderedSitting) {
            this.setInSittingPose(orderedSitting);
         }

         if (--this.nextIdleSoundTick <= 0) {
            if (target == null && this.isAlive()) {
               this.playAxolotlIdleSound(0.75F, 0.92F + this.random.nextFloat() * 0.22F);
            }

            this.nextIdleSoundTick = 180 + this.random.nextInt(361);
         }

         if (!this.isTame() && this.angerTicks > 0) {
            label99: {
               if (target == null && this.angerTargetId != null) {
                  Level var4 = this.level();
                  if (var4 instanceof ServerLevel) {
                     ServerLevel level = (ServerLevel)var4;
                     Player restoredTarget = level.getServer().getPlayerList().getPlayer(this.angerTargetId);
                     if (restoredTarget != null && restoredTarget.isAlive() && restoredTarget.level() == this.level() && !restoredTarget.isCreative() && !restoredTarget.isSpectator()) {
                        this.setTarget(restoredTarget);
                        this.setAggressive(true);
                        target = restoredTarget;
                     }
                  }
               }

               --this.angerTicks;
               if (this.angerTicks != 0 && (target == null || target.isAlive())) {
                  if (!(target instanceof Player)) {
                     break label99;
                  }

                  Player player = (Player)target;
                  if (!player.isCreative() && !player.isSpectator()) {
                     break label99;
                  }
               }

               this.setTarget((LivingEntity)null);
               this.setAggressive(false);
               this.angerTargetId = null;
            }
         }

         boolean canFlap = this.flappingTicks == 0 && this.getTarget() == null && this.getDeltaMovement().horizontalDistanceSqr() < 0.001;
         if (canFlap && --this.nextFlappingTick <= 0) {
            this.flappingTicks = 20;
            this.entityData.set(DATA_FLAPPING, true);
            this.nextFlappingTick = 140 + this.random.nextInt(401);
         } else if (!canFlap && this.flappingTicks == 0) {
            this.nextFlappingTick = Math.max(this.nextFlappingTick, 40);
         }

      }
   }

   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      boolean soulBlock = held.getItem() == ModBlocks.SOUL_BLOCK_ITEM;
      boolean edible = held.get(DataComponents.FOOD) != null;
      if (this.isTame() && (soulBlock || edible) && this.getHealth() < this.getMaxHealth()) {
         if (this.level() instanceof ServerLevel) {
            this.feed(player, hand, held, 1.0F, 4.0F);
            return InteractionResult.SUCCESS_SERVER;
         } else {
            return InteractionResult.SUCCESS;
         }
      } else if (!this.isTame() && soulBlock && this.getTarget() == null && this.angerTicks <= 0) {
         Level var7 = this.level();
         if (var7 instanceof ServerLevel) {
            ServerLevel level = (ServerLevel)var7;
            held.consume(1, player);
            if (this.random.nextInt(3) == 0) {
               this.tame(player);
               if (player instanceof ServerPlayer) {
                  ServerPlayer serverPlayer = (ServerPlayer)player;
                  ModAdvancements.award(serverPlayer, "the_sift/blub");
               }

               this.setPersistenceRequired();
               this.getNavigation().stop();
               this.setTarget((LivingEntity)null);
               this.setAggressive(false);
               this.angerTicks = 0;
               this.angerTargetId = null;
               this.setBlubSitting(false);
               this.setJumping(false);
               this.facePoint(player.getX(), player.getZ());
               this.setYBodyRot(this.getYRot());
               this.setYHeadRot(this.getYRot());
               this.playAxolotlIdleSound(0.95F, 1.08F);
               this.triggerAnim("body", "happy");
               level.broadcastEntityEvent(this, (byte)7);
            } else {
               level.broadcastEntityEvent(this, (byte)6);
            }

            return InteractionResult.SUCCESS_SERVER;
         } else {
            return InteractionResult.SUCCESS;
         }
      } else if (this.isTame() && this.isOwnedBy(player)) {
         if (this.level() instanceof ServerLevel) {
            this.setBlubSitting(!this.isBlubSitting());
            this.setJumping(false);
            this.getNavigation().stop();
            this.setTarget((LivingEntity)null);
            return InteractionResult.SUCCESS_SERVER;
         } else {
            return InteractionResult.SUCCESS;
         }
      } else {
         return super.mobInteract(player, hand);
      }
   }

   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      boolean hurt = super.hurtServer(level, source, amount);
      Entity attacker = source.getEntity();
      if (hurt && !this.isTame() && attacker instanceof Player player) {
         if (!player.isCreative() && !player.isSpectator()) {
            AABB alertArea = this.getBoundingBox().inflate((double)16.0F);

            for (BlubEntity blub : level.getEntitiesOfClass(BlubEntity.class, alertArea, (candidate) -> !candidate.isTame() && candidate.isAlive())) {
               blub.setBlubSitting(false);
               blub.setTarget(player);
               blub.setAggressive(true);
               blub.angerTicks = 600;
               blub.angerTargetId = player.getUUID();
               blub.getNavigation().moveTo(player, 1.2);
            }

            return true;
         }
      }

      return hurt;
   }

   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      if (!this.isTame() && this.angerTicks > 0) {
         output.putInt("TheSiftAngerTicks", this.angerTicks);
         if (this.angerTargetId != null) {
            output.putString("TheSiftAngryAt", this.angerTargetId.toString());
         }
      }

   }

   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      this.angerTicks = Math.max(0, input.getIntOr("TheSiftAngerTicks", 0));
      this.angerTargetId = (UUID)input.getString("TheSiftAngryAt").flatMap((value) -> {
         try {
            return Optional.of(UUID.fromString(value));
         } catch (IllegalArgumentException var2) {
            return Optional.empty();
         }
      }).orElse(null);
      if (this.isTame()) {
         this.angerTicks = 0;
         this.angerTargetId = null;
      }

   }

   public boolean canAttack(LivingEntity target) {
      if (target instanceof BlubEntity) {
         return false;
      } else {
         if (target instanceof OwnableEntity) {
            OwnableEntity pet = (OwnableEntity)target;
            if (pet.getOwner() != null) {
               return false;
            }
         }

         if (target instanceof Player) {
            Player player = (Player)target;
            if (player.isCreative() || player.isSpectator()) {
               return false;
            }
         }

         return !this.isOwnedBy(target) && super.canAttack(target);
      }
   }

   public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
      if (target instanceof BlubEntity) {
         return false;
      } else {
         if (target instanceof OwnableEntity) {
            OwnableEntity pet = (OwnableEntity)target;
            if (pet.getOwner() != null) {
               return false;
            }
         }

         return super.wantsToAttack(target, owner);
      }
   }

   private boolean isMovingForAnimation() {
      return this.isJumping() || !this.onGround() || this.getDeltaMovement().horizontalDistanceSqr() > 0.001;
   }

   public boolean isBlubSitting() {
      return (Boolean)this.entityData.get(DATA_SITTING);
   }

   private void setBlubSitting(boolean sitting) {
      this.setOrderedToSit(sitting);
      this.setInSittingPose(sitting);
      this.entityData.set(DATA_SITTING, sitting);
      if (sitting) {
         this.setJumping(false);
         this.getNavigation().stop();
         this.moveControl.setWait();
         this.setDeltaMovement((double)0.0F, this.getDeltaMovement().y, (double)0.0F);
      }

   }

   private double hoppingAnimationSpeed() {
      double horizontalSpeed = Math.sqrt(this.getDeltaMovement().horizontalDistanceSqr());
      return Mth.clamp(0.72 + horizontalSpeed * (double)7.0F, 0.72, 2.35);
   }

   public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
      return null;
   }

   public boolean canMate(Animal other) {
      return false;
   }

   public boolean isFood(ItemStack stack) {
      return false;
   }

   protected SoundEvent getAmbientSound() {
      return this.isInWater() ? ModSounds.BLUB_IDLE_WATER : ModSounds.BLUB_IDLE_AIR;
   }

   private void playAxolotlIdleSound(float volume, float pitch) {
      if (!this.level().isClientSide()) {
         this.level().playSound((Entity)null, this.blockPosition(), this.isInWater() ? ModSounds.BLUB_IDLE_WATER : ModSounds.BLUB_IDLE_AIR, this.getSoundSource(), volume, pitch);
      }

   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.BLUB_HURT;
   }

   protected SoundEvent getDeathSound() {
      return ModSounds.BLUB_DEATH;
   }

   protected SoundEvent getSwimSound() {
      return ModSounds.BLUB_SWIM;
   }

   protected SoundEvent getSwimSplashSound() {
      return ModSounds.BLUB_SPLASH;
   }

   public boolean doHurtTarget(ServerLevel level, Entity target) {
      boolean hit = super.doHurtTarget(level, target);
      if (hit) {
         this.playSound(ModSounds.BLUB_ATTACK, 0.7F, 0.95F + this.random.nextFloat() * 0.15F);
      }

      return hit;
   }

   protected void playStepSound(BlockPos pos, BlockState state) {
   }

   protected float getJumpPower() {
      float height = this.moveControl.getSpeedModifier() > 1.1 ? 0.36F : 0.3F;
      Path path = this.navigation.getPath();
      if (path != null && !path.isDone() && path.getNextEntityPos(this).y > this.getY() + (double)0.5F) {
         height = 0.5F;
      }

      if (this.horizontalCollision || this.moveControl.getWantedY() > this.getY() + (double)0.5F) {
         height = 0.5F;
      }

      return super.getJumpPower(height / 0.42F);
   }

   public void jumpFromGround() {
      super.jumpFromGround();
      if (this.moveControl.getSpeedModifier() > (double)0.0F && this.getDeltaMovement().horizontalDistanceSqr() < 0.01) {
         this.moveRelative(0.1F, new Vec3((double)0.0F, (double)0.0F, (double)1.0F));
      }

   }

   public void setJumping(boolean jumping) {
      super.setJumping(jumping);
   }

   private void startJumping() {
      this.setJumping(true);
      this.jumpDuration = 15;
      this.jumpTicks = 0;
   }

   private void setSpeedModifier(double speed) {
      this.navigation.setSpeedModifier(speed);
      this.moveControl.setWantedPosition(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ(), speed);
   }

   private void facePoint(double x, double z) {
      this.setYRot((float)(Mth.atan2(z - this.getZ(), x - this.getX()) * (180D / Math.PI)) - 90.0F);
   }

   private void setLandingDelay() {
      double speed = this.moveControl.getSpeedModifier();
      this.jumpDelayTicks = speed >= 1.15 ? 4 : (speed >= 1.05 ? 6 : 8);
      ((BlubJumpControl)this.jumpControl).setCanJump(false);
   }

   protected void customServerAiStep(ServerLevel level) {
      super.customServerAiStep(level);
      if (this.jumpDelayTicks > 0) {
         --this.jumpDelayTicks;
      }

      if (this.isBlubSitting()) {
         this.setJumping(false);
         this.moveControl.setWait();
         this.wasOnGround = this.onGround();
      } else {
         if (this.onGround()) {
            if (!this.wasOnGround) {
               this.setJumping(false);
               this.setLandingDelay();
            }

            BlubJumpControl jumps = (BlubJumpControl)this.jumpControl;
            if (!jumps.wantJump() && this.moveControl.hasWanted() && this.jumpDelayTicks == 0) {
               Vec3 destination = new Vec3(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ());
               Path path = this.navigation.getPath();
               if (path != null && !path.isDone()) {
                  destination = path.getNextEntityPos(this);
               }

               this.facePoint(destination.x, destination.z);
               this.startJumping();
            } else if (jumps.wantJump() && !jumps.canJump()) {
               jumps.setCanJump(true);
            }
         }

         this.wasOnGround = this.onGround();
      }
   }

   public void aiStep() {
      super.aiStep();
      if (this.jumpTicks != this.jumpDuration) {
         ++this.jumpTicks;
      } else if (this.jumpDuration != 0) {
         this.jumpTicks = 0;
         this.jumpDuration = 0;
         this.setJumping(false);
      }

   }

   public boolean canSpawnSprintParticle() {
      return false;
   }

   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add((new AnimationController("body", 2, (state) -> {
         BlubEntity blub = (BlubEntity)state.animatable();
         AnimationController<BlubEntity> controller = state.controller();
         if (controller.isTriggeredAnimation("happy")) {
            if (controller.getCurrentTimelineTime() != (double)-2.0F) {
               return PlayState.CONTINUE;
            }

            controller.stopTriggeredAnimation();
         }

         if (blub.isBlubSitting()) {
            controller.setAnimationSpeed((double)1.0F);
            controller.reset();
            return PlayState.STOP;
         } else if (blub.isMovingForAnimation()) {
            controller.setAnimationSpeed(blub.hoppingAnimationSpeed());
            controller.setAnimation(HOPPING);
            return PlayState.CONTINUE;
         } else {
            controller.setAnimationSpeed((double)1.0F);
            controller.reset();
            return PlayState.STOP;
         }
      })).triggerableAnim("happy", HAPPY).receiveTriggeredAnimations());
      controllers.add(new AnimationController("ears", 1, (state) -> {
         BlubEntity blub = (BlubEntity)state.animatable();
         if ((Boolean)blub.entityData.get(DATA_FLAPPING)) {
            state.controller().setAnimation(FLAPPING);
            return PlayState.CONTINUE;
         } else {
            state.controller().reset();
            return PlayState.STOP;
         }
      }));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   static {
      DATA_FLAPPING = SynchedEntityData.defineId(BlubEntity.class, EntityDataSerializers.BOOLEAN);
      DATA_SITTING = SynchedEntityData.defineId(BlubEntity.class, EntityDataSerializers.BOOLEAN);
      HOPPING = RawAnimation.begin().thenLoop("hopping");
      HAPPY = RawAnimation.begin().thenPlay("happy");
      FLAPPING = RawAnimation.begin().thenPlay("flapping");
   }

   private static final class BlubMoveControl extends MoveControl {
      private double nextJumpSpeed;

      private BlubMoveControl(BlubEntity blub) {
         super(blub);
      }

      public void tick() {
         BlubJumpControl jumps = (BlubJumpControl)((BlubEntity)this.mob).getJumpControl();
         if (((BlubEntity)this.mob).onGround() && !((BlubEntity)this.mob).isJumping() && !jumps.wantJump()) {
            ((BlubEntity)this.mob).setSpeedModifier((double)0.0F);
         } else if (this.hasWanted() || this.operation == Operation.JUMPING) {
            ((BlubEntity)this.mob).setSpeedModifier(this.nextJumpSpeed);
         }

         super.tick();
      }

      public void setWantedPosition(double x, double y, double z, double speed) {
         if (((BlubEntity)this.mob).isInWater()) {
            speed = (double)1.5F;
         }

         super.setWantedPosition(x, y, z, speed);
         if (speed > (double)0.0F) {
            this.nextJumpSpeed = speed;
         }

      }
   }

   private static final class BlubJumpControl extends JumpControl {
      private final BlubEntity blub;
      private boolean canJump;

      private BlubJumpControl(BlubEntity blub) {
         super(blub);
         this.blub = blub;
      }

      private boolean wantJump() {
         return this.jump;
      }

      private boolean canJump() {
         return this.canJump;
      }

      private void setCanJump(boolean canJump) {
         this.canJump = canJump;
      }

      public void tick() {
         if (this.jump) {
            this.blub.startJumping();
            this.jump = false;
         }

      }
   }
}
