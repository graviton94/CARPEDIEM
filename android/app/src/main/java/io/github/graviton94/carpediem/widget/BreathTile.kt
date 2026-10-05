package io.github.graviton94.carpediem.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.graviton94.carpediem.MainActivity
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.DayPart
import java.time.LocalDateTime

/** 숨 바로가기 (C1): 알림 · 타일이 함께 쓰는 ‘지금 때의 숨 1분’ 열기. */
internal fun breathIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(MainActivity.EXTRA_OPEN, "breath")

/** 지금 때의 숨 이름 (하루를 여는 숨 → 잠시 쉬어가는 호흡 → …). */
internal fun breathName(context: Context, now: LocalDateTime): String = context.getString(when (DayPart.of(now.hour)) {
    DayPart.MORNING -> R.string.breath_part_morning; DayPart.DAY -> R.string.breath_part_day
    DayPart.EVENING -> R.string.breath_part_evening; DayPart.NIGHT -> R.string.breath_part_night
})

/** 빠른 설정 타일 ‘숨, 쉼’ (C1): 누르면 알림 창이 접히며 숨 1분이 바로. */
class BreathTile : TileService() {
    override fun onStartListening() {
        qsTile?.apply { state = Tile.STATE_INACTIVE; label = getString(R.string.breathTile_label); if (Build.VERSION.SDK_INT >= 29) subtitle = getString(R.string.breathTile_sub); updateTile() }
    }

    @Suppress("DEPRECATION")
    override fun onClick() {
        val intent = breathIntent(this)
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 7, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        else startActivityAndCollapse(intent)
    }
}
