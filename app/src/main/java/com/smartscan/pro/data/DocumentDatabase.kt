package com.smartscan.pro.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val folderName: String = "Scanned",
    val imagePaths: String, // Pipe '|' separated permanent file paths
    val pdfPath: String? = null,
    val pageCount: Int = 1,
    val extractedText: String = "",
    val fileSizeBytes: Long = 0L,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun pagePathList(): List<String> =
        imagePaths.split("|").map { it.trim() }.filter { it.isNotEmpty() }
}

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query(
        """
        SELECT * FROM documents
        WHERE title LIKE '%' || :query || '%'
           OR folderName LIKE '%' || :query || '%'
           OR extractedText LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
        """
    )
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)
}

@Database(
    entities = [DocumentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SmartScanDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: SmartScanDatabase? = null

        fun getInstance(context: Context): SmartScanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartScanDatabase::class.java,
                    "smart_scan_offline.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
