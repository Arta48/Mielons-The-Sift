package mielon.thesift.mixin;

import mielon.thesift.world.SiftWeatherCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.WeatherCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({WeatherCommand.class})
public abstract class WeatherCommandMixin {
   @Inject(
      method = {"setClear"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void theSift$setClearInSourceDimension(CommandSourceStack source, int duration, CallbackInfoReturnable cir) {
      cir.setReturnValue(SiftWeatherCommand.setCurrent(source, SiftWeatherCommand.Mode.CLEAR, duration));
   }

   @Inject(
      method = {"setRain"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void theSift$setRainInSourceDimension(CommandSourceStack source, int duration, CallbackInfoReturnable cir) {
      cir.setReturnValue(SiftWeatherCommand.setCurrent(source, SiftWeatherCommand.Mode.RAIN, duration));
   }

   @Inject(
      method = {"setThunder"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void theSift$setThunderInSourceDimension(CommandSourceStack source, int duration, CallbackInfoReturnable cir) {
      cir.setReturnValue(SiftWeatherCommand.setCurrent(source, SiftWeatherCommand.Mode.THUNDER, duration));
   }
}
