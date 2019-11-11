package com.hyperion.blescaleexample

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hyperion.blescaleexample.core.bluetooth.BluetoothCommunication
import kotlinx.android.synthetic.main.item_bluetooth.view.*

class MainAdapter(private val itemClickListener: (Int) -> Unit) :
    RecyclerView.Adapter<MainAdapter.Companion.ItemViewHolder>() {

    private val items = mutableListOf<BluetoothCommunication>()

    fun updateData(item: BluetoothCommunication) {
        items.add(item)
        notifyDataSetChanged()
    }

    fun clearData() {
        items.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_bluetooth, parent, false)
        return ItemViewHolder(view, itemClickListener)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    companion object {
        class ItemViewHolder(view: View, private val itemClickListener: (Int) -> Unit) :
            RecyclerView.ViewHolder(view) {

            fun bind(item: BluetoothCommunication, position: Int) {
                with(itemView) {
                    tvName.text = item.driverName()
                    setOnClickListener { itemClickListener.invoke(position) }
                }
            }
        }
    }
}