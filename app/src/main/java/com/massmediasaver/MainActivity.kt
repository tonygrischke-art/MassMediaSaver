package com.massmediasaver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.massmediasaver.ui.BrowserScreen
import com.massmediasaver.ui.DisclaimerDialog
import com.massmediasaver.ui.FilesScreen
import com.massmediasaver.ui.MassMediaSaverTheme
import com.massmediasaver.viewmodel.BrowserViewModel
import com.massmediasaver.viewmodel.DownloadsViewModel

class MainActivity : ComponentActivity() {

    private var hasAcceptedDisclaimer by mutableStateOf(false)
    private var needsStoragePermission by mutableStateOf(false)

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            needsStoragePermission = false
        } else {
            Toast.makeText(this, "Storage permission is required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            MassMediaSaverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("browser") }
                    val browserViewModel: BrowserViewModel = viewModel()
                    val downloadsViewModel: DownloadsViewModel = viewModel()

                    if (!hasAcceptedDisclaimer) {
                        DisclaimerDialog(
                            onAccept = {
                                hasAcceptedDisclaimer = true
                            }
                        )
                    } else {
                        when (currentScreen) {
                            "browser" -> BrowserScreen(
                                viewModel = browserViewModel,
                                onShowDownloads = { currentScreen = "downloads" },
                                onShowArchives = { currentScreen = "archives" }
                            )
                            "downloads" -> FilesScreen(
                                viewModel = downloadsViewModel,
                                onNavigateBack = { currentScreen = "browser" }
                            )
                            "archives" -> FilesScreen(
                                viewModel = downloadsViewModel,
                                onNavigateBack = { currentScreen = "browser" }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            storagePermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
