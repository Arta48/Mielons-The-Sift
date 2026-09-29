package mielon.thesift.portal;

public final class SonorousColors {
   private static final int[] BASE_RGB = new int[]{11546150, 3949738, 16351261, 8991416, 1481884, 8439583, 16701501, 15961002};

   private SonorousColors() {
   }

   public static int[] shades(int soundIndex1to8) {
      int base = packedRGB(soundIndex1to8);
      return new int[]{base, lighten(base, 0.2F), lighten(base, 0.38F)};
   }

   public static int packedRGB(int soundIndex1to8) {
      if (soundIndex1to8 >= 1 && soundIndex1to8 <= BASE_RGB.length) {
         return BASE_RGB[soundIndex1to8 - 1];
      } else {
         throw new IllegalArgumentException("soundIndex1to8 must be between 1 and 8");
      }
   }

   public static float noteParticleHue(int soundIndex1to8) {
      return rgbToHue(packedRGB(soundIndex1to8));
   }

   public static float[] shadeHues(int soundIndex1to8) {
      int[] rgbShades = shades(soundIndex1to8);
      float[] hues = new float[rgbShades.length];

      for (int i = 0; i < rgbShades.length; ++i) {
         hues[i] = rgbToHue(rgbShades[i]);
      }

      return hues;
   }

   private static int lighten(int rgb, float factor) {
      factor = Math.max(0.0F, Math.min(1.0F, factor));
      int r = rgb >> 16 & 255;
      int g = rgb >> 8 & 255;
      int b = rgb & 255;
      r = Math.round((float)r + (float)(255 - r) * factor);
      g = Math.round((float)g + (float)(255 - g) * factor);
      b = Math.round((float)b + (float)(255 - b) * factor);
      return r << 16 | g << 8 | b;
   }

   private static float rgbToHue(int rgb) {
      float r = (float)(rgb >> 16 & 255) / 255.0F;
      float g = (float)(rgb >> 8 & 255) / 255.0F;
      float b = (float)(rgb & 255) / 255.0F;
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      float delta = max - min;
      if (delta < 1.0E-5F) {
         return 0.0F;
      } else {
         float hue;
         if (max == r) {
            hue = (g - b) / delta % 6.0F;
         } else if (max == g) {
            hue = (b - r) / delta + 2.0F;
         } else {
            hue = (r - g) / delta + 4.0F;
         }

         hue /= 6.0F;
         return hue < 0.0F ? hue + 1.0F : hue;
      }
   }
}
