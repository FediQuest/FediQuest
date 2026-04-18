/**
 * FediQuest Spawn Loader
 * 
 * Handles fetching spawn data from server.json with:
 * - ETag/If-None-Match caching
 * - Retry with exponential backoff
 * - GPS permission handling
 * - Cache management utilities
 */

class SpawnLoader {
    constructor(options = {}) {
        this.serverUrl = options.serverUrl || '../server/server.json';
        this.maxRetries = options.maxRetries || 3;
        this.baseDelay = options.baseDelay || 1000; // 1 second
        this.cacheKey = 'fediquest_spawns';
        this.etagKey = 'fediquest_etag';
        this.timestampKey = 'fediquest_timestamp';
        
        this.listeners = {
            load: [],
            error: [],
            cache: []
        };
    }
    
    /**
     * Fetch spawn data with ETag caching and retry logic
     */
    async fetchSpawns() {
        let lastError = null;
        
        for (let attempt = 0; attempt <= this.maxRetries; attempt++) {
            try {
                const cachedEtag = localStorage.getItem(this.etagKey);
                const headers = {};
                
                if (cachedEtag) {
                    headers['If-None-Match'] = cachedEtag;
                }
                
                const response = await fetch(this.serverUrl, {
                    method: 'GET',
                    headers: headers,
                    cache: 'no-cache'
                });
                
                // Handle 304 Not Modified
                if (response.status === 304) {
                    console.log('[SpawnLoader] Data not modified, using cached version');
                    this.notify('cache', { source: 'etag' });
                    return this.getCachedData();
                }
                
                // Handle successful response
                if (response.ok) {
                    const data = await response.json();
                    
                    // Store ETag for future requests
                    const etag = response.headers.get('etag');
                    if (etag) {
                        localStorage.setItem(this.etagKey, etag);
                    }
                    
                    // Cache the data
                    this.cacheData(data);
                    
                    console.log(`[SpawnLoader] Loaded ${data.spawns?.length || 0} spawns`);
                    this.notify('load', data);
                    
                    return data;
                }
                
                // Handle HTTP errors
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                
            } catch (error) {
                lastError = error;
                console.warn(`[SpawnLoader] Attempt ${attempt + 1} failed:`, error.message);
                
                if (attempt < this.maxRetries) {
                    const delay = this.calculateBackoff(attempt);
                    console.log(`[SpawnLoader] Retrying in ${delay}ms...`);
                    await this.sleep(delay);
                }
            }
        }
        
        // All retries failed, try to use cached data
        console.error('[SpawnLoader] All attempts failed, trying cached data');
        const cached = this.getCachedData();
        if (cached) {
            this.notify('cache', { source: 'fallback' });
            return cached;
        }
        
        this.notify('error', lastError);
        throw lastError;
    }
    
    /**
     * Calculate exponential backoff delay
     */
    calculateBackoff(attempt) {
        const exponentialDelay = this.baseDelay * Math.pow(2, attempt);
        const jitter = Math.random() * 0.3 * exponentialDelay;
        return exponentialDelay + jitter;
    }
    
    /**
     * Sleep helper
     */
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
    
    /**
     * Cache spawn data with timestamp
     */
    cacheData(data) {
        try {
            const cacheEntry = {
                data: data,
                timestamp: Date.now()
            };
            localStorage.setItem(this.cacheKey, JSON.stringify(cacheEntry));
            localStorage.setItem(this.timestampKey, Date.now().toString());
        } catch (error) {
            console.warn('[SpawnLoader] Failed to cache data:', error);
        }
    }
    
    /**
     * Get cached data
     */
    getCachedData() {
        try {
            const cached = localStorage.getItem(this.cacheKey);
            if (cached) {
                const entry = JSON.parse(cached);
                return entry.data;
            }
        } catch (error) {
            console.warn('[SpawnLoader] Failed to read cache:', error);
        }
        return null;
    }
    
    /**
     * Clear cached data
     */
    clearCache() {
        localStorage.removeItem(this.cacheKey);
        localStorage.removeItem(this.etagKey);
        localStorage.removeItem(this.timestampKey);
        console.log('[SpawnLoader] Cache cleared');
        this.notify('cache', { source: 'cleared' });
    }
    
    /**
     * Get cache info
     */
    getCacheInfo() {
        const timestamp = localStorage.getItem(this.timestampKey);
        const etag = localStorage.getItem(this.etagKey);
        const cached = this.getCachedData();
        
        return {
            hasCache: !!cached,
            timestamp: timestamp ? new Date(parseInt(timestamp)) : null,
            etag: etag || null,
            spawnCount: cached?.spawns?.length || 0
        };
    }
    
    /**
     * Check if cache is stale (older than maxAge ms)
     */
    isCacheStale(maxAge = 300000) { // Default 5 minutes
        const timestamp = localStorage.getItem(this.timestampKey);
        if (!timestamp) return true;
        
        const age = Date.now() - parseInt(timestamp);
        return age > maxAge;
    }
    
    /**
     * Event listener management
     */
    on(event, callback) {
        if (this.listeners[event]) {
            this.listeners[event].push(callback);
        }
        return () => this.off(event, callback);
    }
    
