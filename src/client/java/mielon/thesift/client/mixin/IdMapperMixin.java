package mielon.thesift.client.mixin;

import mielon.thesift.world.VanillaPaletteRemapper;
import net.minecraft.core.IdMapper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({IdMapper.class})
public abstract class IdMapperMixin {
   @Inject(
      method = {"byId"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$remapVanillaId(int id, CallbackInfoReturnable cir) {
      if (VanillaPaletteRemapper.isVanillaServer && (Object)this == Block.BLOCK_STATE_REGISTRY) {
         BlockState state = VanillaPaletteRemapper.getVanillaState(id);
         if (state != null) {
            cir.setReturnValue(state);
         }
      }
   }
}
