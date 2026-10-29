package mielon.thesift.block;

import mielon.thesift.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

public final class SculkflowerCropBlock extends CropBlock {
   public static final IntegerProperty AGE;
   private static final VoxelShape[] SHAPES;

   public SculkflowerCropBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   protected @NonNull IntegerProperty getAgeProperty() {
      return AGE;
   }

   public int getMaxAge() {
      return 2;
   }

   public @NonNull BlockState getStateForAge(int age) {
      return age >= 2 ? ModBlocks.SCULKFLOWER.defaultBlockState() : this.defaultBlockState().setValue(AGE, age);
   }

   protected int getBonemealAgeIncrease(@NonNull Level level) {
      return 1;
   }

   protected boolean isRandomlyTicking(@NonNull BlockState state) {
      return this.getAge(state) >= 2 || super.isRandomlyTicking(state);
   }

   protected void randomTick(@NonNull BlockState state, @NonNull ServerLevel level, @NonNull BlockPos pos, @NonNull RandomSource random) {
      if (this.getAge(state) >= 2) {
         level.setBlockAndUpdate(pos, ModBlocks.SCULKFLOWER.defaultBlockState());
      } else {
         super.randomTick(state, level, pos, random);
      }
   }

   protected boolean mayPlaceOn(BlockState floor, @NonNull BlockGetter level, @NonNull BlockPos floorPos) {
      return floor.is(Blocks.FARMLAND) || floor.is(Blocks.SCULK);
   }

   protected @NonNull ItemLike getBaseSeedId() {
      return ModItems.SCULKFLOWER_SEEDS;
   }

   protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
      return SHAPES[this.getAge(state)];
   }

   protected void createBlockStateDefinition(StateDefinition.Builder builder) {
      builder.add(AGE);
   }

   static {
      AGE = BlockStateProperties.AGE_2;
      SHAPES = new VoxelShape[]{Block.column(10.0F, 0.0F, 4.0F), Block.column(14.0F, 0.0F, 13.0F), Block.column(16.0F, 0.0F, 16.0F)};
   }
}
