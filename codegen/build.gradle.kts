dependencies {
    implementation(libs.javapoet)
    implementation(libs.nbt)
    implementation(libs.gson)
    implementation(libs.bundles.fastutil)
    implementation(libs.annotations)
}
// GearsMC: üreticileri IDE olmadan çalıştırmak için. Kimlik listesini üreten (…IdEnumGen) ile onu kullanan
// (…ClassGen, BlockPropertyTypeGen) ayrı çağrılmalı; arada derleme gerekir, görev bunu kendisi yapar.
// ./gradlew :codegen:runMain -PmainClass=org.allaymc.codegen.BlockIdEnumGen
tasks.register<JavaExec>("runMain") {
    group = "codegen"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = providers.gradleProperty("mainClass")
    workingDir = rootProject.projectDir
}
