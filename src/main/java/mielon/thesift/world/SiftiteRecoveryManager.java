package mielon.thesift.world;

import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mielon.thesift.entity.MiniRiftEntity;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.item.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

public final class SiftiteRecoveryManager {
   private static final Map PENDING = new HashMap();
   private static final Map DUE_AT = new HashMap();
   private static boolean registered;

   private SiftiteRecoveryManager() {
   }

   public static void register() {
      if (!registered) {
         registered = true;
         ServerLivingEntityEvents.ALLOW_DEATH.register((ServerLivingEntityEvents.AllowDeath)(entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) {
               return true;
            } else if ((Boolean)player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
               return true;
            } else {
               List<ItemStack> protectedStacks = new ArrayList();
               List<Integer> protectedSlots = new ArrayList();

               for (int slot = 0; slot < player.getInventory().getContainerSize(); ++slot) {
                  ItemStack stack = player.getInventory().getItem(slot);
                  if (!stack.isEmpty() && stack.is(ModItems.SIFTITE_ITEMS)) {
                     protectedStacks.add(stack.copy());
                     protectedSlots.add(slot);
                  }
               }

               if (!protectedStacks.isEmpty()) {
                  MinecraftServer server = player.level().getServer();
                  if (server == null) {
                     return true;
                  }

                  List<ItemStack> allPending = new ArrayList(loadPending(server, player.getUUID()));
                  allPending.addAll(protectedStacks);
                  savePending(server, player.getUUID(), allPending);
                  PENDING.put(player.getUUID(), allPending);

                  for (int slot : protectedSlots) {
                     player.getInventory().setItem(slot, ItemStack.EMPTY);
                  }
               }

               return true;
            }
         });
         ServerPlayerEvents.AFTER_RESPAWN.register((ServerPlayerEvents.AfterRespawn)(oldPlayer, newPlayer, alive) -> {
            if (ensureLoaded(newPlayer).size() > 0) {
               DUE_AT.put(newPlayer.getUUID(), newPlayer.level().getGameTime() + 40L);
            }

         });
         ServerPlayerEvents.JOIN.register((ServerPlayerEvents.Join)(player) -> {
            if (ensureLoaded(player).size() > 0) {
               DUE_AT.putIfAbsent(player.getUUID(), player.level().getGameTime() + 40L);
            }

         });
      }
   }

   public static void tick(MinecraftServer server) {
      if (!DUE_AT.isEmpty()) {
         List<UUID> ready = new ArrayList();

         for (Map.Entry entry : (Iterable<Map.Entry>) DUE_AT.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer((UUID)entry.getKey());
            if (player != null && player.isAlive() && player.level().getGameTime() >= (Long)entry.getValue()) {
               ready.add((UUID)entry.getKey());
            }
         }

         for (UUID playerId : ready) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            List<ItemStack> stacks = (List)PENDING.get(playerId);
            if (player != null && stacks != null && !stacks.isEmpty() && spawnMiniRift(player, stacks)) {
               PENDING.remove(playerId);
               DUE_AT.remove(playerId);
               savePending(server, playerId, List.of());
            }
         }

      }
   }

   private static boolean spawnMiniRift(ServerPlayer player, List stacks) {
      ServerLevel level = player.level();
      Vec3 origin = player.position();
      Vec3 position = origin.add(1.55, (double)0.75F, (double)0.0F);

      for (int i = 0; i < 8; ++i) {
         double angle = (double)i * Math.PI / (double)4.0F;
         Vec3 candidate = origin.add(Math.cos(angle) * 1.55, (double)0.75F, Math.sin(angle) * 1.55);
         BlockPos blockPos = BlockPos.containing(candidate);
         if (level.getBlockState(blockPos).isAir() && level.getBlockState(blockPos.above()).isAir()) {
            position = candidate;
            break;
         }
      }

      MiniRiftEntity rift = new MiniRiftEntity(ModEntities.MINI_RIFT, level);
      rift.configure(player.getUUID(), stacks, level.getGameTime());
      rift.setPos(position);
      return level.addFreshEntity(rift);
   }

   private static List ensureLoaded(ServerPlayer player) {
      MinecraftServer server = player.level().getServer();
      return server == null ? List.of() : (List)PENDING.computeIfAbsent(player.getUUID(), (playerId) -> loadPending(server, (UUID) playerId));
   }

   private static List loadPending(MinecraftServer server, UUID playerId) {
      CompoundTag stored = server.getCommandStorage().get(storageId(playerId));
      if (!stored.getBooleanOr("Valid", false)) {
         return new ArrayList();
      } else {
         DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
         List<ItemStack> result = new ArrayList();

         for (Tag raw : stored.getListOrEmpty("Items")) {
            if (raw instanceof CompoundTag) {
               CompoundTag entry = (CompoundTag)raw;
               entry.read("Stack", ItemStack.CODEC, ops).ifPresent((stack) -> {
                  if (!stack.isEmpty()) {
                     result.add(stack.copy());
                  }

               });
            }
         }

         return result;
      }
   }

   private static void savePending(MinecraftServer server, UUID playerId, List stacks) {
      CompoundTag stored = new CompoundTag();
      if (!stacks.isEmpty()) {
         stored.putBoolean("Valid", true);
         DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
         ListTag items = new ListTag();

         for (ItemStack stack : (Iterable<ItemStack>) stacks) {
            if (!stack.isEmpty()) {
               CompoundTag entry = new CompoundTag();
               entry.store("Stack", ItemStack.CODEC, ops, stack);
               items.add(entry);
            }
         }

         stored.put("Items", items);
      }

      server.getCommandStorage().set(storageId(playerId), stored);
   }

   private static Identifier storageId(UUID playerId) {
      return Identifier.fromNamespaceAndPath("the_sift", "siftite_recovery/" + String.valueOf(playerId));
   }

   public static void clearTransientState() {
      PENDING.clear();
      DUE_AT.clear();
   }
}
