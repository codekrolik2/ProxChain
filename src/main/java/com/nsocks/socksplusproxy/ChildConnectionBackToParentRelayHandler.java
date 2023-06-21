/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.nsocks.socksplusproxy;

import com.nsocks.socksplusproxy.encoding.DefaultSocksPlusDataCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public final class ChildConnectionBackToParentRelayHandler extends ChannelInboundHandlerAdapter {

    private final Channel parentChannel;
    private final Integer connectionId;
    private final ConnectionManager connectionManager;

    public ChildConnectionBackToParentRelayHandler(Channel parentChannel, Integer connectionId, ConnectionManager connectionManager) {
        this.parentChannel = parentChannel;
        this.connectionId = connectionId;
        this.connectionManager = connectionManager;
    }

    //TODO: channels lifecycle can be tracked here, when they're getting active and getting inactive (for monitoring UI)
    //TODO: however, channel initialization better be tracked when the connection is created, and channel info to be filled upon reception of Socks CONNECT command.

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (parentChannel.isActive()) {
            System.out.println("Relayed " + msg);
            //TODO: make sure it's ByteBuf here
            parentChannel.writeAndFlush(new DefaultSocksPlusDataCommand(connectionId, (ByteBuf)msg));
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        //TODO: remove channel from manager
        connectionManager.removeChildChannel(ctx.channel());
        //TODO: we shouldn't close parent here
        /*if (parentChannel.isActive()) {
            ServerUtil.closeOnFlush(parentChannel);
        }*/
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
