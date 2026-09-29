package mielon.thesift.portal;

import java.util.Optional;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;

public final class PortalIgnition {
   private PortalIgnition() {
   }

   public static boolean tryIgnite(ServerLevel level, BlockPos reinforcedDeepslatePos) {
      if (!level.getBlockState(reinforcedDeepslatePos).is(Blocks.REINFORCED_DEEPSLATE)) {
         return false;
      } else {
         Optional<PortalFrameScanner.Frame> frame = PortalFrameScanner.scan(level, reinforcedDeepslatePos);
         if (frame.isEmpty()) {
            return false;
         } else {
            PortalGrowth.start(level, ((PortalFrameScanner.Frame)frame.get()).interior(), ((PortalFrameScanner.Frame)frame.get()).axis());
            level.playSound((Entity)null, reinforcedDeepslatePos, ModSounds.SIFT_PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
         }
      }
   }
}
