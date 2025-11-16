package com.example.mypanicapp

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class RecordingsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private var mediaPlayer: MediaPlayer? = null
    private var recordings: MutableList<File> = mutableListOf()
    private lateinit var adapter: RecordingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recordings)

        recyclerView = findViewById(R.id.recordingsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        loadRecordings()
    }

    private fun loadRecordings() {
        val dir = File(getExternalFilesDir(null), "PanicEvidence")
        recordings = if (dir.exists() && dir.isDirectory) {
            dir.listFiles { file -> file.extension == "3gp" }?.toMutableList() ?: mutableListOf()
        } else {
            mutableListOf()
        }
        adapter = RecordingsAdapter(recordings,
            onPlay = { playRecording(it) },
            onDelete = { deleteRecording(it) })
        recyclerView.adapter = adapter
    }

    private fun playRecording(file: File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
        }
        Toast.makeText(this, "Playing: ${file.name}", Toast.LENGTH_SHORT).show()
    }

    private fun deleteRecording(file: File) {
        mediaPlayer?.release()
        if (file.delete()) {
            recordings.remove(file)
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Deleted: ${file.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to delete: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}

class RecordingsAdapter(
    private val recordings: List<File>,
    private val onPlay: (File) -> Unit,
    private val onDelete: (File) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.bind(recordings[position], onPlay, onDelete)
    }

    override fun getItemCount() = recordings.size

    class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(file: File, onPlay: (File) -> Unit, onDelete: (File) -> Unit) {
            val fileNameText = itemView.findViewById<TextView>(R.id.fileNameText)
            val playButton = itemView.findViewById<ImageButton>(R.id.playButton)
            val deleteButton = itemView.findViewById<ImageButton>(R.id.deleteButton)
            fileNameText.text = file.name
            playButton.setOnClickListener { onPlay(file) }
            deleteButton.setOnClickListener { onDelete(file) }
        }
    }
} 