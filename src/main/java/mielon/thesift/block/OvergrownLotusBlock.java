package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

public final class OvergrownLotusBlock extends SiftPlantBlock {
   private static final VoxelShape SHAPE = Block.box(0.0F, 0.0F, 0.0F, 16.0F, 4.0F, 16.0F);

   public OvergrownLotusBlock(BlockBehaviour.Properties properties) {
      super(properties, true);
   }

   protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
      return SHAPE;
   }
}
