package com.composeapp.rtspplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.composeapp.rtspplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        requestPermission()
    }


    private fun initViews() {
        binding.launchPlayerButton.setOnClickListener {
            launchPlayer()
        }
    }

    private fun requestPermission() {
        requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
    }

    private fun launchPlayer() {
        if (binding.rtspUrl.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter RTSP URL", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, PlayerActivity::class.java).apply {
            val server = binding.rtspUrl.text.toString().trim()
            putExtra(
                "server",
                server
            )
        }
        startActivity(intent)
        finish()
    }
}