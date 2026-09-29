package mielon.thesift.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModParticles {
   public static final SimpleParticleType SIFT_PARALLAX = FabricParticleTypes.simple();
   public static final SimpleParticleType SIFT_NOTE = FabricParticleTypes.simple();
   public static final SimpleParticleType SOUND_WAVE = FabricParticleTypes.simple();
   public static final SimpleParticleType SINGER_SOUND_WAVE = FabricParticleTypes.simple();
   public static final SimpleParticleType CANYON_SOUL = FabricParticleTypes.simple();
   public static final SimpleParticleType SOUL_FRAGMENT = FabricParticleTypes.simple();
   public static final SimpleParticleType ICHOR_SURFACE_MIST = FabricParticleTypes.simple();
   public static final SimpleParticleType ICHOR_BUBBLE = FabricParticleTypes.simple();
   public static final SimpleParticleType ICHOR_SPLASH = FabricParticleTypes.simple();
   public static final SimpleParticleType ICHOR_RAIN_SPLASH = FabricParticleTypes.simple();

   private ModParticles() {
   }

   public static void initialize() {
   }

   private static void register(String name, SimpleParticleType type) {
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("the_sift", name), type);
   }

   static {
      register("sift_parallax", SIFT_PARALLAX);
      register("sift_note", SIFT_NOTE);
      register("sound_wave", SOUND_WAVE);
      register("singer_sound_wave", SINGER_SOUND_WAVE);
      register("canyon_soul", CANYON_SOUL);
      register("soul_fragment", SOUL_FRAGMENT);
      register("ichor_surface_mist", ICHOR_SURFACE_MIST);
      register("ichor_bubble", ICHOR_BUBBLE);
      register("ichor_splash", ICHOR_SPLASH);
      register("ichor_rain_splash", ICHOR_RAIN_SPLASH);
   }
}
