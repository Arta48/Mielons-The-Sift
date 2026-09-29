package mielon.thesift.mixin;

import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Snowball.class})
public abstract class IchorSnowballAdvancementMixin {
   @Inject(
      method = {"onHit"},
      at = {@At("HEAD")}
   )
   private void theSift$detectWardenDistraction(HitResult hit, CallbackInfo ci) {
      Snowball snowball = (Snowball)(Object)this;
      Level var6 = snowball.level();
      if (var6 instanceof ServerLevel level) {
         Entity var8 = snowball.getOwner();
         if (var8 instanceof ServerPlayer player) {
            if (snowball.getItem().is(ModItems.ICHOR_SNOWBALL)) {
               Vec3 impact = hit.getLocation();
               AABB vibrationRange = (new AABB(impact, impact)).inflate((double)16.0F);
               if (!level.getEntitiesOfClass(Warden.class, vibrationRange, (warden) -> warden.isAlive() && !warden.isRemoved()).isEmpty()) {
                  ModAdvancements.award(player, "the_sift/false_alarm");
               }

               return;
            }
         }
      }

   }
}
