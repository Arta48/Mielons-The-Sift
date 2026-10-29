package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.EchoGolemEntity;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public final class EchoGolemModel extends GeoModel<EchoGolemEntity> {
   public @NonNull Identifier getModelResource(@NonNull GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/echo_golem");
   }

   public @NonNull Identifier getTextureResource(@NonNull GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/echo_golem.png");
   }

   public @NonNull Identifier getAnimationResource(@NonNull EchoGolemEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/echo_golem");
   }
}
