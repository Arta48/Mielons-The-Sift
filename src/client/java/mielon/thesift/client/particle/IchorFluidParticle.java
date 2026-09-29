package mielon.thesift.client.particle;

import mielon.thesift.fluid.ModFluids;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.WaterDropParticle;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;

public final class IchorFluidParticle {
   private static final float[][] ICHOR_PALETTE = new float[][]{{0.0F, 0.98F, 1.0F}, {0.23F, 0.43F, 1.0F}, {0.94F, 0.03F, 1.0F}, {1.0F, 0.02F, 0.58F}, {1.0F, 0.46F, 0.01F}, {0.15F, 1.0F, 0.57F}};

   private IchorFluidParticle() {
   }

   public static void tintIchor(SingleQuadParticle particle, ClientLevel level, double x, double y, double z, boolean vivid) {
      double time = (double)level.getGameTime() * 0.006;
      double broad = Math.sin(x * 0.075 + z * 0.052 + time);
      double detail = Math.sin(x * -0.041 + z * 0.091 - time * 0.73);
      double noise = Math.sin(x * 12.9898 + y * 78.233 + z * 37.719 + (double)level.getGameTime() * 0.173) * 43758.5453;
      noise -= Math.floor(noise);
      double selector = (double)0.5F + broad * 0.22 + detail * 0.13 + (noise - (double)0.5F) * (vivid ? 0.92 : 0.3);
      selector -= Math.floor(selector);
      float palettePosition = (float)selector * (float)ICHOR_PALETTE.length;
      int firstIndex = Mth.floor(palettePosition) % ICHOR_PALETTE.length;
      int secondIndex = (firstIndex + 1) % ICHOR_PALETTE.length;
      float blend = palettePosition - (float)Mth.floor(palettePosition);
      blend = blend * blend * (3.0F - 2.0F * blend);
      float red = Mth.lerp(blend, ICHOR_PALETTE[firstIndex][0], ICHOR_PALETTE[secondIndex][0]);
      float green = Mth.lerp(blend, ICHOR_PALETTE[firstIndex][1], ICHOR_PALETTE[secondIndex][1]);
      float blue = Mth.lerp(blend, ICHOR_PALETTE[firstIndex][2], ICHOR_PALETTE[secondIndex][2]);
      float saturation = vivid ? 1.18F : 1.04F;
      float average = (red + green + blue) / 3.0F;
      particle.setColor(Mth.clamp(average + (red - average) * saturation, 0.0F, 1.0F), Mth.clamp(average + (green - average) * saturation, 0.0F, 1.0F), Mth.clamp(average + (blue - average) * saturation, 0.0F, 1.0F));
   }

   private static final class Bubble extends SingleQuadParticle {
      private Bubble(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
         super(level, x, y, z, sprite);
         this.setSize(0.02F, 0.02F);
         this.quadSize *= this.random.nextFloat() * 0.6F + 0.2F;
         this.xd = xd * 0.2 + (double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.02F);
         this.yd = yd * 0.2 + (double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.02F);
         this.zd = zd * 0.2 + (double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.02F);
         this.lifetime = (int)((double)8.0F / ((double)this.random.nextFloat() * 0.8 + 0.2));
         IchorFluidParticle.tintIchor(this, level, x, y, z, false);
      }

      public void tick() {
         this.xo = this.x;
         this.yo = this.y;
         this.zo = this.z;
         if (this.lifetime-- <= 0) {
            this.remove();
         } else {
            this.yd += 0.002;
            this.move(this.xd, this.yd, this.zd);
            this.xd *= (double)0.85F;
            this.yd *= (double)0.85F;
            this.zd *= (double)0.85F;
            Fluid fluid = this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).getType();
            if (fluid != ModFluids.ICHOR && fluid != ModFluids.FLOWING_ICHOR) {
               this.remove();
            }

         }
      }

      protected SingleQuadParticle.Layer getLayer() {
         return Layer.OPAQUE;
      }
   }

   private static final class Splash extends WaterDropParticle {
      private Splash(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
         super(level, x, y, z, sprite);
         this.gravity = 0.04F;
         if (yd == (double)0.0F && (xd != (double)0.0F || zd != (double)0.0F)) {
            this.xd = xd;
            this.yd = 0.1;
            this.zd = zd;
         }

         IchorFluidParticle.tintIchor(this, level, x, y, z, true);
      }
   }

   private static final class RainSplash extends WaterDropParticle {
      private RainSplash(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
         super(level, x, y, z, sprite);
         IchorFluidParticle.tintIchor(this, level, x, y, z, true);
      }
   }

   public static final class BubbleProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public BubbleProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public SingleQuadParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
         return new Bubble(level, x, y, z, xd, yd, zd, this.sprites.get(random));
      }
   }

   public static final class SplashProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SplashProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public SingleQuadParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
         return new Splash(level, x, y, z, xd, yd, zd, this.sprites.get(random));
      }
   }

   public static final class RainSplashProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public RainSplashProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public SingleQuadParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
         return new RainSplash(level, x, y, z, this.sprites.get(random));
      }
   }
}
