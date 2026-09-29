package mielon.thesift.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Set;
import mielon.thesift.block.entity.SiftPortalBlockEntity;
import mielon.thesift.client.compat.SiftShaderCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SiftPortalRenderer extends AbstractEndPortalRenderer<SiftPortalBlockEntity, EndPortalRenderState> {
   private static final Identifier SHADERPACK_TEXTURE = Identifier.fromNamespaceAndPath("the_sift", "textures/misc/sift_portal_shaderpack.png");
   private static final int FULL_BRIGHT = 15728880;

   public EndPortalRenderState createRenderState() {
      return new EndPortalRenderState();
   }

   public int getViewDistance() {
      return 512;
   }

   public void extractRenderState(SiftPortalBlockEntity blockEntity, EndPortalRenderState state, float tickProgress, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
      super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
   }

   public void submit(EndPortalRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
      if (!SiftShaderCompat.isShaderPackInUse()) {
         submitCube(state.facesToShow, SiftPortalRenderTypes.SIFT_PORTAL, poseStack, collector);
      } else {
         BlockPos blockPos = state.blockPos == null ? BlockPos.ZERO : state.blockPos;
         Vec3 cameraPos = cameraState.pos;
         Quaternionf inverseCamera = (new Quaternionf(cameraState.orientation)).conjugate();
         collector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(SHADERPACK_TEXTURE, false), (pose, buffer) -> drawShaderpackPortal(state.facesToShow, blockPos, cameraPos, inverseCamera, pose, buffer));
      }
   }

   private static void drawShaderpackPortal(Set faces, BlockPos blockPos, Vec3 cameraPos, Quaternionf inverseCamera, PoseStack.Pose pose, VertexConsumer buffer) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != null) {
         float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
         float seconds = ((float)Math.floorMod(minecraft.level.getGameTime(), 24000L) + partial) / 20.0F;
         float aspect = (float)minecraft.getWindow().getWidth() / (float)Math.max(1, minecraft.getWindow().getHeight());

         for (Direction direction : (Iterable<Direction>) faces) {
            switch (direction) {
               case DOWN:
                  face(blockPos, cameraPos, inverseCamera, pose, buffer, aspect, seconds, direction, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F);
                  break;
               case UP:
                  face(blockPos, cameraPos, inverseCamera, pose, buffer, aspect, seconds, direction, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F);
                  break;
               case NORTH:
                  face(blockPos, cameraPos, inverseCamera, pose, buffer, aspect, seconds, direction, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F);
                  break;
               case SOUTH:
                  face(blockPos, cameraPos, inverseCamera, pose, buffer, aspect, seconds, direction, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F);
                  break;
               case WEST:
                  face(blockPos, cameraPos, inverseCamera, pose, buffer, aspect, seconds, direction, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F);
                  break;
               case EAST:
                  face(blockPos, cameraPos, inverseCamera, pose, buffer, aspect, seconds, direction, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F);
            }
         }

      }
   }

   private static void face(BlockPos blockPos, Vec3 cameraPos, Quaternionf inverseCamera, PoseStack.Pose pose, VertexConsumer buffer, float aspect, float seconds, Direction normal, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
      vertex(blockPos, cameraPos, inverseCamera, pose, buffer, aspect, seconds, normal, x0, y0, z0);
      vertex(blockPos, cameraPos, inverseCamera, pose, buffer, aspect, seconds, normal, x1, y1, z1);
      vertex(blockPos, cameraPos, inverseCamera, pose, buffer, aspect, seconds, normal, x2, y2, z2);
      vertex(blockPos, cameraPos, inverseCamera, pose, buffer, aspect, seconds, normal, x3, y3, z3);
   }

   private static void vertex(BlockPos blockPos, Vec3 cameraPos, Quaternionf inverseCamera, PoseStack.Pose pose, VertexConsumer buffer, float aspect, float seconds, Direction normal, float x, float y, float z) {
      Minecraft minecraft = Minecraft.getInstance();
      double worldX = (double)((float)blockPos.getX() + x);
      double worldY = (double)((float)blockPos.getY() + y);
      double worldZ = (double)((float)blockPos.getZ() + z);
      Vec3 screen = minecraft.gameRenderer.projectPointToScreen(new Vec3(worldX, worldY, worldZ));
      float u = (float)screen.x * 0.5F * aspect * 0.65F + 0.5F;
      float v = (float)screen.y * 0.5F + 0.5F;
      Vector3f view = (new Vector3f((float)(worldX - cameraPos.x), (float)(worldY - cameraPos.y), (float)(worldZ - cameraPos.z))).rotate(inverseCamera);
      float denominator = Math.max(Math.abs(view.z), 1.0F);
      u += view.x / denominator * 0.055F + seconds * 0.006F;
      v += view.y / denominator * 0.055F - seconds * 0.004F;
      buffer.addVertex(pose.pose(), x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setLight(15728880).setNormal((float)normal.getStepX(), (float)normal.getStepY(), (float)normal.getStepZ());
   }
}
