package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class OvergrownWillowFoliageBlock extends TintedParticleLeavesBlock {
   private static final int FALLING_LEAF_COLOR = 4063205;

   public OvergrownWillowFoliageBlock(float leafParticleChance, BlockBehaviour.Properties properties) {
      super(leafParticleChance, properties);
   }

   protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
      ParticleUtils.spawnParticleBelow(level, pos, random, ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 4063205));
   }
}
