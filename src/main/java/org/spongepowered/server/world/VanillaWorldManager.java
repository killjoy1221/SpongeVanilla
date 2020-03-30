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
package org.spongepowered.server.world;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Difficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.api.world.WorldArchetype;
import org.spongepowered.api.world.dimension.DimensionTypes;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.bridge.world.WorldSettingsBridge;
import org.spongepowered.common.bridge.world.dimension.DimensionTypeBridge;
import org.spongepowered.common.world.server.SpongeWorldManager;
import org.spongepowered.common.world.server.WorldRegistration;
import org.spongepowered.server.mixin.core.server.MinecraftServerAccessor_Vanilla;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class VanillaWorldManager implements SpongeWorldManager {

    private final MinecraftServer server;
    private final Map<DimensionType, ServerWorld> worldsByType;
    private final Map<DimensionType, WorldInfo> dataByType;
    private final Map<UUID, ServerWorld> worldById;
    private final Map<String, ServerWorld> worldByDirectoryName;
    private final Map<String, WorldRegistration> pendingWorlds;

    public VanillaWorldManager(MinecraftServer server) {
        this.server = server;
        this.worldsByType = ((MinecraftServerAccessor_Vanilla) this.server).accessor$getWorlds();
        this.dataByType = new IdentityHashMap<>();
        this.worldById = new Object2ObjectOpenHashMap<>();
        this.worldByDirectoryName = new Object2ObjectOpenHashMap<>();
        this.pendingWorlds = new Object2ObjectOpenHashMap<>();
    }

    @Override
    public MinecraftServer getServer() {
        return this.server;
    }

    @Override
    public boolean isDimensionTypeRegistered(DimensionType dimensionType) {
        return Registry.DIMENSION_TYPE.getKey(dimensionType) != null;
    }

    @Override
    public WorldInfo getInfo(DimensionType dimensionType) {
        return this.dataByType.get(dimensionType);
    }

    @Override
    public Path getSavesDirectory() {
        return ((MinecraftServerAccessor_Vanilla) this.server).accessor$getAnvilFile().toPath();
    }

    @Override
    public Optional<org.spongepowered.api.world.server.ServerWorld> getWorld(UUID uniqueId) {
        return Optional.ofNullable((org.spongepowered.api.world.server.ServerWorld) this.worldById.get(checkNotNull(uniqueId)));
    }

    @Override
    public Optional<org.spongepowered.api.world.server.ServerWorld> getWorld(String directoryName) {
        return Optional.ofNullable((org.spongepowered.api.world.server.ServerWorld) this.worldByDirectoryName.get(checkNotNull(directoryName)));
    }

    @Override
    public Collection<org.spongepowered.api.world.server.ServerWorld> getWorlds() {
        return Collections.unmodifiableCollection((Collection< org.spongepowered.api.world.server.ServerWorld>) (Object) this.worldsByType.values());
    }

    @Override
    public String getDefaultPropertiesName() {
        return this.server.getFolderName();
    }

    @Override
    public Optional<WorldProperties> getDefaultProperties() {
        final ServerWorld defaultWorld = this.getDefaultWorld();
        if (defaultWorld == null) {
            return Optional.empty();
        }
        return Optional.of((WorldProperties) defaultWorld.getWorldInfo());
    }

    @Override
    public boolean submitRegistration(String directoryName, WorldArchetype archetype) {
        checkNotNull(directoryName);
        checkNotNull(archetype);

        if (this.pendingWorlds.containsKey(directoryName)) {
            return false;
        }

       this.pendingWorlds.put(directoryName, new WorldRegistration(directoryName, (WorldSettings) (Object) archetype));
        return true;
    }

    @Override
    public Optional<WorldProperties> createProperties(String directoryName, WorldArchetype archetype) throws IOException {
        return Optional.empty();
    }

    @Override
    public Optional<org.spongepowered.api.world.server.ServerWorld> loadWorld(String directoryName) {
        return Optional.empty();
    }

    @Override
    public Optional<org.spongepowered.api.world.server.ServerWorld> loadWorld(WorldProperties properties) {
        return Optional.empty();
    }

    @Override
    public boolean unloadWorld(org.spongepowered.api.world.server.ServerWorld world) {
        return false;
    }

    @Override
    public Optional<WorldProperties> getProperties(String directoryName) {
        return Optional.empty();
    }

    @Override
    public Optional<WorldProperties> getProperties(UUID uniqueId) {
        return Optional.empty();
    }

    @Override
    public Collection<WorldProperties> getUnloadedProperties() {
        return null;
    }

    @Override
    public Collection<WorldProperties> getAllProperties() {
        return null;
    }

    @Override
    public boolean saveProperties(WorldProperties properties) {
        return false;
    }

    @Override
    public CompletableFuture<Optional<WorldProperties>> copyWorld(String directoryName, String copyName) {
        return null;
    }

    @Override
    public Optional<WorldProperties> renameWorld(String oldDirectoryName, String newDirectoryName) {
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Boolean> deleteWorld(String directoryName) {
        return null;
    }

    @Override
    public void adjustWorldForDifficulty(ServerWorld world, Difficulty newDifficulty, boolean isCustom) {

    }

    @Override
    public void loadAllWorlds(MinecraftServer server, String directoryName, String levelName, long seed, WorldType type, JsonElement generatorOptions) {
        this.checkOrGenerateVanillaRegistrations(directoryName, seed, type, generatorOptions);

        for (Map.Entry<String, WorldRegistration> entry : this.pendingWorlds.entrySet()) {

        }
    }

    private void checkOrGenerateVanillaRegistrations(String directoryName, long seed, WorldType type, JsonElement generatorOptions) {
        // Check if plugins have swapped the template for DIM-1/DIM1/<overworld> and handle accordingly
        final WorldRegistration overworld = this.getRegistration(directoryName).orElse(this.newVanillaRegistration(DimensionType.OVERWORLD, DimensionTypes.OVERWORLD, seed, type, generatorOptions));
        final WorldRegistration nether = this.getRegistration("DIM-1").orElse(this.newVanillaRegistration(DimensionType.THE_NETHER, DimensionTypes.THE_NETHER, seed, type, generatorOptions));
        final WorldRegistration end = this.getRegistration("DIM1").orElse(this.newVanillaRegistration(DimensionType.THE_END, DimensionTypes.THE_END, seed, type, generatorOptions));

        // Set logic types for Vanilla dimensions (in-case a plugin swapped it on the template)
        ((DimensionTypeBridge) DimensionType.OVERWORLD).bridge$setSpongeDimensionType(((WorldSettingsBridge) (Object) overworld.getDefaultSettings()).bridge$getDimensionType());
        ((DimensionTypeBridge) DimensionType.THE_NETHER).bridge$setSpongeDimensionType(((WorldSettingsBridge) (Object) nether.getDefaultSettings()).bridge$getDimensionType());
        ((DimensionTypeBridge) DimensionType.THE_END).bridge$setSpongeDimensionType(((WorldSettingsBridge) (Object) end.getDefaultSettings()).bridge$getDimensionType());
    }

    private Optional<WorldRegistration> getRegistration(String directoryName) {
        checkNotNull(directoryName);

        Map.Entry<String, WorldRegistration> found = null;
        for (Map.Entry<String, WorldRegistration> entry : this.pendingWorlds.entrySet()) {
            if (entry.getKey().equals(directoryName)) {
                found = entry;
                break;
            }
        }

        return Optional.ofNullable(found.getValue());
    }

    private WorldRegistration newVanillaRegistration(DimensionType dimensionType, Supplier<org.spongepowered.api.world.dimension.DimensionType> logicType, long seed, WorldType type, JsonElement generatorOptions) {
        return null;
    }
}
