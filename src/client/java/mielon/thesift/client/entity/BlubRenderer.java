package mielon.thesift.client.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import mielon.thesift.entity.BlubEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

public final class BlubRenderer extends GeoEntityRenderer {
   static final DataTicket<BlubEntity> BLUB_ENTITY = DataTicket.create("the_sift_blub_entity", BlubEntity.class);
   private static final DataTicket<Boolean> BLUB_SITTING = DataTicket.create("the_sift_blub_sitting", Boolean.class);

   public BlubRenderer(EntityRendererProvider.Context context) {
      super(context, new BlubModel());
      this.withRenderLayer(new SiftGlowingOutlineLayer(this));
      this.shadowRadius = 0.38F;
   }

   public void addRenderData(BlubEntity animatable, Void relatedObject, EntityRenderState renderState, float partialTick) {
      super.addRenderData(animatable, relatedObject, renderState, partialTick);
      renderState.addGeckolibData(BLUB_ENTITY, animatable);
      renderState.addGeckolibData(BLUB_SITTING, animatable.isBlubSitting());
   }

   public void adjustModelBonesForRender(RenderPassInfo renderPassInfo, @NonNull BoneSnapshots snapshots) {
      BlubEntity blub = (BlubEntity) renderPassInfo.getGeckolibData(BLUB_ENTITY);
      if (blub != null) {
         if (Boolean.TRUE.equals(renderPassInfo.getGeckolibData(BLUB_SITTING))) {
            float degrees = ((float) Math.PI / 180F);
            snapshots.ifPresent("head", (body) -> {
               body.setTranslateY(body.getTranslateY() - 3.25F);
               body.setTranslateZ(body.getTranslateZ() + 0.65F);
               body.setRotX(body.getRotX() + 17.5F * degrees);
            });
            snapshots.ifPresent("front_left_leg", (leg) -> {
               leg.setRotX(leg.getRotX() + 42.0F * degrees);
               leg.setTranslateZ(leg.getTranslateZ() - 0.55F);
            });
            snapshots.ifPresent("front_right_leg", (leg) -> {
               leg.setRotX(leg.getRotX() + 42.0F * degrees);
               leg.setTranslateZ(leg.getTranslateZ() - 0.55F);
            });
            snapshots.ifPresent("back_left_leg", (leg) -> {
               leg.setRotX(leg.getRotX() - 68.0F * degrees);
               leg.setTranslateY(leg.getTranslateY() + 0.55F);
               leg.setTranslateZ(leg.getTranslateZ() + 0.75F);
            });
            snapshots.ifPresent("back_right_leg", (leg) -> {
               leg.setRotX(leg.getRotX() - 68.0F * degrees);
               leg.setTranslateY(leg.getTranslateY() + 0.55F);
               leg.setTranslateZ(leg.getTranslateZ() + 0.75F);
            });
         }

         float healthRatio = Mth.clamp(blub.getHealth() / blub.getMaxHealth(), 0.0F, 1.0F);
         float droop = (1.0F - healthRatio) * 72.0F * ((float) Math.PI / 180F);
         snapshots.ifPresent("left_ear", (ear) -> ear.setRotX(ear.getRotX() + droop));
         snapshots.ifPresent("right_ear", (ear) -> ear.setRotX(ear.getRotX() + droop));
      }
   }
}
