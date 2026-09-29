package mielon.thesift.fluid;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;

public final class IchorWaterlogging {
   private static final Identifier STORAGE_ID = Identifier.fromNamespaceAndPath("the_sift", "ichorlogged_blocks");
   private static final Map POSITIONS = Collections.synchronizedMap(new WeakHashMap());
   private static boolean loaded;
   private static boolean dirty;
   private static int saveCooldown;

   private IchorWaterlogging() {
   }

   public static void mark(LevelAccessor level, BlockPos pos) {
      if (((Set)POSITIONS.computeIfAbsent(level, (ignored) -> ConcurrentHashMap.newKeySet())).add(pos.asLong()) && level instanceof ServerLevel) {
         dirty = true;
      }

   }

   public static boolean remove(LevelAccessor level, BlockPos pos) {
      Set<Long> positions = (Set)POSITIONS.get(level);
      boolean removed = positions != null && positions.remove(pos.asLong());
      if (removed && level instanceof ServerLevel) {
         dirty = true;
      }

      return removed;
   }

   public static boolean contains(LevelAccessor level, BlockPos pos) {
      Set<Long> positions = (Set)POSITIONS.get(level);
      return isIchorlogged(level.getBlockState(pos)) || positions != null && positions.contains(pos.asLong());
   }

   public static boolean canIchorlog(BlockState state) {
      return state.hasProperty(IchorState.ICHORLOGGED) && waterloggedProperty(state) != null;
   }

   public static boolean isIchor(Fluid fluid) {
      return fluid instanceof IchorFluid;
   }

   public static boolean canContain(BlockState state) {
      return canIchorlog(state) && (!state.hasProperty(BlockStateProperties.SLAB_TYPE) || state.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE);
   }

   public static boolean canFill(BlockState state) {
      return canContain(state) && !isFluidlogged(state);
   }

   public static boolean isIchorlogged(BlockState state) {
      return state.hasProperty(IchorState.ICHORLOGGED) && (Boolean)state.getValue(IchorState.ICHORLOGGED) && isFluidlogged(state);
   }

   public static BlockState fillState(BlockState state) {
      return (BlockState)setFluidlogged(state, true).setValue(IchorState.ICHORLOGGED, true);
   }

   public static boolean fill(LevelAccessor level, BlockPos pos, BlockState state) {
      if (!canFill(state)) {
         return false;
      } else {
         if (!level.isClientSide()) {
            if (!level.setBlock(pos, fillState(state), 3)) {
               return false;
            }

            level.scheduleTick(pos, ModFluids.ICHOR, ModFluids.ICHOR.getTickDelay(level));
         }

         return true;
      }
   }

   public static boolean isFluidlogged(BlockState state) {
      BooleanProperty property = waterloggedProperty(state);
      return property != null && (Boolean)state.getValue(property);
   }

   public static BlockState setFluidlogged(BlockState state, boolean value) {
      BooleanProperty property = waterloggedProperty(state);
      BlockState result = property == null ? state : (BlockState)state.setValue(property, value);
      return value ? result : IchorState.dryDefault(result);
   }

   private static BooleanProperty waterloggedProperty(BlockState state) {
      if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
         return BlockStateProperties.WATERLOGGED;
      } else {
         Stream var10000 = state.getProperties().stream();
         Objects.requireNonNull(BooleanProperty.class);
         var10000 = var10000.filter(BooleanProperty.class::isInstance);
         Objects.requireNonNull(BooleanProperty.class);
         return (BooleanProperty)var10000.map(BooleanProperty.class::cast).filter((property) -> "waterlogged".equals(((BooleanProperty) property).getName())).findFirst().orElse(null);
      }
   }

   public static void tick(MinecraftServer server) {
      ensureLoaded(server);

      for (ServerLevel level : server.getAllLevels()) {
         Set<Long> positions = (Set)POSITIONS.get(level);
         if (positions != null && !positions.isEmpty()) {
            Iterator<Long> iterator = positions.iterator();

            while(iterator.hasNext()) {
               long packed = (Long)iterator.next();
               BlockPos pos = BlockPos.of(packed);
               if (level.isLoaded(pos)) {
                  BlockState state = level.getBlockState(pos);
                  if (canContain(state) && isFluidlogged(state)) {
                     level.setBlock(pos, fillState(state), 3);
                     level.scheduleTick(pos, ModFluids.ICHOR, ModFluids.ICHOR.getTickDelay(level));
                  }

                  iterator.remove();
                  dirty = true;
               }
            }
         }
      }

      if (dirty && --saveCooldown <= 0) {
         saveCooldown = 600;
         writeStorage(server);
      }

   }

   public static void clear() {
      POSITIONS.clear();
      loaded = false;
      dirty = false;
      saveCooldown = 0;
   }

   public static void flush(MinecraftServer server) {
      ensureLoaded(server);
      if (dirty) {
         writeStorage(server);
      }

   }

   private static void ensureLoaded(MinecraftServer server) {
      if (!loaded) {
         loaded = true;
         CompoundTag stored = server.getCommandStorage().get(STORAGE_ID);

         for (ServerLevel level : server.getAllLevels()) {
            Set<Long> positions = (Set)POSITIONS.computeIfAbsent(level, (ignored) -> ConcurrentHashMap.newKeySet());
            long[] saved = (long[])stored.getLongArray(level.dimension().identifier().toString()).orElse(new long[0]);

            for (long packed : saved) {
               positions.add(packed);
            }
         }

      }
   }

   private static void writeStorage(MinecraftServer server) {
      CompoundTag stored = new CompoundTag();

      for (ServerLevel level : server.getAllLevels()) {
         Set<Long> positions = (Set)POSITIONS.get(level);
         if (positions != null && !positions.isEmpty()) {
            stored.putLongArray(level.dimension().identifier().toString(), positions.stream().mapToLong(Long::longValue).sorted().toArray());
         }
      }

      server.getCommandStorage().set(STORAGE_ID, stored);
      dirty = false;
   }
}
