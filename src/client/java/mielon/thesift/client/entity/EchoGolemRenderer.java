package mielon.thesift.client.entity;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import mielon.thesift.client.light.EchoGolemLightBridge;
import mielon.thesift.entity.EchoGolemEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;

public final class EchoGolemRenderer extends GeoEntityRenderer {
   static final DataTicket ECHO_GOLEM_ENTITY = DataTicket.create("the_sift_echo_golem_entity", EchoGolemEntity.class);

   public EchoGolemRenderer(EntityRendererProvider.Context context) {
      super(context, new EchoGolemModel());
      this.withRenderLayer(new EchoGolemSoulGlowLayer(this));
      this.withRenderLayer(new SiftGlowingOutlineLayer(this));
      this.shadowRadius = 0.72F;
   }

   public void addRenderData(EchoGolemEntity animatable, Void relatedObject, EntityRenderState renderState, float partialTick) {
      super.addRenderData(animatable, relatedObject, renderState, partialTick);
      renderState.addGeckolibData(ECHO_GOLEM_ENTITY, animatable);
      EchoGolemLightBridge.update(animatable, animatable.getPosition(partialTick).add((double)0.0F, (double)2.125F, (double)0.0F));
   }

   public void preRenderPass(RenderPassInfo renderPassInfo, SubmitNodeCollector renderTasks) {
      EchoGolemEntity golem = (EchoGolemEntity)renderPassInfo.getGeckolibData(ECHO_GOLEM_ENTITY);
      if (golem != null && golem.hasSoulBlock()) {
         renderPassInfo.addLocatorPositionListener("soul_light", (worldPos, modelPos, localPos) -> {
            if (worldPos != null) {
               EchoGolemLightBridge.update(golem, worldPos);
            }

         });
      }
   }

   public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, BoneSnapshots snapshots) {
      EchoGolemEntity golem = (EchoGolemEntity)renderPassInfo.getGeckolibData(ECHO_GOLEM_ENTITY);
      if (golem != null) {
         float partialTick = (Float)renderPassInfo.getOrDefaultGeckolibData(DataTickets.PARTIAL_TICK, 0.0F);
         float headYaw = Mth.rotLerp(partialTick, golem.yHeadRotO, golem.getYHeadRot());
         float bodyYaw = Mth.rotLerp(partialTick, golem.yBodyRotO, golem.yBodyRot);
         float relativeYaw = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -55.0F, 55.0F);
         snapshots.ifPresent("head", (head) -> {
            head.setRotX(0.0F);
            head.setRotY(head.getRotY() + relativeYaw * ((float)Math.PI / 180F));
         });
      }
   }
}
