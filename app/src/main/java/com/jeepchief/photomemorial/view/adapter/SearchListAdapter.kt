package com.jeepchief.photomemorial.view.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class SearchListAdapter(private val list: List<String>) : RecyclerView.Adapter<SearchListAdapter.SearchListViewHolder>() {
    class SearchListViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchListViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: SearchListViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        return list.size
    }
}