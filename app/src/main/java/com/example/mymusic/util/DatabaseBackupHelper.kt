package com.example.mymusic.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DatabaseBackupHelper {
    private const val DB_NAME = "app_database"

    // 백업 Intent (ZIP 파일 생성)
    @JvmStatic
    fun getBackupIntent(): Intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/zip"
        putExtra(Intent.EXTRA_TITLE, "backup_$DB_NAME.zip")
    }

    // 복원 Intent (ZIP 파일 선택)
    @JvmStatic
    fun getRestoreIntent(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/zip"
    }

    /** DB → ZIP으로 백업 */
    @JvmStatic
    fun backupDatabaseToUri(context: Context, uri: Uri): Boolean {
        return try {
            val dbDir = context.getDatabasePath(DB_NAME).parentFile!!
            val files = arrayOf(DB_NAME, "$DB_NAME-wal", "$DB_NAME-shm")
            context.contentResolver.openOutputStream(uri)?.use { output ->
                ZipOutputStream(BufferedOutputStream(output)).use { zos ->
                    for (name in files) {
                        val file = File(dbDir, name)
                        if (file.exists()) {
                            FileInputStream(file).use { fis ->
                                zos.putNextEntry(ZipEntry(name))
                                fis.copyTo(zos)
                                zos.closeEntry()
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** ZIP → DB로 복원 */
    @JvmStatic
    fun restoreDatabaseFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val dbDir = context.getDatabasePath(DB_NAME).parentFile!!
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(BufferedInputStream(input)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(dbDir, entry.name)
                        FileOutputStream(outFile, false).use { zis.copyTo(it) }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** 앱 재시작 */
    @JvmStatic
    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    /** 런처 초기화 (백업) */
    @JvmStatic
    fun initBackupLauncher(activity: AppCompatActivity): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data!!.data
                val success = backupDatabaseToUri(activity, uri!!)
                Toast.makeText(activity, if (success) "DB 백업 성공" else "DB 백업 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 런처 초기화 (복원) */
    @JvmStatic
    fun initRestoreLauncher(activity: AppCompatActivity): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data!!.data
                val success = restoreDatabaseFromUri(activity, uri!!)
                Toast.makeText(activity, if (success) "DB 복원 성공\n앱을 재시작하세요" else "DB 복원 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 개발용: 백업 실행 */
    @JvmStatic
    fun startBackup(activity: AppCompatActivity, launcher: ActivityResultLauncher<Intent>) {
        launcher.launch(getBackupIntent())
    }

    /** 개발용: 복원 실행 */
    @JvmStatic
    fun startRestore(activity: AppCompatActivity, launcher: ActivityResultLauncher<Intent>) {
        launcher.launch(getRestoreIntent())
    }

    // Fragment 버전
    @JvmStatic
    fun initBackupLauncher(fragment: Fragment): ActivityResultLauncher<Intent> {
        return fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data!!.data
                val success = backupDatabaseToUri(fragment.requireContext(), uri!!)
                Toast.makeText(fragment.requireContext(), if (success) "DB 백업 성공" else "DB 백업 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JvmStatic
    fun initRestoreLauncher(fragment: Fragment): ActivityResultLauncher<Intent> {
        return fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data!!.data
                val success = restoreDatabaseFromUri(fragment.requireContext(), uri!!)
                Toast.makeText(fragment.requireContext(), if (success) "DB 복원 성공\n앱을 재시작하세요" else "DB 복원 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
