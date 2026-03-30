package com.massmediasaver.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.massmediasaver.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = viewModel(),
    onShowDownloads: () -> Unit = {},
    onShowArchives: () -> Unit = {}
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val pageTitle by viewModel.pageTitle.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val archiveProgress by viewModel.archiveProgress.collectAsState()
    val downloadComplete by viewModel.downloadComplete.collectAsState()
    val archiveComplete by viewModel.archiveComplete.collectAsState()
    val error by viewModel.error.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotEmpty() && urlInput.isEmpty()) {
            urlInput = currentUrl
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(downloadComplete) {
        downloadComplete?.let {
            snackbarHostState.showSnackbar("Download complete!")
            viewModel.clearDownloadProgress()
        }
    }

    LaunchedEffect(archiveComplete) {
        if (archiveComplete) {
            snackbarHostState.showSnackbar("Archive saved successfully!")
            viewModel.clearArchiveProgress()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pageTitle.ifEmpty { "Mass Media Saver" }) },
                actions = {
                    IconButton(onClick = onShowDownloads) {
                        Icon(Icons.Default.Download, contentDescription = "Downloads")
                    }
                    IconButton(onClick = onShowArchives) {
                        Icon(Icons.Default.Archive, contentDescription = "Archives")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // URL Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter URL") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            val url = if (urlInput.startsWith("http")) urlInput else "https://$urlInput"
                            viewModel.updateUrl(url)
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        val url = if (urlInput.startsWith("http")) urlInput else "https://$urlInput"
                        viewModel.updateUrl(url)
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Go")
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.downloadAllMedia() },
                    modifier = Modifier.weight(1f),
                    enabled = mediaItems.isNotEmpty() && downloadProgress == null
                ) {
                    if (downloadProgress != null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download All")
                }

                Button(
                    onClick = { viewModel.savePageComplete() },
                    modifier = Modifier.weight(1f),
                    enabled = archiveProgress == null
                ) {
                    if (archiveProgress != null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Page")
                }
            }

            // Progress indicator
            if (downloadProgress != null) {
                LinearProgressIndicator(
                    progress = downloadProgress!!.current.toFloat() / downloadProgress!!.total.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
                Text(
                    text = "Downloading ${downloadProgress!!.current}/${downloadProgress!!.total}: ${downloadProgress!!.currentFile}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (archiveProgress != null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
                Text(
                    text = archiveProgress!!,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Media count
            if (mediaItems.isNotEmpty()) {
                Text(
                    text = "${mediaItems.size} media items found (${mediaItems.count { it.type == com.massmediasaver.data.MediaType.IMAGE }} images, ${mediaItems.count { it.type == com.massmediasaver.data.MediaType.VIDEO }} videos)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // WebView
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                viewModel.updateLoading(false)
                                url?.let { viewModel.updateUrl(it) }
                                view?.title?.let { viewModel.updateTitle(it) }

                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        return document.documentElement.outerHTML;
                                    })();
                                    """.trimIndent()
                                ) { html ->
                                    if (!html.isNullOrEmpty()) {
                                        val cleanHtml = html
                                            .replace("\\u003C", "<")
                                            .replace("\\\\", "")
                                            .removeSurrounding("\"")
                                        viewModel.updateHtml(cleanHtml)
                                        viewModel.extractMediaFromPage(cleanHtml, url ?: "")
                                    }
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                viewModel.updateLoading(newProgress < 100)
                            }
                        }
                    }
                },
                update = { webView ->
                    if (currentUrl.isNotEmpty() && webView.url != currentUrl) {
                        webView.loadUrl(currentUrl)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )

            // Loading indicator
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
