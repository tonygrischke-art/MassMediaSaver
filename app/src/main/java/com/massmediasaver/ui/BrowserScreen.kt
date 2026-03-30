package com.massmediasaver.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalActivity
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
    modifier: Modifier = Modifier
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

    var urlInput by remember { mutableStateOf("https://www.example.com") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val focusManager = LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }
    val canGoBack = remember { mutableStateOf(false) }

    val activity = LocalActivity.current

    LaunchedEffect(webView) {
        webView?.let { wv ->
            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    canGoBack.value = view?.canGoBack() ?: false
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
        }
    }

    DisposableEffect(Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView?.canGoBack() == true) {
                    webView?.goBack()
                } else {
                    isEnabled = false
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(callback)
        onDispose { }
    }

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
                navigationIcon = {
                    IconButton(
                        onClick = { webView?.goBack() },
                        enabled = canGoBack.value
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.downloadAllMedia() },
                        enabled = mediaItems.isNotEmpty() && downloadProgress == null
                    ) {
                        if (downloadProgress != null) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Download All Media")
                        }
                    }
                    IconButton(
                        onClick = { viewModel.savePageComplete() },
                        enabled = archiveProgress == null
                    ) {
                        if (archiveProgress != null) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save Page Complete")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                    label = { Text("Enter webpage URL") },
                    placeholder = { Text("Paste or type a URL and tap Go") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            val url = if (urlInput.startsWith("http")) urlInput else "https://$urlInput"
                            viewModel.updateUrl(url)
                            webView?.loadUrl(url)
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
                        webView?.loadUrl(url)
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Go")
                }
            }

            if (downloadProgress != null) {
                LinearProgressIndicator(
                    progress = downloadProgress!!.current.toFloat() / downloadProgress!!.total.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
                Text(
                    text = "Downloading ${downloadProgress!!.current}/${downloadProgress!!.total}: ${downloadProgress!!.currentFile}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (archiveProgress != null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
                Text(
                    text = archiveProgress!!,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (mediaItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${mediaItems.size} media items (${mediaItems.count { it.type == com.massmediasaver.data.MediaType.IMAGE }} images, ${mediaItems.count { it.type == com.massmediasaver.data.MediaType.VIDEO }} videos)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = { viewModel.clearMediaItems() }) {
                        Text("Clear")
                    }
                }
            }

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
                        settings.displayZoomControls = false

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                viewModel.updateLoading(newProgress < 100)
                            }
                        }

                        loadUrl("https://www.example.com")
                        webView = this
                    }
                },
                update = { },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
