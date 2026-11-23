package com.coreui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale
import java.util.concurrent.Executor

private val Ink = Color(0xFF101114)
private val Accent = Color(0xFF9BEAFF)

class MainActivity : ComponentActivity() {
    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) showCamera = true }
    private var showCamera by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Color(0xFF202329),
                primary = Accent, onPrimary = Ink
            )) {
                if (showCamera) CameraScreen(ContextCompat.getMainExecutor(this))
                else PermissionScreen { permissionRequest.launch(Manifest.permission.CAMERA) }
            }
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CoreUI Camera", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text("Для работы камеры требуется разрешение.", color = Color.LightGray)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest) { Text("Разрешить доступ") }
        }
    }
}

@Composable
private fun CameraScreen(executor: Executor) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var zoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Text("CoreUI Camera", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(14.dp))
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(30.dp)),
                factory = { context ->
                    PreviewView(context).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        val future = ProcessCameraProvider.getInstance(context)
                        future.addListener({
                            val provider = future.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(surfaceProvider)
                            }
                            try {
                                provider.unbindAll()
                                val bound = provider.bindToLifecycle(
                                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
                                )
                                camera = bound
                                bound.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
                                    maxZoom = minOf(15f, state.maxZoomRatio)
                                    zoom = zoom.coerceIn(1f, maxZoom)
                                }
                            } catch (_: Exception) { }
                        }, executor)
                    }
                }
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Увеличение", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(String.format(Locale.US, "%.1fx", zoom),
                    color = Accent, style = MaterialTheme.typography.titleLarge)
            }
            Slider(value = zoom, onValueChange = { value ->
                zoom = value
                camera?.cameraControl?.setZoomRatio(value.coerceIn(1f, maxZoom))
            }, valueRange = 1f..15f, steps = 139, enabled = camera != null)
            Text(
                if (maxZoom < 15f) "Максимум этой камеры: ${String.format(Locale.US, "%.1f", maxZoom)}x"
                else "Цифровой зум: 1–15x",
                color = Color.LightGray, style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(1f, 2f, 5f, 10f, 15f).forEach { preset ->
                    FilledTonalButton(
                        onClick = { zoom = preset; camera?.cameraControl?.setZoomRatio(preset) },
                        enabled = preset <= maxZoom,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text("${preset.toInt()}x") }
                }
            }
        }
    }
}
