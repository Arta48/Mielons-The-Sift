package mielon.thesift.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mielon.thesift.TheSiftMod;
import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.entity.MiniRiftEntity;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.RiftEntity;
import mielon.thesift.entity.SiftiteReturnEntity;
import mielon.thesift.network.RiftLoadingPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RiftManager {
   private static final Map NEXT_SPAWN = new HashMap();
   private static final Map PENDING = new LinkedHashMap();
   private static final Map RIFT_EXIT_LATCHES = new HashMap();

   public static void tick(MinecraftServer server) {
      RiftDirectory.tick(server);
      RiftLightCleanup.tick(server);
      List<Request> requests = List.copyOf(PENDING.values());
      PENDING.clear();

      for (Request request : requests) {
         finish(server, request);
      }

      long now = server.overworld().getGameTime();
      if (now % 600L == 0L) {
         PortalTransitGuard.prune(server);
      }

      releaseRiftExitLatches(server);

      for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
         if (player.level().dimension().equals(TheSiftDimension.LEVEL_KEY) && player.isAlive()) {
            long due = (Long)NEXT_SPAWN.computeIfAbsent(player.getUUID(), (id) -> now + 5400L + (long)player.getRandom().nextInt(1201));
            if (now >= due) {
               Optional<Placement> found = findNatural(player.level(), player.blockPosition(), player.getRandom());
               if (found.isPresent()) {
                  Placement p = (Placement)found.get();
                  spawn(player.level(), p, false, now + 6000L, UUID.randomUUID());
               }

               NEXT_SPAWN.put(player.getUUID(), now + (long)(found.isPresent() ? 5400 + player.getRandom().nextInt(1201) : 200));
            }
         } else {
            NEXT_SPAWN.remove(player.getUUID());
         }
      }

      Set<UUID> visited = new HashSet();

      for (RiftEntity rift : (Iterable<RiftEntity>) RiftDirectory.loaded()) {
         if (!rift.isRemoved() && !rift.isClosing()) {
            for (Entity candidate : List.copyOf(rift.level().getEntities(rift, rift.getPortalBounds().inflate((double)8.0F), RiftManager::traveler))) {
               Entity entity = candidate.getRootVehicle();
               if (visited.add(entity.getUUID()) && !entity.isRemoved() && !RIFT_EXIT_LATCHES.containsKey(entity.getUUID()) && !PortalTransitGuard.isBlocked(entity)) {
                  if (touches(entity, rift)) {
                     queue(entity, rift, false, true);
                  } else {
                     visited.remove(entity.getUUID());
                  }
               }
            }
         }
      }

   }

   private static void latchRiftExit(Entity entity) {
      entity.getSelfAndPassengers().forEach((e) -> RIFT_EXIT_LATCHES.put(e.getUUID(), new RiftArrival(e.level().dimension(), e.position())));
   }

   private static void releaseRiftExitLatches(MinecraftServer server) {
      Iterator<Map.Entry<UUID, RiftArrival>> iterator = RIFT_EXIT_LATCHES.entrySet().iterator();

      while(iterator.hasNext()) {
         Map.Entry<UUID, RiftArrival> entry = (Map.Entry)iterator.next();
         Entity entity = server.overworld().getEntityInAnyDimension((UUID)entry.getKey());
         if (entity != null && !entity.isRemoved()) {
            RiftArrival arrival = (RiftArrival)entry.getValue();
            if (arrival.dimension.equals(entity.level().dimension()) && !isTouchingRift(entity) && (hasLoadedRiftAt(arrival) || arrival.pos.distanceToSqr(entity.position()) >= (double)36.0F)) {
               iterator.remove();
            }
         } else {
            iterator.remove();
         }
      }

   }

   private static boolean hasLoadedRiftAt(RiftArrival arrival) {
      for (RiftEntity rift : (Iterable<RiftEntity>) RiftDirectory.loaded()) {
         if (!rift.isRemoved() && rift.level().dimension().equals(arrival.dimension) && rift.getPortalBounds().inflate((double)0.5F).contains(arrival.pos)) {
            return true;
         }
      }

      return false;
   }

   private static boolean traveler(Entity e) {
      return e.isAlive() && !e.isSpectator() && !(e instanceof RiftEntity) && !(e instanceof MiniRiftEntity) && !(e instanceof SiftiteReturnEntity);
   }

   private static boolean touches(Entity entity, RiftEntity rift) {
      Vec3 delta = entity.oldPosition().subtract(entity.position());
      if (delta.lengthSqr() > (double)256.0F) {
         delta = Vec3.ZERO;
      }

      return entity.getBoundingBox().expandTowards(delta).intersects(rift.getPortalBounds());
   }

   public static boolean isTouchingRift(Entity entity) {
      for (RiftEntity rift : (Iterable<RiftEntity>) RiftDirectory.loaded()) {
         if (!rift.isRemoved() && rift.level() == entity.level() && rift.getPortalBounds().intersects(entity.getBoundingBox())) {
            return true;
         }
      }

      return false;
   }

   private static void screen(Entity entity, boolean start, boolean rift) {
      entity.getSelfAndPassengers().forEach((e) -> {
         if (e instanceof ServerPlayer player) {
            RiftLoadingPayload.send(player, start, rift);
         }

      });
   }

   private static void queue(Entity entity, RiftEntity origin, boolean closed, boolean riftScreen) {
      if (!PENDING.containsKey(entity.getUUID())) {
         PortalTransitGuard.block(entity);
         SiftTeleportManager.suppressUntilExit(entity);
         screen(entity, true, riftScreen);
         PENDING.put(entity.getUUID(), new Request(entity, origin, closed, riftScreen));
      }
   }

   public static void returnFromPortal(Entity entity, boolean closed) {
      queue(entity.getRootVehicle(), (RiftEntity)null, closed, false);
   }

   private static void finish(MinecraftServer server, Request request) {
      Entity entity = request.entity;
      List<ServerPlayer> players = entity.getSelfAndPassengers().filter((e) -> e instanceof ServerPlayer).map((e) -> (ServerPlayer)e).toList();

      try {
         if (!entity.isRemoved() && entity.isAlive()) {
            RiftEntity origin = request.origin;
            if (origin != null && entity.level() != origin.level()) {
               return;
            }

            boolean intoSift = origin != null && origin.targetsSift();
            Destination destination = intoSift ? intoSift(server, entity, origin) : outOfSift(server, entity, origin);
            if (destination != null && entity.canTeleport(entity.level(), destination.level)) {
               if (intoSift) {
                  entity.getSelfAndPassengers().forEach(SiftWorldStorage::markRiftEntry);
               }

               Vec3 motion = entity instanceof ServerPlayer ? Vec3.ZERO : entity.getDeltaMovement();
               Entity moved = moveToDestination(entity, destination, motion, TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
               if (moved != null) {
                  if (request.riftScreen) {
                     for (ServerPlayer player : players) {
                        ModAdvancements.award(player, "the_sift/rifter");
                     }
                  }

                  PortalTransitGuard.block(moved);
                  SiftTeleportManager.suppressUntilExit(moved);
                  latchRiftExit(moved);
                  if (!intoSift) {
                     SiftWorldStorage.clearReturnPoint(server, entity.getUUID());
                     if (!moved.getUUID().equals(entity.getUUID())) {
                        SiftWorldStorage.clearReturnPoint(server, moved.getUUID());
                     }
                  }

                  for (ServerPlayer player : players) {
                     if (destination.fallback) {
                        player.sendSystemMessage(Component.translatable("message.the_sift.rift_no_exit").withStyle(ChatFormatting.RED));
                     }

                     if (request.closedPortal) {
                        player.sendSystemMessage(Component.translatable("message.the_sift.portal_closed"));
                     }
                  }
               }

               return;
            }

            return;
         }
      } catch (RuntimeException failure) {
         TheSiftMod.LOGGER.error("Rift transfer failed", failure);
         if (entity.level().dimension().equals(TheSiftDimension.LEVEL_KEY) && !entity.isRemoved()) {
            Destination fallback = fallback(server, entity);
            Entity moved = moveToDestination(entity, fallback, Vec3.ZERO, TeleportTransition.PLACE_PORTAL_TICKET);
            if (moved != null) {
               PortalTransitGuard.block(moved);
               latchRiftExit(moved);
            }

            for (ServerPlayer player : players) {
               player.sendSystemMessage(Component.translatable("message.the_sift.rift_no_exit").withStyle(ChatFormatting.RED));
            }
         }

         return;
      } finally {
         for (ServerPlayer player : players) {
            RiftLoadingPayload.send(player, false, request.riftScreen);
         }

      }

   }

   private static Destination intoSift(MinecraftServer server, Entity entity, RiftEntity origin) {
      ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (sift == null) {
         return null;
      } else {
         BlockPos pos = (BlockPos)SiftWorldStorage.getRiftRoute(server, entity.getUUID(), origin.getUUID(), server.overworld().getGameTime()).orElse(origin.getLinkedPos());
         if (pos == null) {
            pos = (BlockPos)SiftPortalStructure.ensurePortal(server, sift).orElse(null);
         }

         if (pos == null) {
            return null;
         } else {
            load(sift, pos);
            return new Destination(sift, new Vec3((double)pos.getX() + (double)0.5F, (double)pos.getY(), (double)pos.getZ() + (double)0.5F), false);
         }
      }
   }

   private static Destination outOfSift(MinecraftServer server, Entity entity, RiftEntity origin) {
      ServerLevel overworld = server.overworld();
      BlockPos anchor = overworldAnchor(server, entity);
      long expiry = origin == null ? overworld.getGameTime() + 6000L : Math.max(overworld.getGameTime() + 20L, origin.getExpiresAt());
      Optional<RiftDirectory.Exit> existing = RiftDirectory.near(anchor, overworld.getGameTime());
      RiftDirectory.Exit exit;
      if (existing.isPresent()) {
         exit = RiftDirectory.extend((RiftDirectory.Exit)existing.get(), expiry);
         load(overworld, exit.pos());
      } else {
         Optional<Placement> found = findNear(overworld, anchor);
         if (found.isEmpty()) {
            return fallback(server, entity);
         }

         RiftEntity rift = spawn(overworld, (Placement)found.get(), true, expiry, UUID.randomUUID());
         if (origin != null) {
            rift.setLinkedPos(origin.getAnchorPos(), origin.getExpiresAt());
         }

         RiftDirectory.add(rift, anchor);
         exit = new RiftDirectory.Exit(rift.getUUID(), rift.getAnchorPos(), anchor, expiry);
      }

      if (origin != null) {
         entity.getSelfAndPassengers().forEach((e) -> SiftWorldStorage.saveRiftRoute(server, e.getUUID(), exit.id(), origin.getAnchorPos(), origin.getExpiresAt()));
      }

      return new Destination(overworld, new Vec3((double)exit.pos().getX() + (double)0.5F, (double)exit.pos().getY(), (double)exit.pos().getZ() + (double)0.5F), false);
   }

   private static void load(ServerLevel level, BlockPos pos) {
      level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), 3);
      level.getChunk(pos);
   }

   public static BlockPos overworldAnchor(MinecraftServer server, Entity entity) {
      if (entity instanceof ServerPlayer player) {
         if (player.getRespawnConfig() != null) {
            LevelData.RespawnData data = player.getRespawnConfig().respawnData();
            if (data.dimension().equals(Level.OVERWORLD)) {
               return data.pos();
            }
         }
      }

      LevelData.RespawnData data = server.overworld().getRespawnData();
      return data.dimension().equals(Level.OVERWORLD) ? data.pos() : new BlockPos(0, server.overworld().getSeaLevel(), 0);
   }

   private static Destination fallback(MinecraftServer server, Entity entity) {
      ServerLevel level = server.overworld();
      BlockPos anchor = overworldAnchor(server, entity);
      load(level, anchor);
      BlockPos safe = entity.adjustSpawnLocation(level, anchor);
      return new Destination(level, new Vec3((double)safe.getX() + (double)0.5F, (double)safe.getY(), (double)safe.getZ() + (double)0.5F), true);
   }

   private static Entity moveToDestination(Entity entity, Destination destination, Vec3 motion, TeleportTransition.PostTeleportTransition postTransition) {
      if (destination.fallback && entity instanceof ItemEntity item) {
         if (item.getItem().isEmpty()) {
            return null;
         } else {
            ItemEntity returned = new ItemEntity(destination.level, destination.pos.x, destination.pos.y, destination.pos.z, item.getItem().copy(), (double)0.0F, (double)0.0F, (double)0.0F);
            returned.setDefaultPickUpDelay();
            if (!destination.level.addFreshEntity(returned)) {
               return null;
            } else {
               item.discard();
               return returned;
            }
         }
      } else {
         return entity.teleport(new TeleportTransition(destination.level, destination.pos, motion, entity.getYRot(), entity.getXRot(), postTransition));
      }
   }

   public static boolean tryPlaceCreativeRift(ServerLevel level, BlockPos pos, boolean alongX) {
      if (!space(level, pos, alongX)) {
         return false;
      } else {
         RiftEntity rift = spawn(level, new Placement(pos, alongX), !level.dimension().equals(TheSiftDimension.LEVEL_KEY), level.getGameTime() + 6000L, UUID.randomUUID());
         if (level.dimension().equals(Level.OVERWORLD)) {
            RiftDirectory.add(rift, pos);
         }

         return true;
      }
   }

   private static RiftEntity spawn(ServerLevel level, Placement p, boolean target, long expiry, UUID pair) {
      RiftEntity rift = new RiftEntity(ModEntities.RIFT, level);
      rift.configure(target, p.alongX, expiry, pair);
      rift.setPos((double)p.base.getX() + (double)0.5F, (double)(p.base.getY() + 1), (double)p.base.getZ() + (double)0.5F);
      rift.setBoundingBox(rift.getPortalBounds());
      level.addFreshEntity(rift);
      RiftDirectory.track(rift);
      return rift;
   }

   private static boolean plant(BlockState state) {
      if (state.getFluidState().isEmpty() && !(state.getBlock() instanceof LeavesBlock)) {
         return state.getBlock() instanceof VegetationBlock || state.getBlock() instanceof VineBlock || state.getBlock() instanceof HangingRootsBlock;
      } else {
         return false;
      }
   }

   private static boolean space(ServerLevel level, BlockPos base, boolean alongX) {
      if (base.getY() >= level.getMinY() && base.getY() + 5 < level.getMaxY()) {
         boolean supported = false;

         for (int gap = 1; gap <= 3; ++gap) {
            BlockState under = level.getBlockState(base.below(gap));
            if (!under.isAir() && !plant(under)) {
               supported = true;
               break;
            }
         }

         if (!supported) {
            return false;
         } else {
            for (int a = -4; a <= 4; ++a) {
               for (int y = 0; y < 5; ++y) {
                  BlockPos p = alongX ? base.offset(a, y, 0) : base.offset(0, y, a);
                  if (!level.getWorldBorder().isWithinBounds(p)) {
                     return false;
                  }

                  BlockState state = level.getBlockState(p);
                  if (!state.isAir() && !plant(state)) {
                     return false;
                  }
               }
            }

            AABB volume = alongX ? new AABB((double)(base.getX() - 4), (double)base.getY(), (double)base.getZ(), (double)(base.getX() + 5), (double)(base.getY() + 5), (double)(base.getZ() + 1)) : new AABB((double)base.getX(), (double)base.getY(), (double)(base.getZ() - 4), (double)(base.getX() + 1), (double)(base.getY() + 5), (double)(base.getZ() + 5));
            return level.getEntities((Entity)null, volume, (e) -> !e.isRemoved()).isEmpty();
         }
      } else {
         return false;
      }
   }

   private static Optional column(ServerLevel level, int x, int z, boolean axis) {
      int surface = level.getHeight(Types.MOTION_BLOCKING, x, z);

      for (int rise = 0; rise <= 2; ++rise) {
         BlockPos base = new BlockPos(x, surface + rise, z);
         if (space(level, base, axis)) {
            return Optional.of(new Placement(base, axis));
         }

         if (space(level, base, !axis)) {
            return Optional.of(new Placement(base, !axis));
         }
      }

      return Optional.empty();
   }

   private static Optional findNear(ServerLevel level, BlockPos anchor) {
      Optional<Placement> first = column(level, anchor.getX(), anchor.getZ(), true);
      if (first.isPresent()) {
         return first;
      } else {
         for (int radius = 16; radius <= 256; radius += 16) {
            for (int i = 0; i < 16; ++i) {
               double angle = ((double)i + (double)radius * 0.017) * Math.PI / (double)8.0F;
               int x = anchor.getX() + (int)Math.round(Math.cos(angle) * (double)radius);
               int z = anchor.getZ() + (int)Math.round(Math.sin(angle) * (double)radius);
               Optional<Placement> found = column(level, x, z, (i & 1) == 0);
               if (found.isPresent()) {
                  return found;
               }
            }
         }

         return Optional.empty();
      }
   }

   private static Optional findNatural(ServerLevel level, BlockPos center, RandomSource random) {
      for (int i = 0; i < 48; ++i) {
         double angle = random.nextDouble() * Math.PI * (double)2.0F;
         int distance = 20 + random.nextInt(45);
         Optional<Placement> found = column(level, center.getX() + (int)(Math.cos(angle) * (double)distance), center.getZ() + (int)(Math.sin(angle) * (double)distance), random.nextBoolean());
         if (found.isPresent()) {
            return found;
         }
      }

      return Optional.empty();
   }

   public static boolean axisFromFacing(Direction facing) {
      return facing.getAxis() == Axis.Z;
   }

   public static void clearTransientState() {
      NEXT_SPAWN.clear();
      PENDING.clear();
      RIFT_EXIT_LATCHES.clear();
      RiftDirectory.clear();
      PortalTransitGuard.clear();
      RiftLightCleanup.clear();
   }

   private static record Request(Entity entity, RiftEntity origin, boolean closedPortal, boolean riftScreen) {
   }

   private static record RiftArrival(ResourceKey dimension, Vec3 pos) {
   }

   private static record Placement(BlockPos base, boolean alongX) {
   }

   private static record Destination(ServerLevel level, Vec3 pos, boolean fallback) {
   }
}
