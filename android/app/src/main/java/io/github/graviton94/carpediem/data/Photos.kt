package io.github.graviton94.carpediem.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.LruCache
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * 한 줄에 사진 한 장 (11). 원본은 남기지 않는다: 고른 사진을 가운데서 정사각으로 잘라 SIZE 픽셀로 줄인 사본만 폰 안 (files/photos) 에.
 * 화면에는 그대로 보이지 않고 ‘정원의 시간이 묻은 사진’으로: 정원 빛으로 다시 칠하고, 바랜 빛 · 고운 입자 · 한지 결 · 해진 가장자리.
 * 사진이 나이를 먹는다: 남긴 날에서 멀어질수록 조금씩 더 바래고 결이 깊어진다 (그릴 때마다 나이만큼, 사본은 그대로).
 */
object Photos {
    const val SIZE = 640
    private const val QUALITY = 82
    private const val PENDING = "pending.jpg"
    /** 지난 날을 고치며 골라 둔 사진 (오늘 쓰는 중인 사진과 겹치지 않게 따로). */
    private const val EDIT = "edit.jpg"
    /** 방금 지운 날의 사진 (그날 번호를 붙여, 다른 날에 잘못 되돌아가지 않게). */
    private fun undoFile(ctx: Context, day: LocalDate) = File(dir(ctx), "undo-${day.toEpochDay()}.jpg")
    private fun dropUndo(ctx: Context) { dir(ctx).listFiles()?.filter { it.name.startsWith("undo") }?.forEach { it.delete() } }

    private fun dir(ctx: Context) = File(ctx.applicationContext.filesDir, "photos").apply { mkdirs() }
    fun file(ctx: Context, day: LocalDate) = File(dir(ctx), "${day.toEpochDay()}.jpg")
    fun pending(ctx: Context, edit: Boolean = false) = File(dir(ctx), if (edit) EDIT else PENDING)
    fun has(ctx: Context, day: LocalDate) = file(ctx, day).exists()

