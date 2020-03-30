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

import com.google.common.base.MoreObjects; // Sponge
import net.minecraft.world.World;

class QueuedChunk {
    final int x;
    final int z;
    final World world;

    public QueuedChunk(int x, int z, World world) {
        this.x = x;
        this.z = z;
        this.world = world;
    }

    @Override
    public int hashCode() {
        return (this.x * 31 + this.z * 29) ^ this.world.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof QueuedChunk) {
            QueuedChunk other = (QueuedChunk) object;
            return this.x == other.x && this.z == other.z && this.world == other.world;
        }

        return false;
    }

    /* Sponge start: Replace toString
    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");

        result.append(this.getClass().getName() + " {" + NEW_LINE);
        result.append(" x: " + x + NEW_LINE);
        result.append(" z: " + z + NEW_LINE);
        result.append(" world: " + world.getWorldInfo().getWorldName() + NEW_LINE);
        result.append(" dimension: " + world.provider.getDimension() + NEW_LINE);
        result.append(" provider: " + world.provider.getClass().getName() + NEW_LINE);
        result.append("}");

        return result.toString();
    }*/

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(this.world)
                .add("x", this.x)
                .add("z", this.z)
                .toString();
    }
    // Sponge end

}
