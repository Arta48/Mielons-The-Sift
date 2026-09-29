package mielon.thesift.client.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Particle.class})
public interface NoteParticleVelocityAccessor {
   @Accessor("yd")
   double theSift$getVelocityY();
}
