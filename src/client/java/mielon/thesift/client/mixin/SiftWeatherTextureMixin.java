package mielon.thesift.client.mixin;

import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WeatherEffectRenderer.class})
public abstract class SiftWeatherTextureMixin {
   private static final Identifier ICHOR_RAIN = Identifier.fromNamespaceAndPath("the_sift", "textures/environment/ichor_rain.png");
   private static final Identifier ICHOR_SNOW = Identifier.fromNamespaceAndPath("the_sift", "textures/environment/ichor_snow.png");
   @Shadow
   private AbstractTexture rainTexture;
   @Shadow
   private AbstractTexture snowTexture;
   @Shadow
   @Final
   private TextureManager textureManager;
   @Unique
   private AbstractTexture theSift$ichorRainTexture;
   @Unique
   private AbstractTexture theSift$ichorSnowTexture;

   @Redirect(
      method = {"extractRenderState"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
)}
   )
   private Biome.Precipitation theSift$restrictSnowColumns(ClientLevel level, BlockPos pos) {
      Biome.Precipitation original = level.getPrecipitationAt(pos);
      if (!level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         return original;
      } else if (level.getBiome(pos).is(TheSiftDimension.ICHOR_SNOWY_PEAKS)) {
         return Precipitation.SNOW;
      } else {
         return original == Precipitation.SNOW ? Precipitation.RAIN : original;
      }
   }

   @Inject(
      method = {"prepare"},
      at = {@At("HEAD")}
   )
   private void theSift$prepareIchorWeatherTextures(Vec3 cameraPos, WeatherRenderState renderState, CallbackInfo ci) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         if (this.theSift$ichorRainTexture == null) {
            this.theSift$ichorRainTexture = this.textureManager.getTexture(ICHOR_RAIN);
         }

         if (this.theSift$ichorSnowTexture == null) {
            this.theSift$ichorSnowTexture = this.textureManager.getTexture(ICHOR_SNOW);
         }

      }
   }

   @ModifyVariable(
      method = {"renderWeather"},
      at = {@At("HEAD")},
      argsOnly = true,
      ordinal = 0
   )
   private AbstractTexture theSift$useIchorWeatherTexture(AbstractTexture original) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         if (original == this.rainTexture && this.theSift$ichorRainTexture != null) {
            return this.theSift$ichorRainTexture;
         } else {
            return original == this.snowTexture && this.theSift$ichorSnowTexture != null ? this.theSift$ichorSnowTexture : original;
         }
      } else {
         return original;
      }
   }
}
