package ru.shum.testtg1

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ru.shum.testtg1.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice.close()
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        requestPermission()

        binding.takePhotoBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                binding.textureView.visibility = View.VISIBLE
                binding.captureBtn.visibility = View.VISIBLE
                binding.takePhotoBtn.visibility = View.GONE

                binding.textureView.surfaceTextureListener = textureListener
            } else {
                requestPermission()
            }
        }

        binding.captureBtn.setOnClickListener {
            takePicture()

            binding.textureView.visibility = View.GONE
            binding.captureBtn.visibility = View.GONE
            binding.takePhotoBtn.visibility = View.VISIBLE
        }

    }

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    private fun openCamera() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                cameraManager.openCamera(cameraId, cameraStateCallback, null)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun startCameraPreview() {
        val surfaceTexture = binding.textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(binding.textureView.width, binding.textureView.height)
        val surface = Surface(surfaceTexture)
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        cameraDevice.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_MODE,
                        CameraMetadata.CONTROL_MODE_AUTO
                    )
                    captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            null
        )
    }

    private fun takePicture() {
        val bitmap = binding.textureView.bitmap
        val filename = "${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            try {
                val outputStream = resolver.openOutputStream(uri)
                outputStream.use {
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, it!!)
                }
                Toast.makeText(this, "Photo saved to gallery", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("MainActivity", "Failed to save image: ${e.message}")
            }
        } else {
            Log.e("MainActivity", "Failed to create new MediaStore record.")
        }
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }
}