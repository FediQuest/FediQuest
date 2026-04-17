// File: app/src/main/java/com/fediquest/app/ui/map/MapFragment.kt
package com.fediquest.app.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fediquest.app.R
import com.fediquest.app.data.models.Quest
import com.fediquest.app.databinding.FragmentMapBinding
import com.fediquest.app.di.ServiceLocator
import com.fediquest.app.viewmodel.NavigationEvent
import com.fediquest.app.viewmodel.SharedViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Fragment displaying OSMDroid map with nearby eco-quests.
 */
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var viewModel: MapViewModel
    
    private var myLocationOverlay: MyLocationNewOverlay? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", 0)
        )
        
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        sharedViewModel = ViewModelProvider(
            requireActivity(),
            SharedViewModelFactory(ServiceLocator.userRepository)
        )[SharedViewModel::class.java]
        
        viewModel = ViewModelProvider(
            this,
            MapViewModelFactory(ServiceLocator.questRepository)
        )[MapViewModel::class.java]
        
        setupMap()
        observeViewModel()
        checkLocationPermission()
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            
            // Add location overlay
            myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), this).apply {
                enableMyLocation()
                enableFollowLocation()
            }
            overlays.add(myLocationOverlay)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.quests.collect { quests ->
                displayQuests(quests)
            }
        }
        
        lifecycleScope.launch {
            viewModel.currentLocation.collect { location ->
                location?.let {
                    moveToLocation(it.latitude, it.longitude)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.swipeRefresh.isRefreshing = loading
            }
        }
    }

    private fun displayQuests(quests: List<Quest>) {
        // Clear existing markers
        binding.mapView.overlays.filterIsInstance<Marker>().forEach {
            binding.mapView.overlays.remove(it)
        }
        
        // Add quest markers
        quests.forEach { quest ->
            val marker = Marker(binding.mapView).apply {
                position = GeoPoint(quest.latitude, quest.longitude)
                title = quest.title
                subDescription = "${quest.category.icon} ${quest.difficulty.name}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                
                // Set marker icon based on category (simplified)
                setOnMarkerClickListener { _, _ ->
                    sharedViewModel.navigate(NavigationEvent.NavigateToQuestDetail(quest.id))
                    true
                }
            }
            binding.mapView.overlays.add(marker)
        }
        
        binding.mapView.invalidate()
    }

    private fun moveToLocation(lat: Double, lng: Double) {
        binding.mapView.controller.animateTo(GeoPoint(lat, lng))
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.startLocationUpdates()
            }
            else -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.map_permission_rationale),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        myLocationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        myLocationOverlay?.disableMyLocation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Factory for MapViewModel
class MapViewModelFactory(
    private val questRepository: com.fediquest.app.data.repositories.QuestRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(questRepository) as T
    }
}
