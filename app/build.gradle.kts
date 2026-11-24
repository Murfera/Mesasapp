plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "br.com.mesasapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.com.mesasapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room exporta o schema a cada build: versionar isso torna as migracoes auditaveis.
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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

    lint {
        abortOnError = true
        // targetSdk 36 e deliberado: atende a exigencia do Play sem adotar
        // mudancas de comportamento do 37 que ainda nao foram testadas aqui.
        disable += "OldTargetApi"
        // Esta regra sugere renomear mipmap-anydpi-v26 para mipmap-anydpi; o AAPT2
        // nao reconhece a pasta sem o -v26 e o build quebra. Nao seguir a sugestao.
        disable += "ObsoleteSdkInt"
    }

    // Os testes de interface rodam na JVM com Robolectric: nao dependem de o
    // celular estar ligado e desbloqueado, e rodam a cada build aqui no PC.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    // Backup automatico: WorkManager agenda a tarefa diaria e documentfile
    // escreve na pasta que o usuario escolheu, sem permissao de armazenamento.
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.documentfile)

    // Menu > Privacidade. Quem guarda e confere a senha e a digital e o
    // Android: esta biblioteca so faz a pergunta e recebe sim ou nao.
    implementation(libs.androidx.biometric)
    // O biometric puxa o fragment 1.2.5, cujo FragmentActivity recusa os
    // requestCode do rememberLauncherForActivityResult ("Can only use lower 16
    // bits for requestCode") e derrubava o app nos botoes do Backup.
    implementation(libs.androidx.fragment)

    // Geracao do QR Code do Pix, offline. Nenhuma chamada de rede.
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)

    // Testes de interface que rodam no aparelho: tocam nos botoes de verdade.
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
