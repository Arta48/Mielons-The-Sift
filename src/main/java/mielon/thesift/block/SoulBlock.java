package mielon.thesift.block;

import mielon.thesift.particle.ModParticles;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class SoulBlock extends Block {
   public SoulBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      super.onPlace(state, level, pos, oldState, movedByPiston);
      if (!level.isClientSide() && !oldState.is(this)) {
         SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SOUL_CANYON_SOUL_BLOCK, pos);
      }

   }

   protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
      super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
      SiftLandmarkTracker.forget(SiftLandmarkTracker.Kind.SOUL_CANYON_SOUL_BLOCK, pos);
   }

   protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
      super.spawnAfterBreak(state, level, pos, tool, dropExperience);
      RandomSource random = level.getRandom();
      level.sendParticles(ModParticles.CANYON_SOUL, (double)pos.getX() - (double)1.75F + random.nextDouble() * (double)4.5F, (double)pos.getY() + 0.95, (double)pos.getZ() - (double)1.75F + random.nextDouble() * (double)4.5F, 1, 0.04, 0.01, 0.04, (double)0.0F);
   }
}
