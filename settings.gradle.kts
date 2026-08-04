rootProject.name = "Allay"

// GearsMC/Protocol'u kaynak olarak dahil eder. Gradle, libs.versions.toml'daki
// org.allaymc.protocol:bedrock-connection bagimliligini otomatik olarak bu
// yerel build'e yonlendirir; boylece protokol tarafinda yapilan degisiklik
// ayrica publishToMavenLocal calistirmadan Allay'e gecer.
//
// Klasor yoksa (ornegin Protocol klonlanmamis bir ortamda) build kirilmasin
// diye kosullu dahil edilir; o durumda maven'daki surum kullanilir.
val protocolDir = file("../Protocol")
if (protocolDir.resolve("settings.gradle.kts").isFile) {
    includeBuild(protocolDir)
}

// include multi modules
include(":api")
include(":server")
include(":codegen")
include(":data")
