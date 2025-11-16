package com.example.mypanicapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener

class MainActivity : AppCompatActivity() {

    private val SMS_PERMISSION_REQUEST_CODE = 101
    private val LOCATION_PERMISSION_REQUEST_CODE = 102
    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 103
    private val VOICE_RECOGNITION_REQUEST_CODE = 200
    private lateinit var statusText: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private val RECORDING_DURATION_MS = 10000L // 15 seconds
    private val triggerPhrases = mapOf(
        "en" to "i need help",
        "bn" to "আমাকে সাহায্য করুন",
        "hi" to "मुझे मदद चाहिए"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val panicButton: Button = findViewById(R.id.panicButton)
        val settingsButton: ImageButton = findViewById(R.id.settingsButton)
        val micButton: ImageButton = findViewById(R.id.micButton)
        val recordingsButton: Button = findViewById(R.id.recordingsButton)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        panicButton.setOnClickListener {
            checkPermissionsAndSendSMSWithLocation()
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        micButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_PERMISSION_REQUEST_CODE
                )
            } else {
                startVoiceRecognitionIntent()
            }
        }

        recordingsButton.setOnClickListener {
            val intent = Intent(this, RecordingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissionsAndSendSMSWithLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE)
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST_CODE)
        } else {
            sendSMSWithLocation()
        }
    }

    private fun sendSMSWithLocation() {
        statusText.text = "Getting location..."
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            sendSMS("Emergency! I need help!")
            checkAudioPermissionAndStartRecording()
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                var message = "Emergency! I need help!"
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    val mapsLink = "http://maps.google.com/?q=$lat,$lon"
                    message += "\n\nMy current location: $mapsLink"
                }
                sendSMS(message)
                checkAudioPermissionAndStartRecording()
            }
            .addOnFailureListener {
                sendSMS("Emergency! I need help!")
                checkAudioPermissionAndStartRecording()
            }
    }

    private fun sendSMS(message: String) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val json = prefs.getString("favorite_contacts", "[]") ?: "[]"
        val arr = JSONArray(json)
        if (arr.length() == 0) {
            statusText.text = "No favorite contacts saved."
            Toast.makeText(this, "No favorite contacts saved.", Toast.LENGTH_LONG).show()
            return
        }
            val smsManager: SmsManager = SmsManager.getDefault()
        var sentAny = false
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val number = obj.getString("number")
            try {
                smsManager.sendTextMessage(number, null, message, null, null)
                sentAny = true
        } catch (e: Exception) {
                statusText.text = "Failed to send SMS to $number: ${e.message}"
                Toast.makeText(this, "SMS failed to $number: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            }
        }
        if (sentAny) {
            statusText.text = "SMS sent to all favorite contacts!"
            Toast.makeText(this, "Emergency SMS sent!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAudioPermissionAndStartRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST_CODE)
        } else {
            startAudioRecording()
        }
    }

    private fun startAudioRecording() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "panic_audio_${timestamp}.3gp"
            val storageDir = File(getExternalFilesDir(null), "PanicEvidence")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            audioFilePath = File(storageDir, fileName).absolutePath

        mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
                statusText.text = "Recording audio..."
            Toast.makeText(this, "Recording audio...", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    stopAudioRecording()
                }, RECORDING_DURATION_MS)
        } catch (e: Exception) {
            statusText.text = "Audio recording failed: ${e.message}"
            Toast.makeText(this, "Audio recording failed.", Toast.LENGTH_LONG).show()
                e.printStackTrace()
                releaseMediaRecorder()
        }
    }

    private fun stopAudioRecording() {
        try {
        mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
                statusText.text = "Audio recording saved: ${audioFilePath}"
            Toast.makeText(this, "Audio recorded and saved to: ${audioFilePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            statusText.text = "Failed to stop audio: ${e.message}"
            Toast.makeText(this, "Failed to stop audio recording.", Toast.LENGTH_LONG).show()
                e.printStackTrace()
        }
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun startVoiceRecognitionIntent() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val selectedLocale = prefs.getString("selected_language", "en") ?: "en"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLocale)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your panic phrase")
        try {
            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE)
        } catch (e: Exception) {
            statusText.text = "Speech recognition not available."
            Toast.makeText(this, "Speech recognition not available.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = matches?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""
            statusText.text = "Heard: $recognized"
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val selectedLocale = prefs.getString("selected_language", "en") ?: "en"
            val trigger = triggerPhrases[selectedLocale]?.lowercase(Locale.getDefault()) ?: "i need help"
            if (recognized.contains(trigger) || trigger.contains(recognized)) {
                statusText.text = "Trigger phrase detected! Sending SMS..."
                checkPermissionsAndSendSMSWithLocation()
            } else {
                statusText.text = "Phrase not recognized."
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaRecorder()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SMS_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkPermissionsAndSendSMSWithLocation()
                } else {
                    statusText.text = "SMS permission denied. Cannot send alerts."
                    Toast.makeText(this, "SMS permission denied.", Toast.LENGTH_SHORT).show()
                }
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    sendSMSWithLocation()
                } else {
                    sendSMS("Emergency! I need help!")
                    checkAudioPermissionAndStartRecording()
                }
            }
            RECORD_AUDIO_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startVoiceRecognitionIntent()
                } else {
                    statusText.text = "Microphone permission denied. Cannot use voice panic."
                    Toast.makeText(this, "Microphone permission denied.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}