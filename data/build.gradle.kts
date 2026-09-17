dependencies {
    implementation(project(":server"))
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // GearsMC: veri testleri dosyaları üretim araçlarıyla aynı göreli yoldan ("data/resources/...") okur.
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("importBedrockData") {
    group = "data"
    description = "26.50 veri setini herkese açık kaynaklardan staging-1.26.50'ye üretir (ağ gerekir)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "org.allaymc.data.importer.BedrockDataImporter"
    workingDir = rootProject.projectDir
}
