package tech.oliet.generalfelicasimulator

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.NfcFCardEmulation
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException


internal data class Card(val name: String, val idm: String, val sys: String)


class MainActivity : AppCompatActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var nfcFCardEmulation: NfcFCardEmulation? = null
    private var myComponentName: ComponentName? = null

    private lateinit var editTextIDm: EditText
    private lateinit var editTextSys: EditText

    private lateinit var tableLayoutCard: TableLayout

    private val gson = Gson()
    private var cards = mutableListOf<Card>()

    private val jsonPath = "cards.json"
    private lateinit var jsonFile: File
    private var requestingLocationUpdates = false

    private val requestPermissionLauncher = registerForActivityResult<String, Boolean>(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        //⑦ ユーザーが権限を許可したか
        if (isGranted) {
            //⑧a 制限された機能にアクセスする
            requestingLocationUpdates = true
        } else {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextIDm = findViewById(R.id.editTextIDm)
        editTextSys = findViewById(R.id.editTextSys)

        tableLayoutCard = findViewById(R.id.tableLayoutCard)

        val btnUpdate = findViewById<Button>(R.id.button_update)
        btnUpdate.setOnClickListener {
            val idm = editTextIDm.text.toString()
            val sys = editTextSys.text.toString()

            val resultIdm = setIDm(idm)
            val resultSys = setSys(sys)

            if (resultIdm && resultSys) {
                Toast.makeText(applicationContext, "Updated: $idm $sys", Toast.LENGTH_LONG).show()
            }
            if (!resultIdm) {
                Toast.makeText(
                    applicationContext,
                    "${getString(R.string.error_invalid)} ${getString(R.string.idm)}",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
            if (!resultSys) {
                Toast.makeText(
                    applicationContext,
                    "${getString(R.string.error_invalid)} ${getString(R.string.system_code)}",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }

        val btnSave = findViewById<Button>(R.id.button_save)
        btnSave.setOnClickListener {
            val idm = editTextIDm.text.toString()
            val sys = editTextSys.text.toString()

            val editTextName = EditText(this)
            editTextName.hint = "name"

            AlertDialog.Builder(this)
                .setTitle("Save")
                .setMessage("input card name")
                .setView(editTextName)
                .setPositiveButton("OK") { _, _ ->
                    addCard(Card(editTextName.text.toString(), idm, sys))
                }
                .setNegativeButton("cancel") { _, _ -> }
                .setCancelable(false)
                .show()
        }

        jsonFile = File(filesDir, jsonPath)

        loadCards()
        drawCards()

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF)) {
            Log.e("GeneralFelicaSimulator", "HCE-F is not supported")
            AlertDialog.Builder(this)
                .setTitle("Error").setMessage("HCE-F is not supported").setCancelable(false).show()
            btnUpdate.isEnabled = false
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NFC
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            //⑧a 制限された機能にアクセスする
            requestingLocationUpdates = true
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.NFC
            )
        ) {
            val builder = AlertDialog.Builder(this)

            fun toastMake(str: String) {
                val toast = Toast.makeText(this, str, Toast.LENGTH_SHORT)
                toast.show()
            }

            //⑤b 権限が必要な理由・メリットを説明
            builder.setMessage("hoge")
                .setPositiveButton("OK",
                    DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                        requestPermissionLauncher.launch(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    })
                .setNegativeButton("no_thanks") { dialog, id -> toastMake("fuga") }
            builder.create()
            builder.show()
        } else {
            //⑥ システム権限を要求する
            requestPermissionLauncher.launch(
                Manifest.permission.NFC
            )
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter?.isEnabled != true) {
            Log.e("GeneralFelicaSimulator", "NFC is off")
            AlertDialog.Builder(this)
                .setTitle("Error").setMessage("NFC is off").setCancelable(false).show()
            return
        }

        nfcFCardEmulation = NfcFCardEmulation.getInstance(nfcAdapter)
        myComponentName = ComponentName(
            "tech.oliet.generalfelicasimulator",
            "tech.oliet.generalfelicasimulator.HCEFService"
        )
    }

    private fun setIDm(idm: String): Boolean {
        nfcFCardEmulation?.disableService(this)
        val resultIdm = nfcFCardEmulation?.setNfcid2ForService(myComponentName, idm)
        nfcFCardEmulation?.enableService(this, myComponentName)
        return resultIdm == true
    }

    private fun setSys(sys: String): Boolean {
        nfcFCardEmulation?.disableService(this)
        val resultSys = nfcFCardEmulation?.registerSystemCodeForService(myComponentName, sys)
        nfcFCardEmulation?.enableService(this, myComponentName)
        return resultSys == true
    }

    private fun addCard(card: Card) {
        cards.add(card)
        saveCards()
        drawCards()
    }

    private fun saveCards() {
        try {
            jsonFile.writeText(gson.toJson(cards).toString())
        } catch (e: IOException) {
            Log.e("Error", "Save File Write Error")
        }
    }

    private fun loadCards() {
        val mutableListCard = object : TypeToken<MutableList<Card>>() {}.type
        try {
            val jsonCards = gson.fromJson<MutableList<Card>>(jsonFile.readText(), mutableListCard)
            if (jsonCards != null) {
                cards = jsonCards
            }
        } catch (e: IOException) {
            Log.e("Error", "Save File Read Error")
        } catch (e: JsonSyntaxException) {
            Log.e("Error", "Save File Syntax Error")
        }
    }

    private fun drawCards() {
        tableLayoutCard.removeAllViews()

        var i = 1

        for (card in cards) {
            val tableRowCard = layoutInflater.inflate(R.layout.table_row_card, null)

            var name = card.name

            if (name == "") {
                name = getString(R.string.untitled)
                if (i > 1) {
                    name += " $i"
                }
                i++
            }

            tableRowCard.findViewById<TextView>(R.id.card_name).text = name
            tableRowCard.findViewById<TextView>(R.id.card_idm).text = card.idm
            tableRowCard.findViewById<TextView>(R.id.card_sys).text = card.sys
            tableRowCard.findViewById<ImageButton>(R.id.card_delete)
                .setOnClickListener {
                    AlertDialog.Builder(this).apply {
                        setTitle("Confirm")
                        setMessage("delete card?")
                        setPositiveButton("OK") { _, _ ->
                            cards.remove(card)
                            saveCards()
                            drawCards()
                        }
                        setNegativeButton("NO") { _, _ -> }
                    }.show()
                }
            tableRowCard.setOnClickListener {
                editTextIDm.setText(card.idm)
                editTextSys.setText(card.sys)
            }

            tableLayoutCard.addView(
                tableRowCard,
                TableLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()

        val orgSys = nfcFCardEmulation?.getSystemCodeForService(myComponentName)

        if (setSys("8AC3")) {
            findViewById<TextView>(R.id.textViewNoticeUnUnlocked).visibility = View.INVISIBLE
        }

        if (orgSys != null) {
            setSys(orgSys)
        }

        nfcFCardEmulation?.enableService(this, myComponentName)
    }

    override fun onPause() {
        super.onPause()
        nfcFCardEmulation?.disableService(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val idm = editTextIDm.text.toString()
        val sys = editTextSys.text.toString()

        outState.putString("IDm", idm)
        outState.putString("Sys", sys)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val idm = savedInstanceState.getString("IDm")
        val sys = savedInstanceState.getString("Sys")

        editTextIDm.setText(idm)
        editTextSys.setText(sys)
    }
}
