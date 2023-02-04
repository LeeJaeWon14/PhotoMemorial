package com.jeepchief.photomemorial.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jeepchief.photomemorial.R
import com.jeepchief.photomemorial.model.database.PhotoEntity

class ShowAroundAdapter(
    private val list: List<PhotoEntity>,
    private val searchAction: (PhotoEntity) -> Unit,
    private val sheet: BottomSheetDialog) : RecyclerView.Adapter<ShowAroundAdapter.ShowAroundViewHolder>() {
    class ShowAroundViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAround: TextView = view.findViewById(R.id.tv_around)
        val ivAround: ImageView = view.findViewById(R.id.iv_around)
        val rlSearchAround: RelativeLayout = view.findViewById(R.id.rl_search_around)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowAroundViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_around, parent, false)
        return ShowAroundViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShowAroundViewHolder, position: Int) {
        holder.apply {
            tvAround.text = list[position].address
            ivAround.setImageURI(list[position].photo)
            rlSearchAround.setOnClickListener { searchAction.invoke(list[position]) }
            sheet.dismiss()
        }
    }

    override fun getItemCount(): Int = list.size
}