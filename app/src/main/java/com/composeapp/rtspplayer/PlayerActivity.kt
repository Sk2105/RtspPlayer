package com.composeapp.rtspplayer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.composeapp.rtspplayer.databinding.ActivityVideoPlayerBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.HWDecoderUtil

class PlayerActivity : AppCompatActivity() {
    private lateinit var server:String
    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private var _isRecording = false


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipReceiver, IntentFilter("ACTION_CLOSE_PIP"), RECEIVER_NOT_EXPORTED)
        }else{
            registerReceiver(pipReceiver, IntentFilter("ACTION_CLOSE_PIP"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intent = intent
        server = intent.getStringExtra("server") ?: server
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()


    }

    private fun initView() {
        binding.serverText.text = server
        binding.surfaceView.keepScreenOn = true
        binding.recording.setOnClickListener {
            if (_isRecording) {
                stopRecording()
            } else {
                startRecording(server)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.pictureInPicture.visibility = View.VISIBLE
        } else {
            binding.pictureInPicture.visibility = View.GONE
        }


        binding.pictureInPicture.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                onPause()
            }else{
                Toast.makeText(this, "Your Device is not supported for PIP.", Toast.LENGTH_SHORT).show()
            }
        }

        startStreaming()


    }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "ACTION_CLOSE_PIP") {
                Toast.makeText(this@PlayerActivity, "Stream stopped", Toast.LENGTH_SHORT).show()
                stopStream()
                finish()  // Closes the activity
            }
        }

    }

    private fun startStreaming() {
        try {
            val options = arrayListOf(
                "--rtsp-tcp", // Use TCP for RTSP
                "--no-drop-late-frames",
                "--no-skip-frames",
                "--vout=android-display",
                "--verbose=2"
            )

            // Initialize LibVLC
            libVlc = LibVLC(this, options)

            // Initialize MediaPlayer
            mediaPlayer = MediaPlayer(libVlc).apply {

                attachViews(binding.surfaceView, null, false, false)
            }

            // Create Media instance with the RTSP URL
            val media = Media(libVlc, Uri.parse(server)).apply {
                setHWDecoderEnabled(true, false)
                addOption(":codec=mediacodec")
                addOption(":no-mediacodec-dr")
                addOption(":network-caching=1000")
                addOption(":rtsp-frame-buffer-size=100000")
                addOption(":file-caching=1000")
                addOption(":live-caching=1000")
                addOption(":clock-jitter=0")
                addOption(":clock-synchro=0")
            }

            // Release previous media (if any)
            mediaPlayer.media?.release()

            // Assign media to player and play
            mediaPlayer.media = media
            mediaPlayer.play()

            // Optional: Log events
            mediaPlayer.setEventListener { event ->
                Log.d("RTSP", "Event type: ${event.type}, timeChanged: ${event.timeChanged}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("RTSP", "Error ${e.message}")
            Toast.makeText(
                this,
                "Error ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.recording.visibility = View.VISIBLE
        binding.pictureInPicture.visibility = View.VISIBLE
        startStreaming()

    }

    override fun onStop() {
        unregisterReceiver(pipReceiver)
        super.onStop()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPIPMode() {
        val closeIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent("ACTION_CLOSE_PIP"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        startStreaming()
        binding.recording.visibility = View.GONE
        binding.pictureInPicture.visibility = View.GONE
        this.enterPictureInPictureMode(
            PictureInPictureParams.Builder()
                .setAspectRatio(
                    Rational.parseRational("4:3")
                ).setActions(
                    listOf(
                        RemoteAction(
                            Icon.createWithResource(
                                this,
                                R.drawable.baseline_close_24
                            ),
                            "Close",
                            "Close",
                            closeIntent
                        )
                    )
                )
                .build()
        )

    }


    override fun onPause() {
        if (_isRecording) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPIPMode()
        } else {
            stopStream()
        }
        super.onPause()
    }

    override fun onDestroy() {
        stopStream()
        super.onDestroy()
    }

    private fun stopStream() {
        mediaPlayer.stop()
        Toast.makeText(this, "Stream stopped", Toast.LENGTH_SHORT).show()
    }

    private fun startRecording(rtspUrl: String) {
        try {
            stopStream()


            val media = Media(libVlc, Uri.parse(rtspUrl)).apply {
                setHWDecoderEnabled(true, false)

                addOption(":sout=#duplicate{dst=display,dst=standard{access=file,mux=mp4,dst=/sdcard/Download/${System.currentTimeMillis()}.mp4}}")
                addOption(":sout-keep")
                addOption(":no-sout-all")
                addOption(":sout-avformat-mux=mp4")
                addOption("--no-drop-late-frames")
                addOption("--no-skip-frames")
                addOption(":codec=mediacodec")
                addOption(":no-mediacodec-dr")
                addOption(":low-delay")
                addOption(":skip-frames")
            }

            mediaPlayer.media?.release()
            mediaPlayer.media = media
            mediaPlayer.play()
            _isRecording = true

            binding.recording.text = "Stop Recording"
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
            Log.d("RTSP", "Recording started")

        } catch (e: Exception) {
            Log.e("RTSP", "Error starting recording", e)
            Toast.makeText(this, "Error starting recording", Toast.LENGTH_SHORT).show()
        }
    }


    private fun stopRecording() {
        try {
            if (_isRecording) {
                mediaPlayer.stop()
                _isRecording = false
                binding.recording.text = "Start Recording"
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
                Log.d("RTSP", "Recording stopped")
                startStreaming()
            }
        } catch (e: Exception) {
            Log.e("RTSP", "Error stopping recording", e)
            Toast.makeText(this, "Error stopping recording", Toast.LENGTH_SHORT).show()
        }
    }
}