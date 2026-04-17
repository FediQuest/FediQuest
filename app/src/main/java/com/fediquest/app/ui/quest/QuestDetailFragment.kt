// File: app/src/main/java/com/fediquest/app/ui/quest/QuestDetailFragment.kt
package com.fediquest.app.ui.quest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fediquest.app.R
import com.fediquest.app.data.models.Quest
import com.fediquest.app.databinding.FragmentQuestDetailBinding
import com.fediquest.app.di.ServiceLocator
import com.fediquest.app.viewmodel.SharedViewModel
import kotlinx.coroutines.launch

/**
 * Fragment for displaying quest details and completion options.
 */
class QuestDetailFragment : Fragment() {

    private var _binding: FragmentQuestDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: QuestDetailViewModel
    private lateinit var sharedViewModel: SharedViewModel
    
    private val args: QuestDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            QuestDetailViewModelFactory(
                ServiceLocator.questRepository,
                ServiceLocator.userRepository
            )
        )[QuestDetailViewModel::class.java]

        sharedViewModel = ViewModelProvider(
            requireActivity(),
            SharedViewModelFactory(ServiceLocator.userRepository)
        )[SharedViewModel::class.java]

        setupClickListeners()
        observeViewModel()
        loadQuestDetails()
    }

    private fun setupClickListeners() {
        binding.btnStartQuest.setOnClickListener {
            startQuest()
        }

        binding.btnCompleteQuest.setOnClickListener {
            completeQuest()
        }

        binding.btnShareToFediverse.setOnClickListener {
            shareToFediverse()
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.quest.collect { quest ->
                quest?.let { displayQuestDetails(it) }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.validationResult.collect { result ->
                result?.let {
                    showValidationResult(it)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.shareResult.collect { result ->
                result?.let {
                    showShareResult(it)
                }
            }
        }
    }

    private fun displayQuestDetails(quest: Quest) {
        binding.questTitle.text = quest.title
        binding.questDescription.text = quest.description
        binding.questCategory.text = "${quest.category.icon} ${quest.category.displayName}"
        binding.questDifficulty.text = "Difficulty: ${quest.difficulty.name}"
        binding.questXpReward.text = "XP Reward: ${quest.xpReward}"
        binding.questCoinReward.text = "Coins: ${quest.coinReward}"
        binding.questValidationMode.text = "Validation: ${quest.validationMode.name}"
        
        // Update status badge
        when (quest.status) {
            com.fediquest.app.data.models.QuestStatus.AVAILABLE -> {
                binding.statusText.text = "Available"
                binding.statusText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
            com.fediquest.app.data.models.QuestStatus.IN_PROGRESS -> {
                binding.statusText.text = "In Progress"
                binding.statusText.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
            com.fediquest.app.data.models.QuestStatus.COMPLETED -> {
                binding.statusText.text = "Completed"
                binding.statusText.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))
            }
            else -> {
                binding.statusText.text = quest.status.name
            }
        }

        // Show/hide buttons based on status
        when (quest.status) {
            com.fediquest.app.data.models.QuestStatus.AVAILABLE -> {
                binding.btnStartQuest.visibility = View.VISIBLE
                binding.btnCompleteQuest.visibility = View.GONE
                binding.btnShareToFediverse.visibility = View.GONE
            }
            com.fediquest.app.data.models.QuestStatus.IN_PROGRESS -> {
                binding.btnStartQuest.visibility = View.GONE
                binding.btnCompleteQuest.visibility = View.VISIBLE
                binding.btnShareToFediverse.visibility = View.GONE
            }
            com.fediquest.app.data.models.QuestStatus.COMPLETED -> {
                binding.btnStartQuest.visibility = View.GONE
                binding.btnCompleteQuest.visibility = View.GONE
                binding.btnShareToFediverse.visibility = View.VISIBLE
            }
            else -> {
                binding.btnStartQuest.visibility = View.GONE
                binding.btnCompleteQuest.visibility = View.GONE
                binding.btnShareToFediverse.visibility = View.GONE
            }
        }
    }

    private fun startQuest() {
        viewModel.startQuest(args.questId)
    }

    private fun completeQuest() {
        // In a real implementation, this would open the camera for proof capture
        // For now, we'll just complete the quest
        viewModel.completeQuest(args.questId, null)
    }

    private fun shareToFediverse() {
        viewModel.shareToFediverse(args.questId)
    }

    private fun showValidationResult(result: com.fediquest.app.domain.usecases.ValidationResult) {
        if (result.success) {
            binding.messageText.text = "✅ ${result.message}\n+${result.xpReward} XP, +${result.coinReward} Coins"
            binding.messageText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            binding.messageText.text = "❌ ${result.message}"
            binding.messageText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }
        binding.messageText.visibility = View.VISIBLE
        
        if (result.success) {
            // Refresh quest details after successful completion
            loadQuestDetails()
        }
    }

    private fun showShareResult(success: Boolean) {
        if (success) {
            binding.messageText.text = "🌐 Shared to Fediverse successfully!"
        } else {
            binding.messageText.text = "⚠️ Failed to share to Fediverse"
        }
        binding.messageText.visibility = View.VISIBLE
    }

    private fun loadQuestDetails() {
        viewModel.loadQuest(args.questId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
