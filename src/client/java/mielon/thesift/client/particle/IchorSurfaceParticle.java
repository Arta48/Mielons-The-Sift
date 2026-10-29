package mielon.thesift.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;

public final class IchorSurfaceParticle extends SingleQuadParticle {
   private final SpriteSet sprites;
   private final float surfaceYaw;

   private IchorSurfaceParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
      super(level, x, y, z, sprites.get(level.getRandom()));
      this.sprites = sprites;
      this.surfaceYaw = this.random.nextFloat() * ((float) Math.PI * 2F);
      this.quadSize = 0.12F + this.random.nextFloat() * 0.12F;
      this.lifetime = 40 + this.random.nextInt(31);
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.friction = 1.0F;
      this.xd = 0.0F;
      this.yd = 0.0F;
      this.zd = 0.0F;
      this.setAlpha(0.0F);
      this.setSpriteFromAge(sprites);
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         float progress = (float) this.age / (float) this.lifetime;
         this.setAlpha((float) Math.sin(Math.PI * (double) progress) * 0.55F);
         this.setSpriteFromAge(this.sprites);
      }

   }

   public void extract(@NonNull QuadParticleRenderState renderState, @NonNull Camera camera, float partialTick) {
      Quaternionf rotation = (new Quaternionf()).rotationY(this.surfaceYaw).rotateX((-(float) Math.PI / 2F));
      this.extractRotatedQuad(renderState, camera, rotation, partialTick);
   }

   public int getLightCoords(float partialTickTime) {
      return 15728880;
   }

   protected @NonNull Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   public static final class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public IchorSurfaceParticle createParticle(@NonNull SimpleParticleType type, @NonNull ClientLevel level, double x, double y, double z, double xd, double yd, double zd, @NonNull RandomSource random) {
         return new IchorSurfaceParticle(level, x, y, z, this.sprites);
      }
   }
}
