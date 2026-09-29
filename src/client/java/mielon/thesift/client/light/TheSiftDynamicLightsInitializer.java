package mielon.thesift.client.light;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import mielon.thesift.entity.EchoGolemEntity;
import mielon.thesift.entity.SingerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class TheSiftDynamicLightsInitializer implements DynamicLightsInitializer {
   public void onInitializeDynamicLights(DynamicLightsContext context) {
      DynamicLightBehaviorManager manager = context.dynamicLightBehaviorManager();
      SingerHeadLightBridge.install(new SingerLightHandler(manager));
      EchoGolemLightBridge.install(new EchoLightHandler(manager));
   }

   private static final class EchoLightHandler implements EchoGolemLightBridge.Handler {
      private final DynamicLightBehaviorManager manager;
      private final Map sources = new WeakHashMap();

      private EchoLightHandler(DynamicLightBehaviorManager manager) {
         this.manager = manager;
      }

      public void update(EchoGolemEntity golem, Vec3 position) {
         EchoLightBehavior source = (EchoLightBehavior)this.sources.get(golem);
         if (!golem.hasSoulBlock()) {
            if (source != null) {
               this.manager.remove(source);
               this.sources.remove(golem);
            }

         } else {
            if (source != null && !source.isRemoved()) {
               source.setPosition(position);
            } else {
               source = new EchoLightBehavior(golem, position);
               this.sources.put(golem, source);
               this.manager.add(source);
            }

         }
      }
   }

   private static final class EchoLightBehavior implements DynamicLightBehavior {
      private static final double LUMINANCE = (double)14.0F;
      private final WeakReference golem;
      private double x;
      private double y;
      private double z;
      private double previousX = Double.NaN;
      private double previousY = Double.NaN;
      private double previousZ = Double.NaN;

      private EchoLightBehavior(EchoGolemEntity golem, Vec3 position) {
         this.golem = new WeakReference(golem);
         this.setPosition(position);
      }

      private void setPosition(Vec3 position) {
         this.x = position.x;
         this.y = position.y;
         this.z = position.z;
      }

      public double lightAtPos(BlockPos pos, double falloffRatio) {
         EchoGolemEntity entity = (EchoGolemEntity)this.golem.get();
         if (entity != null && entity.hasSoulBlock()) {
            double dx = (double)pos.getX() + (double)0.5F - this.x;
            double dy = (double)pos.getY() + (double)0.5F - this.y;
            double dz = (double)pos.getZ() + (double)0.5F - this.z;
            return Math.max((double)14.0F - Math.sqrt(dx * dx + dy * dy + dz * dz) * falloffRatio, (double)0.0F);
         } else {
            return (double)0.0F;
         }
      }

      public DynamicLightBehavior.BoundingBox getBoundingBox() {
         int blockX = Mth.floor(this.x);
         int blockY = Mth.floor(this.y);
         int blockZ = Mth.floor(this.z);
         return new DynamicLightBehavior.BoundingBox(blockX, blockY, blockZ, blockX + 1, blockY + 1, blockZ + 1);
      }

      public boolean hasChanged() {
         if (Double.compare(this.x, this.previousX) == 0 && Double.compare(this.y, this.previousY) == 0 && Double.compare(this.z, this.previousZ) == 0) {
            return false;
         } else {
            this.previousX = this.x;
            this.previousY = this.y;
            this.previousZ = this.z;
            return true;
         }
      }

      public boolean isRemoved() {
         EchoGolemEntity entity = (EchoGolemEntity)this.golem.get();
         return entity == null || entity.isRemoved() || !entity.hasSoulBlock();
      }
   }

   private static final class SingerLightHandler implements SingerHeadLightBridge.Handler {
      private final DynamicLightBehaviorManager manager;
      private final Map sources = new WeakHashMap();

      private SingerLightHandler(DynamicLightBehaviorManager manager) {
         this.manager = manager;
      }

      public void update(SingerEntity singer, Vec3 position) {
         SingerHeadLightBehavior source = (SingerHeadLightBehavior)this.sources.get(singer);
         if (source == null) {
            source = new SingerHeadLightBehavior(singer, position);
            this.sources.put(singer, source);
            this.manager.add(source);
         } else {
            source.setPosition(position);
         }

      }
   }

   private static final class SingerHeadLightBehavior implements DynamicLightBehavior {
      private static final double LUMINANCE = (double)15.0F;
      private final WeakReference singer;
      private double x;
      private double y;
      private double z;
      private double previousX = Double.NaN;
      private double previousY = Double.NaN;
      private double previousZ = Double.NaN;

      private SingerHeadLightBehavior(SingerEntity singer, Vec3 position) {
         this.singer = new WeakReference(singer);
         this.setPosition(position);
      }

      private void setPosition(Vec3 position) {
         this.x = position.x;
         this.y = position.y;
         this.z = position.z;
      }

      public double lightAtPos(BlockPos pos, double falloffRatio) {
         double dx = (double)pos.getX() + (double)0.5F - this.x;
         double dy = (double)pos.getY() + (double)0.5F - this.y;
         double dz = (double)pos.getZ() + (double)0.5F - this.z;
         double distanceSquared = dx * dx + dy * dy + dz * dz;
         return Math.max((double)15.0F - Math.sqrt(distanceSquared) * falloffRatio, (double)0.0F);
      }

      public DynamicLightBehavior.BoundingBox getBoundingBox() {
         int blockX = Mth.floor(this.x);
         int blockY = Mth.floor(this.y);
         int blockZ = Mth.floor(this.z);
         return new DynamicLightBehavior.BoundingBox(blockX, blockY, blockZ, blockX + 1, blockY + 1, blockZ + 1);
      }

      public boolean hasChanged() {
         if (Double.compare(this.x, this.previousX) == 0 && Double.compare(this.y, this.previousY) == 0 && Double.compare(this.z, this.previousZ) == 0) {
            return false;
         } else {
            this.previousX = this.x;
            this.previousY = this.y;
            this.previousZ = this.z;
            return true;
         }
      }

      public boolean isRemoved() {
         SingerEntity entity = (SingerEntity)this.singer.get();
         return entity == null || entity.isRemoved();
      }
   }
}
