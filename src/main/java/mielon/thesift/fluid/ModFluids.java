package mielon.thesift.fluid;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.FlowingFluid;

public final class ModFluids {
   public static final ResourceKey ICHOR_KEY;
   public static final ResourceKey FLOWING_ICHOR_KEY;
   public static final FlowingFluid ICHOR;
   public static final FlowingFluid FLOWING_ICHOR;
   public static final ResourceKey ICHOR_BLOCK_KEY;
   public static final LiquidBlock ICHOR_BLOCK;
   public static final ResourceKey ICHOR_BUCKET_KEY;
   public static final Item ICHOR_BUCKET;

   private ModFluids() {
   }

   public static void initialize() {
      IchorState.source = ICHOR.getSource(false);
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }

   static {
      ICHOR_KEY = ResourceKey.create(BuiltInRegistries.FLUID.key(), id("ichor"));
      FLOWING_ICHOR_KEY = ResourceKey.create(BuiltInRegistries.FLUID.key(), id("flowing_ichor"));
      ICHOR = (FlowingFluid)Registry.register(BuiltInRegistries.FLUID, ICHOR_KEY, new IchorFluid.Source());
      FLOWING_ICHOR = (FlowingFluid)Registry.register(BuiltInRegistries.FLUID, FLOWING_ICHOR_KEY, new IchorFluid.Flowing());
      ICHOR_BLOCK_KEY = ResourceKey.create(BuiltInRegistries.BLOCK.key(), id("ichor"));
      ICHOR_BLOCK = (LiquidBlock)Registry.register(BuiltInRegistries.BLOCK, ICHOR_BLOCK_KEY, new LiquidBlock(FLOWING_ICHOR, Properties.ofFullCopy(Blocks.WATER).setId(ICHOR_BLOCK_KEY).lightLevel((state) -> 12).noLootTable()));
      ICHOR_BUCKET_KEY = ResourceKey.create(BuiltInRegistries.ITEM.key(), id("ichor_bucket"));
      ICHOR_BUCKET = (Item)Registry.register(BuiltInRegistries.ITEM, ICHOR_BUCKET_KEY, new IchorBucketItem(ICHOR, (new Item.Properties()).setId(ICHOR_BUCKET_KEY).stacksTo(1).craftRemainder(Items.BUCKET)));
   }
}
