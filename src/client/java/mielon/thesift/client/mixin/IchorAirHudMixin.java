package mielon.thesift.client.mixin;

import mielon.thesift.fluid.ModFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({Hud.class})
public abstract class IchorAirHudMixin {
   private static final Identifier VANILLA_AIR = Identifier.withDefaultNamespace("hud/air");
   private static final Identifier VANILLA_BURSTING = Identifier.withDefaultNamespace("hud/air_bursting");
   private static final Identifier ICHOR_AIR = id("hud/ichor_air");
   private static final Identifier ICHOR_BURSTING = id("hud/ichor_air_bursting");

   @ModifyArg(
      method = {"extractAirBubbles"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
)},
      index = 1
   )
   private Identifier theSift$useIchorAirSprite(Identifier original) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return original;
      } else {
         Fluid fluid = player.level().getFluidState(BlockPos.containing(player.getX(), player.getEyeY(), player.getZ())).getType();
         if (fluid != ModFluids.ICHOR && fluid != ModFluids.FLOWING_ICHOR) {
            return original;
         } else if (original.equals(VANILLA_AIR)) {
            return ICHOR_AIR;
         } else {
            return original.equals(VANILLA_BURSTING) ? ICHOR_BURSTING : original;
         }
      }
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }
}
