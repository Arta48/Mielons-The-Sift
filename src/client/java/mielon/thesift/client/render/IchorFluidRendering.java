package mielon.thesift.client.render;

import mielon.thesift.fluid.ModFluids;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

public final class IchorFluidRendering {
   private IchorFluidRendering() {
   }

   public static void register() {
      Material still = new Material(Identifier.fromNamespaceAndPath("the_sift", "block/ichor_still"), true);
      Material flowing = new Material(Identifier.fromNamespaceAndPath("the_sift", "block/ichor_flow"), true);
      Material overlay = new Material(Identifier.withDefaultNamespace("block/water_overlay"), true);
      FluidModel.Unbaked model = new FluidModel.Unbaked(still, flowing, overlay, new IchorTint());
      FluidRenderingRegistry.register(ModFluids.ICHOR, ModFluids.FLOWING_ICHOR, model);
   }

   private static final class IchorTint implements BlockTintSource {
      public int color(BlockState state) {
         return -1;
      }

      public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
         return -1;
      }
   }
}
