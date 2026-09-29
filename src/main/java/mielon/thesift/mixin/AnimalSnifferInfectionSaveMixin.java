package mielon.thesift.mixin;

import mielon.thesift.entity.SnifferInfectionAccess;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Animal.class})
public abstract class AnimalSnifferInfectionSaveMixin {
   @Inject(
      method = {"addAdditionalSaveData"},
      at = {@At("TAIL")}
   )
   private void theSift$saveSnifferInfection(ValueOutput output, CallbackInfo ci) {
      if ((Object)this instanceof Sniffer && (Object)this instanceof SnifferInfectionAccess) {
         SnifferInfectionAccess access = (SnifferInfectionAccess)this;
         if (access.theSift$getSculkInfectionTicks() > 0) {
            output.putInt("TheSiftSculkInfection", access.theSift$getSculkInfectionTicks());
         }
      }

   }

   @Inject(
      method = {"readAdditionalSaveData"},
      at = {@At("TAIL")}
   )
   private void theSift$loadSnifferInfection(ValueInput input, CallbackInfo ci) {
      if ((Object)this instanceof Sniffer && (Object)this instanceof SnifferInfectionAccess) {
         SnifferInfectionAccess access = (SnifferInfectionAccess)this;
         access.theSift$setSculkInfectionTicks(input.getIntOr("TheSiftSculkInfection", 0));
      }

   }
}
