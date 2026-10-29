package mielon.thesift.client;

import mielon.thesift.client.entity.MiniRiftRenderer;
import mielon.thesift.client.entity.RiftRenderer;
import mielon.thesift.client.loading.SiftClientPortalTransition;
import mielon.thesift.client.loading.SiftLoadingScreenState;
import mielon.thesift.client.loading.SiftTransitionScreen;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.network.RiftLoadingPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public final class RiftClientInitializer implements ClientModInitializer {
   public void onInitializeClient() {
      ClientPlayNetworking.registerGlobalReceiver(RiftLoadingPayload.TYPE, (payload, context) -> {
          Minecraft client = context.client();
         if (payload.start() && client.player != null) {
            if (payload.rift()) {
               SiftClientPortalTransition.beginRiftTransition(client.player);
            }

            SiftLoadingScreenState.begin(client.player.level().dimension(), payload.rift());
            client.gui.setScreen(new SiftTransitionScreen());
         } else {
            if (!payload.start()) {
               if (payload.rift() && client.player != null) {
                  SiftClientPortalTransition.endRiftTransition(client.player);
               }

               if (client.gui.screen() instanceof SiftTransitionScreen transition) {
                  if (!transition.hasRealTracker()) {
                     SiftLoadingScreenState.clear();
                     client.gui.setScreen(null);
                  }
               }
            }

         }
      });
      EntityRenderers.register(ModEntities.RIFT, RiftRenderer::new);
      EntityRenderers.register(ModEntities.MINI_RIFT, MiniRiftRenderer::new);
      EntityRenderers.register(ModEntities.SIFTITE_RETURN, (context) -> new ThrownItemRenderer<>(context, 1.0F, true));
   }
}
