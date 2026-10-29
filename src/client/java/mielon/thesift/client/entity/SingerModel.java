package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.SingerEntity;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class SingerModel extends GeoModel<SingerEntity> {
   public @NonNull Identifier getModelResource(@NonNull GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/singer");
   }

   public @NonNull Identifier getTextureResource(@NonNull GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/singer.png");
   }

   public @NonNull Identifier getAnimationResource(@NonNull SingerEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/singer");
   }
}
