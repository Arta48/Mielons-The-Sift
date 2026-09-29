package mielon.thesift.advancement;

import mielon.thesift.block.ModBlocks;
import mielon.thesift.entity.BlubEntity;
import mielon.thesift.entity.DarkSnifferEntity;
import mielon.thesift.entity.RiftEntity;
import mielon.thesift.item.ModItems;
import mielon.thesift.world.RiftDirectory;
import mielon.thesift.world.TheSiftDimension;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ModAdvancements {
   public static final String SONG_OF_THE_PAST = "story/song_of_the_past";
   public static final String SPICEWOOD = "the_sift/spicewood";
   public static final String LIKE_FATHER_AND_SON = "the_sift/like_father_and_son";
   public static final String FRIEND_OF_THE_SIFT = "the_sift/friend_of_the_sift";
   public static final String BLUB = "the_sift/blub";
   public static final String FOOD_CHAIN = "the_sift/food_chain";
   public static final String FALSE_ALARM = "the_sift/false_alarm";
   public static final String PLANTING_THE_DARK = "the_sift/planting_the_dark";
   public static final String KINDA_PURPLEISH = "the_sift/kinda_purpleish";
   public static final String SIFTED_NUGGET = "the_sift/sifted_nugget";
   public static final String BETTER_ENCHANTER = "the_sift/better_enchanter";
   public static final String RIFTER = "the_sift/rifter";
   public static final String RIFT_RESONANCE = "the_sift/rift_resonance";
   public static final String KEEP_INVENTORY = "the_sift/keep_inventory";
   private static boolean registered;

   private ModAdvancements() {
   }

   public static void register() {
      if (!registered) {
         registered = true;
         UseBlockCallback.EVENT.register((UseBlockCallback)(player, level, hand, hit) -> {
            if (level instanceof ServerLevel serverLevel) {
               if (player instanceof ServerPlayer serverPlayer) {
                  ItemStack held = player.getItemInHand(hand);
                  BlockPos clickedPos = hit.getBlockPos();
                  BlockState clicked = level.getBlockState(clickedPos);
                  if (held.is(ModItems.SCULKFLOWER_SEEDS) && hit.getDirection() == Direction.UP && clicked.is(Blocks.SCULK) && level.getBlockState(clickedPos.above()).canBeReplaced()) {
                     award(serverPlayer, "the_sift/planting_the_dark");
                  }

                  if (level.dimension().equals(TheSiftDimension.LEVEL_KEY) && (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) && isWood(clicked)) {
                     BlockPos firePos = clickedPos.relative(hit.getDirection());
                     if (level.getBlockState(firePos).canBeReplaced() && BaseFireBlock.getState(level, firePos).canSurvive(level, firePos)) {
                        award(serverPlayer, "the_sift/spicewood");
                     }
                  }

                  if (held.is(ModItems.MUSIC_DISC_RIFT) && clicked.is(Blocks.JUKEBOX)) {
                     BlockEntity patt0$temp = level.getBlockEntity(clickedPos);
                     if (patt0$temp instanceof JukeboxBlockEntity) {
                        JukeboxBlockEntity jukebox = (JukeboxBlockEntity)patt0$temp;
                        if (jukebox.getTheItem().isEmpty() && isNearOpenRift(serverLevel, Vec3.atCenterOf(clickedPos), (double)16.0F)) {
                           award(serverPlayer, "the_sift/rift_resonance");
                        }
                     }
                  }

                  return InteractionResult.PASS;
               }
            }

            return InteractionResult.PASS;
         });
         PlayerBlockBreakEvents.AFTER.register((PlayerBlockBreakEvents.After)(level, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
               if (state.is(ModBlocks.SIFTSLATE_CHAROITE_ORE)) {
                  award(serverPlayer, "the_sift/kinda_purpleish");
               } else if (state.is(ModBlocks.SIFTSLATE_SIFTITE_ORE)) {
                  award(serverPlayer, "the_sift/sifted_nugget");
               }

            }
         });
         ServerLivingEntityEvents.AFTER_DEATH.register((ServerLivingEntityEvents.AfterDeath)(entity, source) -> {
            if (entity instanceof DarkSnifferEntity) {
               Entity patt0$temp = source.getEntity();
               if (patt0$temp instanceof BlubEntity) {
                  BlubEntity blub = (BlubEntity)patt0$temp;
                  if (blub.isTame()) {
                     LivingEntity patt1$temp = blub.getOwner();
                     if (patt1$temp instanceof ServerPlayer) {
                        ServerPlayer owner = (ServerPlayer)patt1$temp;
                        award(owner, "the_sift/food_chain");
                        return;
                     }
                  }
               }
            }

         });
      }
   }

   public static void award(ServerPlayer player, String path) {
      AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath("the_sift", path));
      if (advancement != null) {
         player.getAdvancements().award(advancement, "event");
      }

   }

   public static void awardNearby(ServerLevel level, Vec3 center, double radius, String path) {
      AABB area = (new AABB(center, center)).inflate(radius);

      for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area, (playerx) -> playerx.isAlive() && !playerx.isSpectator())) {
         award(player, path);
      }

   }

   private static boolean isNearOpenRift(ServerLevel level, Vec3 center, double radius) {
      double maxDistance = radius * radius;

      for (RiftEntity rift : (Iterable<RiftEntity>) RiftDirectory.loaded()) {
         if (!rift.isRemoved() && !rift.isClosing() && rift.level() == level && rift.position().distanceToSqr(center) <= maxDistance) {
            return true;
         }
      }

      return false;
   }

   private static boolean isWood(BlockState state) {
      return state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS) || state.is(BlockTags.WOODEN_BUTTONS) || state.is(BlockTags.WOODEN_PRESSURE_PLATES) || state.is(BlockTags.WOODEN_SHELVES) || state.is(ModBlocks.OVERGROWN_WILLOW_WOOD) || state.is(ModBlocks.STRIPPED_OVERGROWN_WILLOW_WOOD);
   }
}
