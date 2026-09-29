package mielon.thesift.mixin;

import mielon.thesift.fluid.ModFluids;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AgeableMob.class})
public abstract class SnifferBabyMixin {
   @Inject(
      method = {"finalizeSpawn"},
      at = {@At("RETURN")}
   )
   private void theSift$rollNaturalSnifferAge(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData spawnData, CallbackInfoReturnable cir) {
      if ((Object)this instanceof Sniffer sniffer) {
         if (level.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)) {
            if (isIchorSpawn(level, sniffer.blockPosition())) {
               sniffer.discard();
               return;
            }

            if ((reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) && isBabyRoll(sniffer, level)) {
               sniffer.setBaby(true);
            }

            return;
         }
      }

   }

   private static boolean isIchorSpawn(ServerLevelAccessor level, BlockPos pos) {
      return isIchor(level, pos) || isIchor(level, pos.below());
   }

   private static boolean isIchor(ServerLevelAccessor level, BlockPos pos) {
      Fluid fluid = level.getFluidState(pos).getType();
      return fluid == ModFluids.ICHOR || fluid == ModFluids.FLOWING_ICHOR;
   }

   private static boolean isBabyRoll(Sniffer sniffer, ServerLevelAccessor level) {
      long value = level.getLevel().getSeed();
      value ^= sniffer.blockPosition().asLong() * -7046029254386353131L;
      value ^= sniffer.getUUID().getLeastSignificantBits();
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      value *= -7723592293110705685L;
      value ^= value >>> 31;
      return Long.remainderUnsigned(value, 10L) == 0L;
   }
}
