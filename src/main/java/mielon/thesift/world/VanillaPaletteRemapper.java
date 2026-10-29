package mielon.thesift.world;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mielon.thesift.fluid.IchorState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class VanillaPaletteRemapper {
   public static boolean isVanillaServer = false;
   private static BlockState[] VANILLA_PALETTE;
   private static final Reference2IntMap<BlockState> STATE_TO_VANILLA_ID = new Reference2IntOpenHashMap<>();

   static {
      STATE_TO_VANILLA_ID.defaultReturnValue(-1);
   }

   public static void initialize() {
      List<BlockState> list = new ArrayList<>();
      STATE_TO_VANILLA_ID.clear();

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
               state = state.setValue(IchorState.ICHORLOGGED, false);
            }

            int vanillaId = list.size();
            list.add(state);
            STATE_TO_VANILLA_ID.put(state, vanillaId);
         }
      }
      VANILLA_PALETTE = list.toArray(new BlockState[0]);
   }

   @SuppressWarnings("unchecked")
   private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, Comparable<?> value) {
      return state.setValue(property, (T)value);
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
      return Blocks.AIR.defaultBlockState();
   }

   public static int getVanillaId(BlockState state) {
      if (state == null) {
         return -1;
      }
      return STATE_TO_VANILLA_ID.getInt(state);
   }
}