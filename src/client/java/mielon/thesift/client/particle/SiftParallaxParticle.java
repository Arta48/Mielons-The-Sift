package mielon.thesift.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class SiftParallaxParticle extends SingleQuadParticle {
   private final SingleQuadParticle.Layer layer;
   private final float fragmentU;
   private final float fragmentV;
   private final float fragmentWidth;
   private final float fragmentHeight;

   private SiftParallaxParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
      super(level, x, y, z, xd, yd, zd, sprites.get(level.getRandom()));
      RandomSource random = this.random;
      this.quadSize /= 2.0F;
      this.gravity = 1.0F;
      this.friction = 0.98F;
      this.hasPhysics = true;
      this.alpha = 1.0F;
      this.fragmentWidth = 0.3F + random.nextFloat() * 0.3F;
      this.fragmentHeight = 0.3F + random.nextFloat() * 0.3F;
      this.fragmentU = random.nextFloat() * (1.0F - this.fragmentWidth);
      this.fragmentV = random.nextFloat() * (1.0F - this.fragmentHeight);
      this.layer = Layer.bySprite(this.sprite);
   }

   protected float getU0() {
      return this.sprite.getU(this.fragmentU);
   }

   protected float getU1() {
      return this.sprite.getU(this.fragmentU + this.fragmentWidth);
   }

   protected float getV0() {
      return this.sprite.getV(this.fragmentV);
   }

   protected float getV1() {
      return this.sprite.getV(this.fragmentV + this.fragmentHeight);
   }

   public int getLightCoords(float partialTickTime) {
      return 15728880;
   }

   protected SingleQuadParticle.Layer getLayer() {
      return this.layer;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public SiftParallaxParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
         return new SiftParallaxParticle(level, x, y, z, xd, yd, zd, this.sprites);
      }
   }
}
