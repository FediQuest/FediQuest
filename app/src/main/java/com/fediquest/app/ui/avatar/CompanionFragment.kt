// File: app/src/main/java/com/fediquest/app/ui/avatar/CompanionFragment.kt
package com.fediquest.app.ui.avatar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fediquest.app.R
import com.fediquest.app.data.models.Companion
import com.fediquest.app.databinding.FragmentCompanionBinding
import com.fediquest.app.di.ServiceLocator
import kotlinx.coroutines.launch

/**
 * Fragment for viewing and interacting with eco-companions.
 * Companions evolve and gain XP as users complete quests.
 */
class CompanionFragment : Fragment() {

    private var _binding: FragmentCompanionBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CompanionViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompanionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            CompanionViewModelFactory(
                ServiceLocator.companionRepository,
                ServiceLocator.userRepository
            )
        )[CompanionViewModel::class.java]

        setupInteractionButtons()
        observeViewModel()
        loadCompanions()
    }

    private fun setupInteractionButtons() {
        binding.btnFeed.setOnClickListener {
            viewModel.feedCompanion()
        }

        binding.btnPlay.setOnClickListener {
            viewModel.playWithCompanion()
        }

        binding.btnRest.setOnClickListener {
            viewModel.restCompanion()
        }

        binding.btnEquip.setOnClickListener {
            viewModel.equipCurrentCompanion()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.companions.collect { companions ->
                displayCompanions(companions)
            }
        }

        lifecycleScope.launch {
            viewModel.equippedCompanion.collect { companion ->
                companion?.let {
                    updateCompanionDisplay(it)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.message.collect { message ->
                message?.let {
                    showMessage(it)
                }
            }
        }
    }

    private fun displayCompanions(companions: List<Companion>) {
        binding.companionList.removeAllViews()

        if (companions.isEmpty()) {
            binding.emptyState.text = getString(R.string.companion_no_items)
            binding.emptyState.visibility = View.VISIBLE
            return
        }

        binding.emptyState.visibility = View.GONE

        companions.forEach { companion ->
            val companionView = createCompanionViewItem(companion)
            binding.companionList.addView(companionView)
        }
    }

    private fun createCompanionViewItem(companion: Companion): View {
        val context = requireContext()
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.companion_item_margin)
                marginEnd = resources.getDimensionPixelSize(R.dimen.companion_item_margin)
                topMargin = resources.getDimensionPixelSize(R.dimen.companion_item_margin)
                bottomMargin = resources.getDimensionPixelSize(R.dimen.companion_item_margin)
            }
            setBackgroundResource(R.drawable.companion_card_background)
            setPadding(16, 16, 16, 16)
        }

        // Companion name and species
        val titleText = android.widget.TextView(context).apply {
            text = "${companion.name} - ${companion.species.displayName}"
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.black, null))
        }
        layout.addView(titleText)

        // Level
        val levelText = android.widget.TextView(context).apply {
            text = "Level ${companion.level}"
            textSize = 14f
        }
        layout.addView(levelText)

        // XP Progress
        val xpProgress = android.widget.ProgressBar(context).apply {
            max = companion.xpToNextLevel
            progress = companion.currentXP
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            style = android.widget.ProgressBar.STYLE_HORIZONTAL
        }
        layout.addView(xpProgress)

        // Stats
        val statsText = android.widget.TextView(context).apply {
            text = "❤️ Happiness: ${companion.happiness}/100 | ⚡ Energy: ${companion.energy}/100"
            textSize = 12f
        }
        layout.addView(statsText)

        // Evolution stage
        val evolutionText = android.widget.TextView(context).apply {
            text = "Evolution Stage: ${companion.evolutionStage}"
            textSize = 12f
        }
        layout.addView(evolutionText)

        return layout
    }

    private fun updateCompanionDisplay(companion: Companion) {
        binding.companionImage.setImageResource(getCompanionDrawable(companion.species))
        binding.companionName.text = companion.name
        binding.companionLevel.text = "Level ${companion.level}"
        binding.companionXp.progress = companion.currentXP
        binding.companionXp.max = companion.xpToNextLevel
        binding.happinessBar.progress = companion.happiness
        binding.energyBar.progress = companion.energy
    }

    private fun getCompanionDrawable(species: com.fediquest.app.data.models.CompanionSpecies): Int {
        return when (species) {
            com.fediquest.app.data.models.CompanionSpecies.ECO_DRAKE -> R.drawable.ic_eco_drake
            com.fediquest.app.data.models.CompanionSpecies.GREEN_PHOENIX -> R.drawable.ic_green_phoenix
            com.fediquest.app.data.models.CompanionSpecies.FOREST_UNICORN -> R.drawable.ic_forest_unicorn
            com.fediquest.app.data.models.CompanionSpecies.AQUA_TURTLE -> R.drawable.ic_aqua_turtle
            com.fediquest.app.data.models.CompanionSpecies.SOLAR_LION -> R.drawable.ic_solar_lion
        }
    }

    private fun showMessage(message: String) {
        binding.messageText.text = message
        binding.messageText.visibility = View.VISIBLE
        binding.messageText.postDelayed({
            binding.messageText.visibility = View.GONE
        }, 3000)
    }

    private fun loadCompanions() {
        viewModel.loadAllCompanions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
