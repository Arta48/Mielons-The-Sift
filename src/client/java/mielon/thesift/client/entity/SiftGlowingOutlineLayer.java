package mielon.thesift.client.entity;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public final class SiftGlowingOutlineLayer extends GeoRenderLayer {
   public SiftGlowingOutlineLayer(GeoRenderer renderer) {
      super(renderer);
   }

   public void submitRenderTask(RenderPassInfo renderPassInfo, SubmitNodeCollector renderTasks) {
      EntityRenderState state = (EntityRenderState)renderPassInfo.renderState();
      if (renderPassInfo.willRender() && state.appearsGlowing()) {
         int previousColor = (Integer)renderPassInfo.getOrDefaultGeckolibData(DataTickets.RENDER_COLOR, -1);
         int outlineColor = state.outlineColor == 0 ? -1 : state.outlineColor;
         state.addGeckolibData(DataTickets.RENDER_COLOR, outlineColor);

         try {
            this.renderer.submitRenderTasks(renderPassInfo, renderTasks.order(3), RenderTypes.outline(this.getTextureResource(state)));
         } finally {
            state.addGeckolibData(DataTickets.RENDER_COLOR, previousColor);
         }

      }
   }
}
