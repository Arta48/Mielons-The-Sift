package mielon.thesift.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mielon.thesift.block.entity.SonorousDeepslateBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class SonorousBeamRenderer implements BlockEntityRenderer<SonorousDeepslateBlockEntity, SonorousBeamRenderer.State> {
   private static final float MAX_BEAM_HEIGHT = 128.0F;
   private static final float BEAM_MIN = 0.35F;
   private static final float BEAM_MAX = 0.65F;
   private static final float BEAM_ALPHA = 0.55F;
   private static final float V_PER_BLOCK = 0.5F;
   private static final float SCROLL_TILES_PER_SECOND = 1.0F;
   private static final int FULL_BRIGHT = 15728880;

   public SonorousBeamRenderer(BlockEntityRendererProvider.Context context) {
   }

   public State createRenderState() {
      return new State();
   }

   public void extractRenderState(SonorousDeepslateBlockEntity blockEntity, State state, float tickProgress, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
      state.active = blockEntity.hasBeam();
      state.colorRGB = blockEntity.getBeamColor();
      state.growth = blockEntity.getBeamGrowth(tickProgress);
      extractWaterSegments(blockEntity, state);
      state.gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
      state.partialTick = tickProgress;
   }

   public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
      if (state.active && !(state.growth <= 0.0F)) {
         float red = (float)(state.colorRGB >> 16 & 255) / 255.0F;
         float green = (float)(state.colorRGB >> 8 & 255) / 255.0F;
         float blue = (float)(state.colorRGB & 255) / 255.0F;
         float bottomY = 1.0F;
         float topY = bottomY + 128.0F * state.growth;
         float beamHeight = topY - bottomY;
         float time = (float)state.gameTime + state.partialTick;
         float vScroll = -(time / 20.0F) * 1.0F;
         poseStack.pushPose();
         collector.submitCustomGeometry(poseStack, SonorousBeamRenderTypes.BEAM, (pose, vertexConsumer) -> renderBeamColumn(vertexConsumer, pose.pose(), 0.35F, bottomY, 0.35F, 0.65F, topY, 0.65F, red, green, blue, vScroll, beamHeight));
         if (state.waterSegmentCount > 0) {
            collector.submitCustomGeometry(poseStack, SonorousBeamRenderTypes.WATER_OVERLAY, (pose, vertexConsumer) -> {
               for (int i = 0; i < state.waterSegmentCount; ++i) {
                  float segmentBottom = state.waterSegments[i * 2];
                  float segmentTop = Math.min(state.waterSegments[i * 2 + 1], topY);
                  if (!(segmentTop <= segmentBottom)) {
                     renderBeamWaterOverlay(vertexConsumer, pose.pose(), segmentBottom, segmentTop, topY, red, green, blue, vScroll);
                  }
               }

            });
         }

         poseStack.popPose();
      }
   }

   private static void extractWaterSegments(SonorousDeepslateBlockEntity blockEntity, State state) {
      state.waterSegmentCount = 0;
      if (state.active && !(state.growth <= 0.0F) && blockEntity.getLevel() != null) {
         float topY = 1.0F + 128.0F * state.growth;
         int openSegment = -1;

         for (int offset = 1; offset <= 128 && !((float)offset >= topY); ++offset) {
            Fluid fluid = blockEntity.getLevel().getFluidState(blockEntity.getBlockPos().above(offset)).getType();
            boolean water = fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
            if (water) {
               if (openSegment < 0) {
                  openSegment = offset;
               }
            } else if (openSegment >= 0) {
               state.addWaterSegment((float)openSegment, Math.min((float)offset, topY));
               openSegment = -1;
            }
         }

         if (openSegment >= 0) {
            state.addWaterSegment((float)openSegment, topY);
         }

      }
   }

   private static void renderBeamWaterOverlay(VertexConsumer buffer, Matrix4f pose, float minY, float maxY, float beamTopY, float red, float green, float blue, float vScroll) {
      float vBottom = vScroll + (beamTopY - minY) * 0.5F;
      float vTop = vScroll + (beamTopY - maxY) * 0.5F;
      renderBeamColumnWithUv(buffer, pose, 0.35F, minY, 0.35F, 0.65F, maxY, 0.65F, red, green, blue, vTop, vBottom);
   }

   private static void renderBeamColumn(VertexConsumer buffer, Matrix4f pose, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float vScroll, float beamHeight) {
      renderBeamColumnWithUv(buffer, pose, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, vScroll, vScroll + beamHeight * 0.5F);
   }

   private static void renderBeamColumnWithUv(VertexConsumer buffer, Matrix4f pose, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float v0, float v1) {
      addVertex(buffer, pose, minX, minY, maxZ, 0.0F, v1, red, green, blue, 0.0F, 0.0F, 1.0F);
      addVertex(buffer, pose, maxX, minY, maxZ, 1.0F, v1, red, green, blue, 0.0F, 0.0F, 1.0F);
      addVertex(buffer, pose, maxX, maxY, maxZ, 1.0F, v0, red, green, blue, 0.0F, 0.0F, 1.0F);
      addVertex(buffer, pose, minX, maxY, maxZ, 0.0F, v0, red, green, blue, 0.0F, 0.0F, 1.0F);
      addVertex(buffer, pose, maxX, minY, minZ, 0.0F, v1, red, green, blue, 0.0F, 0.0F, -1.0F);
      addVertex(buffer, pose, minX, minY, minZ, 1.0F, v1, red, green, blue, 0.0F, 0.0F, -1.0F);
      addVertex(buffer, pose, minX, maxY, minZ, 1.0F, v0, red, green, blue, 0.0F, 0.0F, -1.0F);
      addVertex(buffer, pose, maxX, maxY, minZ, 0.0F, v0, red, green, blue, 0.0F, 0.0F, -1.0F);
      addVertex(buffer, pose, minX, minY, minZ, 0.0F, v1, red, green, blue, -1.0F, 0.0F, 0.0F);
      addVertex(buffer, pose, minX, minY, maxZ, 1.0F, v1, red, green, blue, -1.0F, 0.0F, 0.0F);
      addVertex(buffer, pose, minX, maxY, maxZ, 1.0F, v0, red, green, blue, -1.0F, 0.0F, 0.0F);
      addVertex(buffer, pose, minX, maxY, minZ, 0.0F, v0, red, green, blue, -1.0F, 0.0F, 0.0F);
      addVertex(buffer, pose, maxX, minY, maxZ, 0.0F, v1, red, green, blue, 1.0F, 0.0F, 0.0F);
      addVertex(buffer, pose, maxX, minY, minZ, 1.0F, v1, red, green, blue, 1.0F, 0.0F, 0.0F);
      addVertex(buffer, pose, maxX, maxY, minZ, 1.0F, v0, red, green, blue, 1.0F, 0.0F, 0.0F);
      addVertex(buffer, pose, maxX, maxY, maxZ, 0.0F, v0, red, green, blue, 1.0F, 0.0F, 0.0F);
   }

   private static void addVertex(VertexConsumer buffer, Matrix4f pose, float x, float y, float z, float u, float v, float red, float green, float blue, float normalX, float normalY, float normalZ) {
      buffer.addVertex(pose, x, y, z).setColor(red, green, blue, 0.55F).setUv(u, v).setLight(15728880).setNormal(normalX, normalY, normalZ);
   }

   public static class State extends BlockEntityRenderState {
      boolean active;
      int colorRGB;
      float growth;
      long gameTime;
      float partialTick;
      final float[] waterSegments = new float[256];
      int waterSegmentCount;

      void addWaterSegment(float minY, float maxY) {
         if (this.waterSegmentCount < this.waterSegments.length / 2) {
            this.waterSegments[this.waterSegmentCount * 2] = minY;
            this.waterSegments[this.waterSegmentCount * 2 + 1] = maxY;
            ++this.waterSegmentCount;
         }
      }
   }
}
