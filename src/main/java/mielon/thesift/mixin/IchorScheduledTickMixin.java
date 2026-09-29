package mielon.thesift.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mielon.thesift.fluid.IchorWaterlogging;
import mielon.thesift.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({LevelAccessor.class})
public interface IchorScheduledTickMixin {
   @ModifyVariable(
      method = {"createTick"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private Object theSift$scheduleActualFluid(Object type, @Local(argsOnly = true) BlockPos pos) {
      return (type == Fluids.WATER || type == Fluids.FLOWING_WATER) && IchorWaterlogging.isIchorlogged(((LevelAccessor)this).getBlockState(pos)) ? ModFluids.ICHOR : type;
   }
}
