package mielon.thesift.portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PortalFrameScanner {
   private static final int MIN_TOTAL_SIZE = 3;
   private static final int MAX_SCAN = 23;

   public static Optional scan(Level level, BlockPos clicked) {
      Optional<Frame> onX = scanAxis(level, clicked, Axis.X, false);
      return onX.isPresent() ? onX : scanAxis(level, clicked, Axis.Z, false);
   }

   public static Optional scanForClosing(Level level, BlockPos clicked) {
      Optional<Frame> onX = scanAxis(level, clicked, Axis.X, true);
      return onX.isPresent() ? onX : scanAxis(level, clicked, Axis.Z, true);
   }

   private static boolean isFrame(Level level, BlockPos pos) {
      return level.getBlockState(pos).is(Blocks.REINFORCED_DEEPSLATE);
   }

   public static boolean isAirLikeInterior(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.isAir() || state.is(Blocks.SCULK_VEIN) || state.is(Blocks.GLOW_LICHEN);
   }

   private static boolean isOpenInterior(Level level, BlockPos pos, boolean closing) {
      if (isAirLikeInterior(level, pos)) {
         return true;
      } else {
         return closing && level.getBlockState(pos).is(ModBlocks.SIFT_PORTAL);
      }
   }

   private static Optional scanAxis(Level level, BlockPos clicked, Direction.Axis axis, boolean closing) {
      Direction positiveDir = axis == Axis.X ? Direction.EAST : Direction.SOUTH;
      Direction negativeDir = positiveDir.getOpposite();
      BlockPos anchor = clicked;
      int steps = 0;

      while(isFrame(level, anchor.below())) {
         anchor = anchor.below();
         ++steps;
         if (steps > 23) {
            return Optional.empty();
         }
      }

      steps = 0;

      while(isFrame(level, anchor.relative(negativeDir))) {
         anchor = anchor.relative(negativeDir);
         ++steps;
         if (steps > 23) {
            return Optional.empty();
         }
      }

      int width = 1;

      while(isFrame(level, anchor.relative(positiveDir, width))) {
         ++width;
         if (width > 23) {
            return Optional.empty();
         }
      }

      int height = 1;

      while(isFrame(level, anchor.above(height))) {
         ++height;
         if (height > 23) {
            return Optional.empty();
         }
      }

      if (width >= 3 && height >= 3) {
         List<BlockPos> interior = new ArrayList();
         boolean hasPortalBlock = false;

         for (int w = 0; w < width; ++w) {
            for (int h = 0; h < height; ++h) {
               BlockPos pos = anchor.relative(positiveDir, w).above(h);
               boolean border = w == 0 || w == width - 1 || h == 0 || h == height - 1;
               if (border) {
                  if (!isFrame(level, pos)) {
                     return Optional.empty();
                  }
               } else {
                  if (!isOpenInterior(level, pos, closing)) {
                     return Optional.empty();
                  }

                  interior.add(pos);
                  if (level.getBlockState(pos).is(ModBlocks.SIFT_PORTAL)) {
                     hasPortalBlock = true;
                  }
               }
            }
         }

         if (closing && !hasPortalBlock) {
            return Optional.empty();
         } else {
            double middle = (double)(width - 1) * (double)0.5F;
            Vec3 bottomCenter = new Vec3((double)anchor.getX() + (double)0.5F + (double)positiveDir.getStepX() * middle, (double)anchor.getY() + (double)1.0F, (double)anchor.getZ() + (double)0.5F + (double)positiveDir.getStepZ() * middle);
            return Optional.of(new Frame(interior, axis, bottomCenter));
         }
      } else {
         return Optional.empty();
      }
   }

   public static record Frame(List interior, Direction.Axis axis, Vec3 bottomCenter) {
   }
}
