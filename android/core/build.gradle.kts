plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin { jvmToolchain(17) }

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    // 테스트는 저장소의 원본 데이터(data/*.csv)를 직접 읽는다
    systemProperty("dataDir", rootProject.projectDir.resolve("../data").absolutePath)
}
