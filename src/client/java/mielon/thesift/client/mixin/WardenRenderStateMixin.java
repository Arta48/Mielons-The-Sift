package mielon.thesift.client.mixin;

import mielon.thesift.client.entity.WardenPettingRenderState;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({WardenRenderState.class})
public abstract class WardenRenderStateMixin implements WardenPettingRenderState {
   @Unique
   private boolean theSift$pettingDarkSniffer;

   public boolean theSift$isPettingDarkSniffer() {
      return this.theSift$pettingDarkSniffer;
   }

   public void theSift$setPettingDarkSniffer(boolean petting) {
      this.theSift$pettingDarkSniffer = petting;
   }
}
