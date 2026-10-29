package mielon.thesift.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

public record RiftLoadingPayload(boolean start, boolean rift) implements CustomPacketPayload {
   public static final Type<RiftLoadingPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("the_sift", "rift_loading"));
   public static final StreamCodec<ByteBuf, RiftLoadingPayload> CODEC;

   public @NonNull Type<RiftLoadingPayload> type() {
      return TYPE;
   }

   public static void register() {
      PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
   }

   public static void send(ServerPlayer player, boolean start) {
      send(player, start, true);
   }

   public static void send(ServerPlayer player, boolean start, boolean rift) {
      if (ServerPlayNetworking.canSend(player, TYPE)) {
         ServerPlayNetworking.send(player, new RiftLoadingPayload(start, rift));
      }

   }

   static {
      CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, RiftLoadingPayload::start, ByteBufCodecs.BOOL, RiftLoadingPayload::rift, RiftLoadingPayload::new);
   }
}
