package org.spongepowered.server.resource;

import com.google.common.collect.Streams;
import net.minecraft.resources.ResourcePack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PluginResourcePack extends ResourcePack {

    private final FileSystem pluginFileSystem;
    private final String name;

    public PluginResourcePack(String name, FileSystem pluginFileSystem) {
        super(new File("dummy"));
        this.name = name;
        this.pluginFileSystem = pluginFileSystem;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    protected InputStream getInputStream(String resourcePath) throws IOException {
        return Files.newInputStream(pluginFileSystem.getPath(resourcePath));
    }

    @Override
    protected boolean resourceExists(String resourcePath) {
        return Files.exists(pluginFileSystem.getPath(resourcePath));
    }

    @Override
    public Collection<ResourceLocation> getAllResourceLocations(ResourcePackType type, String pathIn, int maxDepth, Predicate<String> filter) {
        try {
            Path root = pluginFileSystem.getPath(type.getDirectoryName()).toAbsolutePath();
            Path inputPath = root.resolve(pathIn);

            return Files.walk(root)
                    .map(path -> root.relativize(path.toAbsolutePath()))

                    .filter(path -> path.getNameCount() > 1 && path.getNameCount() - 1 <= maxDepth) // Make sure the depth is within bounds, ignoring domain
                    .filter(path -> !path.toString().endsWith(".mcmeta")) // Ignore .mcmeta files
                    .filter(path -> path.subpath(1, path.getNameCount()).startsWith(inputPath)) // Make sure the target path is inside this one (again ignoring domain)
                    .filter(path -> filter.test(path.getFileName().toString())) // Test the file name against the predicate
                    // Finally we need to form the RL, so use the first name as the domain, and the rest as the path
                    // It is VERY IMPORTANT that we do not rely on Path.toString as this is inconsistent between operating systems
                    // Join the path names ourselves to force forward slashes
                    .map(path -> new ResourceLocation(path.getName(0).toString(),
                            Streams.stream(path.subpath(1, Math.min(maxDepth, path.getNameCount())))
                                    .map(Object::toString)
                                    .collect(Collectors.joining("/"))))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getResourceNamespaces(ResourcePackType type) {
        try {
            Path root = pluginFileSystem.getPath(type.getDirectoryName()).toAbsolutePath();
            return Files.walk(root, 1)
                    .map(path -> root.relativize(path.toAbsolutePath()))
                    .filter(path -> path.getNameCount() > 0) // skip the root entry
                    .map(p -> p.toString().replaceAll("/$", "")) // remove the trailing slash, if present
                    .filter(s -> !s.isEmpty()) // filter empty strings, otherwise empty strings default to minecraft in ResourceLocations
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    @Override
    public void close() {

    }
}
