package mielon.thesift.client.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class RiftAssetModel {
   public final List bones;
   public final float duration;
   public static final RiftAssetModel INSTANCE = new RiftAssetModel();

   private static JsonObject json(String path) {
      try {
         InputStream stream = RiftAssetModel.class.getResourceAsStream("/assets/the_sift/" + path);

         JsonObject var2;
         try {
            if (stream == null) {
               throw new IllegalStateException("Missing Rift asset: " + path);
            }

            var2 = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
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

         return var2;
      } catch (IOException e) {
         throw new IllegalStateException("Invalid Rift asset " + path, e);
      }
   }

   private RiftAssetModel() {
      JsonObject animation = json("geckolib/animations/entity/rift.animation.json").getAsJsonObject("animations").getAsJsonObject("appear");
      this.duration = animation.get("animation_length").getAsFloat();
      JsonObject channels = animation.getAsJsonObject("bones");
      List<Bone> loaded = new ArrayList();
      JsonArray geometry = json("geckolib/models/entity/rift.geo.json").getAsJsonArray("minecraft:geometry");

      for (JsonElement entry : geometry.get(0).getAsJsonObject().getAsJsonArray("bones")) {
         JsonObject bone = entry.getAsJsonObject();
         String name = bone.get("name").getAsString();
         List<Cube> cubes = new ArrayList();

         for (JsonElement raw : bone.getAsJsonArray("cubes")) {
            JsonObject c = raw.getAsJsonObject();
            cubes.add(new Cube(vector(c.get("origin")), vector(c.get("size"))));
         }

         JsonObject channel = channels.getAsJsonObject(name);
         loaded.add(new Bone(name, vector(bone.get("pivot")), List.copyOf(cubes), keys(channel, "position"), keys(channel, "scale")));
      }

      this.bones = List.copyOf(loaded);
   }

   private static float[] vector(JsonElement element) {
      JsonArray a = element.getAsJsonArray();
      return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
   }

   private static List keys(JsonObject bone, String name) {
      List<Key> result = new ArrayList();
      if (bone != null && bone.has(name)) {
         for (Map.Entry e : bone.getAsJsonObject(name).entrySet()) {
            JsonObject key = ((JsonElement)e.getValue()).getAsJsonObject();
            result.add(new Key(Float.parseFloat((String)e.getKey()), vector(key.get("vector")), key.has("easing") ? key.get("easing").getAsString() : "linear"));
         }
      }

      result.sort(Comparator.comparingDouble(Key::time));
      return List.copyOf(result);
   }

   public static float[] sample(List keys, float seconds, float fallback) {
      if (keys.isEmpty()) {
         return new float[]{fallback, fallback, fallback};
      } else if (seconds <= ((Key)keys.getFirst()).time) {
         return ((Key)keys.getFirst()).value;
      } else {
         for (int i = 1; i < keys.size(); ++i) {
            Key right = (Key)keys.get(i);
            Key left = (Key)keys.get(i - 1);
            if (!(seconds > right.time)) {
               float t = (seconds - left.time) / (right.time - left.time);
               float var10000;
               switch (right.easing) {
                  case "easeOutCubic" -> var10000 = 1.0F - (float)Math.pow((double)(1.0F - t), (double)3.0F);
                  case "easeInCubic" -> var10000 = t * t * t;
                  case "easeInOutExpo" -> var10000 = t != 0.0F && t != 1.0F ? (t < 0.5F ? (float)Math.pow((double)2.0F, (double)(20.0F * t - 10.0F)) / 2.0F : (2.0F - (float)Math.pow((double)2.0F, (double)(-20.0F * t + 10.0F))) / 2.0F) : t;
                  default -> var10000 = t;
               }

               t = var10000;
               return new float[]{lerp(left.value[0], right.value[0], t), lerp(left.value[1], right.value[1], t), lerp(left.value[2], right.value[2], t)};
            }
         }

         return ((Key)keys.getLast()).value;
      }
   }

   private static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   public List boxes(float progress) {
      float seconds = Math.max(0.0F, Math.min(1.0F, progress)) * this.duration;
      List<Box> result = new ArrayList(16);

      for (Bone bone : (List<Bone>) this.bones) {
         float[] scale = sample(bone.scale, seconds, 1.0F);
         float[] position = sample(bone.position, seconds, 0.0F);
         if (!(scale[0] < 1.0E-4F) && !(scale[1] < 1.0E-4F) && !(scale[2] < 1.0E-4F)) {
            for (Cube cube : (List<Cube>) bone.cubes) {
               float[] lo = new float[3];
               float[] hi = new float[3];

               for (int a = 0; a < 3; ++a) {
                  lo[a] = (bone.pivot[a] + (cube.origin[a] - bone.pivot[a]) * scale[a] + position[a]) / 16.0F;
                  hi[a] = lo[a] + cube.size[a] * scale[a] / 16.0F;
               }

               result.add(new Box(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]));
            }
         }
      }

      return result;
   }

   public static record Cube(float[] origin, float[] size) {
   }

   public static record Key(float time, float[] value, String easing) {
   }

   public static record Bone(String name, float[] pivot, List cubes, List position, List scale) {
   }

   public static record Box(float x0, float y0, float z0, float x1, float y1, float z1) {
   }
}
