package com.harjot.firestorewithsupabase

interface RecyclerInterface {
    fun onListClick(position: Int)
    fun onEditClick(position: Int)
    fun onDeleteClick(position: Int)
}