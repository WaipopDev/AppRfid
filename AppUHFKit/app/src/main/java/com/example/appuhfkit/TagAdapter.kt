package com.example.appuhfkit

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// ข้อมูลแต่ละ tag
 data class TagItem(
    val epc: String,
    val time: String,
    val rssi: String?
)

class TagAdapter(private val items: List<TagItem>) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {
    
    companion object {
        private const val TAG = "TagAdapter"
    }
    
    class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNo: TextView = itemView.findViewById(R.id.tvNo)
        val tvEpc: TextView = itemView.findViewById(R.id.tvEpc)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        Log.d(TAG, "Created new ViewHolder")
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val item = items[position]
        Log.d(TAG, "Binding item at position $position: EPC=${item.epc}")
        
        holder.tvNo.text = (position + 1).toString()
        holder.tvEpc.text = item.epc
        holder.tvTime.text = item.time
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount called: ${items.size}")
        return items.size
    }
} 