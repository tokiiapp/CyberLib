package com.cyber.ads.onboading

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cyber.ads.R
import com.cyber.ads.databinding.ItemLanguageBinding

class LanguageAdapter(private val onClick: (Int) -> Unit) :
    ListAdapter<LanguageModel, LanguageAdapter.ViewHolder>(DiffCallback()) {

    private var selected = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemLanguageBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updatePosition(position: Int) {
        selected = position
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LanguageModel) {
            binding.ivFlag.setImageResource(item.flag)
            binding.tvName.text = item.name
            if (selected == adapterPosition) {
                binding.rbCheck.isChecked = true
                binding.root.setBackgroundResource(R.drawable.bg_item_language_on)
                binding.tvName.setTextColor(binding.root.context.getColor(R.color.color_item_language_selected))
            } else {
                binding.rbCheck.isChecked = false
                binding.root.setBackgroundResource(R.drawable.bg_item_language_off)
                binding.tvName.setTextColor(binding.root.context.getColor(R.color.color_item_language_unselected))
            }
            binding.rbCheck.setOnClickListener {
                onClick(adapterPosition)
            }
            binding.root.setOnClickListener {
                binding.rbCheck.performClick()
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LanguageModel>() {
        override fun areItemsTheSame(oldItem: LanguageModel, newItem: LanguageModel): Boolean {
            return oldItem.langCode == newItem.langCode
        }

        override fun areContentsTheSame(oldItem: LanguageModel, newItem: LanguageModel): Boolean {
            return oldItem == newItem
        }
    }
}