    /** 고른 사진 → 정사각 사본 (target). 읽지 못하면 false. */
    fun importFile(ctx: Context, uri: Uri, target: File): Boolean = runCatching {
        val src: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { d, info, _ ->
                val m = minOf(info.size.width, info.size.height)
                d.setTargetSampleSize(maxOf(1, m / (SIZE * 2)))
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
            val m = minOf(o.outWidth, o.outHeight).coerceAtLeast(1)
            val opts = BitmapFactory.Options().apply { inSampleSize = maxOf(1, m / (SIZE * 2)) }
            val raw = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return false
            // 안드로이드 8 까지는 사진의 방향 표시를 따로 읽어 돌려 줌 (세운 사진이 눕지 않게)
            val turn = runCatching { ctx.contentResolver.openInputStream(uri)?.use { android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL) } }.getOrNull()
            val deg = when (turn) { android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f; android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f; android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f }
            if (deg == 0f) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, android.graphics.Matrix().apply { postRotate(deg) }, true)
        }
        val m = minOf(src.width, src.height)
        val sq = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        Canvas(sq).drawBitmap(src, Rect((src.width - m) / 2, (src.height - m) / 2, (src.width + m) / 2, (src.height + m) / 2), Rect(0, 0, SIZE, SIZE), Paint(Paint.FILTER_BITMAP_FLAG))
        target.outputStream().use { sq.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        cache.evictAll()
        true
    }.getOrDefault(false)

    fun importFor(ctx: Context, uri: Uri, day: LocalDate) = importFile(ctx, uri, file(ctx, day))
    fun importPending(ctx: Context, uri: Uri, edit: Boolean = false) = importFile(ctx, uri, pending(ctx, edit))

    /** 보낸 한 줄에 맡겨 둔 사진을 그날의 사진으로. */
    fun commitPending(ctx: Context, day: LocalDate, edit: Boolean = false): Boolean { val p = pending(ctx, edit); if (!p.exists()) return false; file(ctx, day).delete(); cache.evictAll(); return p.renameTo(file(ctx, day)) }
    fun dropPending(ctx: Context, edit: Boolean = false) { pending(ctx, edit).delete() }
    /** 한 줄을 지우면 사진도 (방금 지운 것은 되돌리기 전까지 한 장만 맡아 둠). */
    fun remove(ctx: Context, day: LocalDate) { dropUndo(ctx); val f = file(ctx, day); if (f.exists()) f.renameTo(undoFile(ctx, day)); cache.evictAll() }
    fun undo(ctx: Context, day: LocalDate) { val u = undoFile(ctx, day); if (u.exists() && !file(ctx, day).exists()) u.renameTo(file(ctx, day)); dropUndo(ctx); cache.evictAll() }
    fun clear(ctx: Context) { dir(ctx).listFiles()?.forEach { it.delete() }; cache.evictAll() }

    // ───── 기록 옮기기: 사진도 함께 (이름 → base64) ─────
    private fun photoName(n: String) = n.endsWith(".jpg") && n.removeSuffix(".jpg").toLongOrNull() != null
    fun exportTo(ctx: Context, w: android.util.JsonWriter) {
        dir(ctx).listFiles()?.filter { photoName(it.name) }?.forEach { f -> w.name(f.name).value(Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)) }
    }
    /** 들여오는 사진을 잠시 둘 곳 (다 읽고 설정까지 들인 뒤에만 [adopt]). */
    fun stage(ctx: Context): File = File(ctx.applicationContext.cacheDir, "restore-photos").apply { deleteRecursively(); mkdirs() }
    fun stageOne(stage: File, name: String, b64: String) {
        if (!photoName(name)) return   // 날짜 번호 이름만 (경로를 품은 이름은 버림)
        runCatching { File(stage, name).writeBytes(Base64.decode(b64, Base64.NO_WRAP)) }
    }
    fun adopt(ctx: Context, stage: File) {
        clear(ctx)
        stage.listFiles()?.forEach { f -> if (!f.renameTo(File(dir(ctx), f.name))) { f.copyTo(File(dir(ctx), f.name), overwrite = true); f.delete() } }
        stage.deleteRecursively(); cache.evictAll()
    }

    // ───── 시간이 묻은 사진 ─────
    private val cache = LruCache<String, Bitmap>(12)

    /** 정원에 걸 모습 (한지 액자 포함, 가장자리는 투명). 사진이 없으면 null. 화면 스레드 밖에서 부를 것. */
    fun weathered(ctx: Context, day: LocalDate, today: LocalDate, pending: Boolean = false, edit: Boolean = false): Bitmap? {
        val f = if (pending) pending(ctx, edit) else file(ctx, day)
        if (!f.exists()) return null
        val age = ChronoUnit.DAYS.between(day, today).coerceAtLeast(0)
        val stage = if (pending) 0 else ageStage(age)
        val key = "${f.name}:${f.lastModified()}:$stage"
        cache.get(key)?.let { return it }
        val src = BitmapFactory.decodeFile(f.path) ?: return null
        return render(src, day.toEpochDay(), stage).also { cache.put(key, it) }
    }

    /** 나이 단계 (0 = 막 남긴 사진 … 5 = 몇 해 지난 사진). */
    fun ageStage(days: Long): Int = when { days < 7 -> 0; days < 60 -> 1; days < 365 -> 2; days < 730 -> 3; days < 1095 -> 4; else -> 5 }

    fun render(src: Bitmap, seed: Long, stage: Int): Bitmap {
        val rnd = Random(seed)
        val a = stage / 5f   // 0 … 1
        val pad = 34; val ph = 560; val bottom = 104
        val w = ph + pad * 2; val h = ph + pad + bottom
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        // 한지: 해진 가장자리 (조금씩 들쭉날쭉) + 결
        val edge = Path()
        fun jag() = rnd.nextFloat() * (3f + 3f * a)
        var x = 0f; edge.moveTo(jag(), jag())
        while (x < w) { x += 8f; edge.lineTo(x.coerceAtMost(w.toFloat()) - jag(), jag()) }
        var y = 0f; while (y < h) { y += 8f; edge.lineTo(w - jag(), y.coerceAtMost(h.toFloat())) }
        x = w.toFloat(); while (x > 0) { x -= 8f; edge.lineTo(x.coerceAtLeast(0f) + jag(), h - jag()) }
        y = h.toFloat(); while (y > 0) { y -= 8f; edge.lineTo(jag(), y.coerceAtLeast(0f)) }
        edge.close()
        val paper = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(244 - (8 * a).toInt(), 236 - (12 * a).toInt(), 220 - (20 * a).toInt()) }
        c.drawPath(edge, paper)
        c.save(); c.clipPath(edge)
        val fiber = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f; color = android.graphics.Color.argb(40, 170, 150, 120) }
        repeat(70) { val fx = rnd.nextFloat() * w; val fy = rnd.nextFloat() * h; val p = Path(); p.moveTo(fx, fy); p.quadTo(fx + rnd.nextFloat() * 30 - 15, fy + rnd.nextFloat() * 10, fx + rnd.nextFloat() * 60 - 30, fy + rnd.nextFloat() * 14 - 7); c.drawPath(p, fiber) }
        c.restore()

        // 사진: 정원 빛으로 다시 칠하기 (채도 낮추고 따뜻하게, 어두운 곳은 살짝 띄움) — 나이가 들수록 더
        val px = Bitmap.createScaledBitmap(src, ph, ph, true)
        val cm = ColorMatrix().apply { setSaturation(0.62f - 0.3f * a) }
        val lift = 16f + 22f * a
        cm.postConcat(ColorMatrix(floatArrayOf(
            0.92f, 0.06f, 0.02f, 0f, lift + 6f,
            0.04f, 0.88f, 0.03f, 0f, lift,
            0.03f, 0.08f, 0.70f - 0.12f * a, 0f, lift - 4f,
            0f, 0f, 0f, 1f, 0f)))
        val tinted = Bitmap.createBitmap(ph, ph, Bitmap.Config.ARGB_8888)
        Canvas(tinted).drawBitmap(px, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
        // 크레용처럼 몇 단계로 모으고 (반쯤만), 고운 입자
        val pixels = IntArray(ph * ph); tinted.getPixels(pixels, 0, ph, 0, 0, ph, ph)
        val step = 255f / 6f; val grain = 9 + (10 * a).toInt(); val g = Random(seed * 31 + stage)
        for (i in pixels.indices) {
            val p = pixels[i]
            fun ch(v: Int): Int { val q = (Math.round(v / step) * step); val m = (v * 0.55f + q * 0.45f).toInt(); return m }
            val n = g.nextInt(-grain, grain + 1)
            val r = (ch((p shr 16) and 255) + n).coerceIn(0, 255); val gg = (ch((p shr 8) and 255) + n).coerceIn(0, 255); val b = (ch(p and 255) + n).coerceIn(0, 255)
            pixels[i] = (255 shl 24) or (r shl 16) or (gg shl 8) or b
        }
        tinted.setPixels(pixels, 0, ph, 0, 0, ph, ph)
        val dst = RectF(pad.toFloat(), pad.toFloat(), (pad + ph).toFloat(), (pad + ph).toFloat())
        c.drawBitmap(tinted, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        // 옅은 비네트 · 얼룩 (나이 들수록)
        c.drawRect(dst, Paint().apply { shader = RadialGradient(dst.centerX(), dst.centerY(), ph * 0.75f, intArrayOf(0x00000000, android.graphics.Color.argb((70 + 70 * a).toInt(), 70, 48, 20)), floatArrayOf(0.55f, 1f), Shader.TileMode.CLAMP) })
        if (stage >= 3) repeat(stage - 1) {
            val sx = dst.left + rnd.nextFloat() * ph; val sy = dst.top + rnd.nextFloat() * ph; val sr = 20f + rnd.nextFloat() * 50f
            c.drawCircle(sx, sy, sr, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(sx, sy, sr, android.graphics.Color.argb(36, 200, 160, 90), 0x00000000, Shader.TileMode.CLAMP) })
        }
        // 한지 결이 사진 위에도 아주 옅게
        val over = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = android.graphics.Color.argb((18 + 22 * a).toInt(), 255, 248, 230) }
        repeat(40) { val fx = dst.left + rnd.nextFloat() * ph; val fy = dst.top + rnd.nextFloat() * ph; c.drawLine(fx, fy, fx + rnd.nextFloat() * 50 - 25, fy + rnd.nextFloat() * 8 - 4, over) }
        c.drawRect(dst, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f; color = android.graphics.Color.argb(60, 90, 70, 40) })
        return out
    }
}
