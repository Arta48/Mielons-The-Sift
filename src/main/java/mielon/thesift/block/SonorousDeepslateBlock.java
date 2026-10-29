package mielon.thesift.block;

import java.util.Map;
import mielon.thesift.block.entity.SonorousDeepslateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.NonNull;

public class SonorousDeepslateBlock extends BaseEntityBlock {
   public static final EnumProperty MODE = EnumProperty.create("mode", Mode.class);

   public SonorousDeepslateBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(MODE, Mode.HORN));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder builder) {
      builder.add(MODE);
   }

   protected @NonNull ItemStack getCloneItemStack(@NonNull LevelReader level, @NonNull BlockPos pos, @NonNull BlockState state, boolean includeData) {
      ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
      if (stack.isEmpty()) {
         stack = new ItemStack(ModBlocks.SONOROUS_DEEPSLATE_ITEM);
      }

      stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of(MODE.getName(), ((Mode)state.getValue(MODE)).getSerializedName())));
      return stack;
   }

   public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
      return new SonorousDeepslateBlockEntity(pos, state);
   }

   public BlockEntityTicker getTicker(@NonNull Level level, @NonNull BlockState state, @NonNull BlockEntityType type) {
      return createTickerHelper(type, ModBlocks.SONOROUS_DEEPSLATE_BLOCK_ENTITY, SonorousDeepslateBlockEntity::tick);
   }

   public enum Mode implements StringRepresentable {
      HORN("horn"),
      NOTE("note");

      private final String name;

      Mode(String name) {
         this.name = name;
      }

      public @NonNull String getSerializedName() {
         return this.name;
      }

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{HORN, NOTE};
      }
   }
}
