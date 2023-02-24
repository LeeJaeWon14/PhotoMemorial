package com.jeepchief.photomemorial.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jeepchief.photomemorial.R
import com.jeepchief.photomemorial.databinding.ItemPhotoListBinding
import com.jeepchief.photomemorial.model.database.PhotoEntity

class SearchListAdapter(
    private val _list: List<PhotoEntity>,
    private val dismiss: () -> Unit,
    private val searchAction: (PhotoEntity) -> Unit) : RecyclerView.Adapter<SearchListAdapter.SearchListViewHolder>() {
    private val list get() = _list.sortedBy { it.address }

    class SearchListViewHolder(private val binding: ItemPhotoListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entity: PhotoEntity, dismiss: () -> Unit, action: (PhotoEntity) -> Unit) {
            binding.apply {
                tvAddressBySearch.text = entity.address
                llAddressItem.setOnClickListener {
                    action(entity)
                    dismiss()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_list, parent, false)
        val binding = ItemPhotoListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchListViewHolder, position: Int) {
        holder.bind(list[position], dismiss, searchAction)
    }

    override fun getItemCount(): Int {
        return list.size
    }
}