package mielon.thesift.world;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class TheSiftDimension {
   public static final Identifier ID = Identifier.fromNamespaceAndPath("the_sift", "the_sift");
   public static final ResourceKey LEVEL_KEY;
   public static final ResourceKey SIFT_WASTES;
   public static final ResourceKey OVERGROWN_CLEARING;
   public static final ResourceKey OVERGROWN_FOREST;
   public static final ResourceKey OVERGROWN_FOREST_SLOPES;
   public static final ResourceKey OVERGROWN_SLOPES;
   public static final ResourceKey OVERGROWN_PEAKS;
   public static final ResourceKey ICHOR_SNOWY_PEAKS;

   public static boolean isOvergrownBiome(Holder biome) {
      return biome.is(OVERGROWN_CLEARING) || biome.is(OVERGROWN_FOREST) || biome.is(OVERGROWN_FOREST_SLOPES) || biome.is(OVERGROWN_SLOPES) || biome.is(OVERGROWN_PEAKS);
   }

   private TheSiftDimension() {
   }

   static {
      LEVEL_KEY = ResourceKey.create(Registries.DIMENSION, ID);
      SIFT_WASTES = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "sift_wastes"));
      OVERGROWN_CLEARING = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "overgrown_clearing"));
      OVERGROWN_FOREST = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "overgrown_forest"));
      OVERGROWN_FOREST_SLOPES = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "overgrown_forest_slopes"));
      OVERGROWN_SLOPES = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "overgrown_slopes"));
      OVERGROWN_PEAKS = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "overgrown_peaks"));
      ICHOR_SNOWY_PEAKS = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "ichor_snowy_peaks"));
   }
}
