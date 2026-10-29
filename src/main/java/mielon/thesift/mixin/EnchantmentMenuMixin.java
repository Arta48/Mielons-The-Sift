package mielon.thesift.mixin;

import java.util.ArrayList;
import java.util.List;
import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.item.CharoiteEnchanting;
import mielon.thesift.item.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EnchantmentMenu.class})
public abstract class EnchantmentMenuMixin {
   @Shadow
   @Final
   private Container enchantSlots;
   @Shadow
   @Final
   private ContainerLevelAccess access;
   @Shadow
   @Final
   public int[] costs;
   @Shadow
   @Final
   public int[] enchantClue;
   @Shadow
   @Final
   public int[] levelClue;
   @Unique
   private boolean theSift$charoiteCosts;
   @Unique
   private boolean theSift$usingCharoiteForClick;

   @Inject(
      method = {"clickMenuButton"},
      at = {@At("HEAD")}
   )
   private void theSift$rememberCharoiteUse(Player player, int button, CallbackInfoReturnable<Boolean> cir) {
      this.theSift$usingCharoiteForClick = this.enchantSlots.getItem(1).is(ModItems.CHAROITE);
   }

   @Inject(
      method = {"clickMenuButton"},
      at = {@At("RETURN")}
   )
   private void theSift$markCharoiteEnchantment(Player player, int button, CallbackInfoReturnable<Boolean> cir) {
      if (this.theSift$usingCharoiteForClick && Boolean.TRUE.equals(cir.getReturnValue())) {
         CharoiteEnchanting.markUsed(this.enchantSlots.getItem(0));
         if (player instanceof ServerPlayer serverPlayer) {
             ModAdvancements.award(serverPlayer, "the_sift/better_enchanter");
         }

         ((EnchantmentMenu)(Object)this).broadcastChanges();
      }

      this.theSift$usingCharoiteForClick = false;
   }

   @Inject(
      method = {"slotsChanged"},
      at = {@At("HEAD")}
   )
   private void theSift$beginRecalculation(Container container, CallbackInfo ci) {
      if (container == this.enchantSlots) {
         this.theSift$charoiteCosts = false;
      }

   }

   @Inject(
      method = {"slotsChanged"},
      at = {@At("RETURN")}
   )
   private void theSift$applyCharoitePreview(Container container, CallbackInfo ci) {
      if (container == this.enchantSlots && this.enchantSlots.getItem(1).is(ModItems.CHAROITE)) {
         RegistryAccess registryAccess = this.access.evaluate((level, pos) -> level.isClientSide() ? null : level.registryAccess(), null);
         if (registryAccess != null) {
            Registry<Enchantment> enchantments = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);

            for (int slot = 0; slot < this.costs.length; ++slot) {
               if (this.costs[slot] > 0) {
                  int[] var10000 = this.costs;
                  var10000[slot] += 20;
               }

               if (this.levelClue[slot] >= 0 && this.enchantClue[slot] >= 0 && enchantments.get(this.enchantClue[slot]).map((enchantment) -> enchantment.value().getMaxLevel() > 1).orElse(false)) {
                  int var10002 = this.levelClue[slot]++;
               }
            }

            this.theSift$charoiteCosts = true;
            ((EnchantmentMenu)(Object)this).broadcastChanges();
         }
      }
   }

   @ModifyVariable(
      method = {"getEnchantmentList"},
      at = {@At("HEAD")},
      argsOnly = true,
      index = 4
   )
   private int theSift$useBaseEnchantingPower(int displayedCost) {
      return this.theSift$charoiteCosts ? Math.max(1, displayedCost - 20) : displayedCost;
   }

   @Inject(
      method = {"getEnchantmentList"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void theSift$boostCharoiteEnchantments(RegistryAccess registryAccess, ItemStack stack, int button, int cost, CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
      if (this.theSift$charoiteCosts && !(cir.getReturnValue()).isEmpty()) {
         List<EnchantmentInstance> boosted = new ArrayList<>((cir.getReturnValue()).size());

         for (EnchantmentInstance enchantment : cir.getReturnValue()) {
            int boostedLevel = enchantment.enchantment().value().getMaxLevel() > 1 ? enchantment.level() + 1 : enchantment.level();
            boosted.add(new EnchantmentInstance(enchantment.enchantment(), boostedLevel));
         }

         cir.setReturnValue(boosted);
      }
   }

   @Redirect(
      method = {"quickMoveStack"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"
)}
   )
   private boolean theSift$quickMoveCharoite(ItemStack stack, Object expectedItem) {
      return stack.getItem() == expectedItem || expectedItem == Items.LAPIS_LAZULI && stack.is(ModItems.CHAROITE);
   }
}
