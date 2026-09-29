package mielon.thesift.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.RenderPass;
import mielon.thesift.client.render.SiftProceduralSkyRenderer;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SkyRenderer.class})
public abstract class SiftSkyRendererMixin {
   @Shadow
   @Final
   private RenderSystem.AutoStorageIndexBuffer quadIndices;

   @Inject(
      method = {"renderSunMoonAndStars"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$renderProceduralSky(RenderPass renderPass, PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         SiftProceduralSkyRenderer.render(poseStack, level, renderPass, this.quadIndices);
         ci.cancel();
      }
   }

   @Inject(
      method = {"close"},
      at = {@At("TAIL")}
   )
   private void theSift$closeProceduralSky(CallbackInfo ci) {
      SiftProceduralSkyRenderer.close();
   }
}
