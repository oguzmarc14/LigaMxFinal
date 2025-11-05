package com.example.mireproductorcassette.player

import com.example.mireproductorcassette.R

object PlaylistRepo {
    val songs: MutableList<Song> = mutableListOf(
        Song(
            title = "Club America",
            artist = "Liga MX",
            uri = "android.resource://com.example.mireproductorcassette/${R.raw.aguilas}"
        ),
        Song(
            title = "Himno Mexico",
            artist = "Mexico",
            uri = "android.resource://com.example.mireproductorcassette/${R.raw.mexico}"
        ),
        Song(
            title = "UEFA",
            artist = "Futbol international",
            uri = "android.resource://com.example.mireproductorcassette/${R.raw.uefa}"
        )
    )
}
