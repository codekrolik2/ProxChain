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
package com.nsocks.socksplusclient;

import com.nsocks.socksplusproxy.ConnectionManager;
import com.nsocks.socksplusproxy.encoding.SocksPlusConnectResponseCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusInitResponse;
import com.nsocks.socksplusproxy.encoding.SocksPlusMessage;
import com.nsocks.util.ServerUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;

import java.util.function.Consumer;

/**
 * Handles a client-side channel.
 */
@ChannelHandler.Sharable
public final class SocksPlusClientHandler extends SimpleChannelInboundHandler<SocksPlusMessage> {

    private final ConnectionManager connectionManager;
    private final Consumer<SocksPlusConnectResponseCommand> connectResponseConsumer;

    public SocksPlusClientHandler(ConnectionManager connectionManager, Consumer<SocksPlusConnectResponseCommand> connectResponseConsumer) {
        this.connectionManager = connectionManager;
        this.connectResponseConsumer = connectResponseConsumer;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksPlusMessage socksResponse) {
        if (socksResponse instanceof SocksPlusInitResponse) {
            if (((SocksPlusInitResponse) socksResponse).status() == Socks5CommandStatus.SUCCESS) {
                ctx.pipeline().addLast(new SocksPlusClientCommandHandler(connectionManager, connectResponseConsumer));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(socksResponse);
            } else {
                ctx.close();
            }
        } else {
            ctx.close();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        ServerUtil.closeOnFlush(ctx.channel());
    }
}
