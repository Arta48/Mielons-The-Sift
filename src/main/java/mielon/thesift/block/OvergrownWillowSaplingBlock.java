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
import org.jspecify.annotations.NonNull;

public final class OvergrownWillowSaplingBlock extends BushBlock implements BonemealableBlock {
   private static final VoxelShape SHAPE = Block.box(2.0F, 0.0F, 2.0F, 14.0F, 12.0F, 14.0F);

   public OvergrownWillowSaplingBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
      return SHAPE;
   }

   protected boolean mayPlaceOn(@NonNull BlockState floor, @NonNull BlockGetter level, @NonNull BlockPos pos) {
      return super.mayPlaceOn(floor, level, pos) || floor.is(ModBlocks.SIFTSLATE_GROWTH) || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH);
   }

   protected void randomTick(@NonNull BlockState state, ServerLevel level, BlockPos pos, @NonNull RandomSource random) {
      if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) {
         this.growTree(level, pos, state, random);
      }

   }

   public boolean isValidBonemealTarget(@NonNull LevelReader level, @NonNull BlockPos pos, @NonNull BlockState state, @NonNull BonemealSource source) {
      return true;
   }

   public boolean isBonemealSuccess(@NonNull Level level, RandomSource random, @NonNull BlockPos pos, @NonNull BlockState state, @NonNull BonemealSource source) {
      return random.nextFloat() < 0.45F;
   }

   public void performBonemeal(@NonNull ServerLevel level, @NonNull RandomSource random, @NonNull BlockPos pos, @NonNull BlockState state, @NonNull BonemealSource source) {
      this.growTree(level, pos, state, random);
   }

   private void growTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
      level.removeBlock(pos, false);
      if (!OvergrownWillowTreeFeature.growFromSapling(level, random, pos)) {
         level.setBlock(pos, state, 3);
      }

   }
}
