package mielon.thesift.entity;

import java.util.HashSet;
import java.util.Set;
import mielon.thesift.mixin.SnifferStateAccess;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.sniffer.Sniffer.State;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DarkSnifferEntity extends Sniffer {
   private static final double AGGRO_RANGE = (double)32.0F;
   private static final double ATTACK_REACH = 3.15;
   private static final int ATTACK_COOLDOWN_TICKS = 24;
   private static final int SONIC_WINDUP_TICKS = 10;
   private static final int SONIC_COOLDOWN_TICKS = 100;
   private static final int SONIC_MIN_OUT_OF_REACH_TICKS = 8;
   private static final double SONIC_MIN_DISTANCE = (double)5.0F;
   private static final double SONIC_WAVE_SPEED = (double)1.5F;
   private static final double SONIC_HIT_RADIUS = 0.8;
   private static final double SONIC_OVERSHOOT_DISTANCE = (double)7.5F;
   private static final double SONIC_MAX_LEAD_TICKS = (double)20.0F;
   private static final double SONIC_PULL_STRENGTH = 0.3;
   private static final double NORMAL_SPEED = 0.1;
   private static final double WARDEN_CHASE_SPEED = 1.2;
   private int targetRefreshCooldown;
   private int pathRefreshCooldown;
   private int attackCooldown;
   private Warden wardenPetter;
   private int wardenPettingTicks;
   private int sonicCooldown;
   private int sonicWindupTicks;
   private int sonicFlightTicks;
   private int sonicFlightAge;
   private int ticksWithoutMeleeContact;
   private LivingEntity sonicVictim;
   private LivingEntity observedCombatTarget;
   private Vec3 sonicOrigin;
   private Vec3 sonicDirection;
   private final Set sonicPulledEntityIds;
   private double homeX;
   private double homeY;
   private double homeZ;
   private boolean homeSet;

   public DarkSnifferEntity(EntityType type, Level level) {
      super(type, level);
      this.sonicOrigin = Vec3.ZERO;
      this.sonicDirection = Vec3.ZERO;
      this.sonicPulledEntityIds = new HashSet();
      this.targetRefreshCooldown = level.getRandom().nextInt(10);
      this.pathRefreshCooldown = level.getRandom().nextInt(6);
      this.setPathfindingMalus(PathType.LEAVES, 0.0F);
      this.setPathfindingMalus(PathType.COCOA, 0.0F);
      this.setPathfindingMalus(PathType.DAMAGE_CAUTIOUS, 0.0F);
      PathNavigation var4 = this.getNavigation();
      if (var4 instanceof GroundPathNavigation navigation) {
         navigation.setCanPathToTargetsBelowSurface(true);
      }

   }

   public static AttributeSupplier.Builder createAttributes() {
      return Sniffer.createAttributes().add(Attributes.MAX_HEALTH, (double)40.0F).add(Attributes.MOVEMENT_SPEED, 0.1).add(Attributes.ATTACK_DAMAGE, (double)7.0F).add(Attributes.FOLLOW_RANGE, (double)32.0F).add(Attributes.STEP_HEIGHT, (double)1.25F).add(Attributes.MOVEMENT_EFFICIENCY, (double)1.0F).add(Attributes.ATTACK_KNOCKBACK, (double)0.75F);
   }

   protected boolean canBeABaby() {
      return false;
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

   public boolean canSniff() {
      return this.wardenPettingTicks <= 0 && !this.hasCombatTarget() && super.canSniff();
   }

   public boolean canAttack(LivingEntity target) {
      if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
         return false;
      } else {
         boolean var10000;
         label39: {
            if (target instanceof Player) {
               Player player = (Player)target;
               if (!player.isCreative() && !player.isSpectator()) {
                  var10000 = true;
                  break label39;
               }
            }

            var10000 = false;
         }

         boolean validPlayer = var10000;
         boolean ordinarySniffer = target instanceof Sniffer && !(target instanceof DarkSnifferEntity);
         return (validPlayer || ordinarySniffer) && super.canAttack(target);
      }
   }

   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }

   public void tick() {
      super.tick();
      Level var2 = this.level();
      if (var2 instanceof ServerLevel serverLevel) {
         if (this.attackCooldown > 0) {
            --this.attackCooldown;
         }

         if (this.sonicCooldown > 0) {
            --this.sonicCooldown;
         }

         if (!this.homeSet) {
            this.homeX = this.getX();
            this.homeY = this.getY();
            this.homeZ = this.getZ();
            this.homeSet = true;
         }

         if (this.wardenPettingTicks > 0) {
            this.cancelSonicSniff();
            --this.wardenPettingTicks;
            this.clearCombatTarget();
            this.transitionTo(State.IDLING);
            if (this.wardenPetter != null && this.wardenPetter.isAlive() && this.distanceToSqr(this.wardenPetter) <= (double)576.0F) {
               this.getLookControl().setLookAt(this.wardenPetter, 20.0F, 20.0F);
            } else {
               this.wardenPetter = null;
               this.wardenPettingTicks = 0;
            }

         } else if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.cancelSonicSniff();
            this.clearCombatTarget();
         } else if (this.sonicWindupTicks <= 0 && this.sonicFlightTicks <= 0) {
            LivingEntity target = this.getTarget();
            if (!this.isValidTarget(target)) {
               target = null;
               this.setTarget((LivingEntity)null);
            }

            if (--this.targetRefreshCooldown <= 0) {
               this.targetRefreshCooldown = 10;
               Player nearestPlayer = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), (double)32.0F, (entity) -> {
                  boolean var10000;
                  if (entity instanceof Player player) {
                     if (!player.isCreative() && !player.isSpectator() && player.isAlive()) {
                        var10000 = true;
                        return var10000;
                     }
                  }

                  var10000 = false;
                  return var10000;
               });
               Sniffer nearestSniffer = this.findNearestOrdinarySniffer(serverLevel);
               target = nearestPlayer;
               if (nearestSniffer != null && (nearestPlayer == null || this.distanceToSqr(nearestSniffer) < this.distanceToSqr(nearestPlayer))) {
                  target = nearestSniffer;
               }

               this.setTarget(target);
            }

            if (target == null) {
               this.observedCombatTarget = null;
               this.ticksWithoutMeleeContact = 0;
               this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.1);
               this.setAggressive(false);
               if (this.distanceToSqr(this.homeX, this.homeY, this.homeZ) > (double)10000.0F) {
                  if (this.getNavigation().isDone()) {
                     this.getNavigation().moveTo(this.homeX, this.homeY, this.homeZ, 0.1);
                  }
               } else if (this.getNavigation().isDone() && this.random.nextInt(30) == 0) {
                  this.getNavigation().moveTo(this.getX() + (double)this.random.nextInt(25) - (double)12.0F, this.getY(), this.getZ() + (double)this.random.nextInt(25) - (double)12.0F, 0.1);
               }

            } else {
               this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
               this.setAggressive(true);
               if (this.observedCombatTarget != target) {
                  this.observedCombatTarget = target;
                  this.ticksWithoutMeleeContact = 0;
               }

               double targetDistanceSqr = this.distanceToSqr(target);
               if (targetDistanceSqr > 9.9225) {
                  this.ticksWithoutMeleeContact = Math.min(8, this.ticksWithoutMeleeContact + 1);
               } else {
                  this.ticksWithoutMeleeContact = 0;
               }

               boolean validSonicRange = targetDistanceSqr >= (double)25.0F && this.getSensing().hasLineOfSight(target);
               if (this.sonicCooldown <= 0 && this.ticksWithoutMeleeContact >= 8 && validSonicRange) {
                  this.beginSonicSniff(target);
               } else {
                  this.transitionTo(State.IDLING);
                  this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                  if (--this.pathRefreshCooldown <= 0 || this.getNavigation().isDone()) {
                     this.pathRefreshCooldown = 6;
                     this.getNavigation().moveTo(target, 1.2);
                  }

                  if (this.horizontalCollision && this.onGround() && Math.abs(target.getY() - this.getY()) <= (double)3.0F) {
                     this.getJumpControl().jump();
                  }

                  if (this.attackCooldown <= 0 && this.distanceToSqr(target) <= 9.9225 && this.doHurtTarget(serverLevel, target)) {
                     this.attackCooldown = 24;
                     this.ticksWithoutMeleeContact = 0;
                  }

               }
            }
         } else {
            this.tickSonicSniff(serverLevel);
         }
      }
   }

   private void beginSonicSniff(LivingEntity target) {
      this.sonicVictim = target;
      this.sonicWindupTicks = 10;
      this.sonicFlightTicks = 0;
      this.sonicFlightAge = 0;
      this.sonicCooldown = 100;
      this.ticksWithoutMeleeContact = 0;
      this.getNavigation().stop();
      this.setDeltaMovement(Vec3.ZERO);
      this.getLookControl().setLookAt(target, 30.0F, 30.0F);
      this.setSnifferState(State.SNIFFING);
   }

   private void tickSonicSniff(ServerLevel serverLevel) {
      LivingEntity victim = this.sonicVictim;
      if (victim != null && victim.isAlive() && this.canAttack(victim)) {
         this.getNavigation().stop();
         this.setDeltaMovement(Vec3.ZERO);
         this.getLookControl().setLookAt(victim, 30.0F, 30.0F);
         if (this.sonicWindupTicks > 0) {
            --this.sonicWindupTicks;
            this.setSnifferState(State.SNIFFING);
            if (this.sonicWindupTicks == 0) {
               this.launchSonicWave(serverLevel, victim);
            }

         } else {
            this.setSnifferState(State.SNIFFING);
            Vec3 previousWavePosition = this.sonicOrigin.add(this.sonicDirection.scale((double)1.5F * (double)this.sonicFlightAge));
            ++this.sonicFlightAge;
            Vec3 wavePosition = this.sonicOrigin.add(this.sonicDirection.scale((double)1.5F * (double)this.sonicFlightAge));
            this.pullMobsAlongWave(serverLevel, previousWavePosition, wavePosition);
            if (victim.getBoundingBox().inflate(0.8).intersects(previousWavePosition, wavePosition)) {
               this.pullEntityOnce(victim);
            }

            if (this.sonicFlightAge >= this.sonicFlightTicks) {
               this.cancelSonicSniff();
               this.transitionTo(State.IDLING);
            }

         }
      } else {
         this.cancelSonicSniff();
         this.transitionTo(State.IDLING);
      }
   }

   private void launchSonicWave(ServerLevel serverLevel, LivingEntity victim) {
      Vec3 start = this.getEyePosition();
      Vec3 targetPosition = victim.getEyePosition();
      Vec3 directDelta = targetPosition.subtract(start);
      double directDistance = directDelta.length();
      if (!(directDistance < 0.001) && this.getSensing().hasLineOfSight(victim)) {
         double leadTicks = Math.min((double)20.0F, directDistance / (double)1.5F);
         Vec3 victimVelocity = victim.getDeltaMovement();
         Vec3 predictedTarget = targetPosition.add(victimVelocity.x() * leadTicks, (double)0.0F, victimVelocity.z() * leadTicks);
         Vec3 delta = predictedTarget.subtract(start);
         double predictedDistance = delta.length();
         if (predictedDistance < 0.001) {
            delta = directDelta;
            predictedDistance = directDistance;
         }

         this.sonicOrigin = start;
         this.sonicDirection = delta.scale((double)1.0F / predictedDistance);
         double travelDistance = predictedDistance + (double)7.5F;
         this.sonicFlightTicks = Math.max(1, (int)Math.ceil(travelDistance / (double)1.5F));
         this.sonicFlightAge = 0;
         this.setSnifferState(State.SNIFFING);
         this.playSound(ModSounds.DARK_SNIFFER_SNIFF, 2.5F, 0.78F);
         double encodedMagnitude = (double)1.0F + (double)this.sonicFlightTicks / (double)100.0F;
         serverLevel.sendParticles(ModParticles.SOUND_WAVE, start.x(), start.y(), start.z(), 0, this.sonicDirection.x() * encodedMagnitude, this.sonicDirection.y() * encodedMagnitude, this.sonicDirection.z() * encodedMagnitude, (double)1.0F);
      } else {
         this.cancelSonicSniff();
         this.transitionTo(State.IDLING);
      }
   }

   private void pullMobsAlongWave(ServerLevel serverLevel, Vec3 previousWavePosition, Vec3 wavePosition) {
      AABB searchBox = (new AABB(previousWavePosition, wavePosition)).inflate(0.8);

      for (Mob mob : serverLevel.getEntitiesOfClass(Mob.class, searchBox, (mobx) -> mobx != this && mobx.isAlive() && !this.sonicPulledEntityIds.contains(mobx.getId()))) {
         if (mob.getBoundingBox().inflate(0.8).intersects(previousWavePosition, wavePosition)) {
            this.pullEntityOnce(mob);
         }
      }

   }

   private void pullEntityOnce(LivingEntity victim) {
      if (victim != this && victim.isAlive() && this.sonicPulledEntityIds.add(victim.getId())) {
         Vec3 pull = (new Vec3(this.getX() - victim.getX(), this.getY() - victim.getY(), this.getZ() - victim.getZ())).scale(0.3);
         victim.setDeltaMovement(victim.getDeltaMovement().add(pull));
      }
   }

   private void cancelSonicSniff() {
      this.sonicWindupTicks = 0;
      this.sonicFlightTicks = 0;
      this.sonicFlightAge = 0;
      this.sonicVictim = null;
      this.sonicOrigin = Vec3.ZERO;
      this.sonicDirection = Vec3.ZERO;
      this.sonicPulledEntityIds.clear();
      this.ticksWithoutMeleeContact = 0;
   }

   private void setSnifferState(Sniffer.State state) {
      ((SnifferStateAccess)(Object)this).theSift$setState(state);
   }

   private boolean hasCombatTarget() {
      return this.isValidTarget(this.getTarget());
   }

   private boolean isValidTarget(LivingEntity target) {
      return target != null && target.isAlive() && this.canAttack(target) && this.distanceToSqr(target) <= (double)1024.0F;
   }

   private Sniffer findNearestOrdinarySniffer(ServerLevel level) {
      Sniffer nearest = null;
      double nearestDistance = (double)1024.0F;

      for (Sniffer candidate : level.getEntitiesOfClass(Sniffer.class, this.getBoundingBox().inflate((double)32.0F), (candidatex) -> candidatex != this && !(candidatex instanceof DarkSnifferEntity) && candidatex.isAlive())) {
         double distance = this.distanceToSqr(candidate);
         if (distance < nearestDistance) {
            nearest = candidate;
            nearestDistance = distance;
         }
      }

      return nearest;
   }

   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("TheSiftHomeSet", this.homeSet);
      if (this.homeSet) {
         output.putDouble("TheSiftHomeX", this.homeX);
         output.putDouble("TheSiftHomeY", this.homeY);
         output.putDouble("TheSiftHomeZ", this.homeZ);
      }

   }

   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      this.homeSet = input.getBooleanOr("TheSiftHomeSet", false);
      if (this.homeSet) {
         this.homeX = input.getDoubleOr("TheSiftHomeX", this.getX());
         this.homeY = input.getDoubleOr("TheSiftHomeY", this.getY());
         this.homeZ = input.getDoubleOr("TheSiftHomeZ", this.getZ());
      }

   }

   public boolean isAvailableForWardenPetting() {
      return this.isAlive() && !this.isAggressive() && !this.hasCombatTarget() && this.wardenPettingTicks <= 0;
   }

   public void beginWardenPetting(Warden warden, int durationTicks) {
      if (this.isAlive()) {
         this.wardenPetter = warden;
         this.wardenPettingTicks = Math.max(this.wardenPettingTicks, durationTicks);
         this.clearCombatTarget();
      }
   }

   public void endWardenPetting(Warden warden) {
      if (this.wardenPetter == warden) {
         this.wardenPetter = null;
         this.wardenPettingTicks = 0;
      }

   }

   private void clearCombatTarget() {
      this.setTarget((LivingEntity)null);
      this.setAggressive(false);
      this.getNavigation().stop();
   }
}
