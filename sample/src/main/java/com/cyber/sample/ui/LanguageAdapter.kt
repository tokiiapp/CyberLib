package com.cyber.sample.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cyber.demo.opening.LanguageModel
import com.cyber.sample.databinding.ItemLanguageBinding

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
            binding.rbCheck.isChecked = selected == adapterPosition
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
