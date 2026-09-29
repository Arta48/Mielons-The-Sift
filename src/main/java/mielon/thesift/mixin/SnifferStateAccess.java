package mielon.thesift.mixin;

import net.minecraft.world.entity.animal.sniffer.Sniffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Sniffer.class})
public interface SnifferStateAccess {
   @Invoker("setState")
   Sniffer theSift$setState(Sniffer.State var1);
}
