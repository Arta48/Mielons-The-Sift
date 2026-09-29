package mielon.thesift.portal;

import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.entity.SonorousDeepslateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SonorousBeams {
   private static final int RETRACT_DELAY_TICKS = 60;

   private SonorousBeams() {
   }

   public static void start(ServerLevel level, BlockPos notePos, int soundIndex1to8) {
      BlockPos deepslatePos = notePos.below();
      if (level.getBlockState(deepslatePos).is(ModBlocks.SONOROUS_DEEPSLATE)) {
         BlockEntity var5 = level.getBlockEntity(deepslatePos);
         if (var5 instanceof SonorousDeepslateBlockEntity) {
            SonorousDeepslateBlockEntity beam = (SonorousDeepslateBlockEntity)var5;
            beam.startBeam(SonorousColors.packedRGB(soundIndex1to8));
         }

      }
   }

   public static void clearGroup(ServerLevel level, BlockPos anyBlockInGroup) {
      for (BlockPos deepslatePos : (Iterable<BlockPos>) SonorousConsoles.findGroup(level, anyBlockInGroup)) {
         BlockEntity var6 = level.getBlockEntity(deepslatePos);
         if (var6 instanceof SonorousDeepslateBlockEntity beam) {
            beam.clearBeam();
         }
      }

   }

   public static void scheduleRetract(ServerLevel level, BlockPos anyBlockInGroup) {
      for (BlockPos deepslatePos : (Iterable<BlockPos>) SonorousConsoles.findGroup(level, anyBlockInGroup)) {
         BlockEntity var6 = level.getBlockEntity(deepslatePos);
         if (var6 instanceof SonorousDeepslateBlockEntity beam) {
            beam.scheduleShrink(60);
         }
      }

   }
}
