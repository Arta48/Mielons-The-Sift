package mielon.thesift.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

public final class SonorousEffects {
   private static final int BURST_PARTICLE_COUNT = 3;
   private static final double SPREAD_HORIZONTAL = 0.9;
   private static final double HEIGHT_BASE = 1.1;
   private static final double HEIGHT_RANDOM = 0.4;
   private static final double THE_SIFT_NOTE_MARKER = (double)100.0F;

   private SonorousEffects() {
   }

   public static void spawnColorBurst(ServerLevel level, BlockPos notePos, int soundIndex1to8) {
      RandomSource random = level.getRandom();

      for (int shadeIndex = 0; shadeIndex < 3; ++shadeIndex) {
         double ox = (random.nextDouble() - (double)0.5F) * 0.9;
         double oy = 1.1 + random.nextDouble() * 0.4;
         double oz = (random.nextDouble() - (double)0.5F) * 0.9;
         double encodedColor = (double)100.0F + (double)((soundIndex1to8 - 1) * 3) + (double)shadeIndex;
         level.sendParticles(ParticleTypes.NOTE, (double)notePos.getX() + (double)0.5F + ox, (double)notePos.getY() + oy, (double)notePos.getZ() + (double)0.5F + oz, 0, encodedColor, (double)0.0F, (double)0.0F, (double)1.0F);
      }

   }
}
