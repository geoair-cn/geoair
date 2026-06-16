
package cn.geoair.map.tile.forge.core.bygwc.compact;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Cache that stores data from .bundlx files.
 *
 * <p>Zoom, row, and column of the tile are used as key. Entries contain the path to the .bundle
 * file, the size of the tile and the offset of the image data inside the .bundle file.
 *
 * @author Bjoern Saxe
 */
public class BundlxCache {
    /**
     * Cache key representing a specific tile location.
     * Uses zoom level, row, and column to uniquely identify a tile.
     */
    public static class CacheKey {
        /** Zoom level of the tile. */
        public final int zoom;

        /** Row index of the tile. */
        public final int row;

        /** Column index of the tile. */
        public final int col;

        /**
         * Constructs a new CacheKey with the specified zoom, row, and column.
         *
         * @param zoom the zoom level
         * @param row the row index
         * @param col the column index
         */
        public CacheKey(int zoom, int row, int col) {
            this.zoom = zoom;
            this.row = row;
            this.col = col;
        }

        /**
         * Compares this CacheKey with another object for equality.
         *
         * @param o the object to compare with
         * @return true if the objects are equal, false otherwise
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (zoom != cacheKey.zoom) return false;
            if (row != cacheKey.row) return false;
            return col == cacheKey.col;
        }

        /**
         * Returns the hash code value for this CacheKey.
         *
         * @return the hash code value
         */
        @Override
        public int hashCode() {
            int result = zoom;
            result = 31 * result + row;
            result = 31 * result + col;
            return result;
        }
    }

    /**
     * Cache entry containing information about a tile's location in a bundle file.
     */
    public static class CacheEntry {
        /**
         * Constructs a new CacheEntry with the specified bundle file path, offset, and size.
         *
         * @param pathToBundleFile the path to the bundle file
         * @param offset the offset within the bundle file
         * @param size the size of the tile data
         */
        public CacheEntry(String pathToBundleFile, long offset, int size) {
            this.pathToBundleFile = pathToBundleFile;
            this.offset = offset;
            this.size = size;
        }

        /** Path to the .bundle file containing the tile data. */
        public String pathToBundleFile;

        /** Offset of the tile data within the bundle file. */
        public long offset;

        /** Size of the tile data in bytes. */
        public int size;
    }

    /** The underlying cache storing tile metadata. */
    private Cache<CacheKey, CacheEntry> indexCache;

    /**
     * Constructs a new BundlxCache with the specified maximum size.
     *
     * @param maxSize Maximum size of cache. If the size of the cache equals maxSize, adding a new
     *     entry will remove the least recently used entry from the cache.
     */
    public BundlxCache(int maxSize) {
        indexCache = CacheBuilder.newBuilder().maximumSize(maxSize).build();
    }

    /**
     * Get the entry for a key from the cache.
     *
     * @param key Key.
     * @return Returns the entry. Returns null if the key has a null value or if the key has no
     *     entry.
     */
    public synchronized CacheEntry get(CacheKey key) {
        return indexCache.getIfPresent(key);
    }

    /**
     * Puts a key-entry mapping into this cache.
     *
     * @param key the key to add.
     * @param entry the entry to add.
     */
    public synchronized void put(CacheKey key, CacheEntry entry) {
        indexCache.put(key, entry);
    }
}
