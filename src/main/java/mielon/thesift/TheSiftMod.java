package mielon.thesift;

import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SiftPlantInteractions;
import mielon.thesift.entity.BlubEntity;
import mielon.thesift.entity.DarkSnifferEntity;
import mielon.thesift.entity.EchoGolemEntity;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.SifterEntity;
import mielon.thesift.entity.SingerEntity;
import mielon.thesift.entity.SingerSummoner;
import mielon.thesift.fluid.IchorWaterlogging;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.item.BlubItems;
import mielon.thesift.item.ModItems;
import mielon.thesift.network.RiftLoadingPayload;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.portal.PortalGrowth;
import mielon.thesift.portal.SonorousAutoplay;
import mielon.thesift.portal.SonorousConsoles;
import mielon.thesift.portal.SonorousIgnition;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.RiftDirectory;
import mielon.thesift.world.RiftManager;
import mielon.thesift.world.SiftLocateCommand;
import mielon.thesift.world.SiftTeleportManager;
import mielon.thesift.world.SiftWeatherCommand;
import mielon.thesift.world.SiftiteRecoveryManager;
import mielon.thesift.worldgen.ModWorldgen;
import mielon.thesift.worldgen.SiftFeaturePlacementGuard;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TheSiftMod implements ModInitializer {
   public static final String MOD_ID = "the_sift";
   public static final Logger LOGGER = LoggerFactory.getLogger("the_sift");

   public void onInitialize() {
      ModFluids.initialize();
      ModBlocks.initialize();
      ModWorldgen.initialize();
      SiftPlantInteractions.register();
      SiftLocateCommand.register();
      SiftWeatherCommand.register();
      ModEntities.initialize();
      ModItems.initialize();
      BlubItems.initialize();
      ModAdvancements.register();
      SiftiteRecoveryManager.register();
      RiftLoadingPayload.register();
      ServerTickEvents.START_SERVER_TICK.register(RiftDirectory::tick);
      registerEntityAttributes();
      registerSpawnPlacements();
      ModParticles.initialize();
      ModSounds.initialize();
      SonorousIgnition.register();
      SingerSummoner.register();
      SonorousAutoplay.register();
      registerServerLifecycle();
      PortalGrowth.register();
      LOGGER.info("The Sift initialized.");
   }

   private static void registerEntityAttributes() {
      FabricDefaultAttributeRegistry.register(ModEntities.SINGER, SingerEntity.createAttributes());
      FabricDefaultAttributeRegistry.register(ModEntities.ECHO_GOLEM, EchoGolemEntity.createAttributes());
      FabricDefaultAttributeRegistry.register(ModEntities.DARK_SNIFFER, DarkSnifferEntity.createAttributes());
      FabricDefaultAttributeRegistry.register(ModEntities.BLUB, BlubEntity.createAttributes());
      FabricDefaultAttributeRegistry.register(ModEntities.SIFTER, SifterEntity.createAttributes());
   }

   private static void registerSpawnPlacements() {
      SpawnPlacements.register(ModEntities.ECHO_GOLEM, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, EchoGolemEntity::checkEchoGolemSpawnRules);
      SpawnPlacements.register(ModEntities.BLUB, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, BlubEntity::checkBlubSpawnRules);
      SpawnPlacements.register(ModEntities.SIFTER, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, SifterEntity::checkSifterSpawnRules);
   }

   private static void registerServerLifecycle() {
      ServerTickEvents.END_SERVER_TICK.register((ServerTickEvents.EndTick)(server) -> {
         SiftTeleportManager.tick(server);
         RiftManager.tick(server);
         SiftiteRecoveryManager.tick(server);
         IchorWaterlogging.tick(server);
      });
      ServerLifecycleEvents.SERVER_STOPPED.register((ServerLifecycleEvents.ServerStopped)(server) -> {
         SonorousConsoles.clearTransientBuffers();
         SiftTeleportManager.clearTransientState();
         RiftManager.clearTransientState();
         SiftiteRecoveryManager.clearTransientState();
         SiftFeaturePlacementGuard.clear();
         EchoGolemEntity.clearTransientState();
         IchorWaterlogging.clear();
      });
      ServerLifecycleEvents.SERVER_STOPPING.register(SiftLandmarkTracker::flush);
      ServerLifecycleEvents.SERVER_STOPPING.register(IchorWaterlogging::flush);
   }
}
