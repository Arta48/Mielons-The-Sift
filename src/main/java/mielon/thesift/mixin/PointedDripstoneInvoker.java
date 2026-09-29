package mielon.thesift.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({PointedDripstoneBlock.class})
public interface PointedDripstoneInvoker {
   @Invoker("findFillableCauldronBelowStalactiteTip")
   static BlockPos theSift$findFillableCauldron(Level level, BlockPos tip, Fluid fluid) {
      throw new AssertionError();
   }
}
