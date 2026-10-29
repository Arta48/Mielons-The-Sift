package mielon.thesift.client.mixin;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer"},
   remap = false
)
public abstract class SodiumIchorFluidUvMixin {
   @Unique
   private static final int THE_SIFT$WORLD_TILE_BLOCKS = 16;
   @Unique
   private static final float THE_SIFT$WORLD_TILE_SPAN = 0.0625F;
   @Unique
   private static final Identifier THE_SIFT$ICHOR_STILL = Identifier.fromNamespaceAndPath("the_sift", "block/ichor_still");
   @Unique
   private static final Identifier THE_SIFT$ICHOR_FLOW = Identifier.fromNamespaceAndPath("the_sift", "block/ichor_flow");
   @Unique
   private static final ThreadLocal<UvContext> THE_SIFT$ICHOR_CONTEXT = new ThreadLocal<>();

   @ModifyVariable(
      method = {"render"},
      at = {@At("HEAD")},
      argsOnly = true,
      ordinal = 0,
      require = 0,
      remap = false
   )
   private BlockPos theSift$captureFluidBlockPos(BlockPos pos) {
      THE_SIFT$ICHOR_CONTEXT.set(new UvContext(pos.getX(), pos.getZ()));
      return pos;
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")},
      require = 0,
      remap = false
   )
   private void theSift$clearFluidBlockPos(CallbackInfo ci) {
      THE_SIFT$ICHOR_CONTEXT.remove();
   }

   @Redirect(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getU0()F"
      )},
      require = 0,
      remap = false
   )
   private float theSift$worldU0(TextureAtlasSprite sprite) {
      UvContext context = THE_SIFT$ICHOR_CONTEXT.get();
      return context != null && theSift$isIchorSprite(sprite) ? sprite.getU(context.baseU()) : sprite.getU0();
   }

   @Redirect(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getU1()F"
      )},
      require = 0,
      remap = false
   )
   private float theSift$worldU1(TextureAtlasSprite sprite) {
      UvContext context = THE_SIFT$ICHOR_CONTEXT.get();
      return context != null && theSift$isIchorSprite(sprite) ? sprite.getU(context.baseU() + 0.0625F) : sprite.getU1();
   }

   @Redirect(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getV0()F"
      )},
      require = 0,
      remap = false
   )
   private float theSift$worldV0(TextureAtlasSprite sprite) {
      UvContext context = THE_SIFT$ICHOR_CONTEXT.get();
      return context != null && theSift$isIchorSprite(sprite) ? sprite.getV(context.baseV()) : sprite.getV0();
   }

   @Redirect(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getV1()F"
      )},
      require = 0,
      remap = false
   )
   private float theSift$worldV1(TextureAtlasSprite sprite) {
      UvContext context = THE_SIFT$ICHOR_CONTEXT.get();
      return context != null && theSift$isIchorSprite(sprite) ? sprite.getV(context.baseV() + 0.0625F) : sprite.getV1();
   }

   @Redirect(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getU(F)F"
      )},
      require = 0,
      remap = false
   )
   private float theSift$worldFlowU(TextureAtlasSprite sprite, float localU) {
      UvContext context = THE_SIFT$ICHOR_CONTEXT.get();
      return context != null && theSift$isIchorSprite(sprite) ? sprite.getU(context.baseU() + theSift$clampUnit(localU) * 0.0625F) : sprite.getU(localU);
   }

   @Redirect(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getV(F)F"
      )},
      require = 0,
      remap = false
   )
   private float theSift$worldFlowV(TextureAtlasSprite sprite, float localV) {
      UvContext context = THE_SIFT$ICHOR_CONTEXT.get();
      return context != null && theSift$isIchorSprite(sprite) ? sprite.getV(context.baseV() + theSift$clampUnit(localV) * 0.0625F) : sprite.getV(localV);
   }

   @Unique
   private static boolean theSift$isIchorSprite(TextureAtlasSprite sprite) {
      Identifier name = sprite.contents().name();
      return THE_SIFT$ICHOR_STILL.equals(name) || THE_SIFT$ICHOR_FLOW.equals(name);
   }

   @Unique
   private static float theSift$clampUnit(float coordinate) {
      return Math.clamp(coordinate, 0.0F, 1.0F);
   }

   @Unique
   private record UvContext(float baseU, float baseV) {
      private UvContext(int baseU, int baseV) {
          this((float) Math.floorMod(baseU, 16) * 0.0625F, (float) Math.floorMod(baseV, 16) * 0.0625F);
      }
   }
}
