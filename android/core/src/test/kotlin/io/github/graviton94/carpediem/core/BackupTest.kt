package io.github.graviton94.carpediem.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 기록 옮기기: 다른 파일로는 지우지 않고, 쓴 값은 종류 그대로 돌아온다. */
class BackupTest {
    @Test fun acceptsOnlyOwnFileWithBirth() {
        assertTrue(Backup.accepts(Backup.APP, setOf("birth", "lines")))
        assertFalse(Backup.accepts(Backup.APP, setOf("lines")))          // 생년월일 없는 파일
        assertFalse(Backup.accepts("other-app", setOf("birth")))          // 다른 앱의 파일
        assertFalse(Backup.accepts(null, setOf("birth")))                 // 깨진 파일 (app 없음)
    }

    /** 쓸 때의 종류 → 파일 (숫자는 글로 읽힘) → 읽을 때의 값이 처음과 같다. */
    @Test fun roundTripEveryKind() {
        val prefs: Map<String, Any> = mapOf(
            "birth" to 11089L, "haruSeed" to 4_294_967_295L, "lastBackup" to 1_760_000_000_000L,
            "big" to Long.MAX_VALUE, "small" to Long.MIN_VALUE,
            "breathMinutes" to 3, "neg" to -7, "fontScale" to 1.3f, "tiny" to 0.1f,
            "introSeen" to true, "sound" to "WAVES", "empty" to "", "pageHints" to setOf("write", "flow"),
        )
        for ((k, v) in prefs) {
            val t = Backup.typeOf(v)
            // JsonReader 는 숫자를 글로, 참 · 거짓은 그대로, 묶음은 Set 으로 넘긴다
            val raw: Any = when (v) { is Boolean, is Set<*>, is String -> v; else -> v.toString() }
            assertEquals(v, Backup.value(t, raw), k)
        }
    }

    /** 2^53 을 넘는 Long 도 소수를 거쳐 바뀌지 않는다. */
    @Test fun longKeepsAllDigits() {
        assertEquals(9_007_199_254_740_993L, Backup.value("l", "9007199254740993"))
        assertEquals(1_760_000_000_000L, Backup.value("l", "1.76E12"))   // 예전 파일이 소수 모양으로 썼어도 읽음
        assertEquals(3, Backup.value("i", "3.0"))
    }

    @Test fun wrongOrUnknownGivesNull() {
        assertNull(Backup.value("i", "abc"))
        assertNull(Backup.value("f", "NaN"))
        assertNull(Backup.value("b", "yes"))
        assertNull(Backup.value("s", 3))
        assertNull(Backup.value("ss", setOf("a", 1)))
        assertNull(Backup.value("zz", "1"))
        assertNull(Backup.value("l", null))
        assertNull(Backup.typeOf(listOf("a")))
        assertNull(Backup.typeOf(setOf(1, 2)))
    }

    @Test fun photoNamesCannotEscape() {
        assertTrue(Backup.isPhotoName("20123.jpg"))
        assertFalse(Backup.isPhotoName("../20123.jpg"))
        assertFalse(Backup.isPhotoName("a/20123.jpg"))
        assertFalse(Backup.isPhotoName("20123.png"))
        assertFalse(Backup.isPhotoName(".jpg"))
        assertFalse(Backup.isPhotoName("shared_prefs.jpg"))
    }

    @Test fun notificationPermissionIsAskedAgain() = assertTrue("notifyAsked" in Backup.PER_DEVICE)
}
