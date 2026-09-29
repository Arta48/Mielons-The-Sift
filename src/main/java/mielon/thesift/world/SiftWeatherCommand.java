package mielon.thesift.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.saveddata.WeatherData;

public final class SiftWeatherCommand {
   private SiftWeatherCommand() {
   }

   public static void register() {
      CommandRegistrationCallback.EVENT.register((CommandRegistrationCallback)(dispatcher, registryAccess, environment) -> registerDimensionArguments(dispatcher));
   }

   private static void registerDimensionArguments(CommandDispatcher dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> root = (LiteralArgumentBuilder)Commands.literal("weather").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

      for (Mode mode : SiftWeatherCommand.Mode.values()) {
         root.then(((LiteralArgumentBuilder)Commands.literal(mode.commandName).then(Commands.argument("dimension", DimensionArgument.dimension()).executes((context) -> set((CommandSourceStack)context.getSource(), DimensionArgument.getDimension(context, "dimension"), mode, -1, true)))).then(Commands.argument("duration", TimeArgument.time(1)).then(Commands.argument("dimension", DimensionArgument.dimension()).executes((context) -> set((CommandSourceStack)context.getSource(), DimensionArgument.getDimension(context, "dimension"), mode, IntegerArgumentType.getInteger(context, "duration"), true)))));
      }

      dispatcher.register(root);
   }

   public static int setCurrent(CommandSourceStack source, Mode mode, int requestedDuration) {
      return set(source, source.getLevel(), mode, requestedDuration, false);
   }

   private static int set(CommandSourceStack source, ServerLevel target, Mode mode, int requestedDuration, boolean showDimension) {
      int duration = resolveDuration(target, mode, requestedDuration);
      WeatherData weather = target.getWeatherData();
      weather.setClearWeatherTime(mode == SiftWeatherCommand.Mode.CLEAR ? duration : 0);
      weather.setRainTime(mode == SiftWeatherCommand.Mode.CLEAR ? 0 : duration);
      weather.setThunderTime(mode == SiftWeatherCommand.Mode.THUNDER ? duration : 0);
      weather.setRaining(mode != SiftWeatherCommand.Mode.CLEAR);
      weather.setThundering(mode == SiftWeatherCommand.Mode.THUNDER);
      source.sendSuccess(() -> {
         MutableComponent message = Component.translatable("commands.weather.set." + mode.commandName);
         return showDimension ? message.append(" [" + String.valueOf(target.dimension().identifier()) + "]") : message;
      }, true);
      return duration;
   }

   private static int resolveDuration(ServerLevel target, Mode mode, int requestedDuration) {
      if (requestedDuration >= 0) {
         return requestedDuration;
      } else {
         IntProvider var10000;
         switch (mode.ordinal()) {
            case 0 -> var10000 = ServerLevel.RAIN_DELAY;
            case 1 -> var10000 = ServerLevel.RAIN_DURATION;
            case 2 -> var10000 = ServerLevel.THUNDER_DURATION;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         IntProvider provider = var10000;
         return provider.sample(target.getRandom());
      }
   }

   public static enum Mode {
      CLEAR("clear"),
      RAIN("rain"),
      THUNDER("thunder");

      private final String commandName;

      private Mode(String commandName) {
         this.commandName = commandName;
      }

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{CLEAR, RAIN, THUNDER};
      }
   }
}
