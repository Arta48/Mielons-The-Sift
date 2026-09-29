package mielon.thesift.portal;

record PortalSortKey(double layer, int vertical, int horizontal, int depth) implements Comparable<PortalSortKey> {
   static PortalSortKey fromEdge(int horizontal, int vertical, int depth, int minHorizontal, int maxHorizontal, int minVertical, int maxVertical) {
      int distanceToEdge = Math.min(Math.min(horizontal - minHorizontal, maxHorizontal - horizontal), Math.min(vertical - minVertical, maxVertical - vertical));
      return new PortalSortKey((double)distanceToEdge, vertical, horizontal, depth);
   }

   static PortalSortKey fromCenter(int horizontal, int vertical, int depth, double centerHorizontal, double centerVertical) {
      double distanceFromCenter = Math.hypot((double)horizontal - centerHorizontal, (double)vertical - centerVertical);
      return new PortalSortKey(distanceFromCenter, vertical, horizontal, depth);
   }

   public int compareTo(PortalSortKey other) {
      int result = Double.compare(this.layer, other.layer);
      if (result != 0) {
         return result;
      } else {
         result = Integer.compare(this.vertical, other.vertical);
         if (result != 0) {
            return result;
         } else {
            result = Integer.compare(this.horizontal, other.horizontal);
            return result != 0 ? result : Integer.compare(this.depth, other.depth);
         }
      }
   }
}
