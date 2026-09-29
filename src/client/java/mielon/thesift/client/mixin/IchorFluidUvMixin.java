package mielon.thesift.client.mixin;

import mielon.thesift.fluid.ModFluids;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FluidRenderer.class})
public abstract class IchorFluidUvMixin {
   @Unique
   private static final int THE_SIFT$WORLD_TILE_BLOCKS = 16;
   @Unique
   private static final float THE_SIFT$WORLD_TILE_SPAN = 0.0625F;
   @Unique
   private static final Identifier THE_SIFT$ICHOR_STILL = Identifier.fromNamespaceAndPath("the_sift", "block/ichor_still");
   @Unique
   private static final Identifier THE_SIFT$ICHOR_FLOW = Identifier.fromNamespaceAndPath("the_sift", "block/ichor_flow");
   @Unique
   private static final ThreadLocal THE_SIFT$ICHOR_CONTEXT = new ThreadLocal();

   @Inject(
      method = {"tesselate"},
      at = {@At("HEAD")}
   )
   private void theSift$beginIchorUv(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
      if (fluidState.getType() != ModFluids.ICHOR && fluidState.getType() != ModFluids.FLOWING_ICHOR) {
         THE_SIFT$ICHOR_CONTEXT.remove();
      } else {
         THE_SIFT$ICHOR_CONTEXT.set(new UvContext(pos.getX(), pos.getZ()));
      }

   }

   @Inject(
      method = {"tesselate"},
      at = {@At("RETURN")}
   )
   private void theSift$finishIchorUv(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
      THE_SIFT$ICHOR_CONTEXT.remove();
   }

   @Redirect(
      method = {"tesselate"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getU0()F"
)}
   )
   private float theSift$worldU0(TextureAtlasSprite sprite) {
      UvContext context = (UvContext)THE_SIFT$ICHOR_CONTEXT.get();
      return context != null && theSift$isIchorSprite(sprite) ? sprite.getU(context.baseU()) : sprite.getU0();
   }

   @Redirect(
      method = {"tesselate"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getU1()F"
)}
   )
   private float theSift$worldU1(TextureAtlasSprite sprite) {
      UvContext context = (UvContext)THE_SIFT$ICHOR_CONTEXT.get();
      return context != null && theSift$isIchorSprite(sprite) ? sprite.getU(context.baseU() + 0.0625F) : sprite.getU1();
   }

   @Redirect(
      method = {"tesselate"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getV0()F"
)}
   )
   private float theSift$worldV0(TextureAtlasSprite sprite) {
      UvContext context = (UvContext)THE_SIFT$ICHOR_CONTEXT.get();
      return context != null && theSift$isIchorSprite(sprite) ? sprite.getV(context.baseV()) : sprite.getV0();
   }

   @Redirect(
      method = {"tesselate"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getV1()F"
)}
   )
   private float theSift$worldV1(TextureAtlasSprite sprite) {
      UvContext context = (UvContext)THE_SIFT$ICHOR_CONTEXT.get();
      return context != null && theSift$isIchorSprite(sprite) ? sprite.getV(context.baseV() + 0.0625F) : sprite.getV1();
   }

   @Redirect(
      method = {"tesselate"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getU(F)F"
)}
   )
   private float theSift$worldFlowU(TextureAtlasSprite sprite, float localU) {
      UvContext context = (UvContext)THE_SIFT$ICHOR_CONTEXT.get();
      return context != null && theSift$isIchorSprite(sprite) ? sprite.getU(context.baseU() + theSift$clampUnit(localU) * 0.0625F) : sprite.getU(localU);
   }

   @Redirect(
      method = {"tesselate"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getV(F)F"
)}
   )
   private float theSift$worldFlowV(TextureAtlasSprite sprite, float localV) {
      UvContext context = (UvContext)THE_SIFT$ICHOR_CONTEXT.get();
      return context != null && theSift$isIchorSprite(sprite) ? sprite.getV(context.baseV() + theSift$clampUnit(localV) * 0.0625F) : sprite.getV(localV);
   }

   @Unique
   private static boolean theSift$isIchorSprite(TextureAtlasSprite sprite) {
      Identifier name = sprite.contents().name();
      return THE_SIFT$ICHOR_STILL.equals(name) || THE_SIFT$ICHOR_FLOW.equals(name);
   }

   @Unique
   private static float theSift$clampUnit(float coordinate) {
      return Math.max(0.0F, Math.min(1.0F, coordinate));
   }

   @Unique
   private static final class UvContext {
      private final float baseU;
      private final float baseV;

      private UvContext(int worldX, int worldZ) {
         this.baseU = (float)Math.floorMod(worldX, 16) * 0.0625F;
         this.baseV = (float)Math.floorMod(worldZ, 16) * 0.0625F;
      }

      private float baseU() {
         return this.baseU;
      }

      private float baseV() {
         return this.baseV;
      }
   }
}
