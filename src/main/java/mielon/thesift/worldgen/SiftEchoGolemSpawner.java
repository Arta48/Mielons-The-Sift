package mielon.thesift.worldgen;

import mielon.thesift.entity.EchoGolemEntity;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;

public final class SiftEchoGolemSpawner {
   private static final int CHECK_INTERVAL = 80;
   private static final double MINIMUM_SPACING = (double)165.0F;
   private static int checkCooldown;

   private SiftEchoGolemSpawner() {
   }

   public static void tick(MinecraftServer server) {
      if (--checkCooldown <= 0) {
         checkCooldown = 80;
         ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
         if (level != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
               if (player.level() == level && !player.isSpectator() && player.isAlive()) {
                  AABB nearby = (new AABB(player.blockPosition())).inflate((double)165.0F, (double)96.0F, (double)165.0F);
                  if (level.getEntitiesOfClass(EchoGolemEntity.class, nearby).isEmpty() && trySpawn(level, player.blockPosition())) {
                     return;
                  }
               }
            }

         }
      }
   }

   private static boolean trySpawn(ServerLevel level, BlockPos around) {
      double phase = level.getRandom().nextDouble() * Math.PI * (double)2.0F;

      for (int attempt = 0; attempt < 28; ++attempt) {
         double angle = phase + (double)attempt * 2.399963229728653;
         int radius = 82 + level.getRandom().nextInt(79);
         int x = around.getX() + (int)Math.round(Math.cos(angle) * (double)radius);
         int z = around.getZ() + (int)Math.round(Math.sin(angle) * (double)radius);
         if (level.getChunkSource().getChunkNow(Math.floorDiv(x, 16), Math.floorDiv(z, 16)) != null) {
            BlockPos feet = findSpawnFeet(level, x, z);
            if (feet != null) {
               AABB spacing = (new AABB(feet)).inflate((double)165.0F, (double)96.0F, (double)165.0F);
               if (level.getEntitiesOfClass(EchoGolemEntity.class, spacing).isEmpty()) {
                  EchoGolemEntity golem = (EchoGolemEntity)ModEntities.ECHO_GOLEM.create(level, EntitySpawnReason.NATURAL);
                  if (golem == null) {
                     return false;
                  }

                  golem.snapTo((double)x + (double)0.5F, (double)feet.getY(), (double)z + (double)0.5F, level.getRandom().nextFloat() * 360.0F, 0.0F);
                  golem.setPersistenceRequired();
                  if (level.noCollision(golem) && level.addFreshEntity(golem)) {
                     level.playSound((Entity)null, feet, ModSounds.ECHO_GOLEM_SPAWN, golem.getSoundSource(), 0.8F, 0.94F);
                     return true;
                  }

                  golem.discard();
               }
            }
         }
      }

      return false;
   }

   private static BlockPos findSpawnFeet(ServerLevel level, int x, int z) {
      int top = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      BlockPos.MutableBlockPos floor = new BlockPos.MutableBlockPos(x, top - 1, z);

      for (int drop = 0; drop <= 12; ++drop) {
         floor.setY(top - 1 - drop);
         if (level.getBlockState(floor).isCollisionShapeFullBlock(level, floor)) {
            BlockPos feet = floor.above().immutable();
            if (hasEmptyCollision(level, feet) && hasEmptyCollision(level, feet.above()) && hasEmptyCollision(level, feet.above(2))) {
               return feet;
            }
         }
      }

      return null;
   }

   private static boolean hasEmptyCollision(ServerLevel level, BlockPos pos) {
      return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
   }

   public static void clear() {
      checkCooldown = 0;
   }
}
