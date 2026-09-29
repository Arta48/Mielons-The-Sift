package mielon.thesift.mixin;

import mielon.thesift.world.SiftDayNightCycle;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.ServerClockManager.MoveResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerClockManager.class})
public abstract class ServerClockManagerMixin {
   @Unique
   private boolean theSift$internalClockWrite;
   @Unique
   private Transition theSift$transition;
   @Unique
   private Holder theSift$markerClock;
   @Unique
   private long theSift$beforeMarker;

   @Inject(
      method = {"setTotalTicks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$smoothSetTime(Holder clock, long targetTicks, CallbackInfo ci) {
      if (!this.theSift$internalClockWrite && theSift$isSiftClock(clock)) {
         if (!clock.equals(this.theSift$markerClock)) {
            ServerClockManager self = (ServerClockManager)(Object)this;
            long currentTicks = self.getInstance(clock).totalTicks();
            if (this.theSift$beginTransition(clock, currentTicks, targetTicks)) {
               ci.cancel();
            }

         }
      }
   }

   @Inject(
      method = {"moveToTimeMarker"},
      at = {@At("HEAD")}
   )
   private void theSift$rememberTimeBeforeMarker(Holder clock, ResourceKey marker, CallbackInfoReturnable cir) {
      if (!this.theSift$internalClockWrite && theSift$isSiftClock(clock)) {
         this.theSift$markerClock = clock;
         this.theSift$beforeMarker = ((ServerClockManager)(Object)this).getInstance(clock).totalTicks();
      } else {
         this.theSift$markerClock = null;
      }

   }

   @Inject(
      method = {"moveToTimeMarker"},
      at = {@At("RETURN")}
   )
   private void theSift$smoothTimeMarker(Holder clock, ResourceKey marker, CallbackInfoReturnable cir) {
      if (cir.getReturnValue() == MoveResult.MOVED && clock.equals(this.theSift$markerClock)) {
         ServerClockManager self = (ServerClockManager)(Object)this;
         long targetTicks = self.getInstance(clock).totalTicks();
         this.theSift$beginTransition(clock, this.theSift$beforeMarker, targetTicks);
         this.theSift$markerClock = null;
      } else {
         this.theSift$markerClock = null;
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void theSift$advanceCommandTransition(CallbackInfo ci) {
      Transition transition = this.theSift$transition;
      if (transition != null) {
         int elapsed = transition.elapsed() + 1;
         long displayedTicks = elapsed >= 60 ? transition.targetTicks() : transition.startTicks() + (long)elapsed;
         this.theSift$transition = elapsed >= 60 ? null : new Transition(transition.clock(), transition.startTicks(), transition.targetTicks(), elapsed);
         this.theSift$internalClockWrite = true;

         try {
            ((ServerClockManager)(Object)this).setTotalTicks(transition.clock(), displayedTicks);
         } finally {
            this.theSift$internalClockWrite = false;
         }

      }
   }

   @Unique
   private boolean theSift$beginTransition(Holder clock, long currentTicks, long targetTicks) {
      float currentNight = SiftDayNightCycle.nightBlend(currentTicks);
      float targetNight = SiftDayNightCycle.nightBlend(targetTicks);
      boolean currentIsNight = currentNight >= 0.5F;
      boolean targetIsNight = targetNight >= 0.5F;
      if (currentIsNight == targetIsNight) {
         this.theSift$transition = null;
         return false;
      } else {
         long targetPeriod = targetTicks - Math.floorMod(targetTicks, 24120L);
         long startTicks;
         if (targetIsNight) {
            startTicks = targetPeriod + 12000L;
         } else {
            startTicks = targetPeriod >= 24120L ? targetPeriod - 60L : 24060L;
         }

         this.theSift$transition = new Transition(clock, startTicks, targetTicks, 0);
         this.theSift$internalClockWrite = true;

         try {
            ((ServerClockManager)(Object)this).setTotalTicks(clock, startTicks);
         } finally {
            this.theSift$internalClockWrite = false;
         }

         return true;
      }
   }

   @Unique
   private static boolean theSift$isSiftClock(Holder clock) {
      return (Boolean)clock.unwrapKey().map((key) -> ((ResourceKey<?>) key).identifier().equals(SiftDayNightCycle.CLOCK_ID)).orElse(false);
   }

   @Unique
   private static record Transition(Holder clock, long startTicks, long targetTicks, int elapsed) {
   }
}
