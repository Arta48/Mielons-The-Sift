package mielon.thesift.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import mielon.thesift.client.compat.SiftShaderCompat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class SonorousBeamRenderTypes {
   private static final Identifier BEAM_TEXTURE = id("textures/block/sonorous_deepslate_beam.png");
   private static final RenderPipeline BEAM_PIPELINE;
   private static final RenderPipeline WATER_OVERLAY_PIPELINE;
   public static final RenderType BEAM;
   public static final RenderType WATER_OVERLAY;

   private SonorousBeamRenderTypes() {
   }

   public static void initialize() {
      SiftShaderCompat.assignPipeline(BEAM_PIPELINE, "BEACON_BEAM");
      SiftShaderCompat.assignPipeline(WATER_OVERLAY_PIPELINE, "BEACON_BEAM");
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }

   static {
      BEAM_PIPELINE = RenderPipeline.builder(new RenderPipeline.Snippet[0]).withLocation(id("pipeline/sonorous_beam_depth")).withVertexShader(id("core/sonorous_beam")).withFragmentShader(id("core/sonorous_beam")).withBindGroupLayout(BindGroupLayouts.GLOBALS).withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS).withBindGroupLayout(BindGroupLayouts.PROJECTION).withBindGroupLayout(BindGroupLayouts.SAMPLER0).withVertexBinding(0, DefaultVertexFormat.BLOCK).withPrimitiveTopology(PrimitiveTopology.QUADS).withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), true)).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withCull(false).build();
      WATER_OVERLAY_PIPELINE = RenderPipeline.builder(new RenderPipeline.Snippet[0]).withLocation(id("pipeline/sonorous_beam_water_overlay")).withVertexShader(id("core/sonorous_beam")).withFragmentShader(id("core/sonorous_beam")).withBindGroupLayout(BindGroupLayouts.GLOBALS).withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS).withBindGroupLayout(BindGroupLayouts.PROJECTION).withBindGroupLayout(BindGroupLayouts.SAMPLER0).withVertexBinding(0, DefaultVertexFormat.BLOCK).withPrimitiveTopology(PrimitiveTopology.QUADS).withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false)).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withCull(false).build();
      BEAM = RenderType.create("the_sift_sonorous_beam", RenderSetup.builder(BEAM_PIPELINE).withTexture("Sampler0", BEAM_TEXTURE).createRenderSetup());
      WATER_OVERLAY = RenderType.create("the_sift_sonorous_beam_water_overlay", RenderSetup.builder(WATER_OVERLAY_PIPELINE).withTexture("Sampler0", BEAM_TEXTURE).createRenderSetup());
   }
}
