plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}

subprojects {
    tasks.withType<Test> {
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    }
}

tasks.register<Exec>("setupGitHooks") {
    description = "Setup git hooks for pre-push testing"
    group = "setup"
    commandLine("git", "config", "core.hooksPath", ".githooks")
    doLast {
        println("Git hooks configured: .githooks/pre-push")
    }
}
