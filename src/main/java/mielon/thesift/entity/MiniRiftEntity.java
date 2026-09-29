package mielon.thesift.entity;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class MiniRiftEntity extends Entity {
   public static final int ANIMATION_TICKS = 12;
   private static final int EMIT_INTERVAL = 5;
   private static final EntityDataAccessor DATA_CLOSING;
   private final Deque pending = new ArrayDeque();
   private UUID ownerId;
   private long batchOrder;
   private int nextDeliveryIndex;
   private int closingTicks;

   public MiniRiftEntity(EntityType type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.setPermanentlyInvulnerable(true);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(DATA_CLOSING, false);
   }

   public void configure(UUID ownerId, Collection stacks, long batchOrder) {
      this.ownerId = ownerId;
      this.batchOrder = Math.max(0L, batchOrder);
      this.nextDeliveryIndex = 0;

      for (ItemStack stack : (Iterable<ItemStack>) stacks) {
         if (!stack.isEmpty()) {
            this.pending.addLast(stack.copy());
         }
      }

   }

   public boolean isClosing() {
      return (Boolean)this.entityData.get(DATA_CLOSING);
   }

   public float getOpenScale(float partialTick) {
      return this.isClosing() ? Math.max(0.0F, 1.0F - ((float)this.closingTicks + partialTick) / 12.0F) : Math.min(1.0F, ((float)this.tickCount + partialTick) / 12.0F);
   }

   public void tick() {
      super.tick();
      this.noPhysics = true;
      this.setDeltaMovement(Vec3.ZERO);
      if (this.level().isClientSide()) {
         if (this.isClosing()) {
            ++this.closingTicks;
         }

      } else if (this.isClosing()) {
         if (++this.closingTicks > 12) {
            this.discard();
         }

      } else {
         if (this.tickCount >= 12 && this.tickCount % 5 == 0 && !this.pending.isEmpty()) {
            ItemStack stack = (ItemStack)this.pending.getFirst();
            SiftiteReturnEntity returning = new SiftiteReturnEntity(ModEntities.SIFTITE_RETURN, this.level());
            long deliveryOrder = (this.batchOrder << 16) + Integer.toUnsignedLong(this.nextDeliveryIndex);
            returning.configure(this.ownerId, stack, deliveryOrder);
            returning.setPos(this.getX(), this.getY(), this.getZ());
            if (this.level().addFreshEntity(returning)) {
               this.pending.removeFirst();
               ++this.nextDeliveryIndex;
            }
         }

         if (this.pending.isEmpty() && this.tickCount >= 12) {
            this.entityData.set(DATA_CLOSING, true);
            this.closingTicks = 0;
         }

      }
   }

   protected void readAdditionalSaveData(ValueInput input) {
      this.ownerId = parseUuid(input.getStringOr("Owner", ""));
      this.entityData.set(DATA_CLOSING, input.getBooleanOr("Closing", false));
      this.closingTicks = input.getIntOr("ClosingTicks", 0);
      this.batchOrder = input.getLongOr("BatchOrder", Math.max(0L, this.level().getGameTime()));
      this.nextDeliveryIndex = Math.max(0, input.getIntOr("NextDeliveryIndex", 0));
      input.listOrEmpty("Pending", ItemStack.CODEC).forEach((stack) -> {
         if (!stack.isEmpty()) {
            this.pending.addLast(stack);
         }

      });
   }

   protected void addAdditionalSaveData(ValueOutput output) {
      if (this.ownerId != null) {
         output.putString("Owner", this.ownerId.toString());
      }

      output.putBoolean("Closing", this.isClosing());
      output.putInt("ClosingTicks", this.closingTicks);
      output.putLong("BatchOrder", this.batchOrder);
      output.putInt("NextDeliveryIndex", this.nextDeliveryIndex);
      ValueOutput.TypedOutputList<ItemStack> list = output.list("Pending", ItemStack.CODEC);
      Deque var10000 = this.pending;
      Objects.requireNonNull(list);
      var10000.forEach((item) -> list.add((ItemStack) item));
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
      DATA_CLOSING = SynchedEntityData.defineId(MiniRiftEntity.class, EntityDataSerializers.BOOLEAN);
   }
}
