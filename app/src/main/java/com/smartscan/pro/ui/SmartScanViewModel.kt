package com.smartscan.pro.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartscan.pro.data.DocumentEntity
import com.smartscan.pro.data.SmartScanDatabase
import com.smartscan.pro.engine.ImageEnhancer
import com.smartscan.pro.engine.PdfBuildOptions
import com.smartscan.pro.engine.PdfStudioEngine
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

    private val _pdfStudioDoc = MutableStateFlow<DocumentEntity?>(null)
    val pdfStudioDoc: StateFlow<DocumentEntity?> = _pdfStudioDoc.asStateFlow()

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

    fun openPdfStudio(document: DocumentEntity) {
        _pdfStudioDoc.value = document
    }

    fun closePdfStudio() {
        _pdfStudioDoc.value = null
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

    fun buildPdfForDocument(
        document: DocumentEntity,
        options: PdfBuildOptions,
        onSuccess: (DocumentEntity, File) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = withContext(Dispatchers.IO) {
                val ctx = getApplication<Application>()
                val pages = document.pagePathList()
                val pdfFile = PdfStudioEngine.createPdfFromImages(
                    context = ctx,
                    imagePaths = pages,
                    baseFileName = document.title,
                    options = options
                ) ?: return@withContext null

                // Purana temporary PDF clean karein agar alag file hai
                document.pdfPath?.let { oldPath ->
                    if (oldPath != pdfFile.absolutePath) {
                        runCatching { File(oldPath).delete() }
                    }
                }

                val updatedDoc = document.copy(
                    pdfPath = pdfFile.absolutePath,
                    fileSizeBytes = ImageEnhancer.calculateTotalSize(pages, pdfFile.absolutePath)
                )
                dao.updateDocument(updatedDoc)
                updatedDoc to pdfFile
            }
            _isBusy.value = false

            if (result != null) {
                _pdfStudioDoc.value = result.first
                onSuccess(result.first, result.second)
            } else {
                onError("PDF banane mein error aaya. Pages check karein.")
            }
        }
    }

    fun extractSelectedPages(
        sourceDoc: DocumentEntity,
        selectedIndices: List<Int>,
        newTitle: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (selectedIndices.isEmpty()) {
            onError("Kam se kam ek page select karein.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val createdTitle = withContext(Dispatchers.IO) {
                val ctx = getApplication<Application>()
                val allPages = sourceDoc.pagePathList()
                val docsDir = ImageEnhancer.getDocumentsDir(ctx)

                val copiedPaths = selectedIndices.mapNotNull { idx ->
                    val srcPath = allPages.getOrNull(idx) ?: return@mapNotNull null
                    val srcFile = File(srcPath)
                    if (!srcFile.exists()) return@mapNotNull null
                    val destFile = File(docsDir, "split_${System.currentTimeMillis()}_$idx.jpg")
                    srcFile.copyTo(destFile, overwrite = true)
                    destFile.absolutePath
                }
                if (copiedPaths.isEmpty()) return@withContext null

                val safeTitle = newTitle.trim().ifBlank { "${sourceDoc.title}_Split" }
                val pdfFile = PdfStudioEngine.createPdfFromImages(
                    context = ctx,
                    imagePaths = copiedPaths,
                    baseFileName = safeTitle,
                    options = PdfBuildOptions()
                )
                val newDoc = DocumentEntity(
                    title = safeTitle,
                    folderName = sourceDoc.folderName,
                    imagePaths = copiedPaths.joinToString("|"),
                    pdfPath = pdfFile?.absolutePath,
                    pageCount = copiedPaths.size,
                    fileSizeBytes = ImageEnhancer.calculateTotalSize(copiedPaths, pdfFile?.absolutePath)
                )
                dao.insertDocument(newDoc)
                safeTitle
            }
            _isBusy.value = false

            if (createdTitle != null) {
                onSuccess("\"$createdTitle\" naye document ke roop mein save ho gaya.")
            } else {
                onError("Pages extract karne mein problem aayi.")
            }
        }
    }

    fun mergeDocuments(
        selectedDocs: List<DocumentEntity>,
        mergedTitle: String,
        onSuccess: (DocumentEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        if (selectedDocs.size < 2) {
            onError("Merge karne ke liye kam se kam 2 documents chunein.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val mergedDoc = withContext(Dispatchers.IO) {
                val ctx = getApplication<Application>()
                val docsDir = ImageEnhancer.getDocumentsDir(ctx)
                val combinedPaths = mutableListOf<String>()

                selectedDocs.forEachIndexed { dIdx, doc ->
                    doc.pagePathList().forEachIndexed { pIdx, path ->
                        val srcFile = File(path)
                        if (srcFile.exists()) {
                            val copyFile = File(docsDir, "merge_${System.currentTimeMillis()}_${dIdx}_$pIdx.jpg")
                            srcFile.copyTo(copyFile, overwrite = true)
                            combinedPaths.add(copyFile.absolutePath)
                        }
                    }
                }
                if (combinedPaths.isEmpty()) return@withContext null

                val safeTitle = mergedTitle.trim().ifBlank { "Merged_SmartScan" }
                val pdfFile = PdfStudioEngine.createPdfFromImages(
                    context = ctx,
                    imagePaths = combinedPaths,
                    baseFileName = safeTitle,
                    options = PdfBuildOptions()
                )
                val entity = DocumentEntity(
                    title = safeTitle,
                    folderName = "Scanned",
                    imagePaths = combinedPaths.joinToString("|"),
                    pdfPath = pdfFile?.absolutePath,
                    pageCount = combinedPaths.size,
                    fileSizeBytes = ImageEnhancer.calculateTotalSize(combinedPaths, pdfFile?.absolutePath)
                )
                val newId = dao.insertDocument(entity)
                entity.copy(id = newId)
            }
            _isBusy.value = false

            if (mergedDoc != null) {
                _pdfStudioDoc.value = mergedDoc
                onSuccess(mergedDoc)
            } else {
                onError("Documents merge nahi ho paaye.")
            }
        }
    }

    fun importExternalPdf(
        pdfUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val savedTitle = withContext(Dispatchers.IO) {
                val ctx = getApplication<Application>()
                val pagePaths = PdfStudioEngine.renderExternalPdfToImages(ctx, pdfUri)
                if (pagePaths.isEmpty()) return@withContext null

                val savedPdf = ImageEnhancer.copyUriToPermanentPdf(ctx, pdfUri)?.absolutePath
                val stamp = SimpleDateFormat("ddMMM_HHmm", Locale.getDefault()).format(Date())
                val title = "ImportedPDF_$stamp"
                dao.insertDocument(
                    DocumentEntity(
                        title = title,
                        folderName = "Scanned",
                        imagePaths = pagePaths.joinToString("|"),
                        pdfPath = savedPdf,
                        pageCount = pagePaths.size,
                        fileSizeBytes = ImageEnhancer.calculateTotalSize(pagePaths, savedPdf)
                    )
                )
                title
            }
            _isBusy.value = false

            if (savedTitle != null) {
                onSuccess("\"$savedTitle\" (${documents.value.firstOrNull()?.pageCount ?: 1} pages) import ho gaya.")
            } else {
                onError("PDF file read nahi ho paayi (shayed password locked ya corrupt hai).")
            }
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
