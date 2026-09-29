package mielon.thesift.world;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.worldgen.SiftAllaySpawner;
import mielon.thesift.worldgen.SiftDeferredSnifferSpawner;
import mielon.thesift.worldgen.SiftEchoGolemSpawner;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import mielon.thesift.worldgen.SiftSifterSpawner;
import mielon.thesift.worldgen.SiftSoulCanyonParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SiftTeleportManager {
   private static final Set WAITING_FOR_EXIT = new HashSet();
   private static final Set PORTAL_GUARD_CHECKED = new HashSet();
   private static final Set INITIALIZED_PLAYERS = new HashSet();
   private static final Set PORTAL_CONTACTS = new HashSet();
   private static int cleanupCooldown;

   private SiftTeleportManager() {
   }

   public static void tick(MinecraftServer server) {
      SiftDeferredSnifferSpawner.tick(server);
      SiftLandmarkTracker.tick(server);
      SiftSoulCanyonParticles.tick(server);
      SiftEchoGolemSpawner.tick(server);
      SiftAllaySpawner.tick(server);
      SiftSifterSpawner.tick(server);
      ServerLevel overworld = server.getLevel(Level.OVERWORLD);
      ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (overworld != null && sift != null) {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            processPlayer(server, player);
         }

         processPortalContacts(server, overworld);
         releaseNonPlayerGuards(server, overworld);
         if (--cleanupCooldown <= 0) {
            cleanupCooldown = 600;
            WAITING_FOR_EXIT.removeIf((id) -> overworld.getEntityInAnyDimension((UUID) id) == null);
            PORTAL_GUARD_CHECKED.removeIf((id) -> overworld.getEntityInAnyDimension((UUID) id) == null);
            INITIALIZED_PLAYERS.removeIf((id) -> server.getPlayerList().getPlayer((UUID) id) == null);
         }

      }
   }

   public static void clearTransientState() {
      WAITING_FOR_EXIT.clear();
      PORTAL_GUARD_CHECKED.clear();
      INITIALIZED_PLAYERS.clear();
      PORTAL_CONTACTS.clear();
      cleanupCooldown = 0;
      SiftDeferredSnifferSpawner.clear();
      SiftLandmarkTracker.clearTransientState();
      SiftSoulCanyonParticles.clear();
      SiftEchoGolemSpawner.clear();
      SiftAllaySpawner.clear();
      SiftSifterSpawner.clear();
   }

   public static void suppressUntilExit(Entity entity) {
      WAITING_FOR_EXIT.add(entity.getUUID());
      PORTAL_GUARD_CHECKED.add(entity.getUUID());
      SiftWorldStorage.markPortalExitRequired(entity);
      PortalTransitGuard.block(entity);
   }

   private static void processPlayer(MinecraftServer server, ServerPlayer player) {
      if (player.isAlive()) {
         boolean touchingPortal = isTouchingPortal(player);
         UUID id = player.getUUID();
         if (player.isSpectator()) {
            if (touchingPortal) {
               WAITING_FOR_EXIT.add(id);
            }

         } else if (INITIALIZED_PLAYERS.add(id)) {
            if (touchingPortal) {
               suppressUntilExit(player);
            }

         } else {
            processTouchState(server, player, touchingPortal);
         }
      }
   }

   public static void onPortalContact(Entity entity) {
      if (!(entity instanceof ServerPlayer) && !entity.isRemoved() && entity.isAlive() && entity.level() instanceof ServerLevel) {
         PORTAL_CONTACTS.add(entity.getUUID());
      }

   }

   private static void processPortalContacts(MinecraftServer server, ServerLevel overworld) {
      if (!PORTAL_CONTACTS.isEmpty()) {
         Set<UUID> contacts = new HashSet(PORTAL_CONTACTS);
         PORTAL_CONTACTS.clear();

         for (UUID id : contacts) {
            Entity entity = overworld.getEntityInAnyDimension(id);
            if (entity != null && !(entity instanceof ServerPlayer) && !entity.isRemoved() && entity.isAlive()) {
               processTouchState(server, entity, true);
            }
         }

      }
   }

   private static void releaseNonPlayerGuards(MinecraftServer server, ServerLevel overworld) {
      Iterator<UUID> iterator = WAITING_FOR_EXIT.iterator();

      while(iterator.hasNext()) {
         UUID id = (UUID)iterator.next();
         Entity entity = overworld.getEntityInAnyDimension(id);
         if (entity != null && !(entity instanceof ServerPlayer) && !PortalTransitGuard.isBlocked(entity) && !isTouchingPortal(entity) && !RiftManager.isTouchingRift(entity)) {
            iterator.remove();
            SiftWorldStorage.clearPortalExitRequired(server, id);
         }
      }

   }

   private static void processTouchState(MinecraftServer server, Entity entity, boolean touchingPortal) {
      UUID id = entity.getUUID();
      if (PORTAL_GUARD_CHECKED.add(id) && SiftWorldStorage.requiresPortalExit(server, id)) {
         WAITING_FOR_EXIT.add(id);
      }

      if (!PortalTransitGuard.isBlocked(entity)) {
         if (WAITING_FOR_EXIT.contains(id)) {
            if (!touchingPortal) {
               WAITING_FOR_EXIT.remove(id);
               SiftWorldStorage.clearPortalExitRequired(server, id);
            }

         } else if (touchingPortal) {
            ResourceKeyMatch dimension = classifyDimension(entity.level());
            if (dimension == SiftTeleportManager.ResourceKeyMatch.OVERWORLD) {
               teleportIntoSift(server, entity);
            } else if (dimension == SiftTeleportManager.ResourceKeyMatch.THE_SIFT) {
               teleportBackToOverworld(server, entity);
            }

         }
      }
   }

   private static void teleportIntoSift(MinecraftServer server, Entity entity) {
      ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (sift != null) {
         Optional<BlockPos> anchorOptional = SiftPortalStructure.ensurePortal(server, sift);
         if (!anchorOptional.isEmpty()) {
            SiftWorldStorage.saveReturnPoint(entity);
            BlockPos anchor = (BlockPos)anchorOptional.get();
            Vec3 destination = new Vec3((double)anchor.getX() + (double)0.5F, (double)anchor.getY(), (double)anchor.getZ() + (double)0.5F);
            Entity teleported = teleport(entity, sift, destination, entity.getYRot(), entity.getXRot());
            if (teleported != null) {
               suppressUntilExit(teleported);
            }

         }
      }
   }

   private static void teleportBackToOverworld(MinecraftServer server, Entity entity) {
      ServerLevel overworld = server.getLevel(Level.OVERWORLD);
      if (overworld != null) {
         Optional<SiftWorldStorage.ReturnPoint> stored = SiftWorldStorage.getReturnPoint(server, entity.getUUID());
         if (stored.isPresent()) {
            BlockPos entrance = ((SiftWorldStorage.ReturnPoint)stored.get()).portalBlock();
            overworld.getChunk(entrance);
            if (!overworld.getBlockState(entrance).is(ModBlocks.SIFT_PORTAL)) {
               RiftManager.returnFromPortal(entity, true);
            } else {
               SiftWorldStorage.ReturnPoint point = (SiftWorldStorage.ReturnPoint)stored.get();
               Vec3 destination = new Vec3(point.x(), point.y(), point.z());
               float yaw = point.yaw();
               float pitch = point.pitch();
               Entity teleported = teleport(entity, overworld, destination, yaw, pitch);
               if (teleported != null) {
                  suppressUntilExit(teleported);
                  SiftWorldStorage.clearReturnPoint(server, entity.getUUID());
               }

            }
         } else {
            RiftManager.returnFromPortal(entity, false);
         }
      }
   }

   private static Entity teleport(Entity entity, ServerLevel destinationLevel, Vec3 destination, float yaw, float pitch) {
      return entity.teleport(new TeleportTransition(destinationLevel, destination, Vec3.ZERO, yaw, pitch, TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET)));
   }

   public static boolean isTouchingPortal(Entity entity) {
      AABB box = entity.getBoundingBox().deflate(1.0E-7);
      Level level = entity.level();
      int minX = Mth.floor(box.minX);
      int minY = Mth.floor(box.minY);
      int minZ = Mth.floor(box.minZ);
      int maxX = Mth.floor(box.maxX);
      int maxY = Mth.floor(box.maxY);
      int maxZ = Mth.floor(box.maxZ);
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int x = minX; x <= maxX; ++x) {
         for (int y = minY; y <= maxY; ++y) {
            for (int z = minZ; z <= maxZ; ++z) {
               pos.set(x, y, z);
               if (level.getBlockState(pos).is(ModBlocks.SIFT_PORTAL)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static ResourceKeyMatch classifyDimension(Level level) {
      if (level.dimension().equals(Level.OVERWORLD)) {
         return SiftTeleportManager.ResourceKeyMatch.OVERWORLD;
      } else {
         return level.dimension().equals(TheSiftDimension.LEVEL_KEY) ? SiftTeleportManager.ResourceKeyMatch.THE_SIFT : SiftTeleportManager.ResourceKeyMatch.OTHER;
      }
   }

   private static enum ResourceKeyMatch {
      OVERWORLD,
      THE_SIFT,
      OTHER;

      // $FF: synthetic method
      private static ResourceKeyMatch[] $values() {
         return new ResourceKeyMatch[]{OVERWORLD, THE_SIFT, OTHER};
      }
   }
}
