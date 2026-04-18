// File: app/src/main/java/com/fediquest/app/MainActivity.kt
package com.fediquest.app

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.fediquest.app.databinding.ActivityMainBinding
import com.fediquest.app.di.ServiceLocator
import com.fediquest.app.viewmodel.SharedViewModel
import com.fediquest.app.viewmodel.SharedViewModelFactory

/**
 * Main activity for FediQuest app.
 * 
 * AR Mode Selection:
 * - WEB_DEMO (default): Launches A-Frame + AR.js web demo
 * - ARTOOLKIT: Native ARToolKit integration (requires .so files)
 * - ARCORE: Native ARCore integration (optional, requires Google Play Services)
 * 
 * For the AR GPS prototype PR, the app defaults to WEB_DEMO mode.
 */
class MainActivity : AppCompatActivity() {

    enum class ARMode {
        WEB_DEMO,      // Default: Launch web view with A-Frame/AR.js
        ARTOOLKIT,     // Native ARToolKit (requires prebuilt .so files)
        ARCORE         // Native ARCore (optional alternative)
    }

    // Change this to switch between AR modes
    private val currentMode = ARMode.WEB_DEMO

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedViewModel: SharedViewModel
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check which mode to run
        when (currentMode) {
            ARMode.WEB_DEMO -> {
                setupWebDemo()
                return
            }
            ARMode.ARTOOLKIT -> {
                // Fall through to native setup
                // Note: Requires ARToolKit .so files in jniLibs/
            }
            ARMode.ARCORE -> {
                // Fall through to native setup
                // Note: Requires ARCore dependencies and Google Play Services
            }
        }
        
        // Native mode setup (ARTOOLKIT or ARCORE)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        sharedViewModel = ViewModelProvider(
            this,
            SharedViewModelFactory(ServiceLocator.userRepository)
        )[SharedViewModel::class.java]

        setupNavigation()
        observeNavigationEvents()
    }

    /**
     * Setup WebAR demo using A-Frame + AR.js
     * This is the primary demo flow for the AR GPS prototype
     */
    private fun setupWebDemo() {
        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                geolocationEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            
            // Load the web demo from assets or URL
            // For development: load from local server
            // For production: load from hosted URL
            loadUrl("file:///android_asset/web/index.html")
        }
        
        setContentView(webView)
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        
        val navigationController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navigationController)

        // Hide bottom nav on certain destinations if needed
        navigationController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_map,
                R.id.navigation_scan,
                R.id.navigation_avatar,
                R.id.navigation_companion -> {
                    binding.bottomNavigation.visibility = android.view.View.VISIBLE
                }
                R.id.questDetailFragment -> {
                    binding.bottomNavigation.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun observeNavigationEvents() {
        sharedViewModel.navigationEvents.observe(this) { event ->
            event.getContentIfNotHandled()?.let { navEvent ->
                // Handle navigation events if needed
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}
