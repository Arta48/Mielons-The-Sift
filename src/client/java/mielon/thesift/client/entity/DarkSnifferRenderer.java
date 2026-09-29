package mielon.thesift.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SnifferRenderer;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;
import net.minecraft.resources.Identifier;

public final class DarkSnifferRenderer extends SnifferRenderer {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("the_sift", "textures/entity/dark_sniffer.png");

   public DarkSnifferRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   public Identifier getTextureLocation(SnifferRenderState state) {
      return TEXTURE;
   }
}
