package mielon.thesift.world;

import java.util.Optional;
import java.util.UUID;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class SiftWorldStorage extends SavedData {
   private static final String DIMENSION_STATE = "dimension_state";
   private static final String RETURN_POINTS = "return_points";
   private static final String PORTAL_GUARDS = "portal_guards";
   private static final String RIFT_ROUTES = "rift_routes";
   private static final SavedDataType TYPE;
   private final CompoundTag root;

   private SiftWorldStorage() {
      this(new CompoundTag());
   }

   private SiftWorldStorage(CompoundTag root) {
      this.root = root.copy();
   }

   private CompoundTag saveTag() {
      return this.root.copy();
   }

   private static SiftWorldStorage data(MinecraftServer server) {
      return (SiftWorldStorage)server.getDataStorage().computeIfAbsent(TYPE);
   }

   public static Optional getPortalAnchor(MinecraftServer server) {
      SiftWorldStorage data = data(server);
      CompoundTag tag = data.getOrMigrateDimensionState(server);
      return !tag.getBooleanOr("portal_generated", false) ? Optional.empty() : Optional.of(new BlockPos(tag.getIntOr("portal_x", 0), tag.getIntOr("portal_y", 64), tag.getIntOr("portal_z", 0)));
   }

   public static void setPortalAnchor(MinecraftServer server, BlockPos anchor) {
      CompoundTag tag = new CompoundTag();
      tag.putBoolean("portal_generated", true);
      tag.putInt("portal_x", anchor.getX());
      tag.putInt("portal_y", anchor.getY());
      tag.putInt("portal_z", anchor.getZ());
      SiftWorldStorage data = data(server);
      data.root.put("dimension_state", tag);
      data.setDirty();
   }

   public static void saveReturnPoint(Entity entity) {
      MinecraftServer server = entity.level().getServer();
      if (server != null) {
         CompoundTag tag = new CompoundTag();
         tag.putBoolean("valid", true);
         tag.putDouble("x", entity.getX());
         tag.putDouble("y", entity.getY());
         tag.putDouble("z", entity.getZ());
         tag.putFloat("yaw", entity.getYRot());
         tag.putFloat("pitch", entity.getXRot());
         BlockPos portal = BlockPos.containing(entity.position());
         BlockPos min = BlockPos.containing(entity.getBoundingBox().minX, entity.getBoundingBox().minY, entity.getBoundingBox().minZ);
         BlockPos max = BlockPos.containing(entity.getBoundingBox().maxX, entity.getBoundingBox().maxY, entity.getBoundingBox().maxZ);

         for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (entity.level().getBlockState(pos).is(ModBlocks.SIFT_PORTAL)) {
               portal = pos.immutable();
               break;
            }
         }

         tag.putInt("portal_x", portal.getX());
         tag.putInt("portal_y", portal.getY());
         tag.putInt("portal_z", portal.getZ());
         data(server).putEntry("return_points", entity.getUUID(), tag);
      }
   }

   public static Optional getReturnPoint(MinecraftServer server, UUID entityId) {
      Optional<CompoundTag> optional = data(server).getOrMigrateEntry(server, "return_points", entityId, "return/" + String.valueOf(entityId));
      if (optional.isEmpty()) {
         return Optional.empty();
      } else {
         CompoundTag tag = (CompoundTag)optional.get();
         if (!tag.getBooleanOr("valid", false)) {
            return Optional.empty();
         } else {
            double x = tag.getDoubleOr("x", (double)0.5F);
            double y = tag.getDoubleOr("y", (double)80.0F);
            double z = tag.getDoubleOr("z", (double)0.5F);
            return Optional.of(new ReturnPoint(x, y, z, tag.getFloatOr("yaw", 0.0F), tag.getFloatOr("pitch", 0.0F), new BlockPos(tag.getIntOr("portal_x", (int)Math.floor(x)), tag.getIntOr("portal_y", (int)Math.floor(y)), tag.getIntOr("portal_z", (int)Math.floor(z)))));
         }
      }
   }

   public static void clearReturnPoint(MinecraftServer server, UUID entityId) {
      data(server).removeEntry("return_points", entityId);
      clearLegacyIfPresent(server, "return/" + String.valueOf(entityId));
   }

   public static void markPortalExitRequired(Entity entity) {
      MinecraftServer server = entity.level().getServer();
      if (server != null) {
         CompoundTag tag = new CompoundTag();
         tag.putBoolean("waiting_for_exit", true);
         data(server).putEntry("portal_guards", entity.getUUID(), tag);
      }
   }

   public static boolean requiresPortalExit(MinecraftServer server, UUID entityId) {
      return (Boolean)data(server).getOrMigrateEntry(server, "portal_guards", entityId, "portal_guard/" + String.valueOf(entityId)).map((tag) -> ((CompoundTag) tag).getBooleanOr("waiting_for_exit", false)).orElse(false);
   }

   public static void clearPortalExitRequired(MinecraftServer server, UUID entityId) {
      data(server).removeEntry("portal_guards", entityId);
      clearLegacyIfPresent(server, "portal_guard/" + String.valueOf(entityId));
   }

   public static void markRiftEntry(Entity entity) {
      MinecraftServer server = entity.level().getServer();
      if (server != null) {
         clearReturnPoint(server, entity.getUUID());
      }

   }

   public static void saveRiftRoute(MinecraftServer server, UUID entityId, UUID exitId, BlockPos source, long expires) {
      CompoundTag tag = new CompoundTag();
      tag.putString("exit", exitId.toString());
      tag.putInt("x", source.getX());
      tag.putInt("y", source.getY());
      tag.putInt("z", source.getZ());
      tag.putLong("expires", expires);
      data(server).putEntry("rift_routes", entityId, tag);
   }

   public static Optional getRiftRoute(MinecraftServer server, UUID entityId, UUID exitId, long now) {
      SiftWorldStorage data = data(server);
      Optional<CompoundTag> optional = data.getOrMigrateEntry(server, "rift_routes", entityId, "rift_route/" + String.valueOf(entityId));
      if (optional.isEmpty()) {
         return Optional.empty();
      } else {
         CompoundTag tag = (CompoundTag)optional.get();
         if (tag.getStringOr("exit", "").equals(exitId.toString()) && tag.getLongOr("expires", 0L) > now) {
            return Optional.of(new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 80), tag.getIntOr("z", 0)));
         } else {
            data.removeEntry("rift_routes", entityId);
            clearLegacyIfPresent(server, "rift_route/" + String.valueOf(entityId));
            return Optional.empty();
         }
      }
   }

   private CompoundTag getOrMigrateDimensionState(MinecraftServer server) {
      Optional<CompoundTag> current = this.root.getCompound("dimension_state");
      if (current.isPresent()) {
         return (CompoundTag)current.get();
      } else {
         CompoundTag legacy = server.getCommandStorage().get(legacyId("dimension_state"));
         if (!legacy.isEmpty()) {
            CompoundTag migrated = legacy.copy();
            this.root.put("dimension_state", migrated);
            this.setDirty();
            clearLegacyIfPresent(server, "dimension_state");
            return migrated;
         } else {
            return new CompoundTag();
         }
      }
   }

   private Optional getOrMigrateEntry(MinecraftServer server, String sectionName, UUID entityId, String legacyPath) {
      String key = entityId.toString();
      Optional<CompoundTag> section = this.root.getCompound(sectionName);
      if (section.isPresent()) {
         Optional<CompoundTag> current = ((CompoundTag)section.get()).getCompound(key);
         if (current.isPresent()) {
            return current;
         }
      }

      CompoundTag legacy = server.getCommandStorage().get(legacyId(legacyPath));
      if (legacy.isEmpty()) {
         return Optional.empty();
      } else {
         this.putEntry(sectionName, entityId, legacy);
         clearLegacyIfPresent(server, legacyPath);
         return Optional.of(legacy);
      }
   }

   private void putEntry(String sectionName, UUID entityId, CompoundTag value) {
      CompoundTag section = this.section(sectionName);
      section.put(entityId.toString(), value.copy());
      this.root.put(sectionName, section);
      this.setDirty();
   }

   private void removeEntry(String sectionName, UUID entityId) {
      Optional<CompoundTag> section = this.root.getCompound(sectionName);
      if (!section.isEmpty()) {
         CompoundTag entries = (CompoundTag)section.get();
         if (entries.remove(entityId.toString()) != null) {
            if (entries.isEmpty()) {
               this.root.remove(sectionName);
            } else {
               this.root.put(sectionName, entries);
            }

            this.setDirty();
         }

      }
   }

   private CompoundTag section(String name) {
      Optional<CompoundTag> current = this.root.getCompound(name);
      if (current.isPresent()) {
         return (CompoundTag)current.get();
      } else {
         CompoundTag created = new CompoundTag();
         this.root.put(name, created);
         return created;
      }
   }

   private static void clearLegacyIfPresent(MinecraftServer server, String path) {
      Identifier id = legacyId(path);
      if (!server.getCommandStorage().get(id).isEmpty()) {
         server.getCommandStorage().set(id, new CompoundTag());
      }

   }

   private static Identifier legacyId(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }

   static {
      TYPE = new SavedDataType(Identifier.fromNamespaceAndPath("the_sift", "world_state"), SiftWorldStorage::new, CompoundTag.CODEC.xmap(SiftWorldStorage::new, SiftWorldStorage::saveTag), (DataFixTypes)null);
   }

   public static record ReturnPoint(double x, double y, double z, float yaw, float pitch, BlockPos portalBlock) {
   }
}
