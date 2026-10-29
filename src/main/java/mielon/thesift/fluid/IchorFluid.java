package mielon.thesift.fluid;

import java.util.Optional;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import org.jspecify.annotations.NonNull;

public abstract class IchorFluid extends WaterFluid {
   private static final int REGENERATION_TICKS_AFTER_EXIT = 101;

   public @NonNull Fluid getFlowing() {
      return ModFluids.FLOWING_ICHOR;
   }

   public @NonNull Fluid getSource() {
      return ModFluids.ICHOR;
   }

   public @NonNull Item getBucket() {
      return ModFluids.ICHOR_BUCKET;
   }

   public @NonNull Optional<SoundEvent> getPickupSound() {
      return Optional.of(ModSounds.ICHOR_BUCKET_FILL);
   }

   protected void spreadTo(@NonNull LevelAccessor level, @NonNull BlockPos pos, @NonNull BlockState state, @NonNull Direction direction, @NonNull FluidState fluid) {
      if (IchorWaterlogging.canIchorlog(state)) {
         IchorWaterlogging.fill(level, pos, state);
      } else {
         super.spreadTo(level, pos, state, direction, fluid);
      }
   }

   public @NonNull BlockState createLegacyBlock(@NonNull FluidState state) {
      return ModFluids.ICHOR_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
   }

   public boolean isSame(@NonNull Fluid fluid) {
      return fluid == ModFluids.ICHOR || fluid == ModFluids.FLOWING_ICHOR;
   }

   protected boolean canConvertToSource(@NonNull ServerLevel level) {
      return false;
   }

   public int getSlopeFindDistance(@NonNull LevelReader level) {
      return 6;
   }

   protected @NonNull FluidState getNewLiquid(@NonNull ServerLevel level, @NonNull BlockPos pos, @NonNull BlockState state) {
      FluidState next = super.getNewLiquid(level, pos, state);
      return !next.isEmpty() && next.getType().isSame(this) && !next.isSource() && !(Boolean)next.getValue(FALLING) && next.getAmount() > 6 ? this.getFlowing(6, false) : next;
   }

   public int getDropOff(@NonNull LevelReader level) {
      return 1;
   }

   public int getTickDelay(@NonNull LevelReader level) {
      return 7;
   }

   public void animateTick(@NonNull Level level, @NonNull BlockPos pos, @NonNull FluidState state, @NonNull RandomSource random) {
      super.animateTick(level, pos, state, random);
      if (state.isSource() && level.getBlockState(pos.above()).isAir() && random.nextInt(8) == 0) {
         double surfaceY = (double) ((float) pos.getY() + state.getHeight(level, pos)) + 0.00625;
         level.addParticle(ModParticles.ICHOR_SURFACE_MIST, (double) pos.getX() + 0.1 + random.nextDouble() * 0.8, surfaceY, (double) pos.getZ() + 0.1 + random.nextDouble() * 0.8, 0.0F, 0.0F, 0.0F);
      }

   }

   protected void entityInside(@NonNull Level level, @NonNull BlockPos pos, @NonNull Entity entity, @NonNull InsideBlockEffectApplier effectApplier) {
      super.entityInside(level, pos, entity, effectApplier);
      entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.92, 0.94, 0.92));
      if (!level.isClientSide() && entity instanceof LivingEntity living) {
         living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 101, 0, true, false, true));
      }

   }

   public static final class Flowing extends IchorFluid {
      protected void createFluidStateDefinition(StateDefinition.@NonNull Builder builder) {
         super.createFluidStateDefinition(builder);
         builder.add(LEVEL);
      }

      public int getAmount(FluidState state) {
         return state.getValue(LEVEL);
      }

      public boolean isSource(@NonNull FluidState state) {
         return false;
      }
   }

   public static final class Source extends IchorFluid {
      public int getAmount(@NonNull FluidState state) {
         return 8;
      }

      public boolean isSource(@NonNull FluidState state) {
         return true;
      }
   }
}
