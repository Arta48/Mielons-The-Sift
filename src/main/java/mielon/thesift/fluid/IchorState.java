package mielon.thesift.fluid;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;

public final class IchorState {
   public static final BooleanProperty ICHORLOGGED = BooleanProperty.create("ichorlogged");
   public static FluidState source;

   private IchorState() {
   }

   public static BlockState dryDefault(BlockState state) {
      return state.hasProperty(ICHORLOGGED) ? (BlockState)state.setValue(ICHORLOGGED, false) : state;
   }
}
