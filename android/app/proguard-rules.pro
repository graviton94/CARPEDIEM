# 하루의 정원 — R8 규칙 (release 빌드)
# 이름을 바꾸지 않음: 의견 메일로 오는 오류 기록 (data/Feedback.kt) 을 매핑 없이 읽고, 저장된 이름이 바뀌지 않게.
-dontobfuscate
# 오류 기록의 줄 번호
-keepattributes SourceFile,LineNumberTable
# 위젯 둘 (Glance) 은 서로 다른 클래스로 남겨야 함: R8 이 두 GlanceAppWidget 을 한 클래스로 합치면 updateAll 이 서로의 위젯까지 같은 그림으로 그림
# (1.1.6: 2×2 마음의 기록 위젯에 남은 날 · 오늘의 한 줄이 뜨던 것)
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
