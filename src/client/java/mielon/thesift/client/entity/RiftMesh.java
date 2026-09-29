package mielon.thesift.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class RiftMesh {
   private static final int FULL_BRIGHT = 15728880;
   private final List surface = new ArrayList();
   private final List glow = new ArrayList();
   private final List boxes = new ArrayList();

   public RiftMesh(List input) {
      for (RiftAssetModel.Box b : (Iterable<RiftAssetModel.Box>) input) {
         this.boxes.add(new float[][]{{b.x0(), b.y0(), b.z0()}, {b.x1(), b.y1(), b.z1()}});
      }

      for (float[][] b : (Iterable<float[][]>) this.boxes) {
         for (int axis = 0; axis < 3; ++axis) {
            for (int sign : new int[]{-1, 1}) {
               this.face(b, axis, sign);
            }
         }
      }

   }

   private boolean occupied(float[] p) {
      for (float[][] b : (Iterable<float[][]>) this.boxes) {
         if (p[0] >= b[0][0] - 1.0E-5F && p[0] <= b[1][0] + 1.0E-5F && p[1] >= b[0][1] - 1.0E-5F && p[1] <= b[1][1] + 1.0E-5F && p[2] >= b[0][2] - 1.0E-5F && p[2] <= b[1][2] + 1.0E-5F) {
            return true;
         }
      }

      return false;
   }

   private List cuts(float[][] b, int axis) {
      TreeSet<Float> c = new TreeSet();
      c.add(b[0][axis]);
      c.add(b[1][axis]);

      for (float[][] other : (Iterable<float[][]>) this.boxes) {
         for (float[] p : other) {
            if (p[axis] > b[0][axis] && p[axis] < b[1][axis]) {
               c.add(p[axis]);
            }
         }
      }

      return new ArrayList(c);
   }

   private void face(float[][] b, int a, int sign) {
      int u = (a + 1) % 3;
      int v = (a + 2) % 3;
      float plane = b[sign < 0 ? 0 : 1][a];
      List<Float> us = this.cuts(b, u);
      List<Float> vs = this.cuts(b, v);

      for (int i = 1; i < us.size(); ++i) {
         for (int j = 1; j < vs.size(); ++j) {
            float u0 = (Float)us.get(i - 1);
            float u1 = (Float)us.get(i);
            float v0 = (Float)vs.get(j - 1);
            float v1 = (Float)vs.get(j);
            float[] p = new float[3];
            p[a] = plane + (float)sign * 2.0E-4F;
            p[u] = (u0 + u1) / 2.0F;
            p[v] = (v0 + v1) / 2.0F;
            if (!this.occupied(p)) {
               quad(this.surface, a, sign, plane, u0, u1, v0, v1, a == 2 ? 0 : 100, 255);
               if (a == 2) {
                  p[a] = plane - (float)sign * 2.0E-4F;

                  for (int edge = 0; edge < 4; ++edge) {
                     p[u] = (u0 + u1) / 2.0F;
                     p[v] = (v0 + v1) / 2.0F;
                     if (edge < 2) {
                        p[u] = edge == 0 ? u0 - 2.0E-4F : u1 + 2.0E-4F;
                     } else {
                        p[v] = edge == 2 ? v0 - 2.0E-4F : v1 + 2.0E-4F;
                     }

                     if (!this.occupied(p)) {
                        float width = Math.min(0.032F, Math.min(u1 - u0, v1 - v0) * 0.45F);
                        strip(this.surface, a, sign, plane + (float)sign * 0.005F, u0, u1, v0, v1, edge, 0.0F, width, 180, 255);
                        strip(this.surface, a, sign, plane + (float)sign * 0.003F, u0, u1, v0, v1, edge, width, width * 2.0F, 100, 255);

                        for (int k = 0; k < 10; ++k) {
                           strip(this.glow, a, sign, plane + (float)sign * 0.006F, u0, u1, v0, v1, edge, (float)(-(k + 1)) * 0.065F, (float)(-k) * 0.065F, 255, (int)((double)220.0F * Math.pow((double)1.0F - (double)k / (double)10.0F, (double)2.0F)));
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private static void strip(List out, int a, int sign, float p, float u0, float u1, float v0, float v1, int edge, float from, float to, int mode, int alpha) {
      switch (edge) {
         case 0 -> quad(out, a, sign, p, u0 + from, u0 + to, v0, v1, mode, alpha);
         case 1 -> quad(out, a, sign, p, u1 - to, u1 - from, v0, v1, mode, alpha);
         case 2 -> quad(out, a, sign, p, u0, u1, v0 + from, v0 + to, mode, alpha);
         case 3 -> quad(out, a, sign, p, u0, u1, v1 - to, v1 - from, mode, alpha);
      }

   }

   private static void quad(List out, int a, int sign, float plane, float u0, float u1, float v0, float v1, int mode, int alpha) {
      int u = (a + 1) % 3;
      int v = (a + 2) % 3;
      float[][] points = new float[4][3];
      float[][] uv = new float[][]{{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};

      for (int i = 0; i < 4; ++i) {
         int source = sign > 0 ? i : 3 - i;
         points[i][a] = plane;
         points[i][u] = uv[source][0];
         points[i][v] = uv[source][1];
      }

      out.add(new Quad(points, mode, alpha));
   }

   public int surfaceQuadCount() {
      return this.surface.size();
   }

   public void draw(VertexConsumer buffer, PoseStack.Pose pose, boolean alongX, boolean halo) {
      for (Quad q : (Iterable<Quad>) (halo ? this.glow : this.surface)) {
         for (float[] p : q.points) {
            buffer.addVertex(pose, alongX ? p[0] : p[2], p[1], alongX ? p[2] : -p[0]).setUv((p[0] + 4.5F) / 9.0F, 1.0F - (p[1] + 0.375F) / 4.25F).setColor(q.mode, 255, 255, q.alpha);
         }
      }

   }

   public void drawShaderpack(VertexConsumer buffer, PoseStack.Pose pose, boolean alongX, ShaderpackPass pass, float passAlpha, float seconds) {
      List<Quad> quads = pass == RiftMesh.ShaderpackPass.GLOW ? this.glow : this.surface;
      if (!(passAlpha <= 0.001F)) {
         for (Quad quad : quads) {
            if (belongsToPass(quad, pass)) {
               int red = 255;
               int green = 255;
               int blue = 255;
               int alpha = Math.max(0, Math.min(255, Math.round((float)quad.alpha * passAlpha)));
               if (pass == RiftMesh.ShaderpackPass.FRAME) {
                  if (quad.mode >= 150) {
                     red = 255;
                     green = 250;
                     blue = 230;
                  } else {
                     red = 255;
                     green = 190;
                     blue = 105;
                  }
               } else if (pass == RiftMesh.ShaderpackPass.GLOW) {
                  red = 255;
                  green = 118;
                  blue = 185;
               }

               for (float[] p : quad.points) {
                  float u = (p[0] + 4.5F) / 9.0F;
                  float v = 1.0F - (p[1] + 0.375F) / 4.25F;
                  if (pass == RiftMesh.ShaderpackPass.ENERGY || pass == RiftMesh.ShaderpackPass.FRAME || pass == RiftMesh.ShaderpackPass.GLOW) {
                     u += seconds * 0.0065F;
                     v -= seconds * 0.004F;
                  }

                  float x = alongX ? p[0] : p[2];
                  float y = p[1];
                  float z = alongX ? p[2] : -p[0];
                  buffer.addVertex(pose.pose(), x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setLight(15728880).setNormal(0.0F, 0.0F, 1.0F);
               }
            }
         }

      }
   }

   private static boolean belongsToPass(Quad quad, ShaderpackPass pass) {
      boolean var10000;
      switch (pass.ordinal()) {
         case 0:
         case 1:
            var10000 = quad.mode == 0;
            break;
         case 2:
            var10000 = quad.mode > 0 && quad.mode < 255;
            break;
         case 3:
            var10000 = quad.mode >= 255;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public static enum ShaderpackPass {
      ENERGY,
      DESTINATION,
      FRAME,
      GLOW;

      // $FF: synthetic method
      private static ShaderpackPass[] $values() {
         return new ShaderpackPass[]{ENERGY, DESTINATION, FRAME, GLOW};
      }
   }

   private static record Quad(float[][] points, int mode, int alpha) {
   }
}
