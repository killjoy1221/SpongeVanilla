plugins {
    id("org.spongepowered.gradle.sponge.impl")
    id("net.minecraftforge.gradle")
}

val commonProj = project(":SpongeCommon")

gradle.projectsEvaluated {
    sourceSets {
        main {
            compileClasspath += commonProj.the<SourceSetContainer>().named("launch").get().compileClasspath
            runtimeClasspath += commonProj.the<SourceSetContainer>().named("launch").get().runtimeClasspath
        }
    }
}

dependencies {
    minecraft("net.minecraft:" + commonProj.properties["minecraftDep"] + ":" + commonProj.properties["minecraftVersion"])
}

minecraft {
    mappings(commonProj.properties["mcpType"]!! as String, commonProj.properties["mcpMappings"]!! as String)
}

spongeDev {
}