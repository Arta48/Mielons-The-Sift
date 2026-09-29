package mielon.thesift.client.mixin;

import mielon.thesift.block.ModBlocks;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HangingSignEditScreen.class})
public abstract class WillowHangingSignEditScreenMixin {
   @Shadow
   @Final
   @Mutable
   private Identifier texture;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void theSift$willowBoard(SignBlockEntity sign, SignTextSlot textSlot, boolean filtered, CallbackInfo ci) {
      if (sign.getBlockState().is(ModBlocks.OVERGROWN_WILLOW_HANGING_SIGN) || sign.getBlockState().is(ModBlocks.OVERGROWN_WILLOW_WALL_HANGING_SIGN)) {
         this.texture = Identifier.fromNamespaceAndPath("the_sift", "textures/gui/hanging_signs/overgrown_willow.png");
      }

   }
}
