package com.example.simulated.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.simulated.gallery.GalleryPhoto
import com.example.simulated.gallery.GalleryStorage
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.Executors

enum class CameraFlashOption(val imageCaptureMode: Int) {
    OFF(ImageCapture.FLASH_MODE_OFF),
    AUTO(ImageCapture.FLASH_MODE_AUTO),
    TORCH(ImageCapture.FLASH_MODE_ON);

    fun next(): CameraFlashOption = when (this) {
        OFF -> AUTO
        AUTO -> TORCH
        TORCH -> OFF
    }
}

@Composable
fun CameraApp(
    onOpenGallery: () -> Unit = {},
    onMarkupPhoto: ((android.graphics.Bitmap) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashOption by remember { mutableStateOf(CameraFlashOption.OFF) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(4f) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var latestPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }
    var reviewPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    BackHandler(enabled = reviewPhoto != null) {
        reviewPhoto = null
    }

    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            captureExecutor.shutdown()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        val existing = GalleryStorage.getAllPhotos(context)
        if (existing.isNotEmpty()) {
            latestPhoto = existing.first()
        }
    }

    // Bind CameraX once per (previewView, lensFacing, hasCameraPermission) change
    LaunchedEffect(previewView, lensFacing, hasCameraPermission, lifecycleOwner) {
        val pv = previewView ?: return@LaunchedEffect
        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = pv.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(flashOption.imageCaptureMode)
                    .build()
                imageCapture = capture

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture
                )
                boundCamera = camera
                val zState = camera.cameraInfo.zoomState.value
                minZoomRatio = zState?.minZoomRatio ?: 1f
                maxZoomRatio = zState?.maxZoomRatio ?: 4f
                zoomRatio = 1f.coerceIn(minZoomRatio, maxZoomRatio)
                camera.cameraControl.setZoomRatio(zoomRatio)
                camera.cameraControl.enableTorch(flashOption == CameraFlashOption.TORCH && lensFacing == CameraSelector.LENS_FACING_BACK)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    // Update flash/torch dynamically without rebinding
    LaunchedEffect(flashOption, boundCamera, lensFacing) {
        imageCapture?.flashMode = flashOption.imageCaptureMode
        try {
            boundCamera?.cameraControl?.enableTorch(
                flashOption == CameraFlashOption.TORCH && lensFacing == CameraSelector.LENS_FACING_BACK
            )
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.camera_permission_rationale),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.testTag("grant_camera_permission_btn")
                ) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        } else {
            // Live CameraX Preview with Pinch-to-Zoom
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView = it }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(minZoomRatio, maxZoomRatio, boundCamera) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) {
                                val nextZoom = (zoomRatio * zoom).coerceIn(minZoomRatio, maxZoomRatio)
                                zoomRatio = nextZoom
                                try {
                                    boundCamera?.cameraControl?.setZoomRatio(nextZoom)
                                } catch (_: Exception) {}
                            }
                        }
                    }
            )

            // Top Camera Toolbar (Flash & Zoom)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { flashOption = flashOption.next() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("camera_flash_btn")
                ) {
                    Icon(
                        imageVector = when (flashOption) {
                            CameraFlashOption.OFF -> Icons.Default.FlashOff
                            CameraFlashOption.AUTO -> Icons.Default.FlashAuto
                            CameraFlashOption.TORCH -> Icons.Default.FlashOn
                        },
                        contentDescription = stringResource(R.string.camera_flash_toggle),
                        tint = if (flashOption == CameraFlashOption.TORCH) Color(0xFFFFD54F) else Color.White
                    )
                }

                // Interactive Zoom Chip (tap to cycle 1x / 2x)
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clickable {
                            val target = if (zoomRatio < 1.8f && maxZoomRatio >= 2f) 2f else 1f
                            zoomRatio = target.coerceIn(minZoomRatio, maxZoomRatio)
                            try {
                                boundCamera?.cameraControl?.setZoomRatio(zoomRatio)
                            } catch (_: Exception) {}
                        }
                        .testTag("camera_zoom_chip")
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1fx", zoomRatio),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom camera controls bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 24.dp, horizontal = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Latest gallery thumbnail
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable(onClick = onOpenGallery)
                            .testTag("camera_to_gallery_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (latestPhoto != null) {
                            AsyncImage(
                                model = latestPhoto!!.uri,
                                contentDescription = "Gallery photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = Color.White
                            )
                        }
                    }

                    // Shutter button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(enabled = !isCapturing) {
                                val capture = imageCapture
                                if (capture != null) {
                                    isCapturing = true
                                    capture.takePicture(
                                        captureExecutor,
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(image: ImageProxy) {
                                                val buffer: ByteBuffer = image.planes[0].buffer
                                                val bytes = ByteArray(buffer.remaining())
                                                buffer.get(bytes)
                                                image.close()

                                                val saved = GalleryStorage.savePhoto(context, bytes)
                                                ContextCompat.getMainExecutor(context).execute {
                                                    latestPhoto = saved
                                                    reviewPhoto = saved
                                                    isCapturing = false
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                ContextCompat.getMainExecutor(context).execute {
                                                    isCapturing = false
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            .testTag("camera_shutter_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Switch camera lens (front / back)
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("switch_camera_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Instant Photo Review Overlay
        reviewPhoto?.let { photo ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .padding(16.dp)
                    .testTag("camera_instant_review")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { reviewPhoto = null },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_button), tint = Color.White)
                        }

                        Text(
                            text = stringResource(R.string.camera_review_title),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        IconButton(
                            onClick = {
                                GalleryStorage.deletePhoto(photo)
                                latestPhoto = GalleryStorage.getAllPhotos(context).firstOrNull()
                                reviewPhoto = null
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_button), tint = Color.White)
                        }
                    }

                    AsyncImage(
                        model = photo.uri,
                        contentDescription = photo.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            reviewPhoto = null
                            onOpenGallery()
                        }) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.app_gallery), color = Color.White)
                        }

                        if (onMarkupPhoto != null) {
                            Button(onClick = {
                                val bmp = photo.file?.let { BitmapFactory.decodeFile(it.absolutePath) }
                                if (bmp != null) {
                                    reviewPhoto = null
                                    onMarkupPhoto(bmp)
                                }
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.markup_title))
                            }
                        }

                        Button(onClick = { reviewPhoto = null }) {
                            Text(stringResource(R.string.close_button))
                        }
                    }
                }
            }
        }
    }
}
