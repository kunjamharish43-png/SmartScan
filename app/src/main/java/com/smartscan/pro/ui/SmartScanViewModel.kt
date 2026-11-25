package com.smartscan.pro.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartscan.pro.data.DocumentEntity
import com.smartscan.pro.data.SmartScanDatabase
import com.smartscan.pro.engine.ImageEnhancer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EditorSession(
    val existingDocument: DocumentEntity? = null,
    val initialTitle: String,
    val folderName: String,
    val pagePaths: List<String>,
    val pdfPath: String? = null
)

class SmartScanViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = SmartScanDatabase.getInstance(application).documentDao()

    val documents: StateFlow<List<DocumentEntity>> = dao.getAllDocuments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _editorSession = MutableStateFlow<EditorSession?>(null)
    val editorSession: StateFlow<EditorSession?> = _editorSession.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun startEditorFromUris(
        imageUris: List<Uri>,
        pdfUri: Uri?,
        modeLabel: String,
        onError: (String) -> Unit
    ) {
        if (imageUris.isEmpty()) {
            onError("Koi image select ya scan nahi hui.")
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            val result = withContext(Dispatchers.IO) {
                val ctx = getApplication<Application>()
                val savedImages = imageUris.mapNotNull { uri ->
                    ImageEnhancer.copyUriToPermanentImage(ctx, uri)?.absolutePath
                }
                val savedPdf = pdfUri?.let { ImageEnhancer.copyUriToPermanentPdf(ctx, it)?.absolutePath }
                savedImages to savedPdf
            }
            _isBusy.value = false

            val (paths, savedPdfPath) = result
            if (paths.isEmpty()) {
                onError("Image save karne mein problem aayi. Dobara try karein.")
                return@launch
            }

            val stamp = SimpleDateFormat("ddMMM_HHmm", Locale.getDefault()).format(Date())
            val cleanPrefix = modeLabel.replace(" ", "").take(12)
            _editorSession.value = EditorSession(
                existingDocument = null,
                initialTitle = "${cleanPrefix}_$stamp",
                folderName = if (modeLabel.contains("ID", ignoreCase = true)) "ID Cards" else "Scanned",
                pagePaths = paths,
                pdfPath = savedPdfPath
            )
        }
    }

    fun openExistingDocumentInEditor(document: DocumentEntity) {
        val pages = document.pagePathList().filter { File(it).exists() }
        if (pages.isEmpty()) return
        _editorSession.value = EditorSession(
            existingDocument = document,
            initialTitle = document.title,
            folderName = document.folderName,
            pagePaths = pages,
            pdfPath = document.pdfPath
        )
    }

    fun closeEditor() {
        _editorSession.value = null
    }

    fun saveEditorResult(
        session: EditorSession,
        finalTitle: String,
        finalFolder: String,
        finalPagePaths: List<String>,
        onSaved: (String) -> Unit
    ) {
        viewModelScope.launch {
            val safeTitle = finalTitle.trim().ifEmpty { session.initialTitle }
            val safeFolder = finalFolder.trim().ifEmpty { "Scanned" }

            withContext(Dispatchers.IO) {
                val totalBytes = ImageEnhancer.calculateTotalSize(finalPagePaths, session.pdfPath)
                val existing = session.existingDocument
                if (existing != null) {
                    dao.updateDocument(
                        existing.copy(
                            title = safeTitle,
                            folderName = safeFolder,
                            imagePaths = finalPagePaths.joinToString("|"),
                            pageCount = finalPagePaths.size,
                            fileSizeBytes = totalBytes
                        )
                    )
                } else {
                    dao.insertDocument(
                        DocumentEntity(
                            title = safeTitle,
                            folderName = safeFolder,
                            imagePaths = finalPagePaths.joinToString("|"),
                            pdfPath = session.pdfPath,
                            pageCount = finalPagePaths.size,
                            fileSizeBytes = totalBytes
                        )
                    )
                }
            }
            _editorSession.value = null
            onSaved("\"$safeTitle\" offline library mein save ho gaya.")
        }
    }

    fun renameDocument(document: DocumentEntity, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateDocument(document.copy(title = clean))
        }
    }

    fun toggleFavorite(document: DocumentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateDocument(document.copy(isFavorite = !document.isFavorite))
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            document.pagePathList().forEach { path ->
                runCatching { File(path).delete() }
            }
            document.pdfPath?.let { path ->
                runCatching { File(path).delete() }
            }
            dao.deleteDocument(document)
        }
    }
}
