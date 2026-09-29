package mielon.thesift.client.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import mielon.thesift.entity.SifterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;

public final class SifterRenderer extends GeoEntityRenderer {
   public SifterRenderer(EntityRendererProvider.Context context) {
      super(context, new SifterModel());
      this.withRenderLayer(new SiftGlowingOutlineLayer(this));
      this.shadowRadius = 0.42F;
   }

   public void extractRenderState(SifterEntity entity, EntityRenderState renderState, float partialTick) {
      super.extractRenderState(entity, renderState, partialTick);
      if (entity.isCurrentlyGlowing()) {
         renderState.outlineColor = ARGB.opaque(entity.getTeamColor());
      }

   }
}
