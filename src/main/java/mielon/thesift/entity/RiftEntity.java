package mielon.thesift.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.RiftAnimationClock;
import mielon.thesift.world.RiftDirectory;
import mielon.thesift.world.RiftLightCleanup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RiftEntity extends Entity {
   public static final int LIFETIME_TICKS = 6000;
   public static final int ANIMATION_TICKS = 16;
   private static final EntityDataAccessor DATA_TARGET_SIFT;
   private static final EntityDataAccessor DATA_LONG_ALONG_X;
   private static final EntityDataAccessor DATA_CLOSING;
   private static final EntityDataAccessor DATA_APPEARING;
   private static final EntityDataAccessor DATA_BORN;
   private static final EntityDataAccessor DATA_END;
   private long expiresAt;
   private UUID pairId = UUID.randomUUID();
   private BlockPos linkedPos;
   private long linkedUntil;
   private boolean appearSoundPlayed;
   private int closingTicks;
   private final List placedLights = new ArrayList(3);

   public RiftEntity(EntityType type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.setPermanentlyInvulnerable(true);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(DATA_TARGET_SIFT, false);
      builder.define(DATA_LONG_ALONG_X, true);
      builder.define(DATA_CLOSING, false);
      builder.define(DATA_APPEARING, false);
      builder.define(DATA_BORN, 0L);
      builder.define(DATA_END, 0L);
   }

   public void configure(boolean targetSift, boolean longAlongX, long expiresAt, UUID pairId) {
      this.entityData.set(DATA_TARGET_SIFT, targetSift);
      this.entityData.set(DATA_LONG_ALONG_X, longAlongX);
      this.entityData.set(DATA_APPEARING, true);
      this.expiresAt = expiresAt;
      this.pairId = pairId;
      this.entityData.set(DATA_BORN, this.level().getGameTime());
      this.entityData.set(DATA_END, expiresAt);
   }

   public boolean targetsSift() {
      return (Boolean)this.entityData.get(DATA_TARGET_SIFT);
   }

   public boolean isLongAlongX() {
      return (Boolean)this.entityData.get(DATA_LONG_ALONG_X);
   }

   public boolean isClosing() {
      return (Boolean)this.entityData.get(DATA_CLOSING);
   }

   public long getExpiresAt() {
      return this.expiresAt;
   }

   public void extendExpiresAt(long expiry) {
      if (expiry > this.expiresAt) {
         this.expiresAt = expiry;
         this.entityData.set(DATA_END, expiry);
         if (expiry - this.level().getGameTime() > 16L) {
            this.entityData.set(DATA_CLOSING, false);
            this.closingTicks = 0;
         }

      }
   }

   public UUID getPairId() {
      return this.pairId;
   }

   public BlockPos getLinkedPos() {
      return this.linkedUntil > 0L && this.level().getGameTime() >= this.linkedUntil ? null : this.linkedPos;
   }

   public void setLinkedPos(BlockPos linkedPos) {
      this.linkedPos = linkedPos == null ? null : linkedPos.immutable();
      this.linkedUntil = this.expiresAt;
   }

   public void setLinkedPos(BlockPos pos, long until) {
      this.setLinkedPos(pos);
      this.linkedUntil = until;
   }

   public BlockPos getAnchorPos() {
      return BlockPos.containing(this.getX(), this.getY(), this.getZ());
   }

   public AABB getPortalBounds() {
      return portalBoundsAt(this.position(), this.isLongAlongX());
   }

   private static AABB portalBoundsAt(Vec3 position, boolean longAlongX) {
      double halfX = longAlongX ? (double)4.5F : (double)0.5F;
      double halfZ = longAlongX ? (double)0.5F : (double)4.5F;
      return new AABB(position.x - halfX, position.y - (double)0.375F, position.z - halfZ, position.x + halfX, position.y + (double)3.875F, position.z + halfZ);
   }

   protected AABB makeBoundingBox(Vec3 position) {
      return portalBoundsAt(position, this.isLongAlongX());
   }

   public float getOpenScale(float partialTick) {
      if (this.isClosing()) {
         return RiftAnimationClock.progress(true, (double)((float)this.closingTicks + partialTick));
      } else {
         return !(Boolean)this.entityData.get(DATA_APPEARING) ? 1.0F : RiftAnimationClock.progress(false, (double)((float)this.tickCount + partialTick));
      }
   }

   public void tick() {
      this.setBoundingBox(this.getPortalBounds());
      super.tick();
      this.noPhysics = true;
      this.setDeltaMovement(Vec3.ZERO);
      this.setBoundingBox(this.getPortalBounds());
      if (this.level().isClientSide()) {
         if (this.isClosing()) {
            ++this.closingTicks;
         } else {
            this.closingTicks = 0;
         }

      } else {
         ServerLevel level = (ServerLevel)this.level();
         RiftDirectory.track(this);
         if (this.expiresAt <= 0L) {
            this.expiresAt = level.getGameTime() + 6000L;
            this.entityData.set(DATA_BORN, level.getGameTime());
            this.entityData.set(DATA_END, this.expiresAt);
         }

         if (!this.appearSoundPlayed) {
            this.appearSoundPlayed = true;
            this.playSound(ModSounds.RIFT_APPEAR, 2.0F, 1.0F);
         }

         if ((Boolean)this.entityData.get(DATA_APPEARING) && this.tickCount >= 16) {
            this.entityData.set(DATA_APPEARING, false);
         }

         if (this.placedLights.isEmpty()) {
            this.placeVanillaLights(level);
         }

         long remaining = this.expiresAt - level.getGameTime();
         if (remaining <= 16L && !this.isClosing()) {
            this.entityData.set(DATA_CLOSING, true);
            this.closingTicks = 0;
         }

         if (this.isClosing()) {
            ++this.closingTicks;
         }

         if (remaining <= 0L || this.closingTicks > 16) {
            this.discard();
         }

      }
   }

   private void placeVanillaLights(ServerLevel level) {
      BlockPos anchor = this.getAnchorPos();

      for (int offset : new int[]{-3, 0, 3}) {
         BlockPos lightPos = this.isLongAlongX() ? anchor.offset(offset, 2, 0) : anchor.offset(0, 2, offset);
         if (level.getBlockState(lightPos).isAir()) {
            level.setBlock(lightPos, (BlockState)Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
            this.placedLights.add(lightPos.immutable());
         }
      }

   }

   private void removeVanillaLights() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel level) {
         RiftLightCleanup.enqueue(level, this.placedLights);
         this.placedLights.clear();
      }
   }

   public void onRemoval(Entity.RemovalReason reason) {
      if (reason.shouldDestroy()) {
         this.removeVanillaLights();
      }

      if (!this.level().isClientSide()) {
         RiftDirectory.removed(this, reason.shouldDestroy());
      }

      super.onRemoval(reason);
   }

   protected void readAdditionalSaveData(ValueInput input) {
      this.entityData.set(DATA_TARGET_SIFT, input.getBooleanOr("TargetSift", false));
      this.entityData.set(DATA_LONG_ALONG_X, input.getBooleanOr("LongAlongX", true));
      this.entityData.set(DATA_CLOSING, input.getBooleanOr("Closing", false));
      this.expiresAt = input.getLongOr("ExpiresAt", 0L);
      this.entityData.set(DATA_END, this.expiresAt);
      this.entityData.set(DATA_BORN, input.getLongOr("BornAt", this.level().getGameTime() - 16L));
      this.pairId = parseUuid(input.getStringOr("PairId", ""), UUID.randomUUID());
      input.read("LinkedPos", BlockPos.CODEC).ifPresent(this::setLinkedPos);
      this.linkedUntil = input.getLongOr("LinkedUntil", this.expiresAt);
      this.appearSoundPlayed = input.getBooleanOr("AppearSoundPlayed", true);
      this.closingTicks = input.getIntOr("ClosingTicks", 0);
      this.placedLights.clear();
      input.listOrEmpty("PlacedLights", BlockPos.CODEC).forEach((p) -> this.placedLights.add(p.immutable()));
   }

   protected void addAdditionalSaveData(ValueOutput output) {
      output.putBoolean("TargetSift", this.targetsSift());
      output.putBoolean("LongAlongX", this.isLongAlongX());
      output.putBoolean("Closing", this.isClosing());
      output.putLong("ExpiresAt", this.expiresAt);
      output.putLong("BornAt", (Long)this.entityData.get(DATA_BORN));
      output.putString("PairId", this.pairId.toString());
      if (this.linkedPos != null) {
         output.store("LinkedPos", BlockPos.CODEC, this.linkedPos);
         output.putLong("LinkedUntil", this.linkedUntil);
      }

      output.putBoolean("AppearSoundPlayed", this.appearSoundPlayed);
      output.putInt("ClosingTicks", this.closingTicks);
      ValueOutput.TypedOutputList<BlockPos> lights = output.list("PlacedLights", BlockPos.CODEC);
      List var10000 = this.placedLights;
      Objects.requireNonNull(lights);
      var10000.forEach((pos) -> lights.add((BlockPos) pos));
   }

   private static UUID parseUuid(String value, UUID fallback) {
      try {
         return UUID.fromString(value);
      } catch (IllegalArgumentException var3) {
         return fallback;
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

   protected void doWaterSplashEffect() {
   }

   public boolean isPushedByFluid() {
      return false;
   }

   public AABB getFluidInteractionBox() {
      return new AABB(this.getX(), this.getY(), this.getZ(), this.getX(), this.getY(), this.getZ());
   }

   static {
      DATA_TARGET_SIFT = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
      DATA_LONG_ALONG_X = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
      DATA_CLOSING = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
      DATA_APPEARING = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
      DATA_BORN = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.LONG);
      DATA_END = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.LONG);
   }
}
