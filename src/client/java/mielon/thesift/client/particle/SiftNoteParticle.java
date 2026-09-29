package mielon.thesift.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class SiftNoteParticle extends SingleQuadParticle {
   private static final float[][] PASTEL_COLORS = new float[][]{{0.45F, 0.93F, 0.98F}, {0.52F, 0.76F, 1.0F}, {0.78F, 0.58F, 1.0F}, {1.0F, 0.58F, 0.86F}, {1.0F, 0.74F, 0.46F}, {0.97F, 0.94F, 0.47F}, {0.52F, 0.96F, 0.66F}};
   private final float baseAlpha;
   private final double swayPhase;
   private final double swaySpeed;

   private SiftNoteParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
      super(level, x, y, z, sprites.get(level.getRandom()));
      double angle = this.random.nextDouble() * Math.PI * (double)2.0F;
      double distance = (double)4.0F + this.random.nextDouble() * (double)16.0F;
      double noteX = x + Math.cos(angle) * distance;
      double noteZ = z + Math.sin(angle) * distance;
      double noteY = (double)level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(noteX), (int)Math.floor(noteZ)) + (double)10.0F + this.random.nextDouble() * (double)15.0F;
      this.setPos(noteX, noteY, noteZ);
      this.xo = noteX;
      this.yo = noteY;
      this.zo = noteZ;
      float[] color = PASTEL_COLORS[this.random.nextInt(PASTEL_COLORS.length)];
      float shade = 0.97F + this.random.nextFloat() * 0.03F;
      this.setColor(color[0] * shade, color[1] * shade, color[2] * shade);
      this.baseAlpha = 0.78F + this.random.nextFloat() * 0.14F;
      this.setAlpha(0.0F);
      this.quadSize = (0.085F + this.random.nextFloat() * 0.095F) * 1.75F;
      this.setSize(this.quadSize, this.quadSize);
      this.lifetime = 100 + this.random.nextInt(101);
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.friction = 0.995F;
      this.xd = (this.random.nextDouble() - (double)0.5F) * 0.008;
      this.yd = 0.003 + this.random.nextDouble() * 0.008;
      this.zd = (this.random.nextDouble() - (double)0.5F) * 0.008;
      this.swayPhase = this.random.nextDouble() * Math.PI * (double)2.0F;
      this.swaySpeed = 0.035 + this.random.nextDouble() * 0.035;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      double sway = Math.sin(this.swayPhase + (double)this.age * this.swaySpeed) * 0.004;
      this.move(this.xd + sway, this.yd, this.zd - sway * 0.7);
      this.xd *= (double)this.friction;
      this.zd *= (double)this.friction;
      ++this.age;
      float fadeIn = Math.min(1.0F, (float)this.age / 14.0F);
      float fadeOut = Math.min(1.0F, (float)(this.lifetime - this.age) / 24.0F);
      this.setAlpha(this.baseAlpha * Math.max(0.0F, Math.min(fadeIn, fadeOut)));
      if (this.age >= this.lifetime) {
         this.remove();
      }

   }

   public int getLightCoords(float partialTickTime) {
      return 15728880;
   }

   protected SingleQuadParticle.Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   public static final class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public SiftNoteParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
         return new SiftNoteParticle(level, x, y, z, this.sprites);
      }
   }
}
