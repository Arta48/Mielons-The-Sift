package mielon.thesift.client.mixin;

import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"net.irisshaders.iris.uniforms.WorldTimeUniforms"},
   remap = false
)
public abstract class IrisWorldTimeUniformsMixin {
   private static final int SHADER_DAY_TIME = 3000;
   private static final int SHADER_NIGHT_TIME = 18000;
   private static final int VANILLA_DAY_LENGTH = 24000;

   @Inject(
      method = {"getWorldDayTime()I"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0,
      remap = false
   )
   private static void theSift$mapWorldTime(CallbackInfoReturnable cir) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         cir.setReturnValue(theSift$shaderTime(level.getDefaultClockTime()));
      }
   }

   @Inject(
      method = {"getWorldDay()I"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0,
      remap = false
   )
   private static void theSift$mapWorldDay(CallbackInfoReturnable cir) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         cir.setReturnValue((int)Math.floorDiv(level.getDefaultClockTime(), 24120L));
      }
   }

   private static int theSift$shaderTime(long totalTicks) {
      long tick = Math.floorMod(totalTicks, 24120L);
      if (tick < 12000L) {
         return 3000;
      } else if (tick < 12060L) {
         float t = theSift$smooth((float)(tick - 12000L) / 60.0F);
         return Math.round(3000.0F + 15000.0F * t);
      } else if (tick < 24060L) {
         return 18000;
      } else {
         float t = theSift$smooth((float)(tick - 24060L) / 60.0F);
         int unwrapped = Math.round(18000.0F + 9000.0F * t);
         return Math.floorMod(unwrapped, 24000);
      }
   }

   private static float theSift$smooth(float value) {
      float clamped = Math.max(0.0F, Math.min(1.0F, value));
      return clamped * clamped * (3.0F - 2.0F * clamped);
   }
}
