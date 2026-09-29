package mielon.thesift.entity;

import mielon.thesift.item.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;

public final class ModEntities {
   public static final EntityType SINGER;
   public static final EntityType ECHO_GOLEM;
   public static final EntityType DARK_SNIFFER;
   public static final EntityType BLUB;
   public static final EntityType SIFTER;
   public static final EntityType RIFT;
   public static final EntityType MINI_RIFT;
   public static final EntityType SIFTITE_RETURN;
   public static final EntityType OVERGROWN_WILLOW_BOAT;
   public static final EntityType OVERGROWN_WILLOW_CHEST_BOAT;

   private ModEntities() {
   }

   private static ResourceKey key(String path) {
      return ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), Identifier.fromNamespaceAndPath("the_sift", path));
   }

   private static EntityType register(String path, EntityType.Builder builder) {
      ResourceKey<EntityType<?>> key = key(path);
      return (EntityType)Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
   }

   public static void initialize() {
   }

   static {
      SINGER = register("singer", Builder.of(SingerEntity::new, MobCategory.MISC).sized(0.8F, 4.0F));
      ECHO_GOLEM = register("echo_golem", Builder.of(EchoGolemEntity::new, MobCategory.CREATURE).sized(1.15F, 1.65F).eyeHeight(1.17F).clientTrackingRange(10));
      DARK_SNIFFER = register("dark_sniffer", Builder.of(DarkSnifferEntity::new, MobCategory.MONSTER).sized(1.9F, 1.75F).eyeHeight(1.05F).passengerAttachments(new float[]{2.09375F}).nameTagOffset(2.05F).clientTrackingRange(10));
      BLUB = register("blub", Builder.of(BlubEntity::new, MobCategory.CREATURE).sized(0.8F, 0.9F).eyeHeight(0.62F).clientTrackingRange(10));
      SIFTER = register("sifter", Builder.of(SifterEntity::new, MobCategory.CREATURE).sized(0.9F, 0.95F).eyeHeight(0.72F).clientTrackingRange(10));
      RIFT = register("rift", Builder.of(RiftEntity::new, MobCategory.MISC).sized(1.0F, 4.25F).clientTrackingRange(12).updateInterval(1).noLootTable());
      MINI_RIFT = register("mini_rift", Builder.of(MiniRiftEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(8).updateInterval(1).noLootTable());
      SIFTITE_RETURN = register("siftite_return", Builder.of(SiftiteReturnEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).noLootTable());
      OVERGROWN_WILLOW_BOAT = register("overgrown_willow_boat", EntityType.Builder.<Boat>of((type, level) -> new Boat(type, level, () -> ModItems.OVERGROWN_WILLOW_BOAT), MobCategory.MISC).noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10));
      OVERGROWN_WILLOW_CHEST_BOAT = register("overgrown_willow_chest_boat", EntityType.Builder.<ChestBoat>of((type, level) -> new ChestBoat(type, level, () -> ModItems.OVERGROWN_WILLOW_CHEST_BOAT), MobCategory.MISC).noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10));
   }
}
