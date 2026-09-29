package mielon.thesift.item;

import mielon.thesift.entity.ModEntities;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class BlubItems {
   public static final ResourceKey BLUB_SPAWN_EGG_KEY;
   public static final Item BLUB_SPAWN_EGG;

   private BlubItems() {
   }

   public static void initialize() {
   }

   static {
      BLUB_SPAWN_EGG_KEY = ResourceKey.create(BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "blub_spawn_egg"));
      BLUB_SPAWN_EGG = (Item)Registry.register(BuiltInRegistries.ITEM, BLUB_SPAWN_EGG_KEY, new SpawnEggItem((new Item.Properties()).setId(BLUB_SPAWN_EGG_KEY).spawnEgg(ModEntities.BLUB)));
   }
}
