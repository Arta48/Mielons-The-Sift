package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.SingerEntity;
import net.minecraft.resources.Identifier;

public class SingerModel extends GeoModel<SingerEntity> {
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/singer");
   }

   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/singer.png");
   }

   public Identifier getAnimationResource(SingerEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/singer");
   }
}
