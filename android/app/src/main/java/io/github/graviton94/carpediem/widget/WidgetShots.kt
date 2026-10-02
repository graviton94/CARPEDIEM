package io.github.graviton94.carpediem.widget

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import java.io.File

/**
 * 디버그 빌드 전용 (cd.widgetShots): 위젯 고르는 화면의 미리보기 그림을 실제 위젯 그대로 그려 파일로 남긴다.
 * 화면 캡처 CI 가 꺼내 res/drawable-nodpi/widget_preview_*.png 로 넣는다.
 */
object WidgetShots {
    private class Shot(val name: String, val widget: GlanceAppWidget, val w: Int, val h: Int)

    private val shots get() = listOf(
        Shot("days_left_glass", DaysLeftWidget(), 160, 160), Shot("days_left_garden", DaysLeftGardenWidget(), 160, 160),
        Shot("today_glass", TodayWidget(), 160, 160), Shot("today_garden", TodayGardenWidget(), 160, 160),
        Shot("calendar_glass", LifeCalendarWidget(), 320, 160), Shot("calendar_garden", LifeCalendarGardenWidget(), 320, 160),
        Shot("family_garden", FamilyGardenWidget(), 320, 160), Shot("record_garden", RecordWidget(), 160, 160),
        Shot("breath_garden", BreathWidget(), 160, 160),
    )

    @OptIn(androidx.glance.ExperimentalGlanceApi::class)
    suspend fun save(activity: Activity) {
        val dir = File(activity.getExternalFilesDir(null), "widgets").apply { mkdirs() }
        val d = activity.resources.displayMetrics.density
        for (s in shots) runCatching {
            val rv = s.widget.compose(activity, size = DpSize(s.w.dp, s.h.dp))
            val wPx = (s.w * d).toInt(); val hPx = (s.h * d).toInt()
            val host = FrameLayout(activity)
            val v = rv.apply(activity, host)
            host.addView(v, FrameLayout.LayoutParams(wPx, hPx))
            host.measure(View.MeasureSpec.makeMeasureSpec(wPx, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(hPx, View.MeasureSpec.EXACTLY))
            host.layout(0, 0, wPx, hPx)
            val raw = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
            host.draw(Canvas(raw))
            // 홈 화면처럼 둥근 모서리로
            val out = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
            val c = Canvas(out); val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val r = 20 * d
            c.drawRoundRect(RectF(0f, 0f, wPx.toFloat(), hPx.toFloat()), r, r, p)
            p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            c.drawBitmap(raw, 0f, 0f, p)
            File(dir, "widget_preview_${s.name}.png").outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }.onFailure { android.util.Log.e("WidgetShots", s.name, it) }
        File(dir, "done").writeText("ok")
    }
}
