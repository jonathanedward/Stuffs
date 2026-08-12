plugins {
    id("com.android.application")
}

android {
    namespace = "com.jonathanedward.dayarc"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jonathanedward.dayarc"
        // Wear OS 4 (API 33) is the floor for Watch Face Format v1.
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // Resource-only bundle: nothing to split on language.
    bundle {
        language {
            enableSplit = false
        }
    }
}

// No dependencies. Watch Face Format is declarative XML — if this block ever
// needs filling in, something has gone wrong with the approach.
dependencies {
}
