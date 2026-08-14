package com.spawnspamdetector.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.spawnspamdetector.tracking.ServerTrackingManager;


public class PacketClientTrackingSettings implements IMessage {

    private boolean detectionEnabled;

    public PacketClientTrackingSettings() {
    }

    public PacketClientTrackingSettings(boolean detectionEnabled) {
        this.detectionEnabled = detectionEnabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        detectionEnabled = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeBoolean(detectionEnabled);
    }

    public boolean isDetectionEnabled() {
        return detectionEnabled;
    }

    public static class Handler implements IMessageHandler<PacketClientTrackingSettings, IMessage> {

        @Override
        public IMessage onMessage(PacketClientTrackingSettings message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> ServerTrackingManager.updateClientSettings(
                ctx.getServerHandler().player,
                message.isDetectionEnabled()
            ));
            return null;
        }
    }
}