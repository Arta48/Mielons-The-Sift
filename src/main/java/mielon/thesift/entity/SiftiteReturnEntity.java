package mielon.thesift.entity;

import java.util.UUID;
import mielon.thesift.advancement.ModAdvancements;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class SiftiteReturnEntity extends Entity implements ItemSupplier {
   private static final EntityDataAccessor DATA_ITEM;
   private UUID ownerId;
   private long deliveryOrder;

   protected InterpolationHandler createInterpolationHandler() {
      return LinearInterpolationHandler.create(this, 3);
   }

   public SiftiteReturnEntity(EntityType type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.setPermanentlyInvulnerable(true);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(DATA_ITEM, ItemStack.EMPTY);
   }

   public void configure(UUID ownerId, ItemStack stack, long deliveryOrder) {
      this.ownerId = ownerId;
      this.deliveryOrder = deliveryOrder;
      this.entityData.set(DATA_ITEM, stack.copy());
   }

   public ItemStack getItem() {
      return (ItemStack)this.entityData.get(DATA_ITEM);
   }

   public void tick() {
      super.tick();
      this.noPhysics = true;
      Level var2 = this.level();
      if (var2 instanceof ServerLevel level) {
         ServerPlayer owner = this.ownerId == null ? null : level.getServer().getPlayerList().getPlayer(this.ownerId);
         if (owner != null && owner.isAlive() && owner.level() == level) {
            Vec3 center = owner.position().add((double)0.0F, (double)owner.getBbHeight() * 0.62, (double)0.0F);
            boolean waitingForEarlierItem = this.hasEarlierDelivery(level);
            ItemStack remaining = this.getItem().copy();
            if (!waitingForEarlierItem && this.distanceToSqr(center) <= 0.42250000000000004) {
               int countBeforeDelivery = remaining.getCount();
               owner.getInventory().add(remaining);
               if (remaining.getCount() < countBeforeDelivery) {
                  ModAdvancements.award(owner, "the_sift/keep_inventory");
               }

               if (remaining.isEmpty()) {
                  this.discard();
                  return;
               }

               if (remaining.getCount() != this.getItem().getCount()) {
                  this.entityData.set(DATA_ITEM, remaining);
               }
            }

            boolean inventoryFull = waitingForEarlierItem || !canFitAny(owner, this.getItem());
            Vec3 destination;
            if (inventoryFull) {
               double angle = (double)this.tickCount * 0.17 + (double)(this.getId() & 15) * 0.39;
               destination = center.add(Math.cos(angle) * 1.65, 0.35 + Math.sin(angle * 1.7) * 0.35, Math.sin(angle) * 1.65);
            } else {
               destination = center;
            }

            Vec3 delta = destination.subtract(this.position());
            Vec3 desired = delta.scale(inventoryFull ? 0.3 : 0.38);
            Vec3 velocity = this.getDeltaMovement().scale(0.65).add(desired.scale(0.35));
            double maxSpeed = inventoryFull ? 0.45 : (double)1.25F;
            if (velocity.lengthSqr() > maxSpeed * maxSpeed) {
               velocity = velocity.normalize().scale(maxSpeed);
            }

            this.setDeltaMovement(velocity);
            this.move(MoverType.SELF, velocity);
            if ((this.tickCount & 1) == 0) {
               level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 1, 0.03, 0.03, 0.03, 0.005);
            }

         } else {
            this.setDeltaMovement(Vec3.ZERO);
         }
      } else {
         this.getInterpolation().interpolate();
      }
   }

   private boolean hasEarlierDelivery(ServerLevel level) {
      for (SiftiteReturnEntity other : level.getEntitiesOfClass(SiftiteReturnEntity.class, this.getBoundingBox().inflate((double)32.0F), (candidate) -> candidate != this && !candidate.isRemoved() && candidate.ownerId != null && candidate.ownerId.equals(this.ownerId) && !candidate.getItem().isEmpty())) {
         if (other.deliveryOrder < this.deliveryOrder) {
            return true;
         }
      }

      return false;
   }

   private static boolean canFitAny(ServerPlayer player, ItemStack incoming) {
      for (int slot = 0; slot < 36; ++slot) {
         ItemStack existing = player.getInventory().getItem(slot);
         if (existing.isEmpty()) {
            return true;
         }

         if (ItemStack.isSameItemSameComponents(existing, incoming) && existing.getCount() < existing.getMaxStackSize()) {
            return true;
         }
      }

      return false;
   }

   protected void readAdditionalSaveData(ValueInput input) {
      this.ownerId = parseUuid(input.getStringOr("Owner", ""));
      this.deliveryOrder = input.getLongOr("DeliveryOrder", 0L);
      input.read("Item", ItemStack.CODEC).ifPresent((stack) -> this.entityData.set(DATA_ITEM, stack));
   }

   protected void addAdditionalSaveData(ValueOutput output) {
      if (this.ownerId != null) {
         output.putString("Owner", this.ownerId.toString());
      }

      output.putLong("DeliveryOrder", this.deliveryOrder);
      if (!this.getItem().isEmpty()) {
         output.store("Item", ItemStack.CODEC, this.getItem());
      }

   }

   private static UUID parseUuid(String value) {
      try {
         return UUID.fromString(value);
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }

   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      return false;
   }

   public boolean hurtClient(DamageSource source) {
      return false;
   }

   public boolean isAttackable() {
      return false;
   }

   public boolean isPickable() {
      return false;
   }

   public boolean canBeHitByProjectile() {
      return false;
   }

   public boolean isPushable() {
      return false;
   }

   public boolean canCollideWith(Entity other) {
      return false;
   }

   public boolean canBeCollidedWith(Entity other) {
      return false;
   }

   public boolean skipAttackInteraction(Entity attacker) {
      return true;
   }

   static {
      DATA_ITEM = SynchedEntityData.defineId(SiftiteReturnEntity.class, EntityDataSerializers.ITEM_STACK);
   }
}
