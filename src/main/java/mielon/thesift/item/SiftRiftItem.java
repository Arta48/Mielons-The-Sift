package mielon.thesift.item;

import mielon.thesift.world.RiftManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class SiftRiftItem extends Item {
   public SiftRiftItem(Item.Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player != null && player.getAbilities().instabuild) {
         Level var4 = context.getLevel();
         if (var4 instanceof ServerLevel) {
            ServerLevel level = (ServerLevel)var4;
            BlockPos var6 = context.getClickedPos().relative(context.getClickedFace());
            boolean longAlongX = RiftManager.axisFromFacing(context.getHorizontalDirection());
            if (RiftManager.tryPlaceCreativeRift(level, var6, longAlongX)) {
               return InteractionResult.SUCCESS_SERVER;
            } else {
               player.sendOverlayMessage(Component.translatable("message.the_sift.rift_no_space"));
               return InteractionResult.FAIL;
            }
         } else {
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.FAIL;
      }
   }
}
