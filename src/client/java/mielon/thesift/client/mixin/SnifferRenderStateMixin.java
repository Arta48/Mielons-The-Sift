package mielon.thesift.client.mixin;

import mielon.thesift.client.entity.SnifferInfectionRenderState;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({SnifferRenderState.class})
public abstract class SnifferRenderStateMixin implements SnifferInfectionRenderState {
   @Unique
   private boolean theSift$infected;

   public boolean theSift$isInfected() {
      return this.theSift$infected;
   }

   public void theSift$setInfected(boolean infected) {
      this.theSift$infected = infected;
   }
}
