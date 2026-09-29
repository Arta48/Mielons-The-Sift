package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SnifferCaveFeature implements Feature {
   public static final MapCodec OVERGROWN_CODEC = MapCodec.unit(() -> new SnifferCaveFeature(16, 6002815919507592773L, 4));
   public static final MapCodec WASTES_CODEC = MapCodec.unit(() -> new SnifferCaveFeature(28, 6002815919508111699L, 3));
   private static final Direction[] HORIZONTAL;
   private static final Block[] NEST_PLANTS;
   private final int cellChunks;
   private final long placementSalt;
   private final int candidateCount;

   public SnifferCaveFeature(int cellChunks, long placementSalt, int candidateCount) {
      this.cellChunks = cellChunks;
      this.placementSalt = placementSalt;
      this.candidateCount = candidateCount;
   }

   public MapCodec codec() {
      return this.cellChunks == 16 && this.placementSalt == 6002815919507592773L && this.candidateCount == 4 ? OVERGROWN_CODEC : WASTES_CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      if (!SiftLandmarkPlacement.isSelectedChunk(level.getSeed(), origin, this.cellChunks, this.placementSalt, this.candidateCount)) {
         return false;
      } else {
         SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(origin);
         int length = 14 + random.nextInt(3);
         CaveEntrance entrance = findEntrance(level, origin);
         Direction direction = entrance == null ? null : chooseDirection(level, bounds, entrance.pos(), length, random);
         if (entrance != null && direction != null) {
            Set<BlockPos> interior = new HashSet();
            Set<BlockPos> shell = new HashSet();
            Direction sideways = direction.getClockWise();
            double phase = random.nextDouble() * Math.PI * (double)2.0F;
            BlockPos lastCenter = entrance.pos();

            for (int distance = 0; distance <= length; distance += 2) {
               double progress = (double)distance / (double)length;
               int lateral = (int)Math.round(Math.sin(phase + progress * Math.PI * 1.7) * (0.6 + progress * 1.35));
               int lift = (int)Math.round(Math.sin(progress * Math.PI) * 0.9) - (int)Math.round(progress * (double)9.0F);
               BlockPos center = entrance.pos().relative(direction, distance).relative(sideways, lateral).above(lift);
               double swell = Math.sin(progress * Math.PI);
               double radiusXz = 2.1 + swell * 1.65 + random.nextDouble() * 0.35;
               double radiusY = 1.9 + swell * 1.15 + random.nextDouble() * (double)0.25F;
               addEllipsoid(center, radiusXz, radiusY, radiusXz * 0.92, interior, shell);
               lastCenter = center;
            }

            BlockPos nestCenter = lastCenter.relative(direction).above();
            addEllipsoid(nestCenter, 5.2, 3.7, (double)5.0F, interior, shell);
            IchorPoolPlan pool = createIchorPoolPlan(nestCenter, sideways, random);
            addOrientedEllipsoid(pool.chamberCenter(), pool.chamberOutwardRadius(), pool.chamberVerticalRadius(), pool.chamberCrossRadius(), pool.outward(), interior, shell);
            shell.removeAll(interior);
            IchorBasin basin = createIchorBasin(pool);
            Set<BlockPos> writes = new HashSet(interior);
            writes.addAll(shell);
            writes.addAll(basin.fluid());
            writes.addAll(basin.lining());
            writes.addAll(basin.clearance());
            if (!SiftFeaturePlacementGuard.intersectsPortal(nestCenter, 12) && validMass(level, bounds, nestCenter) && canCarveAll(level, bounds, writes)) {
               for (BlockPos pos : shell) {
                  if (!bounds.contains(pos)) {
                     return false;
                  }

                  BlockState current = level.getBlockState(pos);
                  if (isSiftTerrain(current) && level.ensureCanWrite(pos)) {
                     float healthyChance = 0.18F + (distanceSquared(pos, nestCenter) < 90 ? 0.13F : 0.0F);
                     BlockState lining = random.nextFloat() < healthyChance ? ModBlocks.HEALTHY_SCULK.defaultBlockState() : ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState();
                     level.setBlock(pos, lining, 2);
                  }
               }

               for (BlockPos pos : interior) {
                  if (bounds.contains(pos) && level.ensureCanWrite(pos)) {
                     level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                  }
               }

               placeIchorPool(level, random, basin);
               scatterSnifferPlants(level, random, interior, nestCenter);
               List<BlockPos> nest = buildNest(level, random, nestCenter);
               SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SNIFFER_CAVE, nestCenter);
               if (nest.isEmpty()) {
                  return true;
               } else {
                  placeNestContents(level, random, nest);
                  SiftDeferredSnifferSpawner.enqueueFamily(level, random, nest);
                  return true;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   private static CaveEntrance findEntrance(WorldGenLevel level, BlockPos origin) {
      int x = (origin.getX() & -16) + 8;
      int z = (origin.getZ() & -16) + 8;
      int surface = level.getHeight(Types.WORLD_SURFACE_WG, x, z);
      BlockPos entrance = new BlockPos(x, surface, z);
      return surface > level.getMinY() + 18 && isSiftTerrain(level.getBlockState(entrance.below())) ? new CaveEntrance(entrance) : null;
   }

   private static Direction chooseDirection(WorldGenLevel level, SiftWorldgenBounds bounds, BlockPos entrance, int length, RandomSource random) {
      List<DirectionScore> candidates = new ArrayList();
      int start = random.nextInt(HORIZONTAL.length);

      for (int index = 0; index < HORIZONTAL.length; ++index) {
         Direction direction = HORIZONTAL[(start + index) % HORIZONTAL.length];
         BlockPos end = entrance.relative(direction, length + 1).below(8);
         if (bounds.contains(end.offset(6, 4, 6)) && bounds.contains(end.offset(-6, -5, -6))) {
            int surface = level.getHeight(Types.WORLD_SURFACE_WG, end.getX(), end.getZ());
            int cover = surface - end.getY();
            if (cover >= 5) {
               candidates.add(new DirectionScore(direction, cover + random.nextInt(4)));
            }
         }
      }

      return (Direction)candidates.stream().max((first, second) -> Integer.compare(first.score(), second.score())).map(DirectionScore::direction).orElse(null);
   }

   private static void addEllipsoid(BlockPos center, double radiusX, double radiusY, double radiusZ, Set interior, Set shell) {
      int maxX = (int)Math.ceil(radiusX + (double)1.0F);
      int maxY = (int)Math.ceil(radiusY + (double)1.0F);
      int maxZ = (int)Math.ceil(radiusZ + (double)1.0F);

      for (int dx = -maxX; dx <= maxX; ++dx) {
         for (int dy = -maxY; dy <= maxY; ++dy) {
            for (int dz = -maxZ; dz <= maxZ; ++dz) {
               double inner = square((double)dx / radiusX) + square((double)dy / radiusY) + square((double)dz / radiusZ);
               BlockPos pos = center.offset(dx, dy, dz);
               if (inner <= (double)1.0F) {
                  interior.add(pos);
               } else {
                  double outer = square((double)dx / (radiusX + (double)1.0F)) + square((double)dy / (radiusY + (double)1.0F)) + square((double)dz / (radiusZ + (double)1.0F));
                  if (outer <= (double)1.0F) {
                     shell.add(pos);
                  }
               }
            }
         }
      }

   }

   private static void addOrientedEllipsoid(BlockPos center, double outwardRadius, double verticalRadius, double crossRadius, Direction outward, Set interior, Set shell) {
      double radiusX = outward.getAxis() == Axis.X ? outwardRadius : crossRadius;
      double radiusZ = outward.getAxis() == Axis.Z ? outwardRadius : crossRadius;
      addEllipsoid(center, radiusX, verticalRadius, radiusZ, interior, shell);
   }

   private static IchorPoolPlan createIchorPoolPlan(BlockPos nestCenter, Direction sideways, RandomSource random) {
      int variant = random.nextInt(4);
      Direction outward = random.nextBoolean() ? sideways : sideways.getOpposite();
      double var10000;
      switch (variant) {
         case 1 -> var10000 = 4.4;
         case 2 -> var10000 = 3.8;
         case 3 -> var10000 = 4.1;
         default -> var10000 = (double)4.0F;
      }

      double chamberOutward = var10000;
      switch (variant) {
         case 1 -> var10000 = 2.7;
         case 2 -> var10000 = (double)3.5F;
         case 3 -> var10000 = 3.1;
         default -> var10000 = (double)3.0F;
      }

      double chamberCross = var10000;
      double chamberVertical = variant == 3 ? (double)3.0F : 2.7;
      BlockPos center = nestCenter.relative(outward, 5).below();
      return new IchorPoolPlan(center, outward, variant, chamberOutward, chamberVertical, chamberCross);
   }

   private static IchorBasin createIchorBasin(IchorPoolPlan plan) {
      byte var10000;
      switch (plan.variant()) {
         case 1 -> var10000 = 3;
         case 2 -> var10000 = 2;
         case 3 -> var10000 = 3;
         default -> var10000 = 2;
      }

      int outwardRadius = var10000;
      switch (plan.variant()) {
         case 1 -> var10000 = 2;
         case 2 -> var10000 = 3;
         case 3 -> var10000 = 2;
         default -> var10000 = 2;
      }

      int crossRadius = var10000;
      Direction cross = plan.outward().getClockWise();
      Set<BlockPos> surface = new HashSet();
      Set<BlockPos> fluid = new HashSet();
      Set<BlockPos> lining = new HashSet();
      Set<BlockPos> clearance = new HashSet();

      for (int along = -outwardRadius; along <= outwardRadius; ++along) {
         for (int across = -crossRadius; across <= crossRadius; ++across) {
            double normalized = square((double)along / ((double)outwardRadius + 0.35)) + square((double)across / ((double)crossRadius + 0.35));
            double edgeWobble = Math.sin((double)along * 2.17 + (double)across * 1.41 + (double)plan.variant() * 0.83) * 0.11;
            if (!(normalized > (double)1.0F + edgeWobble)) {
               BlockPos top = plan.chamberCenter().below(3).relative(plan.outward(), along).relative(cross, across);
               surface.add(top);
               fluid.add(top);
               if (plan.variant() == 3 && normalized < 0.55) {
                  fluid.add(top.below());
               }
            }
         }
      }

      for (BlockPos wet : fluid) {
         lining.add(wet.below());

         for (Direction side : HORIZONTAL) {
            lining.add(wet.relative(side));
         }
      }

      lining.removeAll(fluid);

      for (BlockPos top : surface) {
         for (int dy = 1; dy <= 3; ++dy) {
            clearance.add(top.above(dy));
         }

         for (Direction side : HORIZONTAL) {
            BlockPos bank = top.relative(side);
            if (!surface.contains(bank)) {
               lining.add(bank.below());
               lining.add(bank.below(2));
               clearance.add(bank.above());
               clearance.add(bank.above(2));
            }
         }
      }

      lining.removeAll(fluid);
      return new IchorBasin(Set.copyOf(fluid), Set.copyOf(lining), Set.copyOf(clearance));
   }

   private static void placeIchorPool(WorldGenLevel level, RandomSource random, IchorBasin basin) {
      for (BlockPos pos : (Iterable<BlockPos>) basin.clearance()) {
         level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
      }

      for (BlockPos pos : (Iterable<BlockPos>) basin.lining()) {
         level.setBlock(pos, (random.nextFloat() < 0.55F ? ModBlocks.HEALTHY_SCULK : ModBlocks.DRY_HEALTHY_SCULK).defaultBlockState(), 2);
      }

      for (BlockPos pos : (Iterable<BlockPos>) basin.fluid()) {
         level.setBlock(pos, ModFluids.ICHOR_BLOCK.defaultBlockState(), 2);
      }

   }

   private static boolean canCarveAll(WorldGenLevel level, SiftWorldgenBounds bounds, Set interior) {
      int siftMass = 0;
      int protectedBlocks = 0;

      for (BlockPos pos : (Iterable<BlockPos>) interior) {
         if (!bounds.contains(pos) || !level.ensureCanWrite(pos)) {
            return false;
         }

         BlockState state = level.getBlockState(pos);
         if (!state.is(ModBlocks.REINFORCED_SIFTSLATE) && !state.is(ModBlocks.SIFT_PORTAL) && !state.is(ModBlocks.SONOROUS_DEEPSLATE) && !state.is(ModBlocks.OVERGROWN_WILLOW_LOG) && !state.is(ModBlocks.OVERGROWN_WILLOW_WOOD)) {
            if (isSiftTerrain(state)) {
               ++siftMass;
            } else if (!isCarvableVegetation(state) && !state.isAir()) {
               return false;
            }
         } else {
            ++protectedBlocks;
         }
      }

      return protectedBlocks == 0 && (double)siftMass >= (double)interior.size() * 0.4;
   }

   private static boolean validMass(WorldGenLevel level, SiftWorldgenBounds bounds, BlockPos center) {
      int solid = 0;
      int total = 0;

      for (int dx = -5; dx <= 5; dx += 2) {
         for (int dy = -3; dy <= 3; dy += 2) {
            for (int dz = -5; dz <= 5; dz += 2) {
               ++total;
               BlockPos sample = center.offset(dx, dy, dz);
               if (!bounds.contains(sample)) {
                  return false;
               }

               if (isSiftTerrain(level.getBlockState(sample))) {
                  ++solid;
               }
            }
         }
      }

      return (double)solid >= (double)total * 0.56;
   }

   private static void scatterSnifferPlants(WorldGenLevel level, RandomSource random, Set interior, BlockPos nestCenter) {
      List<BlockPos> sample = new ArrayList(interior);
      int placed = 0;
      SiftSnifferPlantPlacement.PlantType blobType = SiftSnifferPlantPlacement.chooseBlobType(random, 0.6F);

      for (int attempt = 0; attempt < 72 && placed < 14; ++attempt) {
         BlockPos air = (BlockPos)sample.get(random.nextInt(sample.size()));
         BlockPos floor = findFloor(level, air.getX(), air.getZ(), nestCenter.getY());
         if (floor != null && level.isEmptyBlock(floor.above())) {
            BlockState floorState = level.getBlockState(floor);
            if (floorState.is(ModBlocks.HEALTHY_SCULK) || floorState.is(ModBlocks.DRY_HEALTHY_SCULK)) {
               BlockPos plantPos = floor.above();
               if (SiftSnifferPlantPlacement.place(level, plantPos, blobType, random)) {
                  ++placed;
               }
            }
         }
      }

   }

   private static List buildNest(WorldGenLevel level, RandomSource random, BlockPos center) {
      List<BlockPos> nest = new ArrayList();
      BlockState foliage = (BlockState)ModBlocks.OVERGROWN_WILLOW_FOLIAGE.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

      for (int dx = -3; dx <= 3; ++dx) {
         for (int dz = -3; dz <= 3; ++dz) {
            double distance = square((double)dx / (double)3.25F) + square((double)dz / (double)3.25F);
            if (!(distance > (double)1.0F) && (!(distance > 0.68) || !(random.nextFloat() < 0.34F))) {
               BlockPos floor = findFloor(level, center.getX() + dx, center.getZ() + dz, center.getY());
               if (floor != null && level.isEmptyBlock(floor.above())) {
                  BlockState current = level.getBlockState(floor);
                  if (current.is(ModBlocks.HEALTHY_SCULK) || current.is(ModBlocks.DRY_HEALTHY_SCULK)) {
                     level.setBlock(floor, foliage, 2);
                     nest.add(floor);
                  }
               }
            }
         }
      }

      return nest;
   }

   private static void placeNestContents(WorldGenLevel level, RandomSource random, List nest) {
      int eggs = random.nextFloat() < 0.5F ? 1 : 0;
      if (eggs == 1 && random.nextFloat() < 0.1F) {
         ++eggs;
      }

      for (int index = 0; index < eggs; ++index) {
         placeOnRandomFreeNestBlock(level, random, nest, Blocks.SNIFFER_EGG.defaultBlockState());
      }

      int plants = 4 + random.nextInt(5);

      for (int index = 0; index < plants; ++index) {
         Block plant = NEST_PLANTS[random.nextInt(NEST_PLANTS.length)];
         placeOnRandomFreeNestBlock(level, random, nest, plant.defaultBlockState());
      }

   }

   private static void placeOnRandomFreeNestBlock(WorldGenLevel level, RandomSource random, List nest, BlockState state) {
      for (int attempt = 0; attempt < 18; ++attempt) {
         BlockPos pos = ((BlockPos)nest.get(random.nextInt(nest.size()))).above();
         if (level.isEmptyBlock(pos) && state.canSurvive(level, pos)) {
            level.setBlock(pos, state, 2);
            return;
         }
      }

   }

   private static BlockPos findFloor(WorldGenLevel level, int x, int z, int aroundY) {
      for (int y = aroundY + 3; y >= aroundY - 7; --y) {
         BlockPos air = new BlockPos(x, y, z);
         BlockPos floor = air.below();
         if (level.getBlockState(air).isAir() && isSiftTerrain(level.getBlockState(floor))) {
            return floor;
         }
      }

      return null;
   }

   private static boolean isCarvableVegetation(BlockState state) {
      return state.is(ModBlocks.OVERGROWN_FRONDS) || state.is(ModBlocks.OVERGROWN_CHARD) || state.is(ModBlocks.OVERGROWN_STALKS) || state.is(ModBlocks.SIFTSLATE_STALKS) || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS) || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS) || state.is(ModBlocks.SIFTSLATE_HANGING_ROOTS) || state.is(ModBlocks.OVERGROWN_HANGING_ROOTS);
   }

   private static boolean isSiftTerrain(BlockState state) {
      return state.is(ModBlocks.SIFTSLATE) || state.is(ModBlocks.SIFTSLATE_GROWTH) || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH) || state.is(ModBlocks.HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK);
   }

   private static int distanceSquared(BlockPos first, BlockPos second) {
      int dx = first.getX() - second.getX();
      int dy = first.getY() - second.getY();
      int dz = first.getZ() - second.getZ();
      return dx * dx + dy * dy + dz * dz;
   }

   private static double square(double value) {
      return value * value;
   }

   static {
      HORIZONTAL = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
      NEST_PLANTS = new Block[]{ModBlocks.OVERGROWN_FRONDS, ModBlocks.OVERGROWN_CHARD, ModBlocks.OVERGROWN_STALKS, ModBlocks.SIFTSLATE_STALKS, ModBlocks.HEALTHY_SCULK_SPROUTS, ModBlocks.DRY_HEALTHY_SCULK_SPROUTS};
   }

   private static record IchorBasin(Set fluid, Set lining, Set clearance) {
   }

   private static record CaveEntrance(BlockPos pos) {
   }

   private static record DirectionScore(Direction direction, int score) {
   }

   private static record IchorPoolPlan(BlockPos chamberCenter, Direction outward, int variant, double chamberOutwardRadius, double chamberVerticalRadius, double chamberCrossRadius) {
   }
}
