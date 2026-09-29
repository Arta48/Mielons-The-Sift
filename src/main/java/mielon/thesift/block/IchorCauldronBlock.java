package mielon.thesift.block;

import mielon.thesift.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.material.Fluid;

public final class IchorCauldronBlock extends LayeredCauldronBlock {
   public static final CauldronInteraction.Dispatcher INTERACTIONS = new CauldronInteraction.Dispatcher();

   public IchorCauldronBlock(BlockBehaviour.Properties properties) {
      super(Precipitation.NONE, INTERACTIONS, properties);
   }

   protected boolean canReceiveStalactiteDrip(Fluid fluid) {
      return fluid == ModFluids.ICHOR || fluid == ModFluids.FLOWING_ICHOR;
   }

   protected void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid) {
      if (this.canReceiveStalactiteDrip(fluid) && !this.isFull(state)) {
         BlockState next = (BlockState)state.setValue(LEVEL, (Integer)state.getValue(LEVEL) + 1);
         level.setBlockAndUpdate(pos, next);
         level.gameEvent(GameEvent.BLOCK_CHANGE, pos, Context.of(next));
         level.levelEvent(1047, pos, 0);
      }
   }
}
