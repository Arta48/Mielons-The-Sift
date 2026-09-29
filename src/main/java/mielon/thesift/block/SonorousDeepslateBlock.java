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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class SonorousDeepslateBlock extends BaseEntityBlock {
   public static final EnumProperty MODE = EnumProperty.create("mode", Mode.class);

   public SonorousDeepslateBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(MODE, SonorousDeepslateBlock.Mode.HORN));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder builder) {
      builder.add(new Property[]{MODE});
   }

   protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
      ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
      if (stack.isEmpty()) {
         stack = new ItemStack(ModBlocks.SONOROUS_DEEPSLATE_ITEM);
      }

      stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of(MODE.getName(), ((Mode)state.getValue(MODE)).getSerializedName())));
      return stack;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new SonorousDeepslateBlockEntity(pos, state);
   }

   public BlockEntityTicker getTicker(Level level, BlockState state, BlockEntityType type) {
      return createTickerHelper(type, ModBlocks.SONOROUS_DEEPSLATE_BLOCK_ENTITY, SonorousDeepslateBlockEntity::tick);
   }

   public static enum Mode implements StringRepresentable {
      HORN("horn"),
      NOTE("note");

      private final String name;

      private Mode(String name) {
         this.name = name;
      }

      public String getSerializedName() {
         return this.name;
      }

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{HORN, NOTE};
      }
   }
}
