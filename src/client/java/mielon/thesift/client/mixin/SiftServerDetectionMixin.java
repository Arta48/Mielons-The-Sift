package mielon.thesift.client.mixin;

import mielon.thesift.item.ModItems;
import mielon.thesift.world.VanillaPaletteRemapper;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPacketListener.class})
public abstract class SiftServerDetectionMixin {
   @Inject(
      method = {"handleLogin"},
      at = {@At("RETURN")}
   )
   private void theSift$detectServerFeatures(ClientboundLoginPacket packet, CallbackInfo ci) {
      ClientPacketListener listener = (ClientPacketListener)(Object)this;

      boolean serverHasTheSift = listener.registryAccess()
         .lookup(Registries.JUKEBOX_SONG)
         .flatMap(reg -> reg.get(ModItems.RIFT_JUKEBOX_SONG_KEY))
         .isPresent();

      VanillaPaletteRemapper.isVanillaServer = !serverHasTheSift;
      System.out.println("[TheSift] Connected to server. Has The Sift: " + serverHasTheSift + ", remap active: " + VanillaPaletteRemapper.isVanillaServer);
   }
}
