package org.spongepowered.server.accessor.world.storage;

import com.mojang.datafixers.DataFixer;
import net.minecraft.world.storage.SaveFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.file.Path;

@Mixin(SaveFormat.class)
public interface SaveFormatAccessor_Vanilla {

    @Accessor("savesDir") Path accessor$getSavesDir();

    @Accessor("dataFixer") DataFixer accessor$getDataFixer();
}
