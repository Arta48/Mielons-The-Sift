package mielon.thesift.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.FacingCameraMode;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

public final class SingerSoundWaveParticle extends SingleQuadParticle {
   private static final float BASE_SIZE = 0.2F;
   private static final double SPEED = 0.6;
   private final Vector3f direction;
   private final float startSize;
   private final int travelLifetime;

   private SingerSoundWaveParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
      super(level, x, y, z, (double)0.0F, (double)0.0F, (double)0.0F, sprites.get(level.getRandom()));
      double encodedMagnitude = Math.sqrt(xd * xd + yd * yd + zd * zd);
      if (encodedMagnitude < 1.0E-6) {
         encodedMagnitude = (double)1.0F;
      }

      Vector3f suppliedDirection = new Vector3f((float)(xd / encodedMagnitude), (float)(yd / encodedMagnitude), (float)(zd / encodedMagnitude));
      if (suppliedDirection.lengthSquared() < 1.0E-6F) {
         suppliedDirection.set(0.0F, 0.0F, 1.0F);
      } else {
         suppliedDirection.normalize();
      }

      this.direction = suppliedDirection;
      this.travelLifetime = Math.max(1, (int)Math.round((encodedMagnitude - (double)1.0F) * (double)100.0F));
      this.lifetime = this.travelLifetime;
      this.gravity = 0.0F;
      this.friction = 1.0F;
      this.hasPhysics = false;
      this.alpha = 1.0F;
      this.startSize = 0.2F;
      this.quadSize = 0.2F;
      this.setSize(0.2F, 0.2F);
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.move((double)this.direction.x() * 0.6, (double)this.direction.y() * 0.6, (double)this.direction.z() * 0.6);
      ++this.age;
      float progress = Math.min(1.0F, (float)this.age / (float)Math.max(1, this.travelLifetime));
      this.quadSize = this.startSize * (1.0F + progress);
      if (this.age >= this.travelLifetime) {
         this.remove();
      }

   }

   public float getQuadSize(float partialTickTime) {
      float progress = Math.min(1.0F, ((float)this.age + partialTickTime) / (float)Math.max(1, this.travelLifetime));
      return this.startSize * (1.0F + progress);
   }

   public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
      return FacingCameraMode.LOOKAT_XYZ;
   }

   public int getLightCoords(float partialTickTime) {
      return 15728880;
   }

   protected SingleQuadParticle.Layer getLayer() {
      return Layer.bySprite(this.sprite);
   }

   public static final class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public SingerSoundWaveParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
         return new SingerSoundWaveParticle(level, x, y, z, xd, yd, zd, this.sprites);
      }
   }
}
