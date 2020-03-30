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
package net.minecraftforge.common.chunkio;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.common.SpongeImpl; // Sponge
import org.spongepowered.server.mixin.chunkio.ChunkProviderServerAccessor_Vanilla;
import org.spongepowered.server.mixin.core.world.chunk.storage.AnvilChunkLoaderAccessor_Vanilla;
import org.spongepowered.server.world.chunkio.AsyncAnvilChunkLoader; // Sponge
//import net.minecraftforge.common.MinecraftForge; // Sponge
//import net.minecraftforge.event.world.ChunkDataEvent; // Sponge

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

class ChunkIOProvider implements Runnable
{
    private final QueuedChunk chunkInfo;
    private final AnvilChunkLoader loader;
    private final ChunkProviderServer provider;

    private Chunk chunk;
    private NBTTagCompound nbt;
    private final ConcurrentLinkedQueue<Consumer<Chunk>> callbacks = new ConcurrentLinkedQueue<>(); // Sponge: Runnable -> Consumer<Chunk>
    private boolean ran = false;

    ChunkIOProvider(QueuedChunk chunk, AnvilChunkLoader loader, ChunkProviderServer provider)
    {
        this.chunkInfo = chunk;
        this.loader = loader;
        this.provider = provider;
    }

    public void addCallback(Consumer<Chunk> callback) // Sponge: Runnable -> Consumer<Chunk>
    {
        this.callbacks.add(callback);
    }
    public void removeCallback(Consumer<Chunk> callback) // Sponge: Runnable -> Consumer<Chunk>
    {
        this.callbacks.remove(callback);
    }

    @Override
    public void run() // async stuff
    {
        synchronized(this)
        {
            //Object[] data = null; // Sponge
            try
            {
                // Sponge start: Use Sponge's async chunk load method
                //data = this.loader.loadChunk__Async(chunkInfo.world, chunkInfo.x, chunkInfo.z);
                this.nbt = AsyncAnvilChunkLoader.read(this.loader, this.chunkInfo.x, this.chunkInfo.z);
                if (this.nbt != null) {
                    this.chunk = ((AnvilChunkLoaderAccessor_Vanilla) this.loader).accessor$checkedReadChunkFromNBT(this.chunkInfo.world, this.chunkInfo.x, this.chunkInfo.z, this.nbt);
                }
                // Sponge end
            }
            catch (IOException e)
            {
                // Sponge: Use Sponge logging
                //e.printStackTrace();
                SpongeImpl.getLogger().error("Could not load chunk in {} @ ({}, {})", this.chunkInfo.world, this.chunkInfo.x, this.chunkInfo.z, e);
            }

            // Sponge start: data is not used
            /*if (data != null)
            {
                this.nbt   = (NBTTagCompound)data[1];
                this.chunk = (Chunk)data[0];
            }*/
            // Sponge end

            this.ran = true;
            this.notifyAll();
        }
    }

    // sync stuff
    public void syncCallback()
    {
        if (this.chunk == null)
        {
            this.runCallbacks();
            return;
        }

        // Load Entities
        // Sponge: Use Sponge's loadEntities method
        //this.loader.loadEntities(this.chunkInfo.world, this.nbt.getCompoundTag("Level"), this.chunk);
        AsyncAnvilChunkLoader.loadEntities(this.chunkInfo.world, this.chunk, this.nbt);

        // Sponge: Don't call Forge event
        //MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(this.chunk, this.nbt)); // Don't call ChunkDataEvent.Load async

        final ChunkProviderServerAccessor_Vanilla accessor = (ChunkProviderServerAccessor_Vanilla) this.provider;
        this.chunk.setLastSaveTime(accessor.chunkIOAccessor$getWorld().getTotalWorldTime());
        accessor.chunkIOAccessor$getChunkGenerator().recreateStructures(this.chunk, this.chunkInfo.x, this.chunkInfo.z);

        accessor.chunkIOAccessor$getLoadedChunks().put(ChunkPos.asLong(this.chunkInfo.x, this.chunkInfo.z), this.chunk);
        this.chunk.onLoad();
        this.chunk.populate(this.provider, accessor.chunkIOAccessor$getChunkGenerator());

        this.runCallbacks();
    }

    public Chunk getChunk()
    {
        return this.chunk;
    }

    public boolean runFinished()
    {
        return this.ran;
    }

    public boolean hasCallback()
    {
        return this.callbacks.size() > 0;
    }

    public void runCallbacks()
    {
        for (Consumer<Chunk> r : this.callbacks) // Sponge: Runnable -> Consumer<Chunk>
        {
            // Sponge: Pass chunk argument
            //r.run();
            r.accept(this.chunk);
        }

        this.callbacks.clear();
    }
}
