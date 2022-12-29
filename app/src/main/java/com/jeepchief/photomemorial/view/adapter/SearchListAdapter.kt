package com.jeepchief.photomemorial.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.jeepchief.photomemorial.R
import com.jeepchief.photomemorial.model.database.PhotoEntity
import com.jeepchief.photomemorial.viewmodel.MainViewModel

class SearchListAdapter(
    private val _list: List<PhotoEntity>,
    private val viewModel: MainViewModel,
    private val dlg: AlertDialog) : RecyclerView.Adapter<SearchListAdapter.SearchListViewHolder>() {
    private val list get() = _list.sortedBy { it.address }

    class SearchListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAddress = view.findViewById<TextView>(R.id.tv_address_by_search)
        val llAddressItem = view.findViewById<LinearLayout>(R.id.ll_address_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_list, parent, false)
        return SearchListViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchListViewHolder, position: Int) {
        holder.apply {
            tvAddress.text = list[position].address
            llAddressItem.setOnClickListener {
                viewModel.photoLocationBySearch.value = list[position]
                dlg.dismiss()
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}