package org.spongepowered.server.launch;

import org.spongepowered.common.launch.InternalLaunchService;
import org.spongepowered.server.launch.transformer.deobf.SrgRemapper;

public interface VanillaInternalLaunchService extends InternalLaunchService {

    SrgRemapper getRemapper();

}
