package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class SiftslateGrowthBlock extends Block {
   public static final BooleanProperty SNOWY;

   public SiftslateGrowthBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(SNOWY, false));
   }

   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      BlockPos abovePos = pos.above();
      BlockState above = level.getBlockState(abovePos);
      boolean ichorSnowCover = isIchorSnowCover(above);
      boolean submerged = !above.getFluidState().isEmpty();
      boolean coveredByFullBlock = above.isCollisionShapeFullBlock(level, abovePos);
      boolean tooDark = level.getMaxLocalRawBrightness(abovePos) < 4;
      if (ichorSnowCover || !submerged && !coveredByFullBlock && !tooDark) {
         if ((Boolean)state.getValue(SNOWY) != ichorSnowCover) {
            level.setBlockAndUpdate(pos, (BlockState)state.setValue(SNOWY, ichorSnowCover));
         }
      } else {
         level.setBlockAndUpdate(pos, ModBlocks.SIFTSLATE.defaultBlockState());
      }

   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
      if (direction == Direction.UP) {
         if (isIchorSnowCover(neighbourState)) {
            return (BlockState)state.setValue(SNOWY, true);
         } else {
            return neighbourState.isCollisionShapeFullBlock(level, neighbourPos) ? ModBlocks.SIFTSLATE.defaultBlockState() : (BlockState)state.setValue(SNOWY, false);
         }
      } else {
         return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighbourPos, neighbourState, random);
      }
   }

   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      super.onPlace(state, level, pos, oldState, movedByPiston);
      if (!level.isClientSide()) {
         boolean snowy = isIchorSnowCover(level.getBlockState(pos.above()));
         if ((Boolean)state.getValue(SNOWY) != snowy) {
            level.setBlockAndUpdate(pos, (BlockState)state.setValue(SNOWY, snowy));
         }
      }

   }

   public static boolean isIchorSnowCover(BlockState state) {
      return state.is(ModBlocks.ICHOR_SNOW) || state.is(ModBlocks.ICHOR_SNOW_BLOCK);
   }

   protected void createBlockStateDefinition(StateDefinition.Builder builder) {
      builder.add(new Property[]{SNOWY});
   }

   static {
      SNOWY = BlockStateProperties.SNOWY;
   }
}
