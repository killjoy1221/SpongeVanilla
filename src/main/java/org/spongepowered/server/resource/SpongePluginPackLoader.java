package org.spongepowered.server.resource;

import net.minecraft.resources.ResourcePackList;
import org.spongepowered.api.plugin.PluginContainer;

/**
 * Automatically loads packs from plugin jars.
 */
public class SpongePluginPackLoader {

    private final Map<PluginContainer, PluginResourcePack> pluginResourcePacks;
    private final ResourcePackList<?> resourcePacks;

    SpongePluginPackLoader(ResourcePackList<?> resourcePacks) {

        this.resourcePacks = resourcePacks;
    }
}
