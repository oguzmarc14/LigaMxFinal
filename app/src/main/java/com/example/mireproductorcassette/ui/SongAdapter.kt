package com.example.mireproductorcassette.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mireproductorcassette.databinding.ItemSongBinding
import com.example.mireproductorcassette.player.Song

class SongAdapter(
    private val items: List<Song>,
    private val onPlayClick: (Int, Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.VH>() {

    inner class VH(val b: ItemSongBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.b.title.text = s.title
        holder.b.artist.text = s.artist
        holder.b.btnPlay.setOnClickListener { onPlayClick(position, s) }
    }

    override fun getItemCount(): Int = items.size
}
