package com.spawnspamdetector.network;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.spawnspamdetector.tracking.ServerTrackingManager;


public class PacketTopTrackedChunksRequest implements IMessage {

    int limit = 0;

    public PacketTopTrackedChunksRequest() {
    }

    public PacketTopTrackedChunksRequest(int limit) {
        this.limit = limit;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        limit = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(limit);
    }

    public static class Handler implements IMessageHandler<PacketTopTrackedChunksRequest, IMessage> {

        @Override
        public IMessage onMessage(PacketTopTrackedChunksRequest message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(
                () -> ServerTrackingManager.printTopTrackedChunks(
                    ctx.getServerHandler().player,
                    message.limit
                ));
            return null;
        }
    }
}
