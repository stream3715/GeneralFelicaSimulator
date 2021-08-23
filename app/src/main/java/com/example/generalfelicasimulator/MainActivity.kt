package com.example.generalfelicasimulator

import android.app.AlertDialog
import android.content.ComponentName
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.NfcFCardEmulation
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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

        val btn_update = findViewById<Button>(R.id.button_update)
        btn_update.setOnClickListener {
            val idm = findViewById<EditText>(R.id.editTextIDm).text.toString()
            val sys = findViewById<EditText>(R.id.editTextSys).text.toString()

            nfcFCardEmulation?.disableService(this)
            val result_idm = nfcFCardEmulation?.setNfcid2ForService(myComponentName, idm)
            val result_sys = nfcFCardEmulation?.registerSystemCodeForService(myComponentName, sys)
            nfcFCardEmulation?.enableService(this, myComponentName)

            if (result_idm == true && result_sys == true) {
                Toast.makeText(applicationContext, "Updated: $idm $sys", Toast.LENGTH_LONG).show()
            } else {
                if (result_idm == false) {
                    Toast.makeText(applicationContext, "Error. Invalid IDm", Toast.LENGTH_LONG)
                        .show()
                }
                if (result_sys == false) {
                    Toast.makeText(applicationContext, "Error. Invalid Sys", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
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