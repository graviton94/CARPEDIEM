// 자동 생성 파일 — 직접 고치지 말고 scripts/generate.py 를 실행하세요.
package io.github.graviton94.carpediem.ui

import io.github.graviton94.carpediem.R

/** 버전마다 새로워진 점 (최신이 위). 원본: design/changelog.json */
object Changelog {
    val entries: List<Pair<String, List<Int>>> = listOf(
        "1.1.4" to listOf(R.string.news_v1_1_4_1, R.string.news_v1_1_4_2, R.string.news_v1_1_4_3, R.string.news_v1_1_4_4, R.string.news_v1_1_4_5),
        "1.1.3" to listOf(R.string.news_v1_1_3_1, R.string.news_v1_1_3_2, R.string.news_v1_1_3_3, R.string.news_v1_1_3_4, R.string.news_v1_1_3_5, R.string.news_v1_1_3_6),
        "1.1.2" to listOf(R.string.news_v1_1_2_1, R.string.news_v1_1_2_2, R.string.news_v1_1_2_3, R.string.news_v1_1_2_4, R.string.news_v1_1_2_5),
    )
    fun of(version: String) = entries.firstOrNull { it.first == version }
}
