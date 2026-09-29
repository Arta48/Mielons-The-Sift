package mielon.thesift.portal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.entity.SiftPortalBlockEntity;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PortalGrowth {
   public static void register() {
   }

   public static void start(ServerLevel level, List interior, Direction.Axis axis) {
      cancelAnimationsfor (level, interior);
      List<BlockPos> queue = new ArrayList(interior);
      sortFromEdgesToCenter(queue, axis);
      if (!queue.isEmpty()) {
         BlockPos anchorPos = (BlockPos)queue.get(0);
         if (PortalFrameScanner.isAirLikeInterior(level, anchorPos)) {
            level.setBlockAndUpdate(anchorPos, ModBlocks.SIFT_PORTAL.defaultBlockState());
            level.sendParticles(ParticleTypes.END_ROD, (double)anchorPos.getX() + (double)0.5F, (double)anchorPos.getY() + (double)0.5F, (double)anchorPos.getZ() + (double)0.5F, 5, 0.32, 0.32, 0.32, 0.025);
            level.playSound((Entity)null, anchorPos, ModSounds.SIFT_PORTAL_AMBIENT, SoundSource.BLOCKS, 0.3F, 1.15F);
         }

         BlockEntity var6 = level.getBlockEntity(anchorPos);
         if (var6 instanceof SiftPortalBlockEntity) {
            SiftPortalBlockEntity controller = (SiftPortalBlockEntity)var6;
            level.playSound((Entity)null, anchorPos, ModSounds.THE_SIFT_PORTAL_OPEN, SoundSource.BLOCKS, 4.0F, 1.0F);
            controller.startGrowth(queue, 1);
         }
      }
   }

   public static void startClosing(ServerLevel level, List interior, Direction.Axis axis) {
      List<BlockPos> queue = new ArrayList();

      for (BlockPos pos : (Iterable<BlockPos>) interior) {
         if (level.getBlockState(pos).is(ModBlocks.SIFT_PORTAL)) {
            queue.add(pos);
         }
      }

      if (!queue.isEmpty()) {
         cancelAnimationsfor (level, queue);
         sortFromCenterToEdges(queue, axis);
         BlockPos anchorPos = (BlockPos)queue.get(queue.size() - 1);
         BlockEntity var6 = level.getBlockEntity(anchorPos);
         if (var6 instanceof SiftPortalBlockEntity) {
            SiftPortalBlockEntity controller = (SiftPortalBlockEntity)var6;
            level.playSound((Entity)null, anchorPos, ModSounds.THE_SIFT_PORTAL_CLOSE, SoundSource.BLOCKS, 4.0F, 1.0F);
            controller.startClosing(queue);
         }
      }
   }

   private static void sortFromEdgesToCenter(List queue, Direction.Axis axis) {
      sortFromCenterToEdges(queue, axis);
      Collections.reverse(queue);
   }

   private static void sortFromCenterToEdges(List queue, Direction.Axis axis) {
      if (!queue.isEmpty()) {
         boolean useX = axis == Axis.X;
         double minW = Double.POSITIVE_INFINITY;
         double maxW = Double.NEGATIVE_INFINITY;
         double minH = Double.POSITIVE_INFINITY;
         double maxH = Double.NEGATIVE_INFINITY;

         for (BlockPos pos : (Iterable<BlockPos>) queue) {
            double w = useX ? (double)pos.getX() : (double)pos.getZ();
            double h = (double)pos.getY();
            minW = Math.min(minW, w);
            maxW = Math.max(maxW, w);
            minH = Math.min(minH, h);
            maxH = Math.max(maxH, h);
         }

         double centerW = (minW + maxW) * (double)0.5F;
         double centerH = (minH + maxH) * (double)0.5F;
         queue.sort(Comparator.comparing((posx) -> {
            BlockPos p = (BlockPos) posx;
            int w = useX ? p.getX() : p.getZ();
            int h = p.getY();
            int depth = useX ? p.getZ() : p.getX();
            return PortalSortKey.fromCenter(w, h, depth, centerW, centerH);
         }));
      }
   }

   private static void cancelAnimationsfor (ServerLevel level, List positions) {
      if (!positions.isEmpty()) {
         Set<BlockPos> positionSet = new HashSet(positions);

         for (BlockPos pos : (Iterable<BlockPos>) positions) {
            BlockEntity var6 = level.getBlockEntity(pos);
            if (var6 instanceof SiftPortalBlockEntity) {
               SiftPortalBlockEntity portal = (SiftPortalBlockEntity)var6;
               if (portal.isAnimatingPortal() && portal.animationOverlaps(positionSet)) {
                  portal.stopPortalAnimation();
               }
            }
         }

      }
   }
}
