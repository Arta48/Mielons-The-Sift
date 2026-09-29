package mielon.thesift.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class RiftRenderTypes {
   private static final RenderPipeline SURFACE_PIPELINE = createPipeline(false);
   private static final RenderPipeline GLOW_PIPELINE = createPipeline(true);
   public static final RenderType SIFT = createType(true, false);
   public static final RenderType OVERWORLD = createType(false, false);
   public static final RenderType GLOW = createType(true, true);

   private RiftRenderTypes() {
   }

   private static RenderPipeline createPipeline(boolean glow) {
      RenderPipeline.Builder builder = RenderPipeline.builder(new RenderPipeline.Snippet[0]).withLocation(id(glow ? "pipeline/rift_glow" : "pipeline/rift_surface")).withVertexShader(id("core/rift")).withFragmentShader(id("core/rift")).withBindGroupLayout(BindGroupLayouts.GLOBALS).withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS).withBindGroupLayout(BindGroupLayouts.PROJECTION).withBindGroupLayout(BindGroupLayouts.SAMPLER0).withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS).withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), !glow)).withColorTargetState(glow ? new ColorTargetState(BlendFunction.TRANSLUCENT) : ColorTargetState.DEFAULT).withCull(true);
      return builder.build();
   }

   private static RenderType createType(boolean sift, boolean glow) {
      Identifier texture = id("textures/entity/rift/" + (sift ? "sift" : "overworld") + ".png");
      return RenderType.create("sift_rift_" + sift + "_" + glow, RenderSetup.builder(glow ? GLOW_PIPELINE : SURFACE_PIPELINE).withTexture("Sampler0", texture).createRenderSetup());
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }
}
