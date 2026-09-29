package mielon.thesift.item;

import mielon.thesift.fluid.ModFluids;
import mielon.thesift.sound.ModSounds;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

public final class IchorBottleItem extends Item {
   private static boolean interactionsRegistered;

   public IchorBottleItem(Item.Properties properties) {
      super(properties);
   }

   public static void registerInteractions() {
      if (!interactionsRegistered) {
         interactionsRegistered = true;
         UseItemCallback.EVENT.register(IchorBottleItem::fillFromSource);
      }
   }

   private static InteractionResult fillFromSource(Player player, Level level, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (stack.is(Items.GLASS_BOTTLE) && !player.isSpectator()) {
         BlockHitResult hit = getPlayerPOVHitResult(level, player, Fluid.SOURCE_ONLY);
         if (hit.getType() != Type.BLOCK) {
            return InteractionResult.PASS;
         } else {
            BlockPos pos = hit.getBlockPos();
            FluidState fluid = level.getFluidState(pos);
            if (fluid.isSource() && fluid.getType().isSame(ModFluids.ICHOR)) {
               if (level.mayInteract(player, pos) && player.mayUseItemAt(pos, hit.getDirection(), stack)) {
                  if (!level.isClientSide()) {
                     ItemStack filled = new ItemStack(ModItems.ICHOR_BOTTLE);
                     player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled));
                     player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                     level.playSound((Entity)null, pos, ModSounds.ICHOR_BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                     level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
                  }

                  return InteractionResult.SUCCESS;
               } else {
                  return InteractionResult.PASS;
               }
            } else {
               return InteractionResult.PASS;
            }
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public static InteractionResult fillFromCauldron(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
      int levelValue = (Integer)state.getValue(LayeredCauldronBlock.LEVEL);
      if (levelValue <= 0) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      } else {
         if (!level.isClientSide()) {
            ItemStack filled = new ItemStack(ModItems.ICHOR_BOTTLE);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            level.playSound((Entity)null, pos, ModSounds.ICHOR_BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
         }

         return InteractionResult.SUCCESS;
      }
   }
}
