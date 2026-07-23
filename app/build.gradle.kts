import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    jacoco
}

android {
    namespace = "com.personalbookkeeping"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.personalbookkeeping.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0-rc1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
        create("baselineProfile") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val coreCoverageIncludes = listOf(
    "com/personalbookkeeping/common/Money*",
    "com/personalbookkeeping/common/NameNormalizer*",
    "com/personalbookkeeping/domain/model/InsightsModelsKt*",
    "com/personalbookkeeping/domain/model/TransactionFilter*",
    "com/personalbookkeeping/domain/model/AccountType*",
    "com/personalbookkeeping/domain/validation/TransactionValidator*",
    "com/personalbookkeeping/domain/usecase/CreateTransactionUseCase*",
    "com/personalbookkeeping/backup/BackupArchive*",
    "com/personalbookkeeping/backup/BackupValidator*",
    "com/personalbookkeeping/backup/BackupModelsKt*",
    "com/personalbookkeeping/export/CsvExporter*",
    "com/personalbookkeeping/security/AppLockCoordinator*",
)

val debugCoreClasses = fileTree(
    layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"),
) {
    include(coreCoverageIncludes)
}

tasks.register<JacocoReport>("coreCoverageReport") {
    group = "verification"
    description = "Generates line and branch coverage for the explicitly tested core rule set."
    dependsOn("testDebugUnitTest")
    executionData(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    classDirectories.setFrom(debugCoreClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    reports {
        xml.required = true
        csv.required = true
        html.required = true
        xml.outputLocation = layout.buildDirectory.file("reports/jacoco/core/core.xml")
        csv.outputLocation = layout.buildDirectory.file("reports/jacoco/core/core.csv")
        html.outputLocation = layout.buildDirectory.dir("reports/jacoco/core/html")
    }
}

tasks.register<JacocoCoverageVerification>("verifyCoreCoverage") {
    group = "verification"
    description = "Fails when core rule line or branch coverage falls below 80%."
    dependsOn("testDebugUnitTest")
    executionData(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    classDirectories.setFrom(debugCoreClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.profileinstaller)
    ksp(libs.androidx.room.compiler)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
