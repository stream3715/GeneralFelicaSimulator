package tech.oliet.generalfelicasimulator

import android.nfc.cardemulation.HostNfcFService
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import tech.oliet.generalfelicasimulator.ByteTool.Companion.b

const val PACKET_LENGTH_POS = 0
const val PACKET_LENGTH_SIZE = 1

const val COMMAND_TYPE_POS = 1
const val COMMAND_TYPE_SIZE = 1

const val IDM_POS = 2
const val IDM_SIZE = 8

const val RESPONSE_SIZE = 1
const val STATUS_FLAG1_SIZE = 1
const val STATUS_FLAG2_SIZE = 1

const val BLOCK_NUM_SIZE = 1
const val BLOCK_SIZE = 16

const val SYSTEM_CODE_NUM_SIZE = 1
const val SYSTEM_CODE_SIZE = 2

const val ONE_BLOCK = 1

//---
const val CMD_POLLING = 0x04.toByte()
const val RES_POLLING = 0x05.toByte()
const val CMD_READ_WITHOUT_ENCRYPTION = 0x06.toByte()
const val RES_READ_WITHOUT_ENCRYPTION = 0x07.toByte()
const val CMD_WRITE_WITHOUT_ENCRYPTION = 0x08.toByte()
const val RES_WRITE_WITHOUT_ENCRYPTION = 0x09.toByte()
const val CMD_REQUEST_SYSTEM_CODE = 0x0c.toByte()
const val RES_REQUEST_SYSTEM_CODE = 0x0d.toByte()

//---
const val STATUS_FLAG1_SUCCESS = 0x00.toByte()
const val STATUS_FLAG1_FAILED = 0xFF.toByte()
const val STATUS_FLAG1_FELICA_LITE_ERROR = 0x01.toByte()

const val STATUS_FLAG2_SUCCESS = 0x00.toByte()
const val STATUS_FLAG2_READ_ERROR = 0x70.toByte()
const val STATUS_FLAG2_READONLY = 0xA8.toByte()
const val STATUS_FLAG2_NEED_AUTH = 0xB1.toByte()
const val STATUS_FLAG2_SERVICE_NUM_ERROR = 0xA1.toByte()
const val STATUS_FLAG2_BLOCK_NUM_ERROR = 0xA2.toByte()
const val STATUS_FLAG2_SERVICE_CODE = 0xA6.toByte()
const val STATUS_FLAG2_ACCESS_MODE = 0xA7.toByte()

const val SERVICE_CODE_READ_ONLY = ((0x10 and 0xff) shl 8) or (0x0b and 0xff)


