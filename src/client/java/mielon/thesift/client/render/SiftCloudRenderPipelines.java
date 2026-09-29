package mielon.thesift.client.render;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.resources.Identifier;

public final class SiftCloudRenderPipelines {
   private static final Identifier SHADER = Identifier.fromNamespaceAndPath("the_sift", "core/rendertype_ichor_clouds");
   public static final RenderPipeline FANCY;
   public static final RenderPipeline FLAT;
   public static final OitPipelineSet OIT_FANCY;
   public static final OitPipelineSet OIT_FLAT;

   private SiftCloudRenderPipelines() {
   }

   public static void initialize() {
   }

   static {
      FANCY = RenderPipelines.register(RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.CLOUDS_SNIPPET}).withLocation(Identifier.fromNamespaceAndPath("the_sift", "pipeline/ichor_clouds")).withVertexShader(SHADER).withFragmentShader(SHADER).build());
      FLAT = RenderPipelines.register(RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.CLOUDS_SNIPPET}).withLocation(Identifier.fromNamespaceAndPath("the_sift", "pipeline/flat_ichor_clouds")).withVertexShader(SHADER).withFragmentShader(SHADER).withCull(false).build());
      OIT_FANCY = RenderPipelines.register(OitPipelineSet.builder("the_sift/ichor_clouds", RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.OIT_CLOUDS_SNIPPET}).withVertexShader(SHADER).withFragmentShader(SHADER)).build());
      OIT_FLAT = RenderPipelines.register(OitPipelineSet.builder("the_sift/flat_ichor_clouds", RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.OIT_CLOUDS_SNIPPET}).withVertexShader(SHADER).withFragmentShader(SHADER).withCull(false)).build());
   }
}
