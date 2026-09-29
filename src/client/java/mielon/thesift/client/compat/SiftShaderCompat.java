package mielon.thesift.client.compat;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.lang.reflect.Method;
import mielon.thesift.TheSiftMod;
import net.fabricmc.loader.api.FabricLoader;

public final class SiftShaderCompat {
   private static boolean initialized;
   private static boolean irisPresent;
   private static Object irisApi;
   private static Method isShaderPackInUse;
   private static Method assignPipeline;
   private static Class irisProgramClass;

   private SiftShaderCompat() {
   }

   public static void initialize() {
      if (!initialized) {
         initialized = true;
         irisPresent = FabricLoader.getInstance().isModLoaded("iris");
         if (irisPresent) {
            try {
               Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
               irisApi = apiClass.getMethod("getInstance").invoke(null);
               isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");

               try {
                  irisProgramClass = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
                  assignPipeline = apiClass.getMethod("assignPipeline", RenderPipeline.class, irisProgramClass);
               } catch (ReflectiveOperationException var2) {
                  assignPipeline = null;
                  irisProgramClass = null;
               }

               TheSiftMod.LOGGER.info("Iris detected; The Sift shader compatibility is enabled.");
            } catch (LinkageError | ReflectiveOperationException exception) {
               irisApi = null;
               isShaderPackInUse = null;
               assignPipeline = null;
               irisProgramClass = null;
               TheSiftMod.LOGGER.warn("Iris was detected, but its compatibility API could not be initialized.", exception);
            }

         }
      }
   }

   public static boolean isIrisPresent() {
      initialize();
      return irisPresent;
   }

   public static boolean isShaderPackInUse() {
      initialize();
      if (irisApi != null && isShaderPackInUse != null) {
         try {
            return Boolean.TRUE.equals(isShaderPackInUse.invoke(irisApi));
         } catch (RuntimeException | ReflectiveOperationException var1) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void assignPipeline(RenderPipeline pipeline, String irisProgram) {
      initialize();
      if (irisApi != null && assignPipeline != null && irisProgramClass != null) {
         try {
            Object program = enumValue(irisProgramClass, irisProgram);
            assignPipeline.invoke(irisApi, pipeline, program);
         } catch (IllegalArgumentException | ReflectiveOperationException exception) {
            TheSiftMod.LOGGER.warn("Could not assign The Sift pipeline {} to Iris program {}.", new Object[]{pipeline.getLocation(), irisProgram, exception});
         }

      }
   }

   private static Object enumValue(Class enumClass, String name) {
      return Enum.valueOf(enumClass.asSubclass(Enum.class), name);
   }
}
