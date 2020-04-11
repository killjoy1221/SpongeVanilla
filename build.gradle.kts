import org.spongepowered.gradle.dev.SourceType

plugins {
    id("org.spongepowered.gradle.sponge.impl") version "0.11.1"
    id("net.minecraftforge.gradle")
}

spongeDev {
    common(project.project(":SpongeCommon"))
    api(common.map { it.project("SpongeAPI") })
    addForgeFlower.set(true)
    addedSourceSets {
        register("mixins") {
            sourceType.set(SourceType.Mixin)
        }
        register("accessors") {
            sourceType.set(SourceType.Accessor)
        }
        register("launch") {
            sourceType.set(SourceType.Launch)
        }
        register("launchWrapper") {
            dependsOn += "launch"
        }
        register("invalid") {
            sourceType.set(SourceType.Invalid)
        }
    }
}

val common by spongeDev.common

dependencies {
    minecraft("net.minecraft:" + common.properties["minecraftDep"] + ":" + common.properties["minecraftVersion"])
    implementation(common)

    "launchImplementation"("net.sf.jopt-simple:jopt-simple:5.0.4")
    "launchImplementation"(group = "org.spongepowered", name = "plugin-meta", version = "0.4.1")
}

minecraft {
    evaluationDependsOnChildren()
    mappings(common.properties["mcpType"]!! as String, common.properties["mcpMappings"]!! as String)
    runs {
        create("server") {
            workingDirectory( project.file("./run"))
            mods {
                create("sponge") {
                    source(project.sourceSets["main"])
                }
            }
        }
    }

    project.sourceSets["main"].resources
            .filter { it.name.endsWith("_at.cfg") }
            .files
            .forEach { accessTransformer(it) }
}

