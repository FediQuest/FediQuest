/**
 * AR Manager - Handles WebXR AR functionality using model-viewer
 */
export class ARManager {
    constructor() {
        this.isSupported = false;
        this.isARActive = false;
        this.modelViewer = null;
    }

    /**
     * Initialize AR capabilities
     * @returns {Promise<boolean>} Whether AR is available
     */
    async initialize() {
        try {
            // Check for WebXR support
            if ('xr' in navigator) {
                const isARSupported = await navigator.xr.isSessionSupported('immersive-ar');
                this.isSupported = isARSupported;
                
                if (!isARSupported) {
                    console.log('WebXR AR not supported, falling back to 3D viewer');
                }
            } else {
                console.log('WebXR not supported, using 3D viewer mode');
                this.isSupported = true; // Still allow 3D viewing
            }

            // Get model-viewer element
            this.modelViewer = document.getElementById('ar-model');
            
            if (!this.modelViewer) {
                throw new Error('model-viewer element not found');
            }

            // Set up event listeners
            this.setupEventListeners();

            return this.isSupported;
        } catch (error) {
            console.error('Failed to initialize AR:', error);
            throw error;
        }
    }

    /**
     * Set up model-viewer event listeners
     */
    setupEventListeners() {
        if (!this.modelViewer) return;

        this.modelViewer.addEventListener('load', (event) => {
            console.log('Model loaded successfully');
        });

        this.modelViewer.addEventListener('error', (event) => {
            console.error('Error loading model:', event);
        });

        this.modelViewer.addEventListener('ar-status', (event) => {
            console.log('AR status:', event.detail.status);
            this.isARActive = event.detail.status === 'session-started';
        });
    }

    /**
     * Load a 3D model for AR display
     * @param {string} modelUrl - URL to the glTF/GLB model
     */
    loadModel(modelUrl) {
        if (!this.modelViewer) {
            throw new Error('AR not initialized');
        }

        this.modelViewer.src = modelUrl;
        this.modelViewer.setAttribute('alt', 'Quest object');
    }

    /**
     * Place an AR object at a specific location
     * @param {number} latitude - GPS latitude
     * @param {number} longitude - GPS longitude
     * @param {string} modelUrl - Model to display
     */
    placeAtLocation(latitude, longitude, modelUrl) {
        console.log(`Placing AR object at ${latitude}, ${longitude}`);
        this.loadModel(modelUrl);
        
        // Store location data for later use
        this.currentLocation = { latitude, longitude };
    }

    /**
     * Start AR session
     * @returns {Promise<void>}
     */
    async startAR() {
        if (!this.isSupported) {
            throw new Error('AR not supported on this device');
        }

        if (this.modelViewer && this.modelViewer.canActivateAR) {
            this.modelViewer.activateAR();
        } else {
            console.log('AR activation not available, using 3D mode');
        }
    }

    /**
     * Stop AR session
     */
    stopAR() {
        if (this.modelViewer) {
            // Hide AR view
            this.modelViewer.hidden = true;
        }
        this.isARActive = false;
    }

    /**
     * Take a screenshot of the current AR/3D view
     * @returns {Promise<Blob>} Screenshot image
     */
    async takeScreenshot() {
        if (!this.modelViewer) {
            throw new Error('AR not initialized');
        }

        // Use model-viewer's screenshot capability
        return await this.modelViewer.toBlob({ mimeType: 'image/png' });
    }

    /**
     * Check if device supports hit testing for surface detection
     * @returns {boolean}
     */
    hasHitTestSupport() {
        return 'xr' in navigator && navigator.xr.hitTest !== undefined;
    }

    /**
     * Get current AR status
     * @returns {{isSupported: boolean, isActive: boolean}}
     */
    getStatus() {
        return {
            isSupported: this.isSupported,
            isActive: this.isARActive
        };
    }
}
