package com.shohan.pro.downloader.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohan.pro.downloader.LinkDownloaderApp
import com.shohan.pro.downloader.ui.dialogs.DownloadResolutionDialog
import com.shohan.pro.downloader.ui.theme.LinkDownloaderTheme

class FloatingDownloadActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "extra_download_url"

        fun start(context: Context, url: String) {
            val intent = Intent(context, FloatingDownloadActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        }
    }

    private val viewModel: MainViewModel by viewModels {
        val app = application as LinkDownloaderApp
        MainViewModel.Factory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetUrl = intent?.getStringExtra(EXTRA_URL)
        if (targetUrl.isNullOrBlank()) {
            finish()
            return
        }

        viewModel.analyzeUrl(targetUrl)

        setContent {
            LinkDownloaderTheme {
                val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
                val analyzedMedia by viewModel.analyzedMedia.collectAsStateWithLifecycle()
                val selectedOption by viewModel.selectedOption.collectAsStateWithLifecycle()
                val customFileName by viewModel.customFileName.collectAsStateWithLifecycle()
                val showDialog by viewModel.showResolutionDialog.collectAsStateWithLifecycle()

                // If user dismissed dialog, finish activity
                LaunchedEffect(showDialog, isAnalyzing) {
                    if (!showDialog && !isAnalyzing && analyzedMedia == null) {
                        finish()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (analyzedMedia != null && showDialog) {
                        DownloadResolutionDialog(
                            mediaInfo = analyzedMedia!!,
                            selectedOption = selectedOption,
                            fileName = customFileName,
                            onFileNameChange = { viewModel.updateCustomFileName(it) },
                            onOptionSelected = { viewModel.selectResolutionOption(it) },
                            onDownloadNow = {
                                viewModel.startDownloadNow(this@FloatingDownloadActivity)
                                Toast.makeText(this@FloatingDownloadActivity, "Download Started 🚀", Toast.LENGTH_SHORT).show()
                                finish()
                            },
                            onAddToQueue = {
                                viewModel.addToQueue(this@FloatingDownloadActivity)
                                Toast.makeText(this@FloatingDownloadActivity, "Added to Download Queue 📥", Toast.LENGTH_SHORT).show()
                                finish()
                            },
                            onDismiss = {
                                viewModel.dismissResolutionDialog()
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = intent.getStringExtra(EXTRA_URL)
        if (!url.isNullOrBlank()) {
            viewModel.analyzeUrl(url)
        }
    }
}
