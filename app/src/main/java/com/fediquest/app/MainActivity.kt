// File: app/src/main/java/com/fediquest/app/MainActivity.kt
package com.fediquest.app

import android.os.Bundle
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
 * Hosts the navigation graph and bottom navigation bar.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
