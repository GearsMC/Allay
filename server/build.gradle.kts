import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import org.gradle.kotlin.dsl.support.serviceOf
import java.io.ByteArrayOutputStream

plugins {
    id("jacoco")
    id("application")
    alias(libs.plugins.jmh)
    alias(libs.plugins.gitproperties)
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("org.allaymc.server.Allay")
}

dependencies {
    api(project(":api"))
    api(libs.bundles.logging)
    api(libs.bundles.leveldb)
    api(libs.bundles.rng)
    api(libs.bundles.fastutil)
    api(libs.bundles.stateupdater)
    api(libs.semver4j)
    api(libs.commonslang3)
    api(libs.commonsio)
    api(libs.mcterminal)
    api(libs.jline.reader)
    api(libs.disruptor)
    api(libs.netty.epoll)
    api(libs.netty.kqueue)
    api(libs.fastreflect)
    api(libs.oshi)
    api(libs.flatlaf)
    api(libs.formsrt)
    api(libs.sentry)
    api(libs.jctools)
    api(libs.caffeine)
    api(libs.protocol) {
        exclude(group = "org.cloudburstmc", module = "nbt") // Use allaymc's nbt library
        exclude(group = "org.cloudburstmc.fastutil.commons")
        exclude(group = "org.cloudburstmc.fastutil.maps")
    }
    api(libs.okaeri.configs.yaml.snakeyaml) {
        exclude(group = "org.yaml", module = "snakeyaml") // Use the latest version
    }
    api(libs.bstats)
    api(libs.lz4.java)

    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
}

gitProperties {
    dotGitDirectory = project.rootProject.layout.projectDirectory.dir(".git")
    gitPropertiesResourceDir.set(file("${rootProject.projectDir}/data/resources"))
    gitProperties {
        customProperty("git.build.is_dev_build", rootProject.property("allay.is-dev-build").toString().toBoolean())
        /**
         * The version of allay-api.
         *
         * There are two versions in git.properties:
         * - version: The version of allay-server
         * - api_version: The version of allay-api
         */
        customProperty("git.build.api_version", project(":api").version)
    }
}

jacoco {
    reportsDirectory = layout.buildDirectory.dir("${rootProject.projectDir}/.jacoco")
}

tasks {
    processResources {
        dependsOn("generateGitProperties")
        // input directory
        from("${rootProject.projectDir}/data/resources")
        // exclude unpacked folder and block_palette.nbt
        exclude("unpacked")
    }

    sourcesJar {
        dependsOn("generateGitProperties")
    }

    shadowJar {
        archiveFileName = getShadedJarName()
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        // Log4j config fix
        filesMatching("META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        transform<Log4j2PluginsCacheFileTransformer>()
        mergeServiceFiles()

        exclude("META-INF/maven/**")
        exclude("META-INF/native-image/**")
        exclude("META-INF/proguard/**")

        exclude("META-INF/AL2.0")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/*-LICENSE")
        exclude("META-INF/*-NOTICE")
        exclude("META-INF/io.netty.versions.properties")
        exclude("META-INF/LGPL2.1")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/thirdparty-LICENSE")
    }

    runShadow {
        workingDir = file("${rootProject.projectDir}/.run/")
    }

    // GearsMC: 26.50 köşe/bağlantı durumlarını mevcut dünyalara tek seferde yazar. Sunucu KAPALIYKEN ve yedekten sonra:
    // ./gradlew :server:migrateConnections -Pworlds=/yol/dunya1,/yol/dunya2 [-PdryRun=true]
    register<JavaExec>("migrateConnections") {
        group = "application"
        description = "Mevcut dünyaların merdiven köşelerini ve çit/panel/parmaklık/tuzak ipi bağlantılarını düzeltir"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass = "org.allaymc.server.block.connection.ConnectionWorldMigration"
        // Allay başlatılırken ayar dosyaları çalışma dizinine yazılabiliyor; test dizini kullanılır.
        workingDir = file("${rootProject.projectDir}/.test/")
        val worlds = providers.gradleProperty("worlds").orElse("")
        val dryRun = providers.gradleProperty("dryRun").orElse("false")
        doFirst {
            workingDir.mkdirs()
            args = (if (dryRun.get().toBoolean()) listOf("--dry-run") else emptyList()) + worlds.get().split(",")
        }
    }

    jacocoTestReport {
        reports {
            xml.required = true
            html.required = false
        }
        additionalClassDirs(file("${rootProject.projectDir}/api/build/classes/java/main"))
        additionalSourceDirs(file("${rootProject.projectDir}/api/src/main/java"))
    }

    test {
        useJUnitPlatform()
        workingDir = file("${rootProject.projectDir}/.test/")
        // GearsMC: klasör yoksa Gradle test sürecini başlatamıyor ve yalnızca "Cannot abort process 'Gradle Test
        // Executor N'" diyor. .test/.keep bir kez yanlışlıkla silindi ve CI'daki temiz kopya bu yüzden kırıldı.
        val testWorkingDir = workingDir
        doFirst { testWorkingDir.mkdirs() }
    }

    register("cleanWorkingDir") {
        description = "Clean all files in `.run` directory except `Allay.run.xml` file"
        group = "application"
        doLast {
            rootProject.rootDir.resolve(".run").listFiles { f -> !f.name.equals("Allay.run.xml") }?.forEach {
                delete(it)
            }
        }
    }
}

fun getShadedJarName(): String {
    return "allay-server-${version}-${getShortGitHash()}-shaded.jar"
}

fun getShortGitHash(): String {
    val execOperations = project.serviceOf<ExecOperations>()

    val stdout = ByteArrayOutputStream()
    execOperations.exec {
        commandLine = mutableListOf("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}