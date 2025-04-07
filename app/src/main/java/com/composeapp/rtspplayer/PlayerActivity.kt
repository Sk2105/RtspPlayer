package com.composeapp.rtspplayer

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

class PlayerActivity : AppCompatActivity() {
    private lateinit var server:String
    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private var _isRecording = false


    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipReceiver, IntentFilter("ACTION_CLOSE_PIP"), RECEIVER_NOT_EXPORTED)
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
        binding.recording.setOnClickListener {
            if (_isRecording) {
                stopRecording()
            } else {
                startRecording(server)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.pictureInPicture.visibility = View.VISIBLE
        } else {
            binding.pictureInPicture.visibility = View.GONE
        }


        binding.pictureInPicture.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                onPause()
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
                "--rtsp-tcp", // Use TCP instead of UDP
                "--no-drop-late-frames",
                "--no-skip-frames",
                "--vout=android-display",
                "--verbose=2"
            )

            libVlc =
                LibVLC(this, options)

            mediaPlayer = MediaPlayer(libVlc).apply {
                // Set up the media (RTSP URL)
                attachViews(
                    binding.surfaceView,
                    null,
                    false,
                    false
                )
            }

            val media = Media(libVlc, Uri.parse(server)).apply {
                setHWDecoderEnabled(true, false)
                addOption(":codec=mediacodec")
                addOption(":no-mediacodec-dr")
                addOption(":low-delay")
                addOption(":skip-frames")
                addOption("--no-drop-late-frames")
                addOption("--no-skip-frames")

            }

            // release previous media safely
            mediaPlayer.media?.release()

            mediaPlayer.media = media
            mediaPlayer.play()

            mediaPlayer.setEventListener { event ->
                Log.d("RTSP", "Event: ${event}")
            }
        } catch (e: Exception) {
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
                )
                .setCloseAction(
                    RemoteAction(
                        Icon.createWithResource(this, R.drawable.baseline_close_24),
                        "Close",
                        "Close",
                        closeIntent
                    )
                )

                .build()
        )

    }


    override fun onPause() {
        if (_isRecording) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        stopStream()
        super.onBackPressed()
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