package mielon.thesift.mixin;

import java.util.Map;
import net.minecraft.core.cauldron.CauldronInteraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CauldronInteraction.Dispatcher.class})
public interface CauldronDispatcherAccessor {
   @Accessor("items")
   Map theSift$getItems();
}
