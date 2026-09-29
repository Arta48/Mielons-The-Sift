package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusFlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class SculkflowerBlock extends CactusFlowerBlock {
   public SculkflowerBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos floorPos) {
      return floor.is(Blocks.FARMLAND) || floor.is(Blocks.SCULK) || super.mayPlaceOn(floor, level, floorPos);
   }
}
