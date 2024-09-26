package com.example.landmarkremark

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.landmarkremark.MarkerAdapter.MarkerViewHolder

class MarkerAdapter(
    private val markerList: List<MarkerData>,
    private val listener: OnMarkerClickListener
    ) : RecyclerView.Adapter<MarkerViewHolder>() {
        interface OnMarkerClickListener {
            fun onMarkerClick(markerData: MarkerData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.mark_item, parent, false)
        return MarkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: MarkerViewHolder, position: Int) {
        val markerData = markerList[position]
        holder.bind(markerData, listener)
        holder.textViewName.text = markerData.name
        holder.textViewContent.text = markerData.content
    }

    override fun getItemCount(): Int {
        return markerList.size
    }

    class MarkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView
        val textViewContent: TextView

        init {
            textViewName = itemView.findViewById(R.id.tvMarkerName)
            textViewContent = itemView.findViewById(R.id.tvMarkerContent)
        }

        fun bind(markerData: MarkerData, listener: OnMarkerClickListener) {
            textViewName.text = markerData.name
            textViewContent.text = markerData.content
            itemView.setOnClickListener { v: View? -> listener.onMarkerClick(markerData) }
        }
    }
}
