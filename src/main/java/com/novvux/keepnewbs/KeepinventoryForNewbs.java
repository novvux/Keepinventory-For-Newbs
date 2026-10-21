package com.novvux.keepnewbs;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(KeepinventoryForNewbs.MODID)
public class KeepinventoryForNewbs {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "keepinventoryfornewbs";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public KeepinventoryForNewbs(ModContainer modContainer) {
        // The grace period is a server side setting, it is stored in
        // <server>/config/keepinventoryfornewbs-server.toml
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        LOGGER.info("Keep-inventory grace period loaded, default grace period is {} hours", Config.DEFAULT_GRACE_PERIOD_HOURS);
    }
}
