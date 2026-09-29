package mielon.thesift.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class SiftPortalRenderTypes {
   private static final Identifier PIPELINE_ID = Identifier.fromNamespaceAndPath("the_sift", "pipeline/sift_portal");
   private static final Identifier SHADER_ID = Identifier.fromNamespaceAndPath("the_sift", "core/sift_portal");
   private static final Identifier MIST_TEXTURE = Identifier.fromNamespaceAndPath("the_sift", "textures/block/sift_portal_mist.png");
   private static final RenderPipeline SIFT_PORTAL_PIPELINE;
   public static final RenderType SIFT_PORTAL;

   private SiftPortalRenderTypes() {
   }

   static {
      SIFT_PORTAL_PIPELINE = RenderPipeline.builder(new RenderPipeline.Snippet[0]).withLocation(PIPELINE_ID).withVertexShader(SHADER_ID).withFragmentShader(SHADER_ID).withBindGroupLayout(BindGroupLayouts.GLOBALS).withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS).withBindGroupLayout(BindGroupLayouts.PROJECTION).withBindGroupLayout(BindGroupLayouts.FOG).withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1).withVertexBinding(0, DefaultVertexFormat.POSITION).withPrimitiveTopology(PrimitiveTopology.QUADS).withDepthStencilState(DepthStencilState.DEFAULT).withColorTargetState(ColorTargetState.DEFAULT).withCull(true).withShaderDefine("PORTAL_LAYERS", 15).build();
      SIFT_PORTAL = RenderType.create("the_sift_portal", RenderSetup.builder(SIFT_PORTAL_PIPELINE).withTexture("Sampler0", MIST_TEXTURE).withTexture("Sampler1", MIST_TEXTURE).createRenderSetup());
   }
}
