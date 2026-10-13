package io.github.graviton94.carpediem.widget

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.graviton94.carpediem.MainActivity
import io.github.graviton94.carpediem.R

/**
 * 빠른 설정 타일 ‘돌멍하기’ (1.1.6): 잠들기 전 알림 창을 내려 누르면 정원에서 “돌멍하러 갈까요?” 한 번 (바로가기 · 숨 타일과 같은 길).
 * 잠들기 바퀴는 돌멍하기 안에서 고른다.
 */
class GazeTile : TileService() {
    override fun onStartListening() {
        qsTile?.apply { state = Tile.STATE_INACTIVE; label = getString(R.string.gaze); updateTile() }
    }

    @Suppress("DEPRECATION")
    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(MainActivity.EXTRA_OPEN, "gaze")
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 8, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        else startActivityAndCollapse(intent)
    }
}
