package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class SifterEntity extends Monster implements GeoEntity {
   private static final String ATTACK_CONTROLLER = "attack_controller";
   private static final String BITE_TRIGGER = "bite";
   private static final int BITE_ANIMATION_TICKS = 14;
   private static final int BITE_SOUND_TICK = 8;
   private static final int BITE_DAMAGE_TICK = 13;
   private static final int BITE_DURATION_TICKS = 14;
   private static final double BITE_REACH = (double)2.5F;
   private static final RawAnimation RUNNING = RawAnimation.begin().thenLoop("running");
   private static final RawAnimation BITE = RawAnimation.begin().thenPlay("attack");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private Entity biteTarget;
   private int biteAge = -1;

   public SifterEntity(EntityType type, Level level) {
      super(type, level);
      this.xpReward = 7;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, (double)40.0F).add(Attributes.MOVEMENT_SPEED, 0.31).add(Attributes.ATTACK_DAMAGE, (double)10.0F).add(Attributes.FOLLOW_RANGE, (double)28.0F).add(Attributes.STEP_HEIGHT, (double)1.0F);
   }

   public static boolean checkSifterSpawnRules(EntityType type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
      return level.getLevel().getDifficulty() != Difficulty.PEACEFUL && Mob.checkMobSpawnRules(type, level, reason, pos, random) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) && (pos.getY() >= level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - 1 || random.nextFloat() < 0.2F);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.18, false));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.95));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public boolean canAttack(LivingEntity target) {
      return this.level().getDifficulty() != Difficulty.PEACEFUL && super.canAttack(target);
   }

   public boolean doHurtTarget(ServerLevel level, Entity target) {
      if (this.biteAge < 0 && target instanceof LivingEntity living) {
         if (living.isAlive() && this.canAttack(living)) {
            this.biteTarget = target;
            this.biteAge = 0;
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.triggerAnim("attack_controller", "bite");
            return true;
         }
      }

      return false;
   }

   public void tick() {
      super.tick();
      if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
         this.setTarget((LivingEntity)null);
         this.setAggressive(false);
         this.biteAge = -1;
         this.biteTarget = null;
      } else {
         Level var2 = this.level();
         if (var2 instanceof ServerLevel) {
            ServerLevel level = (ServerLevel)var2;
            if (this.biteAge >= 0) {
               ++this.biteAge;
               this.getNavigation().stop();
               if (this.biteTarget != null && this.biteTarget.isAlive()) {
                  this.getLookControl().setLookAt(this.biteTarget, 30.0F, 30.0F);
               }

               if (this.biteAge == 8) {
                  this.playSound(ModSounds.SIFTER_ATTACK, 0.9F, 1.15F + this.random.nextFloat() * 0.15F);
               }

               if (this.biteAge == 13) {
                  this.applyBiteDamage(level);
               }

               if (this.biteAge >= 14) {
                  this.biteAge = -1;
                  this.biteTarget = null;
               }

               return;
            }
         }

      }
   }

   private void applyBiteDamage(ServerLevel level) {
      Entity var3 = this.biteTarget;
      if (var3 instanceof LivingEntity living) {
         if (living.level() == this.level() && living.isAlive() && this.canAttack(living) && !(this.distanceToSqr(living) > (double)6.25F) && this.getSensing().hasLineOfSight(living)) {
            super.doHurtTarget(level, living);
            return;
         }
      }

   }

   public int getMaxSpawnClusterSize() {
      return 5;
   }

   protected SoundEvent getAmbientSound() {
      return ModSounds.SIFTER_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.SIFTER_HURT;
   }

   protected SoundEvent getDeathSound() {
      return ModSounds.SIFTER_DEATH;
   }

   protected void playStepSound(BlockPos pos, BlockState state) {
      this.playSound(ModSounds.SIFTER_STEP, 0.12F, 0.9F + this.random.nextFloat() * 0.2F);
   }

   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add((new AnimationController("attack_controller", 3, (state) -> {
         AnimationController<SifterEntity> controller = state.controller();
         if (controller.isTriggeredAnimation("bite")) {
            controller.setAnimationSpeed((double)1.0F);
            if (controller.getCurrentTimelineTime() != (double)-2.0F) {
               return PlayState.CONTINUE;
            }

            controller.stopTriggeredAnimation();
         }

         if (this.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            controller.setAnimationSpeed((double)2.5F);
            controller.setAnimation(RUNNING);
            return PlayState.CONTINUE;
         } else {
            controller.setAnimationSpeed((double)1.0F);
            controller.reset();
            return PlayState.STOP;
         }
      })).triggerableAnim("bite", BITE).receiveTriggeredAnimations());
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
