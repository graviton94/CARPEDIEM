// 자동 생성 파일 — 직접 고치지 말고 scripts/generate.py 를 실행하세요.
package io.github.graviton94.carpediem.ui

import io.github.graviton94.carpediem.R

/** 버전마다 새로워진 점 (최신이 위). 원본: design/changelog.json */
object Changelog {
    val entries: List<Pair<String, List<Int>>> = listOf(
        "1.1.12" to listOf(R.string.news_v1_1_12_1, R.string.news_v1_1_12_2, R.string.news_v1_1_12_3, R.string.news_v1_1_12_4),
        "1.1.11" to listOf(R.string.news_v1_1_11_1, R.string.news_v1_1_11_2, R.string.news_v1_1_11_3, R.string.news_v1_1_11_4),
        "1.1.10" to listOf(R.string.news_v1_1_10_1, R.string.news_v1_1_10_2),
        "1.1.9" to listOf(R.string.news_v1_1_9_1, R.string.news_v1_1_9_2, R.string.news_v1_1_9_3, R.string.news_v1_1_9_4),
        "1.1.8" to listOf(R.string.news_v1_1_8_1, R.string.news_v1_1_8_2, R.string.news_v1_1_8_3, R.string.news_v1_1_8_4, R.string.news_v1_1_8_5, R.string.news_v1_1_8_6),
        "1.1.7" to listOf(R.string.news_v1_1_7_1, R.string.news_v1_1_7_2, R.string.news_v1_1_7_3, R.string.news_v1_1_7_4, R.string.news_v1_1_7_5, R.string.news_v1_1_7_6, R.string.news_v1_1_7_7),
        "1.1.6" to listOf(R.string.news_v1_1_6_1, R.string.news_v1_1_6_2, R.string.news_v1_1_6_3, R.string.news_v1_1_6_4, R.string.news_v1_1_6_5, R.string.news_v1_1_6_6),
        "1.1.5" to listOf(R.string.news_v1_1_5_1, R.string.news_v1_1_5_2, R.string.news_v1_1_5_3, R.string.news_v1_1_5_4, R.string.news_v1_1_5_5, R.string.news_v1_1_5_6, R.string.news_v1_1_5_7, R.string.news_v1_1_5_8, R.string.news_v1_1_5_9, R.string.news_v1_1_5_10),
        "1.1.4" to listOf(R.string.news_v1_1_4_1, R.string.news_v1_1_4_2, R.string.news_v1_1_4_3, R.string.news_v1_1_4_4, R.string.news_v1_1_4_5, R.string.news_v1_1_4_6, R.string.news_v1_1_4_7),
        "1.1.3" to listOf(R.string.news_v1_1_3_1, R.string.news_v1_1_3_2, R.string.news_v1_1_3_3, R.string.news_v1_1_3_4, R.string.news_v1_1_3_5, R.string.news_v1_1_3_6),
        "1.1.2" to listOf(R.string.news_v1_1_2_1, R.string.news_v1_1_2_2, R.string.news_v1_1_2_3, R.string.news_v1_1_2_4, R.string.news_v1_1_2_5),
    )
    fun of(version: String) = entries.firstOrNull { it.first == version }
}
