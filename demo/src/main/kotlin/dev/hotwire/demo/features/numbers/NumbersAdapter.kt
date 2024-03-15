package dev.hotwire.demo.features.numbers

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import dev.hotwire.demo.R

class NumbersAdapter(val callback: NumbersFragmentCallback) : RecyclerView.Adapter<NumbersAdapter.ViewHolder>() {
    private val type = R.layout.adapter_numbers_row

    private var items = emptyList<Int>()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun setData(numbers: List<Int>) {
        items = numbers
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun getItemViewType(position: Int): Int {
        return type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView: MaterialTextView = view.findViewById(R.id.number)

        fun bind(number: Int) {
            textView.text = "$number"
            itemView.setOnClickListener {
                callback.onItemClicked(number)
            }
        }
    }
}
