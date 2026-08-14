package com.spawnspamdetector.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import com.spawnspamdetector.Tags;


public final class SpawnSpamDetectorNetwork {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MODID);

    private static boolean initialized;

    private SpawnSpamDetectorNetwork() {
    }

    public static void init() {
        if (initialized) return;

        int packetId = 0;

        INSTANCE.registerMessage(PacketClientTrackingSettings.Handler.class, PacketClientTrackingSettings.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(PacketTrackingSnapshot.ClientHandler.class, PacketTrackingSnapshot.class, packetId++, Side.CLIENT);
        INSTANCE.registerMessage(PacketTrackingDelta.ClientHandler.class, PacketTrackingDelta.class, packetId++, Side.CLIENT);

        initialized = true;
    }
}