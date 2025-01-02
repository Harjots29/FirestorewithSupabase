package com.harjot.firestorewithsupabase

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.harjot.firestorewithsupabase.databinding.ActivityMainBinding
import com.harjot.firestorewithsupabase.databinding.CustomDialogLayoutBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), RecyclerInterface {
    val pickImageRequest = 1
    val permissionRequestCode = 100
    val externalStorageRequestCode = 101
    private var collectionName = "FirestoreSupabase"
    private var database = Firebase.firestore
    lateinit var binding: ActivityMainBinding
    var arrayList = ArrayList<Model>()
    var recyclerAdapter = RecyclerAdapter(arrayList,this,this)
    lateinit var supabaseClient: SupabaseClient
    var imgUri: Uri? = null
    var imagegUrl:  String?=null
    lateinit var dialogBinding : CustomDialogLayoutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        dialogBinding = CustomDialogLayoutBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment

        supabaseClient = (application as MyApplication).supabaseClient
        checkAndRequestPermission()

        binding.rv.layoutManager = LinearLayoutManager(this)
        binding.rv.adapter = recyclerAdapter
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        arrayList.clear()
        database.collection(collectionName)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (snapshot in snapshots!!.documentChanges) {
                    val userModel = convertObject(snapshot.document)

                    when (snapshot.type) {
                        DocumentChange.Type.ADDED -> {
                            userModel?.let { arrayList.add(it) }
                            Log.e(ContentValues.TAG, "userModelList ${arrayList.size}")
                        }
                        DocumentChange.Type.MODIFIED -> {
                            userModel?.let {
                                var index = getIndex(userModel)
                                if (index > -1) {
                                    arrayList.set(index, it)
                                }
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            userModel?.let {
                                var index = getIndex(userModel)
                                if (index > -1) {
                                    arrayList.removeAt(index)
                                }
                            }
                        }
                    }
                }
                recyclerAdapter.notifyDataSetChanged()
            }
        binding.fabAdd.setOnClickListener {
            dialog()
        }
    }

    override fun onListClick(position: Int) { }

    override fun onEditClick(position: Int) {
        Toast.makeText(this, "$position ", Toast.LENGTH_SHORT).show()
        dialog(position)
    }

    override fun onDeleteClick(position: Int) {
        Toast.makeText(this, "$position", Toast.LENGTH_SHORT).show()
        var alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Delete Item")
        alertDialog.setMessage("Do you want to delete the item?")
        alertDialog.setCancelable(false)
        alertDialog.setNegativeButton("No") { _, _ ->
            alertDialog.setCancelable(true)
        }
        alertDialog.setPositiveButton("Yes") { _, _ ->
            if (arrayList.size == 0){
                Toast.makeText(this, "List Is Empty", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(
                    this,
                    "The item is  deleted",
                    Toast.LENGTH_SHORT
                ).show()
                database.collection(collectionName)
                    .document(arrayList[position].id ?: "").delete()
//                arrayList.removeAt(position)
//                recyclerAdapater.notifyDataSetChanged()
            }
        }
        alertDialog.show()
    }
    fun dialog(position: Int = -1){
        dialogBinding = CustomDialogLayoutBinding.inflate(layoutInflater)
        var dialog = Dialog(this).apply {
            setContentView(dialogBinding.root)
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            if (position > -1){
                dialogBinding.btnAdd.setText("Update")
                dialogBinding.etName.setText(arrayList[position].name)
                dialogBinding.etPhoneNo.setText(arrayList[position].phoneNo.toString())
                dialogBinding.etEmail.setText(arrayList[position].email)
                Glide.with(this@MainActivity)
                    .load(arrayList[position].image)
                    .centerCrop()
                    .into(dialogBinding.ivImage)
                imgUri = arrayList[position].imgUri?.toUri()
                Toast.makeText(this@MainActivity, "${imgUri}", Toast.LENGTH_SHORT).show()

            }else{
                dialogBinding.btnAdd.setText("Add")
            }
            dialogBinding.ivImage.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent,pickImageRequest)
            }
            dialogBinding.btnAdd.setOnClickListener {
                Toast.makeText(this@MainActivity, "in add click", Toast.LENGTH_SHORT).show()
                if (dialogBinding.etName.text.toString().trim().isNullOrEmpty()){
                    dialogBinding.etName.error = "Enter Name"
                }else  if (dialogBinding.etEmail.text.toString().trim().isNullOrEmpty()){
                    dialogBinding.etEmail.error = "Enter Email"
                }else  if (dialogBinding.etPhoneNo.text.toString().trim().isNullOrEmpty()){
                    dialogBinding.etPhoneNo.error = "Enter PhoneNo"
                }else{
                    if (position > -1){
                        if (imgUri == null){
                            Toast.makeText(this@MainActivity, "Select Image", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        addUpdate(position)
                    }else{
                        binding.llLoader.visibility = View.VISIBLE
                        uploadImageToSupabase(imgUri!!,position)
                    }

                    dismiss()
                }
            }
            show()
        }
    }
    fun convertObject(snapshot: QueryDocumentSnapshot) : Model?{
        val userModel: Model =
            snapshot.toObject(Model::class.java)
        userModel.id = snapshot.id ?: ""
        return userModel
    }
    fun getIndex(userModel: Model) : Int{
        var index = -1
        index = arrayList.indexOfFirst { element ->
            element.id?.equals(userModel.id) == true
        }
        return index
    }
    private fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                if (Environment.isExternalStorageManager()){
                    //permission granted, proceed
                }else{
                    //ask for permission
                    requestManageExternalStoragePermission()
                }
            }else{
                if(ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    requestManageExternalStoragePermission()
                }
            }
        }else{
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), permissionRequestCode)
            }
        }
    }
    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            try{
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent,permissionRequestCode)
            }catch (e: ActivityNotFoundException){
                Toast.makeText(this, "Activity not Found", Toast.LENGTH_SHORT).show()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            permissionRequestCode->{
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "granted", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this, "denied", Toast.LENGTH_SHORT).show()
                }
            }
            externalStorageRequestCode ->{
                if (Environment.isExternalStorageManager()){
                    Toast.makeText(this, "full storage access granted", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this, "permission not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == pickImageRequest){
            data?.data?.let { uri->
                Toast.makeText(this, "$uri", Toast.LENGTH_SHORT).show()
                dialogBinding.ivImage.setImageURI(uri)
                imgUri = uri
//                uploadImageToSupabase(uri)
            }
        }
    }
    private fun uploadImageToSupabase(uri: Uri,position: Int) {
        Toast.makeText(this, "inUploadImage", Toast.LENGTH_SHORT).show()
        val byteArray = uriToByteArray(this,uri)
        val filename = "images/${System.currentTimeMillis()}.jpg"

        val bucket = supabaseClient.storage.from("firestore_supabase")

        lifecycleScope.launch(Dispatchers.IO) {
            try{
                bucket.uploadAsFlow(filename,byteArray).collect{status->
                    withContext(Dispatchers.Main){
                        when(status){
                            is UploadStatus.Progress->{
                                println("InProgress")
                                Toast.makeText(this@MainActivity, "progress", Toast.LENGTH_SHORT).show()
                            }
                            is UploadStatus.Success->{
                                binding.llLoader.visibility = View.GONE
                                println("InSuccess")
                                Toast.makeText(this@MainActivity, "Upload Success", Toast.LENGTH_SHORT).show()
                                val imgUrl = bucket.publicUrl(filename)
                                val img = dialogBinding.ivImage
                                imagegUrl = imgUrl
                                addUpdate(position)
                                Glide.with(this@MainActivity)
                                    .load(imgUrl)
                                    .placeholder(R.drawable.ic_img)
                                    .into(img)
                            }
                        }
                    }
                }
            }catch (e: Exception){
                val TAG = "Upload"
//                mainActivity.binding.llLoader.visibility = View.GONE
                Log.e(TAG, "uploadImageToSupabase: ${e.message}", )
//                Toast.makeText(this@MainActivity, "${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun uriToByteArray(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
        return inputStream?.readBytes() ?: ByteArray(0)
    }
    fun addUpdate(position: Int){
        if (position > -1){
            Toast.makeText(this, "In Update", Toast.LENGTH_SHORT).show()
            binding.llLoader.visibility = View.VISIBLE
//                        arrayList[position] = Model(
//                            "",
//                            name =  dialogBinding.etName.text.toString(),
//                            email = dialogBinding.etEmail.text.toString(),
//                            phoneNo = dialogBinding.etPhoneNo.text.toString(),
//                        )
//                        arrayList[position].name = dialogBinding.etName.text.toString()
//                        arrayList[position].email = dialogBinding.etEmail.text.toString()
//                        arrayList[position].phoneNo = dialogBinding.etPhoneNo.text.toString()
            val userModel = Model(
                id = arrayList[position].id,
                name = dialogBinding.etName.text.toString(),
                email = dialogBinding.etEmail.text.toString(),
                phoneNo = dialogBinding.etPhoneNo.text.toString(),
                image = imagegUrl,
                imgUri = imgUri.toString()
            )
            database.collection(collectionName).document(arrayList[position].id?:"").set(userModel)
                .addOnSuccessListener {
                    Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
                    binding.llLoader.visibility = View.GONE
                }
                .addOnFailureListener {
                    Toast.makeText(this, "failed", Toast.LENGTH_SHORT).show()
                    binding.llLoader.visibility = View.GONE
                }
        }else{
            Toast.makeText(this, "In Add", Toast.LENGTH_SHORT).show()
            binding.llLoader.visibility = View.VISIBLE
            Toast.makeText(this@MainActivity, "$position", Toast.LENGTH_SHORT).show()
            database.collection(collectionName).add(
                Model(
                    "",
                    name = dialogBinding.etName.text.toString(),
                    email = dialogBinding.etEmail.text.toString(),
                    phoneNo = dialogBinding.etPhoneNo.text.toString(),
                    image = imagegUrl,
                    imgUri = imgUri.toString()
                )
            ).addOnSuccessListener {
                binding.llLoader.visibility = View.GONE
                Toast.makeText(this@MainActivity, "success", Toast.LENGTH_SHORT).show()
            }
                .addOnFailureListener {
                    binding.llLoader.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "failure", Toast.LENGTH_SHORT).show()
                }
        }
        recyclerAdapter.notifyDataSetChanged()
    }
}