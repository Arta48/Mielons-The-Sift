package mielon.thesift.mixin;

import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.entity.SingerSummoner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InstrumentItem.class})
public abstract class GoatHornMixin {
   @Inject(
      method = {"play"},
      at = {@At("HEAD")}
   )
   private static void theSift$onInstrumentFinished(Level level, Player player, Instrument instrument, CallbackInfo ci) {
      if (level instanceof ServerLevel serverLevel) {
         SingerSummoner.onGoatHornFinished(serverLevel, player);
         if (player instanceof ServerPlayer serverPlayer) {
            if (SingerSummoner.isNearAncientCityCenter(serverLevel, player.position())) {
               ModAdvancements.award(serverPlayer, "story/song_of_the_past");
            }
         }

      }
   }
}
