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

import com.nsocks.socksplusproxy.encoding.DefaultSocksPlusInitResponse;
import com.nsocks.socksplusproxy.encoding.SocksPlusConnectRequestCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusDataCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusDisconnectCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusInitRequest;
import com.nsocks.socksplusproxy.encoding.SocksPlusMessage;
import com.nsocks.util.ServerUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;

public final class SocksPlusServerHandler extends SimpleChannelInboundHandler<SocksPlusMessage> {

    private final ConnectionManager connectionManager;

    public SocksPlusServerHandler(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksPlusMessage socksRequest) {
        if (socksRequest instanceof SocksPlusInitRequest) {
            ctx.write(new DefaultSocksPlusInitResponse(Socks5CommandStatus.SUCCESS));
        } else if (socksRequest instanceof SocksPlusConnectRequestCommand ||
                socksRequest instanceof SocksPlusDisconnectCommand ||
                socksRequest instanceof SocksPlusDataCommand) {
            connectionManager.addParentChannel(ctx.channel());
            ctx.pipeline().addLast(new SocksPlusServerCommandHandler(connectionManager));
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(socksRequest);
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
