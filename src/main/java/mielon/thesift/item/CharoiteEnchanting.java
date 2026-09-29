package mielon.thesift.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class CharoiteEnchanting {
   private static final String USED_TAG = "the_sift:charoite_used";

   private CharoiteEnchanting() {
   }

   public static void markUsed(ItemStack stack) {
      if (!stack.isEmpty()) {
         CustomData.update(DataComponents.CUSTOM_DATA, stack, (tag) -> tag.putBoolean("the_sift:charoite_used", true));
      }

   }

   public static boolean wasUsed(ItemStack stack) {
      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      return data != null && data.copyTag().getBooleanOr("the_sift:charoite_used", false);
   }

   public static boolean blocksAnvil(ItemStack base, ItemStack addition) {
      if (!base.isEmpty() && !addition.isEmpty()) {
         return wasUsed(base) || wasUsed(addition);
      } else {
         return false;
      }
   }
}
