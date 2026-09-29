package mielon.thesift.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({SpeleothemBlock.class})
public interface SpeleothemInvoker {
   @Invoker("findTip")
   static BlockPos theSift$findTip(BlockState state, LevelAccessor level, BlockPos pos, int maximumDistance, boolean includeMergedTip) {
      throw new AssertionError();
   }
}
