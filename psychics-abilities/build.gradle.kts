plugins {
    kotlin("jvm") version "1.8.20"
}

val core = project(":${rootProject.name}-core")

subprojects {
    if (version == "unspecified") {
        version = rootProject.version
    }

    val projectName = name.removePrefix("ability-")

    project.extra.apply {
        set("projectName", projectName)
        set("packageName", name.removePrefix("ability-").replace("-", ""))
        set("abilityName", name.removePrefix("ability-").split('-').joinToString(separator = "") { it.capitalize() })
    }

    dependencies {
        implementation(core)
    }

    tasks {
        processResources {
            filesMatching("**/*.yml") {
                expand(project.properties)
            }
        }

        val paperJar = register<Jar>("paperJar") {
            archiveVersion.set("")
            archiveBaseName.set("${project.group}.${project.name.removePrefix("ability-")}")
            from(sourceSets["main"].output)
        }

        rootProject.tasks {
            register<DefaultTask>(projectName) {
                dependsOn(paperJar)
            }
        }
    }
}
