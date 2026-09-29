package mielon.thesift.client.particle;

import java.util.Arrays;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.FacingCameraMode;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class SoulParticle extends SingleQuadParticle {
   private final boolean canyonSoul;
   private final float startSize;
   private final double swayPhase;
   private final int rimSurfaceY;
   private final SingleQuadParticle.FacingCameraMode facingMode;
   private int ticksAboveSurface;

   private SoulParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, boolean canyonSoul) {
      super(level, x, y, z, xd, yd, zd, sprites.get(level.getRandom()));
      this.canyonSoul = canyonSoul;
      this.swayPhase = this.random.nextDouble() * Math.PI * (double)2.0F;
      this.facingMode = canyonSoul ? FacingCameraMode.LOOKAT_Y : FacingCameraMode.LOOKAT_XYZ;
      this.rimSurfaceY = canyonSoul ? findRimSurface(level, (int)Math.floor(x), (int)Math.floor(z)) : Integer.MAX_VALUE;
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.friction = canyonSoul ? 0.995F : 0.94F;
      this.alpha = canyonSoul ? 0.86F : 0.92F;
      if (canyonSoul) {
         this.xd = (this.random.nextDouble() - (double)0.5F) * 0.018;
         this.yd = 0.18 + this.random.nextDouble() * 0.08;
         this.zd = (this.random.nextDouble() - (double)0.5F) * 0.018;
         this.startSize = 0.92F + this.random.nextFloat() * 0.62F;
         this.lifetime = 140 + this.random.nextInt(41);
      } else {
         this.xd = xd + (this.random.nextDouble() - (double)0.5F) * 0.055;
         this.yd = Math.abs(yd) + 0.035 + this.random.nextDouble() * 0.065;
         this.zd = zd + (this.random.nextDouble() - (double)0.5F) * 0.055;
         this.startSize = 0.075F + this.random.nextFloat() * 0.095F;
         this.lifetime = 18 + this.random.nextInt(15);
      }

      this.quadSize = this.startSize;
      this.setSize(this.startSize, this.startSize);
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.canyonSoul) {
         double sway = Math.sin(this.swayPhase + (double)this.age * 0.22) * 0.007;
         this.move(this.xd + sway, this.yd, this.zd - sway * 0.65);
         this.yd = Math.min(0.31, this.yd + 0.0016);
         if (this.y > (double)this.rimSurfaceY + (double)12.0F) {
            ++this.ticksAboveSurface;
         }

         float exitFade = 1.0F - (float)this.ticksAboveSurface / 26.0F;
         float lifeFade = (float)(this.lifetime - this.age) / 20.0F;
         this.alpha = 0.86F * Math.max(0.0F, Math.min(1.0F, Math.min(exitFade, lifeFade)));
         this.quadSize = this.startSize * (1.0F + Math.min(0.28F, (float)this.age / 180.0F));
      } else {
         this.move(this.xd, this.yd, this.zd);
         this.xd *= (double)this.friction;
         this.yd *= 0.965;
         this.zd *= (double)this.friction;
         this.alpha = 0.92F * Math.max(0.0F, (float)(this.lifetime - this.age) / 9.0F);
      }

      ++this.age;
      if (this.age >= this.lifetime || this.alpha <= 0.01F) {
         this.remove();
      }

   }

   public int getLightCoords(float partialTickTime) {
      return 15728880;
   }

   public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
      return this.facingMode;
   }

   protected SingleQuadParticle.Layer getLayer() {
      return Layer.bySprite(this.sprite);
   }

   private static int findRimSurface(ClientLevel level, int x, int z) {
      int[] heights = new int[8];
      int radius = 6;
      int index = 0;

      for (int dx = -1; dx <= 1; ++dx) {
         for (int dz = -1; dz <= 1; ++dz) {
            if (dx != 0 || dz != 0) {
               heights[index++] = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x + dx * radius, z + dz * radius);
            }
         }
      }

      Arrays.sort(heights);
      return (heights[3] + heights[4]) / 2;
   }

   public static final class RiseProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public RiseProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public SoulParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
         return new SoulParticle(level, x, y, z, xd, yd, zd, this.sprites, true);
      }
   }

   public static final class FragmentProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public FragmentProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public SoulParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
         return new SoulParticle(level, x, y, z, xd, yd, zd, this.sprites, false);
      }
   }
}
