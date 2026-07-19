package cn.xm1221.MieHexRevolution.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;


import java.util.HashMap;
import java.util.Map;

public class HexMonitorRecords {
    public static final Map<ServerPlayer, Entity> CAMERAS = new HashMap<>();
    public static final Map<Entity,ServerPlayer> CASTERS = new HashMap<>();
}

