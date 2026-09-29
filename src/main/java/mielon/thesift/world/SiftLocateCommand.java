package mielon.thesift.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import mielon.thesift.worldgen.SiftLandmarkPlacement;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class SiftLocateCommand {
   private SiftLocateCommand() {
   }

   public static void register() {
   }

   public static int locateMainPortal(CommandSourceStack source) {
      if (!source.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         source.sendFailure(Component.literal("The Sift main portal can only be located inside The Sift."));
         return 0;
      } else {
         ServerLevel sift = source.getLevel();
         Optional<BlockPos> located = SiftWorldStorage.getPortalAnchor(source.getServer());
         if (located.isEmpty()) {
            located = SiftPortalStructure.ensurePortal(source.getServer(), sift);
         }

         if (located.isEmpty()) {
            source.sendFailure(Component.literal("The Sift main portal could not be generated."));
            return 0;
         } else {
            return sendLocated(source, "the_sift:main_portal", (BlockPos)located.get());
         }
      }
   }

   public static int locateAbandonedPortal(CommandSourceStack source) {
      if (!isInsideSift(source)) {
         return 0;
      } else {
         ServerLevel level = source.getLevel();
         int sourceChunkX = Math.floorDiv((int)Math.floor(source.getPosition().x), 16);
         int sourceChunkZ = Math.floorDiv((int)Math.floor(source.getPosition().z), 16);
         int cellSize = 32;
         int centerCellX = Math.floorDiv(sourceChunkX, cellSize);
         int centerCellZ = Math.floorDiv(sourceChunkZ, cellSize);
         List<LandmarkCandidate> candidates = collectCandidates(level, centerCellX, centerCellZ, cellSize, 4702392765337521733L, 1, source);
         return locateGeneratedLandmark(source, "the_sift:abandoned_main_portal", SiftLandmarkTracker.Kind.ABANDONED_MAIN_PORTAL, candidates, 32);
      }
   }

   private static int locateGeneratedLandmark(CommandSourceStack source, String id, SiftLandmarkTracker.Kind kind, List candidates, int generationBudget) {
      ServerLevel level = source.getLevel();
      SiftLandmarkTracker.tick(source.getServer());
      BlockPos origin = BlockPos.containing(source.getPosition());
      Optional<BlockPos> best = SiftLandmarkTracker.nearest(kind, origin);
      double bestDistance = (Double)best.map((pos) -> horizontalDistanceSquared(source, pos.getX(), pos.getZ())).orElse(Double.POSITIVE_INFINITY);
      int generated = 0;

      for (LandmarkCandidate candidate : (Iterable<LandmarkCandidate>) candidates) {
         if (generated >= generationBudget || candidate.distanceSquared() >= bestDistance) {
            break;
         }

         int chunkX = Math.floorDiv(candidate.pos().getX(), 16);
         int chunkZ = Math.floorDiv(candidate.pos().getZ(), 16);
         level.getChunk(chunkX, chunkZ);
         ++generated;
         Optional<BlockPos> newlyRecorded = SiftLandmarkTracker.nearest(kind, origin);
         if (newlyRecorded.isPresent()) {
            double distance = horizontalDistanceSquared(source, ((BlockPos)newlyRecorded.get()).getX(), ((BlockPos)newlyRecorded.get()).getZ());
            if (distance < bestDistance) {
               best = newlyRecorded;
               bestDistance = distance;
            }
         }
      }

      if (best.isEmpty()) {
         source.sendFailure(Component.literal("No successfully generated " + id + " was found nearby."));
         return 0;
      } else {
         return sendLocated(source, id, (BlockPos)best.get());
      }
   }

   private static List collectCandidates(ServerLevel level, int centerCellX, int centerCellZ, int cellSize, long salt, int candidateCount, CommandSourceStack source) {
      List<LandmarkCandidate> raw = new ArrayList();

      for (int dx = -7; dx <= 7; ++dx) {
         for (int dz = -7; dz <= 7; ++dz) {
            for (int candidateIndex = 0; candidateIndex < candidateCount; ++candidateIndex) {
               ChunkPos chunk = SiftLandmarkPlacement.candidateChunk(level.getSeed(), centerCellX + dx, centerCellZ + dz, cellSize, salt, candidateIndex);
               int x = (chunk.x() << 4) + 8;
               int z = (chunk.z() << 4) + 8;
               double distanceSquared = horizontalDistanceSquared(source, x, z);
               raw.add(new LandmarkCandidate(new BlockPos(x, 0, z), distanceSquared));
            }
         }
      }

      raw.sort(Comparator.comparingDouble(LandmarkCandidate::distanceSquared));
      List<LandmarkCandidate> matching = new ArrayList();

      for (LandmarkCandidate candidate : raw) {
         int x = candidate.pos().getX();
         int z = candidate.pos().getZ();
         int surface = level.getChunkSource().getGenerator().getBaseHeight(x, z, Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
         matching.add(new LandmarkCandidate(new BlockPos(x, surface, z), candidate.distanceSquared()));
         if (matching.size() >= 96) {
            break;
         }
      }

      matching.sort(Comparator.comparingDouble(LandmarkCandidate::distanceSquared));
      return matching;
   }

   private static boolean isInsideSift(CommandSourceStack source) {
      if (source.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         return true;
      } else {
         source.sendFailure(Component.literal("This landmark can only be located inside The Sift."));
         return false;
      }
   }

   private static int sendLocated(CommandSourceStack source, String id, BlockPos pos) {
      int distance = (int)Math.floor(Math.sqrt(horizontalDistanceSquared(source, pos.getX(), pos.getZ())));
      int var10000 = pos.getX();
      String teleportCommand = "/tp @s " + var10000 + " ~ " + pos.getZ();
      MutableComponent coordinates = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", new Object[]{pos.getX(), "~", pos.getZ()})).withStyle((style) -> style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent.SuggestCommand(teleportCommand)).withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip"))));
      source.sendSuccess(() -> Component.translatable("commands.locate.structure.success", new Object[]{id, coordinates, distance}), false);
      return distance;
   }

   private static double horizontalDistanceSquared(CommandSourceStack source, int x, int z) {
      double dx = (double)x + (double)0.5F - source.getPosition().x;
      double dz = (double)z + (double)0.5F - source.getPosition().z;
      return dx * dx + dz * dz;
   }

   private static record LandmarkCandidate(BlockPos pos, double distanceSquared) {
   }
}
