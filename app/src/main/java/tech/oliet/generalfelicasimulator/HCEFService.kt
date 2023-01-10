package tech.oliet.generalfelicasimulator

import android.nfc.cardemulation.HostNfcFService
import android.os.Bundle

class HCEFService : HostNfcFService() {
    override fun processNfcFPacket(commandPacket: ByteArray, extras: Bundle?): ByteArray? = null
    override fun onDeactivated(p0: Int) {}
}