package mielon.thesift.block;

import java.util.List;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class SiftPlantInteractions {
   private static final List GROWTH_PLANTS;

   private SiftPlantInteractions() {
   }

   public static void register() {
      UseBlockCallback.EVENT.register((UseBlockCallback)(player, level, hand, hit) -> {
         ItemStack stack = player.getItemInHand(hand);
         if (!stack.is(Items.BONE_MEAL)) {
            return InteractionResult.PASS;
         } else {
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.SIFTSLATE) && level.getBlockState(pos.above()).isAir() && hasAdjacentGrowth(level, pos)) {
               if (!level.isClientSide()) {
                  level.setBlockAndUpdate(pos, ModBlocks.SIFTSLATE_GROWTH.defaultBlockState());
                  consume(player, stack);
                  level.levelEvent(1505, pos, 0);
               }

               return InteractionResult.SUCCESS;
            } else if (state.is(ModBlocks.SIFTSLATE_GROWTH)) {
               if (!level.isClientSide()) {
                  growGrowthPatch((ServerLevel)level, pos);
                  consume(player, stack);
                  level.levelEvent(1505, pos, 0);
               }

               return InteractionResult.SUCCESS;
            } else {
               Block patt0$temp = state.getBlock();
               if (patt0$temp instanceof SiftPlantBlock) {
                  SiftPlantBlock plant = (SiftPlantBlock)patt0$temp;
                  if (!level.isClientSide()) {
                     spreadSamePlant((ServerLevel)level, pos, plant);
                     consume(player, stack);
                     level.levelEvent(1505, pos, 0);
                  }

                  return InteractionResult.SUCCESS;
               } else {
                  return InteractionResult.PASS;
               }
            }
         }
      });
   }

   private static boolean hasAdjacentGrowth(Level level, BlockPos pos) {
      for (Direction d : Plane.HORIZONTAL) {
         if (level.getBlockState(pos.relative(d)).is(ModBlocks.SIFTSLATE_GROWTH)) {
            return true;
         }
      }

      return false;
   }

   private static void growGrowthPatch(ServerLevel level, BlockPos origin) {
      RandomSource random = level.getRandom();
      int patches = 3 + random.nextInt(3);

      for (int p = 0; p < patches; ++p) {
         Block plant = (Block)GROWTH_PLANTS.get(random.nextInt(GROWTH_PLANTS.size()));
         int cx = origin.getX() + random.nextInt(7) - 3;
         int cz = origin.getZ() + random.nextInt(7) - 3;
         int attempts = 4 + random.nextInt(7);

         for (int i = 0; i < attempts; ++i) {
            int x = cx + random.nextInt(5) - 2;
            int z = cz + random.nextInt(5) - 2;
            BlockPos floor = findNearbySurface(level, x, origin.getY(), z, ModBlocks.SIFTSLATE_GROWTH);
            if (floor != null) {
               placePlant(level, floor.above(), plant);
            }
         }
      }

   }

   private static void spreadSamePlant(ServerLevel level, BlockPos origin, SiftPlantBlock plant) {
      RandomSource random = level.getRandom();
      Block block = plant;
      int attempts = 7 + random.nextInt(7);

      for (int i = 0; i < attempts; ++i) {
         int x = origin.getX() + random.nextInt(7) - 3;
         int z = origin.getZ() + random.nextInt(7) - 3;
         BlockPos floor = findAnySiftFloor(level, x, origin.getY() - 1, z);
         if (floor != null) {
            placePlant(level, floor.above(), block);
         }
      }

   }

   private static BlockPos findNearbySurface(ServerLevel level, int x, int aroundY, int z, Block wanted) {
      for (int dy = 3; dy >= -3; --dy) {
         BlockPos p = new BlockPos(x, aroundY + dy, z);
         if (level.getBlockState(p).is(wanted) && level.getBlockState(p.above()).isAir()) {
            return p;
         }
      }

      return null;
   }

   private static BlockPos findAnySiftFloor(ServerLevel level, int x, int aroundY, int z) {
      for (int dy = 3; dy >= -3; --dy) {
         BlockPos p = new BlockPos(x, aroundY + dy, z);
         BlockState s = level.getBlockState(p);
         if ((s.is(ModBlocks.SIFTSLATE) || s.is(ModBlocks.SIFTSLATE_GROWTH) || s.is(ModBlocks.HEALTHY_SCULK) || s.is(ModBlocks.DRY_HEALTHY_SCULK) || s.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) && level.getBlockState(p.above()).isAir()) {
            return p;
         }
      }

      return null;
   }

   private static void placePlant(ServerLevel level, BlockPos pos, Block plant) {
      if (level.getBlockState(pos).isAir()) {
         BlockState state = plant.defaultBlockState();
         if (state.canSurvive(level, pos)) {
            level.setBlockAndUpdate(pos, state);
         }

      }
   }

   private static void consume(Player player, ItemStack stack) {
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }

   }

   static {
      GROWTH_PLANTS = List.of(ModBlocks.OVERGROWN_FRONDS, ModBlocks.OVERGROWN_FRONDS, ModBlocks.OVERGROWN_FRONDS, ModBlocks.OVERGROWN_CHARD, ModBlocks.OVERGROWN_CHARD, ModBlocks.OVERGROWN_STALKS);
   }
}
