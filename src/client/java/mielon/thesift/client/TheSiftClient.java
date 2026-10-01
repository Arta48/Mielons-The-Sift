package mielon.thesift.client;

import mielon.thesift.block.ModBlocks;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.entity.BlubRenderer;
import mielon.thesift.client.entity.DarkSnifferRenderer;
import mielon.thesift.client.entity.EchoGolemRenderer;
import mielon.thesift.client.entity.SifterRenderer;
import mielon.thesift.client.entity.SingerRenderer;
import mielon.thesift.client.particle.IchorFluidParticle;
import mielon.thesift.client.particle.IchorSurfaceParticle;
import mielon.thesift.client.particle.SiftNoteParticle;
import mielon.thesift.client.particle.SiftParallaxParticle;
import mielon.thesift.client.particle.SingerSoundWaveParticle;
import mielon.thesift.client.particle.SoulParticle;
import mielon.thesift.client.particle.SoundWaveParticle;
import mielon.thesift.client.render.IchorFluidRendering;
import mielon.thesift.client.render.SiftCloudRenderPipelines;
import mielon.thesift.client.render.SiftPortalRenderer;
import mielon.thesift.client.render.SiftProceduralSkyRenderer;
import mielon.thesift.client.render.SonorousBeamRenderTypes;
import mielon.thesift.client.render.SonorousBeamRenderer;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.world.VanillaPaletteRemapper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.resources.Identifier;

public final class TheSiftClient implements ClientModInitializer {
   private static final ModelLayerLocation OVERGROWN_WILLOW_BOAT_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("the_sift", "boat/overgrown_willow"), "main");
   private static final ModelLayerLocation OVERGROWN_WILLOW_CHEST_BOAT_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("the_sift", "chest_boat/overgrown_willow"), "main");

   public void onInitializeClient() {
      VanillaPaletteRemapper.initialize();
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
         VanillaPaletteRemapper.isVanillaServer = false;
      });
      SiftShaderCompat.initialize();
      SonorousBeamRenderTypes.initialize();
      SiftProceduralSkyRenderer.initialize();
      IchorFluidRendering.register();
      SiftCloudRenderPipelines.initialize();
      ModelLayerRegistry.registerModelLayer(OVERGROWN_WILLOW_BOAT_LAYER, BoatModel::createBoatModel);
      ModelLayerRegistry.registerModelLayer(OVERGROWN_WILLOW_CHEST_BOAT_LAYER, BoatModel::createChestBoatModel);
      EntityRendererRegistry.register(ModEntities.OVERGROWN_WILLOW_BOAT, (context) -> new BoatRenderer(context, OVERGROWN_WILLOW_BOAT_LAYER));
      EntityRendererRegistry.register(ModEntities.OVERGROWN_WILLOW_CHEST_BOAT, (context) -> new BoatRenderer(context, OVERGROWN_WILLOW_CHEST_BOAT_LAYER));
      BlockEntityRendererRegistry.register(ModBlocks.SIFT_PORTAL_BLOCK_ENTITY, (context) -> new SiftPortalRenderer());
      BlockEntityRendererRegistry.register(ModBlocks.SONOROUS_DEEPSLATE_BLOCK_ENTITY, SonorousBeamRenderer::new);
      EntityRendererRegistry.register(ModEntities.SINGER, SingerRenderer::new);
      EntityRendererRegistry.register(ModEntities.ECHO_GOLEM, EchoGolemRenderer::new);
      EntityRendererRegistry.register(ModEntities.DARK_SNIFFER, DarkSnifferRenderer::new);
      EntityRendererRegistry.register(ModEntities.BLUB, BlubRenderer::new);
      EntityRendererRegistry.register(ModEntities.SIFTER, SifterRenderer::new);
      ParticleProviderRegistry.getInstance().register(ModParticles.SIFT_PARALLAX, SiftParallaxParticle.Provider::new);
      ParticleProviderRegistry.getInstance().register(ModParticles.SIFT_NOTE, SiftNoteParticle.Provider::new);
      ParticleProviderRegistry.getInstance().register(ModParticles.SOUND_WAVE, SoundWaveParticle.Provider::new);
      ParticleProviderRegistry.getInstance().register(ModParticles.SINGER_SOUND_WAVE, SingerSoundWaveParticle.Provider::new);
      ParticleProviderRegistry.getInstance().register(ModParticles.CANYON_SOUL, SoulParticle.RiseProvider::new);
      ParticleProviderRegistry.getInstance().register(ModParticles.SOUL_FRAGMENT, SoulParticle.FragmentProvider::new);
      ParticleProviderRegistry.getInstance().register(ModParticles.ICHOR_SURFACE_MIST, IchorSurfaceParticle.Provider::new);
      ParticleProviderRegistry.getInstance().register(ModParticles.ICHOR_BUBBLE, IchorFluidParticle.BubbleProvider::new);
      ParticleProviderRegistry.getInstance().register(ModParticles.ICHOR_SPLASH, IchorFluidParticle.SplashProvider::new);
      ParticleProviderRegistry.getInstance().register(ModParticles.ICHOR_RAIN_SPLASH, IchorFluidParticle.RainSplashProvider::new);
   }
}
