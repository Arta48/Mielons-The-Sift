package mielon.thesift.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mielon.thesift.fluid.IchorState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class VanillaPaletteRemapper {
   public static boolean isVanillaServer = false;
   private static BlockState[] VANILLA_PALETTE;

   public static void initialize() {
      List<BlockState> list = new ArrayList<>();
      for (Block block : BuiltInRegistries.BLOCK) {
         Identifier id = BuiltInRegistries.BLOCK.getKey(block);
         if (!"minecraft".equals(id.getNamespace())) {
            continue;
         }

         List<Property<?>> properties = new ArrayList<>();
         for (Property<?> p : block.getStateDefinition().getProperties()) {
            if (!"ichorlogged".equals(p.getName())) {
               properties.add(p);
            }
         }

         List<Map<Property<?>, Comparable<?>>> combinations = cartesianProduct(properties);
         for (Map<Property<?>, Comparable<?>> combination : combinations) {
            BlockState state = block.defaultBlockState();
            for (Map.Entry<Property<?>, Comparable<?>> entry : combination.entrySet()) {
               state = applyProperty(state, entry.getKey(), entry.getValue());
            }
            if (state.hasProperty(IchorState.ICHORLOGGED)) {
               state = (BlockState)state.setValue(IchorState.ICHORLOGGED, false);
            }
            list.add(state);
         }
      }
      VANILLA_PALETTE = (BlockState[])list.toArray(new BlockState[0]);
   }

   @SuppressWarnings("unchecked")
   private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, Comparable<?> value) {
      return (BlockState)state.setValue(property, (T)value);
   }

   private static List<Map<Property<?>, Comparable<?>>> cartesianProduct(List<Property<?>> properties) {
      List<Map<Property<?>, Comparable<?>>> result = new ArrayList<>();
      result.add(new HashMap<>());
      for (Property<?> property : properties) {
         List<Map<Property<?>, Comparable<?>>> next = new ArrayList<>();
         for (Map<Property<?>, Comparable<?>> map : result) {
            for (Comparable<?> val : property.getPossibleValues()) {
               Map<Property<?>, Comparable<?>> copy = new HashMap<>(map);
               copy.put(property, val);
               next.add(copy);
            }
         }
         result = next;
      }
      return result;
   }

   public static BlockState getVanillaState(int id) {
      if (VANILLA_PALETTE != null && id >= 0 && id < VANILLA_PALETTE.length) {
         return VANILLA_PALETTE[id];
      }
      return null;
   }
}
