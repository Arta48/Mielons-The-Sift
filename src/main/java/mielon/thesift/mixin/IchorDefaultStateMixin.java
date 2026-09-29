package mielon.thesift.mixin;

import mielon.thesift.fluid.IchorState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Block.class})
public abstract class IchorDefaultStateMixin {
   @Shadow
   private BlockState defaultBlockState;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void theSift$initialDefault(CallbackInfo ci) {
      this.defaultBlockState = IchorState.dryDefault(this.defaultBlockState);
   }

   @ModifyVariable(
      method = {"registerDefaultState"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private BlockState theSift$dryDefault(BlockState state) {
      return IchorState.dryDefault(state);
   }
}
