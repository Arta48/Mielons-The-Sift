package mielon.thesift.mixin;

import java.util.Map;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CauldronInteraction.Dispatcher.class})
public interface CauldronDispatcherAccessor {
   @Accessor("items")
   Map<Item, CauldronInteraction> theSift$getItems();
}
