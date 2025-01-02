package com.harjot.firestorewithsupabase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecyclerAdapter (var arrayList:ArrayList<Model>, var recyclerInterface: RecyclerInterface, var mainActivity: MainActivity):
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var name = view.findViewById<TextView>(R.id.tvName)
        var email = view.findViewById<TextView>(R.id.tvEmail)
        var phoneNo = view.findViewById<TextView>(R.id.tvPhoneNo)
        var lv = view.findViewById<LinearLayout>(R.id.lv)
        var ivImage = view.findViewById<ImageView>(R.id.ivImg)
        var update = view.findViewById<Button>(R.id.btnUpdate)
        var delete = view.findViewById<Button>(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name.setText(arrayList[position].name)
        holder.email.setText(arrayList[position].email)
        holder.phoneNo.setText(arrayList[position].phoneNo.toString())
        holder.lv.setOnClickListener {
            recyclerInterface.onListClick(position)
        }
        holder.update.setOnClickListener {
            recyclerInterface.onEditClick(position)
        }
        holder.delete.setOnClickListener {
            recyclerInterface.onDeleteClick(position)
        }
        Glide
            .with(mainActivity)
            .load(arrayList[position].image)
            .centerCrop()
            .into(holder.ivImage)
    }
}