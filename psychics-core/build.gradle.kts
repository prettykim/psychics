dependencies {
    implementation("io.github.monun:kommand-api:3.1.7")
    implementation("io.github.monun:invfx-api:3.3.2")
}

tasks {
    processResources {
        filesMatching("**/*.yml") {
            expand(project.properties)
        }
    }

    register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    register<Jar>("paperJar") {
        archiveVersion.set("")
        archiveBaseName.set("Psychics")
        from(sourceSets["main"].output)
    }
}
