package mielon.thesift.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_1 = register("block.sonorous_deepslate.sound_1");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_2 = register("block.sonorous_deepslate.sound_2");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_3 = register("block.sonorous_deepslate.sound_3");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_4 = register("block.sonorous_deepslate.sound_4");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_5 = register("block.sonorous_deepslate.sound_5");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_6 = register("block.sonorous_deepslate.sound_6");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_7 = register("block.sonorous_deepslate.sound_7");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_8 = register("block.sonorous_deepslate.sound_8");
   public static final SoundEvent SONOROUS_AUTOPLAY_GLITCH = register("sonorous_autoplay_glitch");
   public static final SoundEvent THE_SIFT_PORTAL_OPEN = register("the_sift_portal_open");
   public static final SoundEvent THE_SIFT_PORTAL_CLOSE = register("the_sift_portal_close");
   public static final SoundEvent RIFT_APPEAR = registerFixedRange("entity.rift.appear", 64.0F);
   public static final SoundEvent SINGER_SING = registerFixedRange("entity.singer.sing", 4096.0F);
   public static final SoundEvent SINGER_IDLE = register("entity.singer.idle");
   public static final SoundEvent SINGER_HURT = register("entity.singer.hurt");
   public static final SoundEvent SINGER_DEATH = register("entity.singer.death");
   public static final SoundEvent SINGER_HAPPY = register("entity.singer.happy");
   public static final SoundEvent SIFTER_AMBIENT = register("entity.sifter.ambient");
   public static final SoundEvent SIFTER_HURT = register("entity.sifter.hurt");
   public static final SoundEvent SIFTER_DEATH = register("entity.sifter.death");
   public static final SoundEvent SIFTER_STEP = register("entity.sifter.step");
   public static final SoundEvent SIFTER_ATTACK = register("entity.sifter.attack");
   public static final SoundEvent BLUB_IDLE_WATER = register("entity.blub.idle_water");
   public static final SoundEvent BLUB_IDLE_AIR = register("entity.blub.idle_air");
   public static final SoundEvent BLUB_HURT = register("entity.blub.hurt");
   public static final SoundEvent BLUB_DEATH = register("entity.blub.death");
   public static final SoundEvent BLUB_SWIM = register("entity.blub.swim");
   public static final SoundEvent BLUB_SPLASH = register("entity.blub.splash");
   public static final SoundEvent BLUB_ATTACK = register("entity.blub.attack");
   public static final SoundEvent SINGER_EMERGE = register("entity.singer.emerge");
   public static final SoundEvent SINGER_DIG = register("entity.singer.dig");
   public static final SoundEvent DARK_SNIFFER_SNIFF = register("entity.dark_sniffer.sniff");
   public static final SoundEvent ECHO_GOLEM_SPAWN = register("entity.echo_golem.spawn");
   public static final SoundEvent ECHO_GOLEM_ITEM_DROP = register("entity.echo_golem.item_drop");
   public static final SoundEvent ECHO_GOLEM_ITEM_GET = register("entity.echo_golem.item_get");
   public static final SoundEvent ECHO_GOLEM_HURT = register("entity.echo_golem.hurt");
   public static final SoundEvent ECHO_GOLEM_DEATH = register("entity.echo_golem.death");
   public static final SoundEvent ECHO_GOLEM_STEP = register("entity.echo_golem.step");
   public static final SoundEvent SIFT_PORTAL_AMBIENT = register("block.sift_portal.ambient");
   public static final SoundEvent SIFT_PORTAL_TRIGGER = register("block.sift_portal.trigger");
   public static final SoundEvent DARK_SNIFFER_BLOOM = register("block.dark_sniffer.bloom");
   public static final SoundEvent ICHOR_BOTTLE_FILL = register("item.ichor_bottle.fill");
   public static final SoundEvent ICHOR_BOTTLE_EMPTY = register("item.ichor_bottle.empty");
   public static final SoundEvent ICHOR_BUCKET_FILL = register("item.ichor_bucket.fill");
   public static final SoundEvent ICHOR_BUCKET_EMPTY = register("item.ichor_bucket.empty");
   public static final SoundEvent SIFTITE_ARMOR_EQUIP = register("item.siftite_armor.equip");
   public static final SoundEvent MUSIC_DISC_RIFT = register("music_disc.rift");
   public static final SoundEvent[] BY_INDEX;

   private ModSounds() {
   }

   public static void initialize() {
   }

   private static SoundEvent register(String path) {
      ResourceKey<SoundEvent> key = key(path);
      return (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, key, SoundEvent.createVariableRangeEvent(key.identifier()));
   }

   private static SoundEvent registerFixedRange(String path, float range) {
      ResourceKey<SoundEvent> key = key(path);
      return (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, key, SoundEvent.createFixedRangeEvent(key.identifier(), range));
   }

   private static ResourceKey key(String path) {
      return ResourceKey.create(BuiltInRegistries.SOUND_EVENT.key(), Identifier.fromNamespaceAndPath("the_sift", path));
   }

   static {
      BY_INDEX = new SoundEvent[]{SONOROUS_DEEPSLATE_SOUND_1, SONOROUS_DEEPSLATE_SOUND_2, SONOROUS_DEEPSLATE_SOUND_3, SONOROUS_DEEPSLATE_SOUND_4, SONOROUS_DEEPSLATE_SOUND_5, SONOROUS_DEEPSLATE_SOUND_6, SONOROUS_DEEPSLATE_SOUND_7, SONOROUS_DEEPSLATE_SOUND_8};
   }
}
