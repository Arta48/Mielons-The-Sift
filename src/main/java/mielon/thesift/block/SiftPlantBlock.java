package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public class SiftPlantBlock extends BushBlock {
   public SiftPlantBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   protected SiftPlantBlock(BlockBehaviour.Properties properties, boolean ignoredAllowVanillaVegetationSoil) {
      super(properties);
   }

   protected boolean mayPlaceOn(BlockState floor, @NonNull BlockGetter level, @NonNull BlockPos pos) {
      return floor.is(ModBlocks.SIFTSLATE) || floor.is(ModBlocks.SIFTSLATE_GROWTH) || floor.is(ModBlocks.HEALTHY_SCULK) || floor.is(ModBlocks.DRY_HEALTHY_SCULK) || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH) || floor.is(ModBlocks.OVERGROWN_WILLOW_FOLIAGE) || floor.is(BlockTags.SUPPORTS_VEGETATION);
   }
}
