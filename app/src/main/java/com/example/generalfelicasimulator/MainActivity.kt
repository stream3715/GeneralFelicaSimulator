package com.example.generalfelicasimulator

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF)) {
            Log.e("GeneralFelicaSimulator", "HCE-F is not supported")
            AlertDialog.Builder(this).setTitle("Error").setMessage("HCE-F is not supported").show()
        }
    }
}