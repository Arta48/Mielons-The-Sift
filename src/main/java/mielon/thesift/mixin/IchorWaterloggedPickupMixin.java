package mielon.thesift.mixin;

import mielon.thesift.fluid.IchorWaterlogging;
import mielon.thesift.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({BucketItem.class})
public abstract class IchorWaterloggedPickupMixin {
   @Redirect(
      method = {"use"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"
)}
   )
   private ItemStack theSift$pickupIchor(BucketPickup pickup, LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
      if (!IchorWaterlogging.isIchorlogged(state) && !IchorWaterlogging.remove(level, pos)) {
         return pickup.pickupBlock(user, level, pos, state);
      } else {
         level.setBlock(pos, IchorWaterlogging.setFluidlogged(state, false), 3);
         if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
         }

         return new ItemStack(ModFluids.ICHOR_BUCKET);
      }
   }
}
