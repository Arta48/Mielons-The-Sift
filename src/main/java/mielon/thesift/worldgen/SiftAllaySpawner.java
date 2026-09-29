package mielon.thesift.worldgen;

import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;

public final class SiftAllaySpawner {
   private static final int CHECK_INTERVAL = 80;
   private static int checkCooldown;

   private SiftAllaySpawner() {
   }

   public static void tick(MinecraftServer server) {
      if (--checkCooldown <= 0) {
         checkCooldown = 80;
         ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
         if (level != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
               if (player.level() == level && player.isAlive() && !player.isSpectator() && trySpawnNear(level, player.blockPosition())) {
                  return;
               }
            }

         }
      }
   }

   private static boolean trySpawnNear(ServerLevel level, BlockPos around) {
      double phase = level.getRandom().nextDouble() * Math.PI * (double)2.0F;

      for (int attempt = 0; attempt < 18; ++attempt) {
         double angle = phase + (double)attempt * 2.399963229728653;
         int radius = 34 + level.getRandom().nextInt(47);
         int x = around.getX() + (int)Math.round(Math.cos(angle) * (double)radius);
         int z = around.getZ() + (int)Math.round(Math.sin(angle) * (double)radius);
         if (level.getChunkSource().getChunkNow(Math.floorDiv(x, 16), Math.floorDiv(z, 16)) != null) {
            int surface = level.getHeight(Types.WORLD_SURFACE, x, z);
            BlockPos center = new BlockPos(x, surface + 6, z);
            SpawnProfile profile = profileAt(level, center);
            if (profile != null) {
               if (level.getRandom().nextDouble() >= profile.chance()) {
                  return false;
               }

               AABB local = (new AABB(center)).inflate((double)96.0F, (double)48.0F, (double)96.0F);
               int localPopulation = level.getEntitiesOfClass(Allay.class, local).size();
               int available = profile.localCap() - localPopulation;
               if (available < profile.minimumGroup()) {
                  return false;
               }

               int maximumGroup = Math.min(profile.maximumGroup(), available);
               int groupSize = profile.minimumGroup() + level.getRandom().nextInt(maximumGroup - profile.minimumGroup() + 1);
               return spawnGroup(level, center, profile, groupSize);
            }
         }
      }

      return false;
   }

   private static boolean spawnGroup(ServerLevel level, BlockPos center, SpawnProfile profile, int groupSize) {
      int spawned = 0;

      for (int member = 0; member < groupSize; ++member) {
         BlockPos feet = null;

         for (int attempt = 0; attempt < 10 && feet == null; ++attempt) {
            int x = center.getX() + level.getRandom().nextInt(9) - 4;
            int z = center.getZ() + level.getRandom().nextInt(9) - 4;
            int y = level.getHeight(Types.WORLD_SURFACE, x, z) + 5 + level.getRandom().nextInt(8);
            BlockPos candidate = new BlockPos(x, y, z);
            if (profileAt(level, candidate) == profile && level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty() && level.getBlockState(candidate.above()).getCollisionShape(level, candidate.above()).isEmpty()) {
               feet = candidate;
            }
         }

         if (feet != null) {
            Allay allay = (Allay)EntityTypes.ALLAY.create(level, EntitySpawnReason.NATURAL);
            if (allay != null) {
               allay.snapTo((double)feet.getX() + (double)0.5F, (double)feet.getY(), (double)feet.getZ() + (double)0.5F, level.getRandom().nextFloat() * 360.0F, 0.0F);
               if (level.noCollision(allay) && level.addFreshEntity(allay)) {
                  ++spawned;
               } else {
                  allay.discard();
               }
            }
         }
      }

      return spawned > 0;
   }

   private static SpawnProfile profileAt(ServerLevel level, BlockPos pos) {
      if (level.getBiome(pos).is(TheSiftDimension.OVERGROWN_FOREST)) {
         return SiftAllaySpawner.SpawnProfile.FOREST;
      } else if (level.getBiome(pos).is(TheSiftDimension.SIFT_WASTES)) {
         return SiftAllaySpawner.SpawnProfile.WASTES;
      } else {
         return level.getBiome(pos).is(TheSiftDimension.OVERGROWN_CLEARING) ? SiftAllaySpawner.SpawnProfile.CLEARING : null;
      }
   }

   public static void clear() {
      checkCooldown = 0;
   }

   private static enum SpawnProfile {
      FOREST(0.72, 2, 4, 16),
      WASTES((double)0.5F, 2, 3, 12),
      CLEARING(0.32, 1, 3, 8);

      private final double chance;
      private final int minimumGroup;
      private final int maximumGroup;
      private final int localCap;

      private SpawnProfile(double chance, int minimumGroup, int maximumGroup, int localCap) {
         this.chance = chance;
         this.minimumGroup = minimumGroup;
         this.maximumGroup = maximumGroup;
         this.localCap = localCap;
      }

      private double chance() {
         return this.chance;
      }

      private int minimumGroup() {
         return this.minimumGroup;
      }

      private int maximumGroup() {
         return this.maximumGroup;
      }

      private int localCap() {
         return this.localCap;
      }

      // $FF: synthetic method
      private static SpawnProfile[] $values() {
         return new SpawnProfile[]{FOREST, WASTES, CLEARING};
      }
   }
}