class HCEFService : HostNfcFService() {
    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    override fun onCreate() {
        Log.d("HCEFService(NFC-F)", "onCreate")
        super.onCreate()
        Toast.makeText(this, "onCreate", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        Log.d("HCEFService(NFC-F)", "onDestroy")
        super.onDestroy()
    }

    override fun processNfcFPacket(commandPacket: ByteArray, extras: Bundle?): ByteArray? {
        Log.d("HCEFService(NFC-F)", "processNfcFPacket: Received NFC-F")
        Toast.makeText(this, "processNfcFPacket", Toast.LENGTH_LONG).show()

        //必須情報より小さいならエラー
        if (commandPacket.size < PACKET_LENGTH_SIZE + COMMAND_TYPE_SIZE + IDM_SIZE) {
            Log.e("HCEFService(NFC-F)", "processNfcFPacket: Packet size too short")
            return ByteArray(0)
        }

        return when (commandPacket[COMMAND_TYPE_POS]) {
            CMD_POLLING -> ByteArray(0) //stub
//            CMD_READ_WITHOUT_ENCRYPTION -> readWithoutEncryption(commandPacket) //stub
            CMD_READ_WITHOUT_ENCRYPTION -> requestSystemCode(commandPacket) //stub
            CMD_REQUEST_SYSTEM_CODE -> requestSystemCode(commandPacket)
            else -> ByteArray(0) //該当コマンドなし
        }
    }

    override fun onDeactivated(reason: Int) {
        if (reason == DEACTIVATION_LINK_LOSS) {
            Log.d("HCEFService(NFC-F)", "onDeactivated: DEACTIVATION_LINK_LOSS")
        } else {
            Log.d("HCEFService(NFC-F)", "onDeactivated: Unknown reason")
        }
    }

    private fun readWithoutEncryption(commandPacket: ByteArray): ByteArray {
        Log.d("HCEFService(NFC-F)", "ReadWithoutEncryption")
        Log.d("HCEFService(NFC-F)", commandPacket.toHex())
        //response
        val len =
            PACKET_LENGTH_SIZE + RESPONSE_SIZE + IDM_SIZE + STATUS_FLAG1_SIZE + STATUS_FLAG2_SIZE + BLOCK_NUM_SIZE + BLOCK_SIZE
        val headLen =
            PACKET_LENGTH_SIZE + RESPONSE_SIZE + IDM_SIZE + STATUS_FLAG1_SIZE + STATUS_FLAG2_SIZE
        var responsePacket: ByteArray? = ByteArray(len)
        responsePacket!![PACKET_LENGTH_POS] = len.toByte()
        responsePacket[COMMAND_TYPE_POS] = RES_READ_WITHOUT_ENCRYPTION
        responsePacket[headLen - 2] = STATUS_FLAG1_SUCCESS
        responsePacket[headLen - 1] = STATUS_FLAG2_SUCCESS
        responsePacket[headLen] = ONE_BLOCK.toByte()

        //IDmをセット
        responsePacket = setIDmToPacket(responsePacket, getIDm(commandPacket))

        //analyze
        val serviceNum = commandPacket[10].toInt() and 0xFF
        if (serviceNum != 1) {
            responsePacket[len - 2] = STATUS_FLAG1_FAILED
            responsePacket[len - 1] = STATUS_FLAG2_SERVICE_NUM_ERROR
            Log.d("HCEFService(NFC-F)", "SERVICE_NUM_ERROR")
            return responsePacket //ERROR RES
        }
        val serviceCode =
            commandPacket[11].toInt() and 0xFF or (commandPacket[12].toInt() and 0xFF shl 8)
        Log.d("HCEFService(NFC-F)", String.format("Service code : %04X,", serviceCode))
        if (serviceCode != SERVICE_CODE_READ_ONLY) {
            responsePacket[len - 2] = STATUS_FLAG1_FELICA_LITE_ERROR
            responsePacket[len - 1] = STATUS_FLAG2_SERVICE_CODE
            Log.d("HCEFService(NFC-F)", "STATUS_FLAG2_SERVICE_CODE")
            return responsePacket //ERROR RES
        }
        val blockNum = commandPacket[13].toInt() and 0xFF
        if (blockNum != 1) {
            responsePacket[len - 2] = STATUS_FLAG1_FAILED
            responsePacket[len - 1] = STATUS_FLAG2_BLOCK_NUM_ERROR
            Log.d("HCEFService(NFC-F)", "BLOCK_NUM_ERROR")
            return responsePacket //ERROR RES
        }
        val blockAddress: Int
        when (commandPacket[14].toInt() and 0xFF) {
            0x80 -> {
                //1byte
                blockAddress = commandPacket[15].toInt() and 0xFF
            }

            0x00 -> {
                //2byte
                blockAddress =
                    commandPacket[15].toInt() and 0xFF or (commandPacket[16].toInt() and 0xFF shl 8)
            }

            else -> {
                responsePacket[len - 2] = STATUS_FLAG1_FAILED
                responsePacket[len - 1] = STATUS_FLAG2_ACCESS_MODE
                Log.d("HCEFService(NFC-F)", "SERVICE_NUM_ERROR")
                return responsePacket //ERROR RES
            }
        }
        Log.d("HCEFService(NFC-F)", String.format("Block Address : %04X,", blockAddress))
        val data = byteArrayOf(
            0x00,
            0x01,
            0x02,
            0x03,
            0x04,
            0x05,
            0x06,
            0x07,
            0x08,
            0x09,
            0x0A,
            0x0B,
            0x0C,
            0x0D,
            0x0E,
            0x0F
        )
        System.arraycopy(data, 0, responsePacket, headLen + BLOCK_NUM_SIZE, BLOCK_SIZE)
        var debug = ""
        for (i in responsePacket.indices) {
            debug += String.format("%02X,", responsePacket[i])
        }
        Log.d("HCEFService(NFC-F)", debug)
        Log.d("HCEFService(NFC-F)", "SUCCESS")
        return responsePacket
    }

    /*
     * LEN_OF_RES, CMD, IDM[8], COUNT(SYS), Array<Service>
     */
    private fun requestSystemCode(commandPacket: ByteArray): ByteArray? {
        Log.d("HCEFService(NFC-F)", "RequestSystemCode")
        Log.d("HCEFService(NFC-F)", commandPacket.toHex())
        //response
        val systemCodes = arrayOf(byteArrayOf(b(0x8a), b(0xc3)), byteArrayOf(b(0xfe), b(0x00)))
        val len =
            PACKET_LENGTH_SIZE + RESPONSE_SIZE + IDM_SIZE + SYSTEM_CODE_NUM_SIZE + (SYSTEM_CODE_SIZE * systemCodes.size)
        val headLen = PACKET_LENGTH_SIZE + RESPONSE_SIZE + IDM_SIZE
        var responsePacket = ByteArray(len)
        responsePacket[PACKET_LENGTH_POS] = len.toByte()
        responsePacket[COMMAND_TYPE_POS] = RES_REQUEST_SYSTEM_CODE
        responsePacket[headLen] = systemCodes.size.toByte()

        //IDmをセット
        responsePacket = setIDmToPacket(responsePacket, getIDm(commandPacket))

        // System Codeをセット
        for (i in systemCodes.indices) {
            System.arraycopy(
                systemCodes[i],
                0,
                responsePacket,
                headLen + 1 + (i * SYSTEM_CODE_SIZE),
                SYSTEM_CODE_SIZE
            )
        }

        var debug = ""
        for (i in responsePacket.indices) {
            debug += String.format("%02x", responsePacket[i])
        }
        Log.d("HCEFService(NFC-F)", debug)
        Log.d("HCEFService(NFC-F)", "SUCCESS")
        return responsePacket
    }


    private fun getIDm(commandPacket: ByteArray): ByteArray {
        val idm = ByteArray(8)
        System.arraycopy(commandPacket, 2, idm, 0, 8)
        return idm
    }

    private fun setIDmToPacket(packet: ByteArray, IDm: ByteArray): ByteArray {
        System.arraycopy(IDm, 0, packet, IDM_POS, IDM_SIZE) // NFC-ID2
        return packet
    }
}