package mielon.thesift.block;

import mielon.thesift.block.entity.SiftPortalBlockEntity;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.world.SiftTeleportManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SiftPortalBlock extends BaseEntityBlock {
   public SiftPortalBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new SiftPortalBlockEntity(pos, state);
   }

   public BlockEntityTicker getTicker(Level level, BlockState state, BlockEntityType type) {
      return createTickerHelper(type, ModBlocks.SIFT_PORTAL_BLOCK_ENTITY, SiftPortalBlockEntity::tick);
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
      if (!level.isClientSide()) {
         SiftTeleportManager.onPortalContact(entity);
      }

   }

   public void spawnDestroyParticles(Level level, BlockPos pos, BlockState state) {
      if (level.isClientSide()) {
         RandomSource random = level.getRandom();

         for (int i = 0; i < 48; ++i) {
            double x = (double)pos.getX() + random.nextDouble();
            double y = (double)pos.getY() + random.nextDouble();
            double z = (double)pos.getZ() + random.nextDouble();
            double xd = (random.nextDouble() - (double)0.5F) * 0.18;
            double yd = (random.nextDouble() - 0.15) * 0.18;
            double zd = (random.nextDouble() - (double)0.5F) * 0.18;
            level.addParticle(ModParticles.SIFT_PARALLAX, x, y, z, xd, yd, zd);
         }

      }
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.block();
   }
}
