package mielon.thesift.mixin;

import mielon.thesift.item.CharoiteEnchanting;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AnvilMenu.class})
public abstract class AnvilMenuMixin {
   @Shadow
   @Final
   private DataSlot cost;

   @Inject(
      method = {"createResult"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$blockCharoiteCombination(CallbackInfo ci) {
      AnvilMenu menu = (AnvilMenu)(Object)this;
      ItemStack base = menu.getSlot(0).getItem();
      ItemStack addition = menu.getSlot(1).getItem();
      if (CharoiteEnchanting.blocksAnvil(base, addition)) {
         menu.getSlot(2).set(ItemStack.EMPTY);
         this.cost.set(1);
         ci.cancel();
      }
   }
}
