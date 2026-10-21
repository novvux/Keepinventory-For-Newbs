package com.novvux.keepnewbs;

import net.neoforged.neoforge.common.ModConfigSpec;

// Server side configuration of the keep-inventory grace period.
// The file is written to <world>/serverconfig/keepinventoryfornewbs-server.toml
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final int DEFAULT_GRACE_PERIOD_HOURS = 24;

    public static final ModConfigSpec.IntValue GRACE_PERIOD_HOURS = BUILDER
            .comment("How long (in hours) a player keeps their inventory after dying.",
                    "The countdown starts at the first login of the player on this server.",
                    "Set to 0 to disable the keep-inventory grace period completely.")
            .defineInRange("gracePeriodHours", DEFAULT_GRACE_PERIOD_HOURS, 0, 24 * 365);

    public static final ModConfigSpec.BooleanValue KEEP_EXPERIENCE = BUILDER
            .comment("Whether players inside the grace period also keep their experience level when they die.")
            .define("keepExperience", true);

    public static final ModConfigSpec.BooleanValue ANNOUNCE_ON_JOIN = BUILDER
            .comment("Whether players are told how much of their keep-inventory grace period is left when they join.")
            .define("announceOnJoin", true);

    public static final ModConfigSpec.BooleanValue APPLY_IN_HARDCORE = BUILDER
            .comment("Whether the grace period also applies on hardcore servers.",
                    "On hardcore a death is permanent, so the player never respawns and would lose everything anyway.")
            .define("applyInHardcore", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    // The spec is only loaded once a server is running, so every read has to fall back to the defaults before that.
    public static int gracePeriodHours() {
        return SPEC.isLoaded() ? GRACE_PERIOD_HOURS.get() : DEFAULT_GRACE_PERIOD_HOURS;
    }

    public static boolean keepExperience() {
        return SPEC.isLoaded() ? KEEP_EXPERIENCE.get() : true;
    }

    public static boolean announceOnJoin() {
        return SPEC.isLoaded() ? ANNOUNCE_ON_JOIN.get() : true;
    }

    public static boolean applyInHardcore() {
        return SPEC.isLoaded() ? APPLY_IN_HARDCORE.get() : false;
    }
}
