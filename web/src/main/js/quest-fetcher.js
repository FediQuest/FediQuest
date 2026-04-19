/**
 * Quest Fetcher - Handles ETag-based caching for spawn data
 * Implements If-None-Match header support for bandwidth optimization
 */
export class QuestFetcher {
    constructor() {
        this.etagCache = new Map();
        this.dataCache = new Map();
    }

    /**
     * Fetch spawn data with ETag support
     * @param {string} url - URL to fetch spawn data from
     * @returns {Promise<Array>} Array of spawn objects
     */
    async fetchSpawns(url) {
        try {
            const headers = new Headers();
            
            // Add If-None-Match header if we have a cached ETag
            const cachedEtag = this.etagCache.get(url);
            if (cachedEtag) {
                headers.append('If-None-Match', cachedEtag);
            }

            const response = await fetch(url, { headers });

            // Handle 304 Not Modified
            if (response.status === 304) {
                console.log('Spawn data unchanged, using cached version');
                return this.dataCache.get(url) || [];
            }

            // Parse new data
            const data = await response.json();
            
            // Store ETag for future requests
            const newEtag = response.headers.get('ETag');
            if (newEtag) {
                this.etagCache.set(url, newEtag);
            }

            // Cache the spawn data
            const spawns = data.spawns || [];
            this.dataCache.set(url, spawns);

            return spawns;
        } catch (error) {
            console.error('Failed to fetch spawns:', error);
            
            // Return cached data on error if available
            const cachedData = this.dataCache.get(url);
            if (cachedData) {
                console.log('Returning cached data due to network error');
                return cachedData;
            }
            
            throw error;
        }
    }

    /**
     * Clear cache for a specific URL
     * @param {string} url - URL to clear cache for
     */
    clearCache(url) {
        this.etagCache.delete(url);
        this.dataCache.delete(url);
    }

    /**
     * Clear all caches
     */
    clearAllCaches() {
        this.etagCache.clear();
        this.dataCache.clear();
    }
}
