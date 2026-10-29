package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.BlubEntity;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public final class BlubModel extends GeoModel<BlubEntity> {
   private static final Identifier NORMAL_TEXTURE = Identifier.fromNamespaceAndPath("the_sift", "textures/entity/blub.png");
   private static final Identifier TAMED_TEXTURE = Identifier.fromNamespaceAndPath("the_sift", "textures/entity/tamed_blub.png");
   private static final Identifier MIELON_TEXTURE = Identifier.fromNamespaceAndPath("the_sift", "textures/entity/blub_mielon.png");
   private static final Identifier MIELON_TAMED_TEXTURE = Identifier.fromNamespaceAndPath("the_sift", "textures/entity/blub_mielon_tamed.png");

   public @NonNull Identifier getModelResource(@NonNull GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/blub");
   }

   public @NonNull Identifier getTextureResource(GeoRenderState state) {
      BlubEntity blub = state.getGeckolibData(BlubRenderer.BLUB_ENTITY);
      if (isMielon(blub)) {
         return blub.isTame() ? MIELON_TAMED_TEXTURE : MIELON_TEXTURE;
      } else {
         return blub != null && blub.isTame() ? TAMED_TEXTURE : NORMAL_TEXTURE;
      }
   }

   private static boolean isMielon(BlubEntity blub) {
      if (blub != null && blub.getCustomName() != null) {
         String name = blub.getCustomName().getString();
         return "mielon".equals(name) || "Mielon".equals(name) || "MIELON".equals(name);
      } else {
         return false;
      }
   }

   public @NonNull Identifier getAnimationResource(@NonNull BlubEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/blub");
   }
}
