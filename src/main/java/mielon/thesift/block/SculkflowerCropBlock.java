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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SculkflowerCropBlock extends CropBlock {
   public static final IntegerProperty AGE;
   private static final VoxelShape[] SHAPES;

   public SculkflowerCropBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   protected IntegerProperty getAgeProperty() {
      return AGE;
   }

   public int getMaxAge() {
      return 2;
   }

   public BlockState getStateForAge(int age) {
      return age >= 2 ? ModBlocks.SCULKFLOWER.defaultBlockState() : (BlockState)this.defaultBlockState().setValue(AGE, age);
   }

   protected int getBonemealAgeIncrease(Level level) {
      return 1;
   }

   protected boolean isRandomlyTicking(BlockState state) {
      return this.getAge(state) >= 2 || super.isRandomlyTicking(state);
   }

   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (this.getAge(state) >= 2) {
         level.setBlockAndUpdate(pos, ModBlocks.SCULKFLOWER.defaultBlockState());
      } else {
         super.randomTick(state, level, pos, random);
      }
   }

   protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos floorPos) {
      return floor.is(Blocks.FARMLAND) || floor.is(Blocks.SCULK);
   }

   protected ItemLike getBaseSeedId() {
      return ModItems.SCULKFLOWER_SEEDS;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES[this.getAge(state)];
   }

   protected void createBlockStateDefinition(StateDefinition.Builder builder) {
      builder.add(new Property[]{AGE});
   }

   static {
      AGE = BlockStateProperties.AGE_2;
      SHAPES = new VoxelShape[]{Block.column((double)10.0F, (double)0.0F, (double)4.0F), Block.column((double)14.0F, (double)0.0F, (double)13.0F), Block.column((double)16.0F, (double)0.0F, (double)16.0F)};
   }
}
