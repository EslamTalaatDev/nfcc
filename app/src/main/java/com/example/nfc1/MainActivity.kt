package com.example.nfc1

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.*
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException


class MainActivity : AppCompatActivity() {

    companion object {
        var ERROR_DECETED = "No Nfc Tag Detected"
        var WRITE_SUCCESS = "Text Written Successfuly"
        var WRITE_ERROR = "Error during writing,try again"
    }

    var nfcAdapter: NfcAdapter? = null
    lateinit var pendingIntent: PendingIntent
    lateinit var writingTagFilters: IntentFilter
    var writeMode = false
    var myTag: Tag? = null

    lateinit var edit_message: EditText
    lateinit var nfc_details: TextView
    lateinit var activateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edit_message = findViewById(R.id.edit_message)
        nfc_details = findViewById(R.id.nfc_data)
        activateButton = findViewById(R.id.activateButton)
        activateButton.setOnClickListener {
            try {
                if (myTag == null) {
                    Toast.makeText(this@MainActivity, ERROR_DECETED, Toast.LENGTH_SHORT).show()
                } else {
                    write("PlainText|" + edit_message.text.toString(), myTag!!)
                    Toast.makeText(this@MainActivity, WRITE_SUCCESS, Toast.LENGTH_SHORT).show()

                }

            } catch (e: IOException) {
                Toast.makeText(this@MainActivity, WRITE_ERROR, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } catch (e: FormatException) {
                Toast.makeText(this@MainActivity, WRITE_ERROR, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this@MainActivity, "Device Not Supported", Toast.LENGTH_SHORT).show()
        }
        readFromIntent(intent)
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            0
        )
        var tagDetected = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT)
        writingTagFilters = tagDetected
    }


    fun write(text:String ,tag:Tag){
        var records= createRecord(text) as Array<NdefRecord?>

        var message =NdefMessage(records)
        var ndef=Ndef.get(tag)
        ndef.connect()
        ndef.writeNdefMessage(message)
        ndef.close()
    }

    private fun readFromIntent(intent: Intent) {
        var action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action
            || NfcAdapter.ACTION_TECH_DISCOVERED == action
            || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
        ) {
            var rawMsgs=intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            var msgs: Array<NdefMessage?>?=null
            if(rawMsgs!=null){
                msgs = arrayOfNulls<NdefMessage>(rawMsgs.size)
               for(i in 0 until rawMsgs.size){
                   msgs[i]= rawMsgs[i] as NdefMessage?
               }
            }
            buildTagViews(msgs)

        }

    }
    fun buildTagViews(msgs: Array<NdefMessage?>?) {
        if(msgs == null || msgs.size==0) return

        val record = msgs[0]?.records!![0]

        val payload = record.payload
        val text = String(payload)
        Log.e("tag", "vahid$text")
        nfc_details.text="Nfc Content=${text}"


    }

    fun createRecord(text:String):NdefRecord {
        var lang="en"
        var textBytes=text.toByteArray()
        var langBytes=lang.toByteArray(charset("US-ASCII"))
        var langLenght=langBytes.size
        var textLenght=textBytes.size
        var payload= byteArrayOf((1+langLenght+textLenght).toByte())

        payload[0]= langLenght.toByte()
        System.arraycopy(langBytes,0,payload,1,langLenght)
        System.arraycopy(textBytes,0,payload,1+langLenght,langLenght)
        var recordNfc=NdefRecord(NdefRecord.TNF_WELL_KNOWN,NdefRecord.RTD_TEXT, byteArrayOf(0),payload)
        return recordNfc
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        readFromIntent(intent!!)
        if(NfcAdapter.ACTION_TAG_DISCOVERED == intent.action){
            myTag=intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
    }

    override fun onPause() {
        super.onPause()
        WriteModeOff()
    }

    override fun onResume() {
        super.onResume()
        WriteModeOn()
    }
    fun WriteModeOn(){
        writeMode=true
        nfcAdapter?.enableForegroundDispatch(this,pendingIntent, arrayOf(writingTagFilters),null)
    }
    fun WriteModeOff(){
        writeMode=false
        nfcAdapter?.disableForegroundDispatch(this)
    }

}