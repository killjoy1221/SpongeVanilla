import org.spongepowered.gradle.dev.SourceType

plugins {
    id("org.spongepowered.gradle.sponge.impl")
    id("net.minecraftforge.gradle")
}

val commonProj = spongeDev.common

spongeDev {
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
    }
}
dependencies {
    minecraft("net.minecraft:" + commonProj.properties["minecraftDep"] + ":" + commonProj.properties["minecraftVersion"])
}

minecraft {
    evaluationDependsOnChildren()
    mappings(commonProj.properties["mcpType"]!! as String, commonProj.properties["mcpMappings"]!! as String)
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

