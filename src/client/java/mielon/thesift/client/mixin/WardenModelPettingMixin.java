package mielon.thesift.client.mixin;

import mielon.thesift.client.entity.WardenPettingRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.warden.WardenModel;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WardenModel.class})
public abstract class WardenModelPettingMixin {
   @Shadow
   @Final
   protected ModelPart rightArm;
   @Shadow
   @Final
   protected ModelPart head;

   @Inject(
      method = {"setupAnim(Lnet/minecraft/client/renderer/entity/state/WardenRenderState;)V"},
      at = {@At("TAIL")}
   )
   private void theSift$animatePetting(WardenRenderState state, CallbackInfo ci) {
      if (((WardenPettingRenderState)state).theSift$isPettingDarkSniffer()) {
         float stroke = Mth.sin((double)(state.ageInTicks * 0.32F)) * 0.16F;
         this.rightArm.xRot = -0.66F + stroke;
         this.rightArm.yRot = -0.1F;
         this.rightArm.zRot = -0.08F;
         this.head.xRot = Math.max(this.head.xRot, 0.22F);
      }
   }
}
