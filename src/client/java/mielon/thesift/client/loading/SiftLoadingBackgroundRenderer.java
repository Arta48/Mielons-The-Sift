package mielon.thesift.client.loading;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class SiftLoadingBackgroundRenderer {
   private static final RenderPipeline PORTAL_PIPELINE = createPipeline(false);
   private static final RenderPipeline RIFT_PIPELINE = createPipeline(true);

   private SiftLoadingBackgroundRenderer() {
   }

   public static void draw(GuiGraphicsExtractor graphics, int width, int height) {
      int clock = (int)(System.nanoTime() / 16666667L) & 16777215;
      boolean targetSift = !TheSiftDimension.LEVEL_KEY.equals(SiftLoadingScreenState.sourceDimension());
      Identifier texture = id("textures/entity/rift/" + (targetSift ? "sift" : "overworld") + ".png");
      RenderPipeline pipeline = SiftLoadingScreenState.isRift() ? RIFT_PIPELINE : PORTAL_PIPELINE;
      graphics.blit(pipeline, texture, 0, 0, 0.0F, 0.0F, width, height, 320, 320, 320, 320, -16777216 | clock);
   }

   private static RenderPipeline createPipeline(boolean rift) {
      RenderPipeline.Builder builder = RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.GUI_TEXTURED_SNIPPET}).withLocation(id(rift ? "pipeline/rift_loading" : "pipeline/portal_loading")).withFragmentShader(id("core/sift_loading"));
      if (rift) {
         builder.withShaderDefine("RIFT_LOADING");
      }

      return builder.build();
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }
}
