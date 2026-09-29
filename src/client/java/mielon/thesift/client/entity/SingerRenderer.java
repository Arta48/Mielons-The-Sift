package mielon.thesift.client.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import mielon.thesift.client.light.SingerHeadLightBridge;
import mielon.thesift.entity.SingerEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class SingerRenderer extends GeoEntityRenderer {
   private static final DataTicket SINGER_ENTITY = DataTicket.create("the_sift_singer_entity", SingerEntity.class);

   public SingerRenderer(EntityRendererProvider.Context context) {
      super(context, new SingerModel());
      this.withRenderLayer(new AutoGlowingGeoLayer(this));
      this.withRenderLayer(new SiftGlowingOutlineLayer(this));
      this.shadowRadius = 0.0F;
   }

   public void addRenderData(SingerEntity animatable, Void relatedObject, EntityRenderState renderState, float partialTick) {
      super.addRenderData(animatable, relatedObject, renderState, partialTick);
      renderState.addGeckolibData(SINGER_ENTITY, animatable);
   }

   public void preRenderPass(RenderPassInfo renderPassInfo, SubmitNodeCollector renderTasks) {
      SingerEntity singer = (SingerEntity)renderPassInfo.getGeckolibData(SINGER_ENTITY);
      if (singer != null) {
         renderPassInfo.addLocatorPositionListener("head_light", (worldPos, modelPos, localPos) -> {
            if (worldPos != null) {
               SingerHeadLightBridge.update(singer, worldPos);
            }

         });
      }
   }
}
