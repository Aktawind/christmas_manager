package com.aktawind.monnoel.core.backup.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aktawind.monnoel.core.backup.format.BackupArchive
import com.aktawind.monnoel.core.backup.format.BackupData
import com.aktawind.monnoel.core.backup.format.EditionDto
import com.aktawind.monnoel.core.database.MonNoelDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupDataSourceTest {
    private lateinit var db: MonNoelDatabase
    private lateinit var source: BackupDataSource

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MonNoelDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        source = BackupDataSource(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `export puis import restaure exactement les donnees`() = runTest {
        val data = BackupData(
            editions = listOf(
                EditionDto("a", 2025, "ARCHIVED", null, 1L, 2L),
                EditionDto("b", 2026, "ACTIVE", 30_000, 3L, 4L),
            ),
        )
        source.replaceAll(data)
        val out = ByteArrayOutputStream()
        BackupArchive.write(out, source.snapshot(), null, "0.1.0", 1, Instant.EPOCH)

        source.replaceAll(BackupData())
        assertEquals(0, source.snapshot().editions.size)

        source.replaceAll(BackupArchive.read(ByteArrayInputStream(out.toByteArray()), null).data)
        assertEquals(data.editions.sortedBy { it.year }, source.snapshot().editions.sortedBy { it.year })
    }

    @Test
    fun `un import invalide ne modifie rien`() = runTest {
        val original = BackupData(editions = listOf(EditionDto("a", 2025, "ACTIVE", null, 1L, 1L)))
        source.replaceAll(original)
        // Deux années identiques : violation de contrainte unique, la transaction est annulée.
        val invalid = BackupData(
            editions = listOf(
                EditionDto("x", 2030, "ACTIVE", null, 1L, 1L),
                EditionDto("y", 2030, "ACTIVE", null, 1L, 1L),
            ),
        )
        assertFailsWith<Exception> { source.replaceAll(invalid) }
        assertEquals(original.editions, source.snapshot().editions)
    }
}
