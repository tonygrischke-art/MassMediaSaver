package com.massmediasaver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.massmediasaver.ui.BrowserScreen
import com.massmediasaver.ui.DisclaimerDialog
import com.massmediasaver.ui.FilesScreen
import com.massmediasaver.ui.MassMediaSaverTheme
import com.massmediasaver.viewmodel.BrowserViewModel
import com.massmediasaver.viewmodel.DownloadsViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Browser : Screen("browser", "Browser", Icons.Default.Language)
    object Downloads : Screen("downloads", "Downloads", Icons.Default.Download)
    object Archives : Screen("archives", "Archives", Icons.Default.Archive)
}

class MainActivity : ComponentActivity() {

    private var hasAcceptedDisclaimer by mutableStateOf(false)

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
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
                    val browserViewModel: BrowserViewModel = viewModel()
                    val downloadsViewModel: DownloadsViewModel = viewModel()
                    var currentRoute by remember { mutableStateOf(Screen.Browser.route) }

                    if (!hasAcceptedDisclaimer) {
                        DisclaimerDialog(
                            onAccept = {
                                hasAcceptedDisclaimer = true
                            }
                        )
                    } else {
                        Scaffold(
                            bottomBar = {
                                NavigationBar {
                                    listOf(
                                        Screen.Browser,
                                        Screen.Downloads,
                                        Screen.Archives
                                    ).forEach { screen ->
                                        NavigationBarItem(
                                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                                            label = { Text(screen.title) },
                                            selected = currentRoute == screen.route,
                                            onClick = { currentRoute = screen.route }
                                        )
                                    }
                                }
                            }
                        ) { paddingValues ->
                            when (currentRoute) {
                                Screen.Browser.route -> BrowserScreen(
                                    viewModel = browserViewModel,
                                    modifier = Modifier.padding(paddingValues)
                                )
                                Screen.Downloads.route -> {
                                    downloadsViewModel.setSelectedTab(0)
                                    FilesScreen(
                                        viewModel = downloadsViewModel,
                                        modifier = Modifier.padding(paddingValues)
                                    )
                                }
                                Screen.Archives.route -> {
                                    downloadsViewModel.setSelectedTab(1)
                                    FilesScreen(
                                        viewModel = downloadsViewModel,
                                        modifier = Modifier.padding(paddingValues)
                                    )
                                }
                            }
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
