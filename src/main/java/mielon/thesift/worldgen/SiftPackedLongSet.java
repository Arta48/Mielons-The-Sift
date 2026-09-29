package mielon.thesift.worldgen;

import java.util.function.LongConsumer;

final class SiftPackedLongSet {
   private static final float LOAD_FACTOR = 0.65F;
   private long[] keys;
   private int mask;
   private int maxFill;
   private int size;
   private boolean hasZero;

   SiftPackedLongSet(int expectedSize) {
      int capacity = 2;

      for (int needed = Math.max(2, (int)Math.ceil((double)((float)expectedSize / 0.65F))); capacity < needed; capacity <<= 1) {
      }

      this.keys = new long[capacity];
      this.mask = capacity - 1;
      this.maxFill = Math.min(capacity - 1, (int)((float)capacity * 0.65F));
   }

   boolean add(long key) {
      if (key == 0L) {
         if (this.hasZero) {
            return false;
         } else {
            this.hasZero = true;
            if (++this.size >= this.maxFill) {
               this.rehash(this.keys.length << 1);
            }

            return true;
         }
      } else {
         int position;
         for (position = this.position(key); this.keys[position] != 0L; position = position + 1 & this.mask) {
            if (this.keys[position] == key) {
               return false;
            }
         }

         this.keys[position] = key;
         if (++this.size >= this.maxFill) {
            this.rehash(this.keys.length << 1);
         }

         return true;
      }
   }

   boolean remove(long key) {
      if (key == 0L) {
         if (!this.hasZero) {
            return false;
         } else {
            this.hasZero = false;
            --this.size;
            return true;
         }
      } else {
         long current;
         for (int position = this.position(key); (current = this.keys[position]) != 0L; position = position + 1 & this.mask) {
            if (current == key) {
               --this.size;
               this.shiftKeys(position);
               return true;
            }
         }

         return false;
      }
   }

   boolean contains(long key) {
      if (key == 0L) {
         return this.hasZero;
      } else {
         long current;
         for (int position = this.position(key); (current = this.keys[position]) != 0L; position = position + 1 & this.mask) {
            if (current == key) {
               return true;
            }
         }

         return false;
      }
   }

   void forEach(LongConsumer consumer) {
      if (this.hasZero) {
         consumer.accept(0L);
      }

      for (long key : this.keys) {
         if (key != 0L) {
            consumer.accept(key);
         }
      }

   }

   private void shiftKeys(int position) {
      long[] table = this.keys;

      while(true) {
         int last = position;

         long current;
         for (position = position + 1 & this.mask; (current = table[position]) != 0L; position = position + 1 & this.mask) {
            int slot = this.position(current);
            if (last <= position) {
               if (last >= slot || slot > position) {
                  break;
               }
            } else if (last >= slot && slot > position) {
               break;
            }
         }

         if (current == 0L) {
            table[last] = 0L;
            return;
         }

         table[last] = current;
      }
   }

   private int position(long key) {
      long mixed = key ^ key >>> 33;
      mixed *= -49064778989728563L;
      mixed ^= mixed >>> 33;
      mixed *= -4265267296055464877L;
      mixed ^= mixed >>> 33;
      return (int)mixed & this.mask;
   }

   private void rehash(int newCapacity) {
      long[] oldKeys = this.keys;
      this.keys = new long[newCapacity];
      this.mask = newCapacity - 1;
      this.maxFill = Math.min(newCapacity - 1, (int)((float)newCapacity * 0.65F));

      for (long key : oldKeys) {
         if (key != 0L) {
            int position;
            for (position = this.position(key); this.keys[position] != 0L; position = position + 1 & this.mask) {
            }

            this.keys[position] = key;
         }
      }

   }
}
