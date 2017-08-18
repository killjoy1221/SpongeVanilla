/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.server.launch.transformer.deobf;

import com.google.common.collect.ImmutableMap;
import com.sun.nio.zipfs.ZipFileSystem;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.server.launch.VanillaLaunch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.annotation.Nullable;

abstract class DeobfuscationTransformer extends Remapper implements IClassTransformer, IRemapper, SrgRemapper {
    
    /**
     * System property to enable deobfuscation cache
     */
    private static boolean ENABLE_CACHE = "true".equals(System.getProperty("deobf.cache"));
    
    /**
     * System property to use disk cache instead of jar cache 
     */
    private static boolean USE_ZIP = !"false".equals(System.getProperty("deobf.cache.zip"));
    
    /**
     * Path (relative to application path) to create cache 
     */
    private static final String CACHE_PATH = "cache/deobfuscated_classes";
    
    /**
     * Flush changes to zip cache after every n transforms
     */
    private static final int FLUSH_ZIP_EVERY = 500;

    /**
     * MD5 digest for computing hashes
     */
    private final MessageDigest md5;
    
    /**
     * FileSystem env vars to pass to zip 
     */
    private final Map<String, ?> cacheEnv = ImmutableMap.of("create", "true");

    /**
     * For zip caches, the URI of the cache file itself
     */
    protected URI cacheFile;
    
    /**
     * Filesystem to cache
     */
    protected FileSystem cache;
    
    /**
     * Relative root into the target filesystem. For zips this is /, for file system this is the cache dir
     */
    protected Path root;
    
    private int transformCount;

    DeobfuscationTransformer() throws IOException, NoSuchAlgorithmException {
        VanillaLaunch.setRemapper(this);
        
        this.md5 = MessageDigest.getInstance("MD5");
        
        if (DeobfuscationTransformer.ENABLE_CACHE) {
            if (DeobfuscationTransformer.USE_ZIP) {
                Path jarPath = Paths.get(DeobfuscationTransformer.CACHE_PATH + ".jar");
                Files.createDirectories(jarPath.getParent());
                this.cacheFile = URI.create("jar:" + jarPath.toUri());
            }
            
            // Initialise cache
            this.initCache();
            
            // Add shutdown hook to flush cache when terminating the VM
            Runtime.getRuntime().addShutdownHook(new Thread(this::closeCache));
        }
    }

    private void initCache() throws IOException {
        if (DeobfuscationTransformer.USE_ZIP) {
            this.cache = FileSystems.newFileSystem(this.cacheFile, this.cacheEnv, null);
            this.root = this.cache.getPath("/");
        } else {
            this.cache = FileSystems.getDefault();
            this.root = Paths.get(Paths.get(CACHE_PATH).toUri());
        }
    }
    
    protected void closeCache() {
        try {
            if (this.cache instanceof ZipFileSystem) {
                this.cache.close();
                this.cache = null;
            }
        } catch (IOException ex) {
            // well, crap
            ex.printStackTrace();
        }
    }
    
    public void flushCache() throws IOException {
        this.closeCache();
        this.initCache();
    }

    @Override
    public String unmap(String typeName) {
        return typeName;
    }

    // Copied from Remapper#mapDesc with references to 'map' replaced with 'unmap'
    @Override
    public String unmapDesc(String desc) {
        Type t = Type.getType(desc);
        switch (t.getSort()) {
            case Type.ARRAY:
                String s = unmapDesc(t.getElementType().getDescriptor());
                StringBuilder sb = new StringBuilder(s.length());
                for (int i = 0; i < t.getDimensions(); ++i) {
                    sb.append('[');
                }
                sb.append(s);
                return sb.toString();
            case Type.OBJECT:
                String newType = unmap(t.getInternalName());
                if (newType != null) {
                    return 'L' + newType + ';';
                }
        }
        return desc;
    }

    @Override @Nullable
    public final byte[] transform(String name, String transformedName, @Nullable byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        
        byte[] hash = null;
        Path hashPath = null, binPath = null;
        
        if (DeobfuscationTransformer.ENABLE_CACHE) {
            // Compute digest if caching
            hash = this.md5.digest(basicClass);
            
            // Resolve paths to the md5 file and bin file
            String classFileName = transformedName.replace('.', '/');
            hashPath = this.root.resolve(this.cache.getPath(classFileName + ".md5"));
            binPath = this.root.resolve(this.cache.getPath(classFileName + ".class"));
            
            try {
                // Assume no (existing) hash
                boolean hashMatches = false;
                if (Files.exists(hashPath)) {
                    // Got the has file, so let's assume it's good
                    hashMatches = true;
                    
                    byte[] stashedCacheHash = Files.readAllBytes(hashPath);
                    for (int i = 0; i < hash.length; i++) {
                        if (stashedCacheHash[i] != hash[i]) {
                            // It isn't :(
                            hashMatches = false;
                            break;
                        }
                    }
                }
                
                // If we have a good hash and also have the binary payload, 
                if (hashMatches && Files.exists(binPath)) {
                    return Files.readAllBytes(binPath);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                // mismatch digest length??
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(createClassRemapper(reader, writer), 0);
        byte[] bytes = writer.toByteArray();
        
        if (DeobfuscationTransformer.ENABLE_CACHE && binPath != null) {
            try {
                ByteArrayInputStream is = new ByteArrayInputStream(bytes);
                Files.createDirectories(binPath.getParent());
                Files.copy(is, binPath, StandardCopyOption.REPLACE_EXISTING);
                Files.write(hashPath, hash, StandardOpenOption.CREATE);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        if (this.transformCount++ >= DeobfuscationTransformer.FLUSH_ZIP_EVERY) {
            this.transformCount = 0;
            try {
                this.flushCache();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        return bytes;
    }

    ClassVisitor createClassRemapper(ClassReader reader, ClassVisitor cv) {
        return new ClassRemapper(cv, this);
    }

}
