package mielon.thesift.client.mixin;

import mielon.thesift.portal.SonorousColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoteParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({NoteParticle.class})
public abstract class NoteParticleMixin {
   private static final double THE_SIFT_NOTE_MARKER = (double)100.0F;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void theSift$applySonorousColor(ClientLevel level, double x, double y, double z, double color, TextureAtlasSprite sprite, CallbackInfo ci) {
      if (!(color < (double)100.0F)) {
         int encoded = (int)Math.round(color - (double)100.0F);
         if (encoded >= 0 && encoded <= 23) {
            int soundIndex = encoded / 3 + 1;
            int shadeIndex = encoded % 3;
            if (soundIndex >= 1 && soundIndex <= 8) {
               if (shadeIndex >= 0 && shadeIndex <= 2) {
                  int[] shades = SonorousColors.shades(soundIndex);
                  int rgb = shades[shadeIndex];
                  float red = (float)(rgb >> 16 & 255) / 255.0F;
                  float green = (float)(rgb >> 8 & 255) / 255.0F;
                  float blue = (float)(rgb & 255) / 255.0F;
                  ((NoteParticle)(Object)this).setColor(red, green, blue);
               }
            }
         }
      }
   }
}
