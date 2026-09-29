package mielon.thesift.mixin;

import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.WeatherData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerLevel.class})
public abstract class ServerLevelWeatherMixin {
   @Redirect(
      method = {"tickPrecipitation"},
      at = {@At(
   value = "FIELD",
   target = "Lnet/minecraft/world/level/block/Blocks;SNOW:Lnet/minecraft/world/level/block/Block;"
)}
   )
   private Block theSift$useIchorSnowDuringPrecipitation() {
      ServerLevel self = (ServerLevel)(Object)this;
      return (Block)(self.dimension().equals(TheSiftDimension.LEVEL_KEY) ? ModBlocks.ICHOR_SNOW : Blocks.SNOW);
   }

   @Redirect(
      method = {"tickPrecipitation"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
)}
   )
   private boolean theSift$restrictSnowToIchorSnowyPeaks(Biome biome, LevelReader level, BlockPos pos) {
      ServerLevel self = (ServerLevel)(Object)this;
      if (self.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         if (!self.getBiome(pos).is(TheSiftDimension.ICHOR_SNOWY_PEAKS)) {
            return false;
         } else {
            BlockState state = level.getBlockState(pos);
            return level.isInsideBuildHeight(pos.getY()) && level.getBrightness(LightLayer.BLOCK, pos) < 10 && (state.isAir() || state.is(ModBlocks.ICHOR_SNOW)) && ModBlocks.ICHOR_SNOW.defaultBlockState().canSurvive(level, pos);
         }
      } else {
         return biome.shouldSnow(level, pos);
      }
   }

   @ModifyArg(
      method = {"tickPrecipitation"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
)},
      index = 1
   )
   private BlockState theSift$capIchorSnowLayers(BlockState state) {
      ServerLevel self = (ServerLevel)(Object)this;
      return self.dimension().equals(TheSiftDimension.LEVEL_KEY) && state.is(ModBlocks.ICHOR_SNOW) && (Integer)state.getValue(SnowLayerBlock.LAYERS) > 2 ? (BlockState)state.setValue(SnowLayerBlock.LAYERS, 2) : state;
   }

   @Inject(
      method = {"getWeatherData"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$getPerDimensionWeather(CallbackInfoReturnable cir) {
      ServerLevel self = (ServerLevel)(Object)this;
      if (!self.dimension().equals(Level.OVERWORLD)) {
         cir.setReturnValue((WeatherData)self.getDataStorage().computeIfAbsent(WeatherData.TYPE));
      }

   }

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void theSift$initializePerDimensionWeatherLevels(CallbackInfo ci) {
      ServerLevel self = (ServerLevel)(Object)this;
      if (!self.dimension().equals(Level.OVERWORLD)) {
         WeatherData weather = self.getWeatherData();
         self.setRainLevel(weather.isRaining() ? 1.0F : 0.0F);
         self.setThunderLevel(weather.isThundering() ? 1.0F : 0.0F);
      }
   }

   @Redirect(
      method = {"advanceWeatherCycle"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"
)}
   )
   private void theSift$broadcastWeatherInsideDimension(PlayerList players, Packet packet) {
      ServerLevel self = (ServerLevel)(Object)this;
      players.broadcastAll(packet, self.dimension());
   }
}
