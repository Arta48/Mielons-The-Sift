package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public final class DryHealthySculkGrowthBlock extends Block {
   public DryHealthySculkGrowthBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   protected void randomTick(@NonNull BlockState state, ServerLevel level, BlockPos pos, @NonNull RandomSource random) {
      BlockPos abovePos = pos.above();
      BlockState above = level.getBlockState(abovePos);
      if (!above.getFluidState().isEmpty() || above.isCollisionShapeFullBlock(level, abovePos) || level.getMaxLocalRawBrightness(abovePos) < 4) {
         level.setBlockAndUpdate(pos, ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState());
      }

   }

   protected @NonNull BlockState updateShape(@NonNull BlockState state, @NonNull LevelReader level, @NonNull ScheduledTickAccess scheduledTickAccess, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighbourPos, @NonNull BlockState neighbourState, @NonNull RandomSource random) {
      return direction != Direction.UP || !neighbourState.isCollisionShapeFullBlock(level, neighbourPos) && neighbourState.getFluidState().isEmpty() ? super.updateShape(state, level, scheduledTickAccess, pos, direction, neighbourPos, neighbourState, random) : ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState();
   }
}
