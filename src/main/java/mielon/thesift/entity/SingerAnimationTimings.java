package mielon.thesift.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public record SingerAnimationTimings(int appearTicks, int singTicks, int disappearTicks) {
   private static final String RESOURCE = "/assets/the_sift/geckolib/animations/entity/singer.animation.json";
   private static final int FALLBACK_TICKS = 20;

   public static SingerAnimationTimings load() {
      try {
         InputStream stream = SingerAnimationTimings.class.getResourceAsStream("/assets/the_sift/geckolib/animations/entity/singer.animation.json");

         SingerAnimationTimings var7;
         label48: {
            SingerAnimationTimings var3;
            try {
               if (stream == null) {
                  var7 = new SingerAnimationTimings(20, 20, 20);
                  break label48;
               }

               JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
               JsonObject animations = root.getAsJsonObject("animations");
               var3 = new SingerAnimationTimings(ticksfor (animations, "appear"), ticksfor (animations, "sing"), ticksfor (animations, "disappear"));
            } catch (Throwable var5) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (stream != null) {
               stream.close();
            }

            return var3;
         }

         if (stream != null) {
            stream.close();
         }

         return var7;
      } catch (Exception var6) {
         return new SingerAnimationTimings(20, 20, 20);
      }
   }

   private static int ticksfor (JsonObject animations, String name) {
      JsonElement element = animations == null ? null : animations.get(name);
      if (element != null && element.isJsonObject()) {
         JsonObject animation = element.getAsJsonObject();
         double length = (double)1.0F;
         JsonElement lengthElement = animation.get("animation_length");
         if (lengthElement != null && lengthElement.isJsonPrimitive()) {
            length = Math.max(0.05, lengthElement.getAsDouble());
         }

         return Math.max(1, (int)Math.ceil(length * (double)20.0F));
      } else {
         return 20;
      }
   }
}
