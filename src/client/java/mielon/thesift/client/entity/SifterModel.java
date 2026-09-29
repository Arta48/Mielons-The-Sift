package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.SifterEntity;
import net.minecraft.resources.Identifier;

public final class SifterModel extends GeoModel<SifterEntity> {
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/sifter");
   }

   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/sifter.png");
   }

   public Identifier getAnimationResource(SifterEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/sifter");
   }
}
