// File: app/src/main/java/com/fediquest/app/ui/avatar/AvatarFragment.kt
package com.fediquest.app.ui.avatar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fediquest.app.R
import com.fediquest.app.data.models.Avatar
import com.fediquest.app.data.models.AvatarCategory
import com.fediquest.app.databinding.FragmentAvatarBinding
import com.fediquest.app.di.ServiceLocator
import kotlinx.coroutines.launch

/**
 * Fragment for customizing user avatar.
 * Users can equip different avatar parts (hair, outfit, accessories, etc.)
 */
class AvatarFragment : Fragment() {

    private var _binding: FragmentAvatarBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AvatarViewModel
    private var currentCategory: AvatarCategory = AvatarCategory.HAIR

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAvatarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            AvatarViewModelFactory(
                ServiceLocator.avatarRepository,
                ServiceLocator.userRepository
            )
        )[AvatarViewModel::class.java]

        setupCategorySelector()
        observeViewModel()
        loadAvatars()
    }

    private fun setupCategorySelector() {
        binding.categorySelector.removeAllViews()
        
        AvatarCategory.values().forEach { category ->
            val button = android.widget.Button(requireContext()).apply {
                text = category.name
                setOnClickListener {
                    currentCategory = category
                    loadAvatars()
                }
            }
            binding.categorySelector.addView(button)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.avatars.collect { avatars ->
                displayAvatars(avatars)
            }
        }

        lifecycleScope.launch {
            viewModel.equippedAvatar.collect { avatar ->
                avatar?.let {
                    updateAvatarPreview(it)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun displayAvatars(avatars: List<Avatar>) {
        binding.avatarGrid.removeAllViews()

        if (avatars.isEmpty()) {
            binding.emptyState.text = getString(R.string.avatar_no_items)
            binding.emptyState.visibility = View.VISIBLE
            return
        }

        binding.emptyState.visibility = View.GONE

        avatars.forEach { avatar ->
            val avatarView = createAvatarViewItem(avatar)
            binding.avatarGrid.addView(avatarView)
        }
    }

    private fun createAvatarViewItem(avatar: Avatar): View {
        val context = requireContext()
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.avatar_item_margin)
                marginEnd = resources.getDimensionPixelSize(R.dimen.avatar_item_margin)
                topMargin = resources.getDimensionPixelSize(R.dimen.avatar_item_margin)
                bottomMargin = resources.getDimensionPixelSize(R.dimen.avatar_item_margin)
            }
            gravity = android.view.Gravity.CENTER
        }

        // Avatar image placeholder
        val imageView = android.widget.ImageView(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.avatar_image_size),
                resources.getDimensionPixelSize(R.dimen.avatar_image_size)
            )
            setImageResource(avatar.drawableResId)
            contentDescription = avatar.name
            
            if (!avatar.isUnlocked) {
                alpha = 0.5f
            }
        }
        layout.addView(imageView)

        // Avatar name
        val textView = android.widget.TextView(context).apply {
            text = avatar.name
            textSize = 12f
            setTextColor(
                if (avatar.isEquipped) 
                    resources.getColor(android.R.color.holo_green_dark, null)
                else 
                    resources.getColor(android.R.color.black, null)
            )
        }
        layout.addView(textView)

        // Equip button
        val equipButton = android.widget.Button(context).apply {
            text = if (avatar.isEquipped) "Equipped" else "Equip"
            isEnabled = avatar.isUnlocked && !avatar.isEquipped
            setOnClickListener {
                viewModel.equipAvatar(avatar.id)
            }
        }
        layout.addView(equipButton)

        return layout
    }

    private fun updateAvatarPreview(avatar: Avatar) {
        binding.avatarPreview.setImageResource(avatar.drawableResId)
    }

    private fun loadAvatars() {
        viewModel.loadAvatarsForCategory(currentCategory)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
