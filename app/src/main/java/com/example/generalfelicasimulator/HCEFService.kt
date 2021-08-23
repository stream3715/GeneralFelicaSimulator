package com.example.generalfelicasimulator

import android.nfc.cardemulation.HostNfcFService
import android.os.Bundle

class HCEFService : HostNfcFService() {
    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun processNfcFPacket(p0: ByteArray?, p1: Bundle?): ByteArray {
        return ByteArray(0)
    }

    override fun onDeactivated(p0: Int) {
    }
}