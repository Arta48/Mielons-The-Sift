package mielon.thesift.worldgen;

import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.SifterEntity;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;

public final class SiftSifterSpawner {
   private static final int CHECK_INTERVAL = 80;
   private static int checkCooldown;

   private SiftSifterSpawner() {
   }

   public static void tick(MinecraftServer server) {
      if (--checkCooldown <= 0) {
         checkCooldown = 80;
         ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
         if (level != null && level.getDifficulty() != Difficulty.PEACEFUL) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
               if (player.level() == level && player.isAlive() && !player.isSpectator() && trySpawnNear(level, player.blockPosition())) {
                  return;
               }
            }

         }
      }
   }

   private static boolean trySpawnNear(ServerLevel level, BlockPos around) {
      RandomSource random = level.getRandom();
      double phase = random.nextDouble() * Math.PI * (double)2.0F;

      for (int attempt = 0; attempt < 18; ++attempt) {
         double angle = phase + (double)attempt * 2.399963229728653;
         int radius = 32 + random.nextInt(45);
         int x = around.getX() + (int)Math.round(Math.cos(angle) * (double)radius);
         int z = around.getZ() + (int)Math.round(Math.sin(angle) * (double)radius);
         if (level.getChunkSource().getChunkNow(Math.floorDiv(x, 16), Math.floorDiv(z, 16)) != null) {
            boolean surface = random.nextFloat() < 0.8F;
            BlockPos center = findSpawnFeet(level, x, z, surface, random);
            if (center != null) {
               SpawnProfile profile = profileAt(level, center);
               if (profile != null && !(random.nextDouble() >= profile.chance())) {
                  AABB local = (new AABB(center)).inflate((double)96.0F, (double)48.0F, (double)96.0F);
                  int localPopulation = level.getEntitiesOfClass(SifterEntity.class, local).size();
                  int available = profile.localCap() - localPopulation;
                  if (available < profile.minimumGroup()) {
                     return false;
                  }

                  int maximumGroup = Math.min(profile.maximumGroup(), available);
                  int groupSize = profile.minimumGroup() + random.nextInt(maximumGroup - profile.minimumGroup() + 1);
                  return spawnGroup(level, center, profile, groupSize, random);
               }
            }
         }
      }

      return false;
   }

   private static boolean spawnGroup(ServerLevel level, BlockPos center, SpawnProfile profile, int groupSize, RandomSource random) {
      int spawned = 0;

      for (int member = 0; member < groupSize; ++member) {
         BlockPos feet = null;

         for (int attempt = 0; attempt < 12 && feet == null; ++attempt) {
            int x = center.getX() + random.nextInt(9) - 4;
            int z = center.getZ() + random.nextInt(9) - 4;
            boolean surface = random.nextFloat() < 0.8F;
            BlockPos candidate = findSpawnFeet(level, x, z, surface, random);
            if (candidate != null && profileAt(level, candidate) == profile && hasEmptyCollision(level, candidate) && hasEmptyCollision(level, candidate.above())) {
               feet = candidate;
            }
         }

         if (feet != null) {
            SifterEntity sifter = (SifterEntity)ModEntities.SIFTER.create(level, EntitySpawnReason.NATURAL);
            if (sifter != null) {
               sifter.snapTo((double)feet.getX() + (double)0.5F, (double)feet.getY(), (double)feet.getZ() + (double)0.5F, random.nextFloat() * 360.0F, 0.0F);
               if (level.noCollision(sifter) && level.addFreshEntity(sifter)) {
                  ++spawned;
               } else {
                  sifter.discard();
               }
            }
         }
      }

      return spawned > 0;
   }

   private static BlockPos findSpawnFeet(ServerLevel level, int x, int z, boolean surface, RandomSource random) {
      int top = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      if (surface) {
         return findFloor(level, x, z, top - 1, 10, 1);
      } else {
         int minY = level.getMinY() + 2;
         int highestCaveFloor = top - 6;
         if (highestCaveFloor <= minY) {
            return null;
         } else {
            int startY = minY + random.nextInt(highestCaveFloor - minY + 1);
            return findFloor(level, x, z, startY, 18, -1);
         }
      }
   }

   private static BlockPos findFloor(ServerLevel level, int x, int z, int startY, int attempts, int direction) {
      for (int offset = 0; offset <= attempts; ++offset) {
         int floorY = startY + offset * direction;
         BlockPos floor = new BlockPos(x, floorY, z);
         if (level.getBlockState(floor).isCollisionShapeFullBlock(level, floor)) {
            BlockPos feet = floor.above();
            if (hasEmptyCollision(level, feet) && hasEmptyCollision(level, feet.above())) {
               return feet;
            }
         }
      }

      return null;
   }

   private static boolean hasEmptyCollision(ServerLevel level, BlockPos pos) {
      return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
   }

   private static SpawnProfile profileAt(ServerLevel level, BlockPos pos) {
      if (TheSiftDimension.isOvergrownBiome(level.getBiome(pos))) {
         return level.getBiome(pos).is(TheSiftDimension.OVERGROWN_FOREST) ? SiftSifterSpawner.SpawnProfile.OVERGROWN_FOREST : SiftSifterSpawner.SpawnProfile.OVERGROWN;
      } else {
         return level.getBiome(pos).is(TheSiftDimension.SIFT_WASTES) ? SiftSifterSpawner.SpawnProfile.WASTES : SiftSifterSpawner.SpawnProfile.OTHER_SIFT;
      }
   }

   public static void clear() {
      checkCooldown = 0;
   }

   private static enum SpawnProfile {
      OVERGROWN_FOREST(0.36, 8),
      OVERGROWN(0.28, 6),
      WASTES(0.22, 5),
      OTHER_SIFT(0.18, 5);

      private final double chance;
      private final int localCap;

      private SpawnProfile(double chance, int localCap) {
         this.chance = chance;
         this.localCap = localCap;
      }

      private double chance() {
         return this.chance;
      }

      private int minimumGroup() {
         return 1;
      }

      private int maximumGroup() {
         return 5;
      }

      private int localCap() {
         return this.localCap;
      }

      // $FF: synthetic method
      private static SpawnProfile[] $values() {
         return new SpawnProfile[]{OVERGROWN_FOREST, OVERGROWN, WASTES, OTHER_SIFT};
      }
   }
}
