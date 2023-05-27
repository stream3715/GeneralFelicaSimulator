package tech.oliet.generalfelicasimulator

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class HCEFServiceUnitTest {
    @Test
    fun addition_isCorrect() {
        val target = HCEFService()
        val str = "16060114c7480b210a10010b10048000800780088009"
        val ret = target.processNfcFPacket(str.decodeHex(), null)
        val expected = "4d070114c7480b210a1000000430313030303035313233453030313530303130313531303030310000000000003531303130353030000000000000000000000000000000000000000000000000".decodeHex()
        assertArrayEquals(ret, expected)
    }

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}