package mielon.thesift.client.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.render.SiftCloudRenderPipelines;
import mielon.thesift.world.SiftDayNightCycle;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({CloudRenderer.class})
public abstract class SiftCloudRendererMixin {
   @ModifyVariable(
      method = {"prepare(ILnet/minecraft/client/CloudStatus;FILnet/minecraft/world/phys/Vec3;JF)V"},
      at = {@At("HEAD")},
      argsOnly = true,
      ordinal = 0
   )
   private int theSift$fadeCloudsWithRain(int originalColor) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         float rain = level.getRainLevel(1.0F);
         int originalAlpha = originalColor >>> 24 & 255;
         int fadedAlpha = Math.round((float)originalAlpha * rain * 0.66F);
         if (SiftShaderCompat.isShaderPackInUse()) {
            return fadedAlpha << 24 | originalColor & 16777215;
         } else {
            float night = SiftDayNightCycle.nightBlend(level.getDefaultClockTime());
            int brightness = Math.round(255.0F * (1.0F - night * 0.24F));
            return fadedAlpha << 24 | brightness << 16 | brightness << 8 | brightness;
         }
      } else {
         return originalColor;
      }
   }

   @ModifyVariable(
      method = {"render(Lcom/mojang/renderpearl/api/commands/RenderPass;Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;)V"},
      at = {@At("HEAD")},
      argsOnly = true,
      ordinal = 0
   )
   private RenderPipeline theSift$useAnimatedIchorClouds(RenderPipeline original) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY) && !SiftShaderCompat.isShaderPackInUse()) {
         if (original == RenderPipelines.FLAT_CLOUDS) {
            return SiftCloudRenderPipelines.FLAT;
         } else {
            return original == RenderPipelines.CLOUDS ? SiftCloudRenderPipelines.FANCY : original;
         }
      } else {
         return original;
      }
   }

   @Redirect(
      method = {"renderOit"},
      at = {@At(
   value = "FIELD",
   target = "Lnet/minecraft/client/renderer/RenderPipelines;OIT_CLOUDS:Lnet/minecraft/client/renderer/oit/OitPipelineSet;"
)}
   )
   private OitPipelineSet theSift$useAnimatedIchorOitClouds() {
      ClientLevel level = Minecraft.getInstance().level;
      return level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY) && !SiftShaderCompat.isShaderPackInUse() ? SiftCloudRenderPipelines.OIT_FANCY : RenderPipelines.OIT_CLOUDS;
   }

   @Redirect(
      method = {"renderOit"},
      at = {@At(
   value = "FIELD",
   target = "Lnet/minecraft/client/renderer/RenderPipelines;OIT_FLAT_CLOUDS:Lnet/minecraft/client/renderer/oit/OitPipelineSet;"
)}
   )
   private OitPipelineSet theSift$useAnimatedFlatIchorOitClouds() {
      ClientLevel level = Minecraft.getInstance().level;
      return level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY) && !SiftShaderCompat.isShaderPackInUse() ? SiftCloudRenderPipelines.OIT_FLAT : RenderPipelines.OIT_FLAT_CLOUDS;
   }
}
