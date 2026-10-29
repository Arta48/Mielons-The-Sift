package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.SifterEntity;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public final class SifterModel extends GeoModel<SifterEntity> {
   public @NonNull Identifier getModelResource(@NonNull GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/sifter");
   }

   public @NonNull Identifier getTextureResource(@NonNull GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/sifter.png");
   }

   public @NonNull Identifier getAnimationResource(@NonNull SifterEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/sifter");
   }
}
