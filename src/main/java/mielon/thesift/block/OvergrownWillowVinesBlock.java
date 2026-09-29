package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class OvergrownWillowVinesBlock extends VineBlock {
   public static final BooleanProperty BOTTOM;
   public static final BooleanProperty NORTH_BOTTOM;
   public static final BooleanProperty EAST_BOTTOM;
   public static final BooleanProperty SOUTH_BOTTOM;
   public static final BooleanProperty WEST_BOTTOM;

   public OvergrownWillowVinesBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(BOTTOM, true)).setValue(NORTH_BOTTOM, true)).setValue(EAST_BOTTOM, true)).setValue(SOUTH_BOTTOM, true)).setValue(WEST_BOTTOM, true));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState placed = super.getStateForPlacement(context);
      return placed == null ? null : refreshBottomFaces(placed, context.getLevel().getBlockState(context.getClickedPos().below()));
   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
      BlockState updated = super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
      if (updated.isAir()) {
         return updated;
      } else {
         BlockState below = direction == Direction.DOWN ? neighborState : level.getBlockState(pos.below());
         return refreshBottomFaces(updated, below);
      }
   }

   protected void createBlockStateDefinition(StateDefinition.Builder builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{BOTTOM, NORTH_BOTTOM, EAST_BOTTOM, SOUTH_BOTTOM, WEST_BOTTOM});
   }

   public static BlockState refreshBottomFaces(BlockState state, BlockState below) {
      if (!isVineSegment(state)) {
         return state;
      } else {
         boolean everyActiveFaceEndsHere = true;

         for (Direction direction : Plane.HORIZONTAL) {
            BooleanProperty face = VineBlock.getPropertyForFace(direction);
            boolean active = state.hasProperty(face) && (Boolean)state.getValue(face);
            boolean continues = active && hasFace(below, direction);
            state = (BlockState)state.setValue(getBottomProperty(direction), !continues);
            if (continues) {
               everyActiveFaceEndsHere = false;
            }
         }

         return (BlockState)state.setValue(BOTTOM, everyActiveFaceEndsHere);
      }
   }

   public static BooleanProperty getBottomProperty(Direction direction) {
      BooleanProperty var10000;
      switch (direction) {
         case NORTH -> var10000 = NORTH_BOTTOM;
         case EAST -> var10000 = EAST_BOTTOM;
         case SOUTH -> var10000 = SOUTH_BOTTOM;
         case WEST -> var10000 = WEST_BOTTOM;
         default -> throw new IllegalArgumentException("Vine bottom is only defined for horizontal faces");
      }

      return var10000;
   }

   private static boolean hasFace(BlockState state, Direction direction) {
      BooleanProperty face = VineBlock.getPropertyForFace(direction);
      return isVineSegment(state) && state.hasProperty(face) && (Boolean)state.getValue(face);
   }

   private static boolean isVineSegment(BlockState state) {
      return state.getBlock() instanceof OvergrownWillowVinesBlock;
   }

   static {
      BOTTOM = BlockStateProperties.BOTTOM;
      NORTH_BOTTOM = BooleanProperty.create("north_bottom");
      EAST_BOTTOM = BooleanProperty.create("east_bottom");
      SOUTH_BOTTOM = BooleanProperty.create("south_bottom");
      WEST_BOTTOM = BooleanProperty.create("west_bottom");
   }
}
