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

// GearsMC/StateUpdater fork'u da kaynak olarak dahil edilir. Blok durumu güncelleyici adımları (26.50 köşe/bağlantı
// gibi) orada yazılıyor ve yayımlanmıyor; libs.versions.toml'daki block-updater sürümü Maven'da olmadığı için
// klasör yoksa derleme bağımlılığı çözemeyip açıkça durur. Kurulum:
//   git clone https://github.com/GearsMC/StateUpdater.git ../StateUpdater
val stateUpdaterDir = file("../StateUpdater")
if (stateUpdaterDir.resolve("settings.gradle.kts").isFile) {
    includeBuild(stateUpdaterDir)
}

// include multi modules
include(":api")
include(":server")
include(":codegen")
include(":data")
