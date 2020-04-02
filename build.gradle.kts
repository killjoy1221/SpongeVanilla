plugins {
    id("org.spongepowered.gradle.sponge.impl")
    id("net.minecraftforge.gradle")
}

val commonProj = spongeDev.common

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

val launch by sourceSets.creating {
    evaluationDependsOnChildren()
    compileClasspath += commonProj.sourceSets["launch"].compileClasspath
}
val launchWrapper by sourceSets.creating {
    evaluationDependsOnChildren()
    compileClasspath += commonProj.sourceSets["launchWrapper"].compileClasspath + launch.compileClasspath
}
val accessors by sourceSets.creating {
    evaluationDependsOnChildren()
    compileClasspath += commonProj.sourceSets["accessors"].compileClasspath + launch.compileClasspath
}
val mixins by sourceSets.creating {
    evaluationDependsOnChildren()
    compileClasspath += commonProj.sourceSets["mixins"].compileClasspath + launch.compileClasspath
}

sourceSets {
    evaluationDependsOnChildren()
    // TODO - once invalid is cleaned up, it can be be removed
    val invalid by creating {
        java {
            srcDir("invalid/main/java")
        }
    }
    main {
        invalid.compileClasspath += compileClasspath + output
        mixins.compileClasspath += compileClasspath
    }
}

commonProj.afterEvaluate {
    sourceSets {
        named("launch") {
            launch.compileClasspath += this.compileClasspath
        }
        named("launchWrapper") {
            launchWrapper.compileClasspath += this.compileClasspath
        }
        named("accessors") {
            accessors.compileClasspath += this.compileClasspath
        }
        named("mixins") {
            mixins.compileClasspath += this.compileClasspath
        }
    }
}

configurations {
    val accessorsImplementation by getting // Accessor mixins are accessible everywhere

    val minecraft by getting

    val launchCompile by getting {
        evaluationDependsOn(commonProj.path)
        extendsFrom(commonProj.configurations["launchCompile"])
        extendsFrom(minecraft)
    }

    val launchImplementation by getting
    val launchWrapperCompile by getting {
        extendsFrom(minecraft)
    }

    val mixinsImplementation by getting
    val mixinsCompile by getting {
        // Normal mixins should only access everything else {
        evaluationDependsOn(commonProj.path)
        extendsFrom(commonProj.configurations["mixinsCompile"])
    }

    implementation {
        extendsFrom(accessorsImplementation)
        mixinsCompile.extendsFrom(this)
    }

    devOutput {
        extendsFrom(launchImplementation)
        extendsFrom(accessorsImplementation)
        extendsFrom(mixinsImplementation)
    }


    commonProj.afterEvaluate {
        configurations {
            named("accessorsCompile") {
                accessorsImplementation.extendsFrom(this)
            }
            named("mixinsCompile") {
                mixinsCompile.extendsFrom(this)
            }
            named("launchCompile") {
                launchCompile.extendsFrom(this)
            }
            named("launchWrapperCompile") {
                launchWrapperCompile.extendsFrom(this)
            }
            named("implementation") {
                implementation.get().extendsFrom(this)
            }
        }
    }

}


dependencies {
    evaluationDependsOnChildren()
    "launchCompile"(commonProj.sourceSets["launch"].output)
    implementation(launch.output)
    "launchWrapperCompile"(launch.output)
    implementation(launchWrapper.output)
    "accessorsImplementation"(commonProj.sourceSets["accessors"].output)
    implementation(accessors.output)
    "mixinsImplementation"(commonProj.sourceSets["mixins"].output)
    "mixinsImplementation"(commonProj.sourceSets["main"].output)
    "invalidImplementation"(commonProj.sourceSets["invalid"].output)
}
