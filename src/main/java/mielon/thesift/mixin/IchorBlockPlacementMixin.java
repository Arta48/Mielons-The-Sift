package mielon.thesift.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mielon.thesift.fluid.IchorWaterlogging;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({BlockItem.class})
public abstract class IchorBlockPlacementMixin {
   @ModifyArg(
      method = {"place"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"
)},
      index = 1
   )
   private BlockState theSift$placeInIchor(BlockState state, @Local(argsOnly = true) BlockPlaceContext context) {
      return IchorWaterlogging.isIchor(context.getLevel().getFluidState(context.getClickedPos()).getType()) && IchorWaterlogging.canContain(state) ? IchorWaterlogging.fillState(state) : state;
   }
}
