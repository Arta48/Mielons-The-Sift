package mielon.thesift.client.mixin;

import mielon.thesift.fluid.ModFluids;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   targets = {"net.minecraft.client.particle.DripParticle$FallAndLandParticle"}
)
public abstract class IchorDripLandingParticleMixin {
   @Shadow
   @Final
   @Mutable
   protected ParticleOptions landParticle;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void theSift$rememberIchorLandingSplash(ClientLevel level, double x, double y, double z, Fluid fluid, ParticleOptions landParticle, TextureAtlasSprite sprite, CallbackInfo ci) {
      if (level.dimension().equals(TheSiftDimension.LEVEL_KEY) && landParticle.getType() == ParticleTypes.SPLASH && theSift$hasIchorAbove(level, BlockPos.containing(x, y, z))) {
         this.landParticle = ModParticles.ICHOR_SPLASH;
      }
   }

   private static boolean theSift$hasIchorAbove(ClientLevel level, BlockPos origin) {
      for (int offset = 0; offset <= 24; ++offset) {
         Fluid fluid = level.getFluidState(origin.above(offset)).getType();
         if (fluid == ModFluids.ICHOR || fluid == ModFluids.FLOWING_ICHOR) {
            return true;
         }
      }

      return false;
   }
}
