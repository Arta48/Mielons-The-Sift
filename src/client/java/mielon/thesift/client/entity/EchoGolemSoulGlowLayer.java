package mielon.thesift.client.entity;

import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import mielon.thesift.entity.EchoGolemEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class EchoGolemSoulGlowLayer extends AutoGlowingGeoLayer {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("the_sift", "textures/entity/echo_golem/echo_golem_emissive.png");

   public EchoGolemSoulGlowLayer(GeoRenderer renderer) {
      super(renderer);
   }

   protected Identifier getTextureResource(GeoRenderState state) {
      return TEXTURE;
   }

   protected RenderType getRenderType(GeoRenderState state) {
      EchoGolemEntity golem = (EchoGolemEntity)state.getGeckolibData(EchoGolemRenderer.ECHO_GOLEM_ENTITY);
      return golem != null && golem.hasSoulBlock() ? super.getRenderType(state) : null;
   }
}
