package com.spawnspamdetector.tracking;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;


public class ServerTrackingEventHandler {

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;

        ServerTrackingManager.removeClient(((EntityPlayerMP) event.player).getServer(), event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        ServerTrackingManager.onServerTick(FMLCommonHandler.instance().getMinecraftServerInstance());
    }
}