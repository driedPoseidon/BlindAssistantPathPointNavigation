package com.example.blind_people

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var outputStream: OutputStream? = null

    // State to toggle streaming
    private var isStreaming = mutableStateOf(false)

    // REPLACE WITH YOUR LAPTOP'S ACTUAL IP
    private val LAPTOP_IP = "192.168.1.XX"
    private val PORT = 5005

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // 1. Setup UI
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(onToggle = { active -> isStreaming.value = active })
                }
            }
        }

        // 2. Connect to Laptop in Background
        thread {
            try {
                val socket = Socket(LAPTOP_IP, PORT)
                outputStream = socket.getOutputStream()
                Log.d("Network", "SUCCESS: Connected to Laptop")
            } catch (e: Exception) {
                Log.e("Network", "FAILURE: Could not connect. Is the laptop script running?")
            }
        }

        // 3. Handle Permissions
        val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) startCamera()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    @Composable
    fun MainScreen(onToggle: (Boolean) -> Unit) {
        var running by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (running) "TRANSMITTING DATA" else "STANDBY MODE",
                style = MaterialTheme.typography.headlineMedium,
                color = if (running) Color(0xFF4CAF50) else Color.Gray
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    running = !running
                    onToggle(running)
                },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (running) Color(0xFFF44336) else Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (running) "STOP" else "START CAMERA FEED", style = MaterialTheme.typography.titleLarge)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (isStreaming.value) {
                    sendFrame(imageProxy)
                } else {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, imageAnalysis)
            } catch (e: Exception) {
                Log.e("Camera", "Binding failed")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun sendFrame(imageProxy: ImageProxy) {
        try {
            // Physical Device Fix: Convert to Bitmap then to JPEG
            val bitmap = imageProxy.toBitmap()
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream) // 40% quality for high speed
            val bytes = stream.toByteArray()

            outputStream?.let {
                it.write(ByteBuffer.allocate(4).putInt(bytes.size).array())
                it.write(bytes)
                it.flush()
            }
        } catch (e: Exception) {
            Log.e("Network", "Send failed")
        } finally {
            imageProxy.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        outputStream?.close()
    }
}
