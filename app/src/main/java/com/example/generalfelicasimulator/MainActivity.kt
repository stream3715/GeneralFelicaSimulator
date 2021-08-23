package com.example.generalfelicasimulator

import android.app.AlertDialog
import android.content.ComponentName
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.NfcFCardEmulation
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var nfcFCardEmulation: NfcFCardEmulation? = null
    private var myComponentName: ComponentName? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF)) {
            Log.e("GeneralFelicaSimulator", "HCE-F is not supported")
            AlertDialog.Builder(this).setTitle("Error").setMessage("HCE-F is not supported")
                .setOnDismissListener { exitProcess(-1) }.show()
            return
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcFCardEmulation = NfcFCardEmulation.getInstance(nfcAdapter)
        myComponentName = ComponentName(
            "com.example.generalfelicasimulator",
            "com.example.generalfelicasimulator.HCEFService"
        )

        nfcFCardEmulation?.setNfcid2ForService(myComponentName, "02FE000000000000")
        nfcFCardEmulation?.registerSystemCodeForService(myComponentName, "4000")
    }

    override fun onResume() {
        super.onResume()
        nfcFCardEmulation?.enableService(this, myComponentName)
    }

    override fun onPause() {
        super.onPause()
        nfcFCardEmulation?.disableService(this)
    }
}