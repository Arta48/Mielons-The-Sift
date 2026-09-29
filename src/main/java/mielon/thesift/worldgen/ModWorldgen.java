package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModWorldgen {
   public static final MapCodec LUSH_MONOLITH;
   public static final MapCodec BARE_MONOLITH;
   public static final MapCodec CLIFF_ARCH;
   public static final MapCodec DRY_SCULK_SPIKES;
   public static final MapCodec OVERGROWN_WILLOW_TREE;
   public static final MapCodec COVERED_GROWTH_CLEANUP;
   public static final MapCodec ABANDONED_MAIN_PORTAL;
   public static final MapCodec SNIFFER_CAVE_OVERGROWN;
   public static final MapCodec SNIFFER_CAVE_WASTES;
   public static final MapCodec SNIFFER_PLANT_PATCH;
   public static final MapCodec SIFT_FLOWER_PATCH;
   public static final MapCodec SIFTSLATE_PLANT_PATCH;
   public static final MapCodec SOUL_CANYON;
   public static final MapCodec ICHOR_LAKE;
   public static final MapCodec LAVA_FLOOR;
   public static final MapCodec SURFACE_SCULK_REGION;
   public static final MapCodec ICHOR_SNOW;
   public static final MapCodec ICHOR_CAVE_SPRING;

   private ModWorldgen() {
   }

   private static MapCodec register(String name, MapCodec codec) {
      return (MapCodec)Registry.register(BuiltInRegistries.FEATURE_TYPE, Identifier.fromNamespaceAndPath("the_sift", name), codec);
   }

   public static void initialize() {
   }

   static {
      LUSH_MONOLITH = register("lush_monolith", SiftMonolithFeature.LUSH_CODEC);
      BARE_MONOLITH = register("bare_monolith", SiftMonolithFeature.BARE_CODEC);
      CLIFF_ARCH = register("cliff_arch", SiftArchFeature.CODEC);
      DRY_SCULK_SPIKES = register("dry_sculk_spikes", SiftDrySpikeFeature.CODEC);
      OVERGROWN_WILLOW_TREE = register("overgrown_willow_tree", OvergrownWillowTreeFeature.CODEC);
      COVERED_GROWTH_CLEANUP = register("covered_growth_cleanup", CoveredGrowthCleanupFeature.CODEC);
      ABANDONED_MAIN_PORTAL = register("abandoned_main_portal", AbandonedMainPortalFeature.CODEC);
      SNIFFER_CAVE_OVERGROWN = register("sniffer_cave_overgrown", SnifferCaveFeature.OVERGROWN_CODEC);
      SNIFFER_CAVE_WASTES = register("sniffer_cave_wastes", SnifferCaveFeature.WASTES_CODEC);
      SNIFFER_PLANT_PATCH = register("sniffer_plant_patch", SnifferPlantPatchFeature.CODEC);
      SIFT_FLOWER_PATCH = register("sift_flower_patch", SiftFlowerPatchFeature.CODEC);
      SIFTSLATE_PLANT_PATCH = register("siftslate_plant_patch", SiftslatePlantPatchFeature.CODEC);
      SOUL_CANYON = register("soul_canyon", SoulCanyonFeature.CODEC);
      ICHOR_LAKE = register("ichor_lake", IchorLakeFeature.CODEC);
      LAVA_FLOOR = register("lava_floor", SiftLavaFloorFeature.CODEC);
      SURFACE_SCULK_REGION = register("surface_sculk_region", SiftSurfaceSculkRegionFeature.CODEC);
      ICHOR_SNOW = register("ichor_snow", SiftIchorSnowFeature.CODEC);
      ICHOR_CAVE_SPRING = register("ichor_cave_spring", SiftIchorCaveSpringFeature.CODEC);
   }
}
