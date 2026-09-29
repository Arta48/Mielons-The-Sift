package mielon.thesift.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.render.RiftRenderTypes;
import mielon.thesift.entity.RiftEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class RiftRenderer extends EntityRenderer<RiftEntity, RiftRenderState> {
   private static final RiftMesh OPEN;
   private static final Identifier ENERGY_TEXTURE;
   private static final Identifier SIFT_TEXTURE;
   private static final Identifier OVERWORLD_TEXTURE;

   public RiftRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public RiftRenderState createRenderState() {
      return new RiftRenderState();
   }

   public void extractRenderState(RiftEntity entity, RiftRenderState state, float partial) {
      super.extractRenderState(entity, state, partial);
      state.openScale = entity.getOpenScale(partial);
      state.longAlongX = entity.isLongAlongX();
      state.targetSift = entity.targetsSift();
   }

   protected int getBlockLightLevel(RiftEntity entity, BlockPos pos) {
      return 15;
   }

   public void submit(RiftRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
      if (!(state.openScale <= 0.0F)) {
         RiftMesh mesh = state.openScale >= 1.0F ? OPEN : new RiftMesh(RiftAssetModel.INSTANCE.boxes(state.openScale));
         boolean axis = state.longAlongX;
         if (!SiftShaderCompat.isShaderPackInUse()) {
            collector.submitCustomGeometry(poses, state.targetSift ? RiftRenderTypes.SIFT : RiftRenderTypes.OVERWORLD, (pose, buffer) -> mesh.draw(buffer, pose, axis, false));
            collector.submitCustomGeometry(poses, RiftRenderTypes.GLOW, (pose, buffer) -> mesh.draw(buffer, pose, axis, true));
         } else {
            float seconds = shaderSeconds();
            float destinationAlpha = destinationAlpha(state.distanceToCameraSq);
            collector.submitCustomGeometry(poses, RenderTypes.beaconBeam(ENERGY_TEXTURE, false), (pose, buffer) -> {
               mesh.drawShaderpack(buffer, pose, axis, RiftMesh.ShaderpackPass.ENERGY, 1.0F, seconds);
               mesh.drawShaderpack(buffer, pose, axis, RiftMesh.ShaderpackPass.FRAME, 1.0F, seconds);
            });
            if (destinationAlpha > 0.001F) {
               Identifier destination = state.targetSift ? SIFT_TEXTURE : OVERWORLD_TEXTURE;
               collector.submitCustomGeometry(poses, RenderTypes.beaconBeam(destination, true), (pose, buffer) -> mesh.drawShaderpack(buffer, pose, axis, RiftMesh.ShaderpackPass.DESTINATION, destinationAlpha, seconds));
            }

            collector.submitCustomGeometry(poses, RenderTypes.beaconBeam(ENERGY_TEXTURE, true), (pose, buffer) -> mesh.drawShaderpack(buffer, pose, axis, RiftMesh.ShaderpackPass.GLOW, 0.4F, seconds));
         }
      }
   }

   private static float shaderSeconds() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null) {
         return 0.0F;
      } else {
         float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
         return ((float)minecraft.level.getGameTime() + partial) / 20.0F;
      }
   }

   private static float destinationAlpha(double distanceSquared) {
      float distance = (float)Math.sqrt(Math.max((double)0.0F, distanceSquared));
      float t = Math.max(0.0F, Math.min(1.0F, (distance - 3.0F) / 12.0F));
      float smooth = t * t * (3.0F - 2.0F * t);
      return (1.0F - smooth) * 0.78F;
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }

   static {
      OPEN = new RiftMesh(RiftAssetModel.INSTANCE.boxes(1.0F));
      ENERGY_TEXTURE = id("textures/misc/rift_energy_shaderpack.png");
      SIFT_TEXTURE = id("textures/entity/rift/sift.png");
      OVERWORLD_TEXTURE = id("textures/entity/rift/overworld.png");
   }
}
