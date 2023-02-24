package com.jeepchief.photomemorial.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jeepchief.photomemorial.databinding.ItemAroundBinding
import com.jeepchief.photomemorial.model.database.PhotoEntity

class ShowAroundAdapter(
    private val list: List<PhotoEntity>,
    private val searchAction: (PhotoEntity) -> Unit,
    private val dismiss: () -> Unit) : RecyclerView.Adapter<ShowAroundAdapter.ShowAroundViewHolder>() {

    class ShowAroundViewHolder(private val binding: ItemAroundBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entity: PhotoEntity, action: (PhotoEntity) -> Unit, dismiss: () -> Unit) {
            binding.apply {
                tvAround.text = entity.address
                ivAround.setImageURI(entity.photo)
                rlSearchAround.setOnClickListener {
                    action(entity)
                    dismiss()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowAroundViewHolder {
        val binding = ItemAroundBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShowAroundViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShowAroundViewHolder, position: Int) {
        holder.bind(list[position], searchAction, dismiss)
    }

    override fun getItemCount(): Int = list.size
}