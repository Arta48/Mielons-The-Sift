package mielon.thesift.client.mixin;

import mielon.thesift.client.particle.IchorFluidParticle;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ParticleEngine.class})
public abstract class IchorWaterParticleMixin {
   @Inject(
      method = {"createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$replaceBlueWaterParticles(ParticleOptions options, double x, double y, double z, double xd, double yd, double zd, CallbackInfoReturnable cir) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         boolean siftRain = options.getType() == ParticleTypes.RAIN && level.dimension().equals(TheSiftDimension.LEVEL_KEY) && level.isRaining();
         ParticleType<?> replacement = (ParticleType<?>)(siftRain ? ModParticles.ICHOR_RAIN_SPLASH : theSift$replacement(options.getType()));
         if (replacement != null && (siftRain || theSift$isNearIchor(level, BlockPos.containing(x, y, z))) && replacement instanceof ParticleOptions) {
            ParticleOptions replacementOptions = (ParticleOptions)replacement;
            Particle custom = ((ParticleEngine)(Object)this).createParticle(replacementOptions, x, y, z, xd, yd, zd);
            cir.setReturnValue(custom);
         }
      }
   }

   @Inject(
      method = {"createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;"},
      at = {@At("RETURN")}
   )
   private void theSift$tintIchorWaterParticles(ParticleOptions options, double x, double y, double z, double xd, double yd, double zd, CallbackInfoReturnable cir) {
      Particle particle = (Particle)cir.getReturnValue();
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && theSift$isWaterParticle(options.getType())) {
         BlockPos pos = BlockPos.containing(x, y, z);
         boolean siftRainParticle = level.dimension().equals(TheSiftDimension.LEVEL_KEY) && level.isRaining() && theSift$isWeatherWaterParticle(options.getType());
         if (siftRainParticle || theSift$isNearIchor(level, pos) || theSift$isDripParticle(options.getType()) && theSift$hasIchorAbove(level, pos)) {
            if (particle instanceof SingleQuadParticle) {
               SingleQuadParticle quad = (SingleQuadParticle)particle;
               if (siftRainParticle && options.getType() == ParticleTypes.RAIN) {
                  IchorFluidParticle.tintIchor(quad, level, x, y, z, true);
               } else {
                  double time = (double)level.getGameTime() * 0.035;
                  boolean drip = theSift$isDripParticle(options.getType());
                  double variation = drip ? Math.sin(x * 12.9898 + y * 31.733 + z * 7.271 + (double)level.getGameTime() * 0.11) * 0.24 : (double)0.0F;
                  float phase = (float)Mth.clamp(Math.sin(x * 0.19 + z * 0.13 + time) * (double)0.5F + (double)0.5F + variation, (double)0.0F, (double)1.0F);
                  float second = (float)Mth.clamp(Math.sin(x * -0.11 + y * 0.17 + z * 0.21 - time * 0.72) * (double)0.5F + (double)0.5F - variation * 0.72, (double)0.0F, (double)1.0F);
                  float red = Mth.lerp(phase, 0.0F, 1.0F);
                  float green = Mth.lerp(phase, 0.96F, 0.07F);
                  float blue = Mth.lerp(phase, 1.0F, 0.82F);
                  float gold = Mth.clamp((second - 0.6F) / 0.4F, 0.0F, 1.0F) * 0.58F;
                  red = Mth.lerp(gold, red, 1.0F);
                  green = Mth.lerp(gold, green, 0.54F);
                  blue = Mth.lerp(gold, blue, 0.04F);
                  quad.setColor(red, green, blue);
               }
            }
         }
      }
   }

   private static boolean theSift$isNearIchor(ClientLevel level, BlockPos pos) {
      if (!theSift$isIchor(level.getFluidState(pos)) && !theSift$isIchor(level.getFluidState(pos.below())) && !theSift$isIchor(level.getFluidState(pos.below(2)))) {
         BlockPos below = pos.below();
         return theSift$isIchor(level.getFluidState(below.north())) || theSift$isIchor(level.getFluidState(below.south())) || theSift$isIchor(level.getFluidState(below.east())) || theSift$isIchor(level.getFluidState(below.west()));
      } else {
         return true;
      }
   }

   private static boolean theSift$isIchor(FluidState state) {
      return state.getType() == ModFluids.ICHOR || state.getType() == ModFluids.FLOWING_ICHOR;
   }

   private static boolean theSift$isWaterParticle(ParticleType type) {
      return type == ParticleTypes.UNDERWATER || type == ParticleTypes.CURRENT_DOWN || type == ParticleTypes.RAIN || type == ParticleTypes.DRIPPING_WATER || type == ParticleTypes.FALLING_WATER || type == ParticleTypes.DRIPPING_DRIPSTONE_WATER || type == ParticleTypes.FALLING_DRIPSTONE_WATER;
   }

   private static ParticleType theSift$replacement(ParticleType type) {
      if (type == ParticleTypes.BUBBLE) {
         return ModParticles.ICHOR_BUBBLE;
      } else {
         return type == ParticleTypes.SPLASH ? ModParticles.ICHOR_SPLASH : null;
      }
   }

   private static boolean theSift$isDripParticle(ParticleType type) {
      return type == ParticleTypes.DRIPPING_WATER || type == ParticleTypes.FALLING_WATER || type == ParticleTypes.DRIPPING_DRIPSTONE_WATER || type == ParticleTypes.FALLING_DRIPSTONE_WATER;
   }

   private static boolean theSift$isWeatherWaterParticle(ParticleType type) {
      return type == ParticleTypes.RAIN || type == ParticleTypes.DRIPPING_WATER || type == ParticleTypes.FALLING_WATER;
   }

   private static boolean theSift$hasIchorAbove(ClientLevel level, BlockPos pos) {
      for (int offset = 1; offset <= 14; ++offset) {
         if (theSift$isIchor(level.getFluidState(pos.above(offset)))) {
            return true;
         }
      }

      return false;
   }
}