    off(event, callback) {
        if (this.listeners[event]) {
            this.listeners[event] = this.listeners[event].filter(cb => cb !== callback);
        }
    }
    
    notify(event, data) {
        if (this.listeners[event]) {
            this.listeners[event].forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error('[SpawnLoader] Listener error:', error);
                }
            });
        }
    }
}

/**
 * GPS Permission Handler
 */
class GPSHandler {
    constructor(options = {}) {
        this.enableHighAccuracy = options.enableHighAccuracy ?? true;
        this.timeout = options.timeout ?? 30000;
        this.maximumAge = options.maximumAge ?? 10000;
        
        this.currentPosition = null;
        this.watchId = null;
        this.listeners = {
            update: [],
            error: [],
            permission: []
        };
    }
    
    /**
     * Request GPS permission and get current position
     */
    async requestPermission() {
        if (!navigator.geolocation) {
            throw new Error('Geolocation not supported by this browser');
        }
        
        // Note: Permission is requested implicitly by getCurrentPosition
        // There's no explicit permission API for geolocation yet
        
        return new Promise((resolve, reject) => {
            navigator.geolocation.getCurrentPosition(
                (position) => {
                    this.currentPosition = {
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude,
                        accuracy: position.coords.accuracy,
                        timestamp: position.timestamp
                    };
                    this.notify('permission', { granted: true });
                    resolve(this.currentPosition);
                },
                (error) => {
                    this.notify('permission', { 
                        granted: false, 
                        error: this.getPermissionError(error) 
                    });
                    reject(new Error(this.getPermissionError(error)));
                },
                {
                    enableHighAccuracy: this.enableHighAccuracy,
                    timeout: this.timeout,
                    maximumAge: this.maximumAge
                }
            );
        });
    }
    
    /**
     * Start watching position
     */
    startWatching(callback) {
        if (!navigator.geolocation) {
            throw new Error('Geolocation not supported');
        }
        
        if (this.watchId !== null) {
            this.stopWatching();
        }
        
        this.watchId = navigator.geolocation.watchPosition(
            (position) => {
                this.currentPosition = {
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                    accuracy: position.coords.accuracy,
                    timestamp: position.timestamp
                };
                this.notify('update', this.currentPosition);
                if (callback) callback(this.currentPosition);
            },
            (error) => {
                this.notify('error', error);
            },
            {
                enableHighAccuracy: this.enableHighAccuracy,
                timeout: this.timeout,
                maximumAge: this.maximumAge
            }
        );
        
        return this.watchId;
    }
    
    /**
     * Stop watching position
     */
    stopWatching() {
        if (this.watchId !== null) {
            navigator.geolocation.clearWatch(this.watchId);
            this.watchId = null;
        }
    }
    
    /**
     * Get human-readable permission error
     */
    getPermissionError(error) {
        switch (error.code) {
            case error.PERMISSION_DENIED:
                return 'GPS permission denied by user';
            case error.POSITION_UNAVAILABLE:
                return 'GPS position unavailable';
            case error.TIMEOUT:
                return 'GPS request timed out';
            default:
                return 'Unknown GPS error';
        }
    }
    
    /**
     * Calculate distance between two points (Haversine formula)
     */
    calculateDistance(lat1, lon1, lat2, lon2) {
        const R = 6371e3; // Earth's radius in meters
        const φ1 = lat1 * Math.PI / 180;
        const φ2 = lat2 * Math.PI / 180;
        const Δφ = (lat2 - lat1) * Math.PI / 180;
        const Δλ = (lon2 - lon1) * Math.PI / 180;
        
        const a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
                  Math.cos(φ1) * Math.cos(φ2) *
                  Math.sin(Δλ/2) * Math.sin(Δλ/2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return R * c;
    }
    
    /**
     * Event listener management
     */
    on(event, callback) {
        if (this.listeners[event]) {
            this.listeners[event].push(callback);
        }
        return () => this.off(event, callback);
    }
    
    off(event, callback) {
        if (this.listeners[event]) {
            this.listeners[event] = this.listeners[event].filter(cb => cb !== callback);
        }
    }
    
    notify(event, data) {
        if (this.listeners[event]) {
            this.listeners[event].forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error('[GPSHandler] Listener error:', error);
                }
            });
        }
    }
}

/**
 * Cache Clear Helper
 * Clears all FediQuest-related caches
 */
function clearAllCaches() {
    const keysToRemove = [
        'fediquest_spawns',
        'fediquest_etag',
        'fediquest_timestamp',
        'serverEtag'
    ];
    
    keysToRemove.forEach(key => {
        localStorage.removeItem(key);
    });
    
    if ('caches' in window) {
        caches.keys().then(names => {
            names.forEach(name => {
                if (name.startsWith('fediquest')) {
                    caches.delete(name);
                }
            });
        });
    }
    
    console.log('[CacheHelper] All FediQuest caches cleared');
}

// Export for module usage or attach to window for direct script inclusion
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { SpawnLoader, GPSHandler, clearAllCaches };
} else {
    window.SpawnLoader = SpawnLoader;
    window.GPSHandler = GPSHandler;
    window.clearAllCaches = clearAllCaches;
}
