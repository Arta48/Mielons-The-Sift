package mielon.thesift.fluid;

import java.util.Optional;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;

public abstract class IchorFluid extends WaterFluid {
   private static final int REGENERATION_TICKS_AFTER_EXIT = 101;

   public Fluid getFlowing() {
      return ModFluids.FLOWING_ICHOR;
   }

   public Fluid getSource() {
      return ModFluids.ICHOR;
   }

   public Item getBucket() {
      return ModFluids.ICHOR_BUCKET;
   }

   public Optional getPickupSound() {
      return Optional.of(ModSounds.ICHOR_BUCKET_FILL);
   }

   protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState fluid) {
      if (IchorWaterlogging.canIchorlog(state)) {
         IchorWaterlogging.fill(level, pos, state);
      } else {
         super.spreadTo(level, pos, state, direction, fluid);
      }
   }

   public BlockState createLegacyBlock(FluidState state) {
      return (BlockState)ModFluids.ICHOR_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
   }

   public boolean isSame(Fluid fluid) {
      return fluid == ModFluids.ICHOR || fluid == ModFluids.FLOWING_ICHOR;
   }

   protected boolean canConvertToSource(ServerLevel level) {
      return false;
   }

   public int getSlopeFindDistance(LevelReader level) {
      return 6;
   }

   protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state) {
      FluidState next = super.getNewLiquid(level, pos, state);
      return !next.isEmpty() && next.getType().isSame(this) && !next.isSource() && !(Boolean)next.getValue(FALLING) && next.getAmount() > 6 ? this.getFlowing(6, false) : next;
   }

   public int getDropOff(LevelReader level) {
      return 1;
   }

   public int getTickDelay(LevelReader level) {
      return 7;
   }

   public void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
      super.animateTick(level, pos, state, random);
      if (state.isSource() && level.getBlockState(pos.above()).isAir() && random.nextInt(8) == 0) {
         double surfaceY = (double)((float)pos.getY() + state.getHeight(level, pos)) + 0.00625;
         level.addParticle(ModParticles.ICHOR_SURFACE_MIST, (double)pos.getX() + 0.1 + random.nextDouble() * 0.8, surfaceY, (double)pos.getZ() + 0.1 + random.nextDouble() * 0.8, (double)0.0F, (double)0.0F, (double)0.0F);
      }

   }

   protected void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier) {
      super.entityInside(level, pos, entity, effectApplier);
      entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.92, 0.94, 0.92));
      if (!level.isClientSide() && entity instanceof LivingEntity living) {
         living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 101, 0, true, false, true));
      }

   }

   public static final class Flowing extends IchorFluid {
      protected void createFluidStateDefinition(StateDefinition.Builder builder) {
         super.createFluidStateDefinition(builder);
         builder.add(new Property[]{LEVEL});
      }

      public int getAmount(FluidState state) {
         return (Integer)state.getValue(LEVEL);
      }

      public boolean isSource(FluidState state) {
         return false;
      }
   }

   public static final class Source extends IchorFluid {
      public int getAmount(FluidState state) {
         return 8;
      }

      public boolean isSource(FluidState state) {
         return true;
      }
   }
}
