package mielon.thesift.block;

import mielon.thesift.worldgen.OvergrownWillowTreeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OvergrownWillowSaplingBlock extends BushBlock implements BonemealableBlock {
   private static final VoxelShape SHAPE = Block.box((double)2.0F, (double)0.0F, (double)2.0F, (double)14.0F, (double)12.0F, (double)14.0F);

   public OvergrownWillowSaplingBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
      return super.mayPlaceOn(floor, level, pos) || floor.is(ModBlocks.SIFTSLATE_GROWTH) || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH);
   }

   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) {
         this.growTree(level, pos, state, random);
      }

   }

   public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
      return true;
   }

   public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
      return random.nextFloat() < 0.45F;
   }

   public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
      this.growTree(level, pos, state, random);
   }

   private void growTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
      level.removeBlock(pos, false);
      if (!OvergrownWillowTreeFeature.growFromSapling(level, random, pos)) {
         level.setBlock(pos, state, 3);
      }

   }
}
