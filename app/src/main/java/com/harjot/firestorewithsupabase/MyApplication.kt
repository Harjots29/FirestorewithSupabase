package com.harjot.firestorewithsupabase

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

class MyApplication: Application() {
    lateinit var supabaseClient: SupabaseClient

    override fun onCreate() {
        super.onCreate()
        supabaseClient = createSupabaseClient(
            "https://cnndahnmlioiuzameswu.supabase.co",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNubmRhaG5tbGlvaXV6YW1lc3d1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzQ1OTk1MTcsImV4cCI6MjA1MDE3NTUxN30.g_PdxMrwWhR-mVXtQ-_tSru_aBQb-bw6c5pb_Dfvah4"
        ){
            install(Storage)
        }


        val bucket = supabaseClient.storage.from("firestore_supabase")
    }
}