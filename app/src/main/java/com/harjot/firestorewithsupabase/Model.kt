package com.harjot.firestorewithsupabase

import android.net.Uri

data class Model(
    var id:String?=null,
    var name:String?=null,
    var email:String?=null,
    var phoneNo: String?=null,
    var image:String?=null,
    var imgUri: String?=null
)

