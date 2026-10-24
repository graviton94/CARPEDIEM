package io.github.graviton94.carpediem.data

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.Build
import android.os.ParcelFileDescriptor
import io.github.graviton94.carpediem.core.Backup
import java.io.File

/**
 * 폰 백업 (1.1.10): 기록 (shared_prefs/carpediem.xml) 은 늘, 한 줄의 사진은 한도 안에서 최근 날부터.
 * 구글 클라우드 자동 백업은 앱당 한도 (보통 25MB) 를 넘으면 그 앱 백업 전체가 멈추므로, 한도의 [CLOUD_SHARE] 까지만 사진을 채운다.
 * 폰끼리 옮기기 (기기 간 이전) 는 한도가 없어 사진을 모두. 되살리기는 기본 동작 (백업에 든 파일을 제자리로).
 * 백업 규칙 XML 은 두지 않음: 규칙의 폴더 (files/photos) 는 되살릴 때 그 폴더가 이미 있어야 받아들여서, 새 폰에서는 사진이 모두 버려졌음.
 */
class GardenBackupAgent : BackupAgent() {
    override fun onFullBackup(data: FullBackupDataOutput) {
        val prefs = File(dataDir, "shared_prefs/$PREFS")
        if (prefs.exists()) fullBackupFile(prefs, data)
        val toDevice = Build.VERSION.SDK_INT >= 28 && (data.transportFlags and FLAG_DEVICE_TO_DEVICE_TRANSFER) != 0
        val budget = if (toDevice) Long.MAX_VALUE else (data.quota * CLOUD_SHARE).toLong() - prefs.length()
        val dir = File(filesDir, "photos")
        val files = dir.listFiles()?.map { it.name to it.length() }.orEmpty()
        Backup.photosWithin(files, budget).forEach { fullBackupFile(File(dir, it), data) }
    }

    // 자동 백업만 씀 (키-값 백업은 쓰지 않음)
    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) = Unit
    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) = Unit

    private companion object {
        const val PREFS = "carpediem.xml"
        const val CLOUD_SHARE = 0.8
    }
}
