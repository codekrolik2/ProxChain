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

import com.nsocks.socksplusproxy.encoding.DefaultSocksPlusConnectResponseCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusCommandType;
import com.nsocks.socksplusproxy.encoding.SocksPlusConnectRequestCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusConnectResponseCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusDataCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusDisconnectCommand;
import com.nsocks.util.ServerUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

public final class SocksPlusServerCommandHandler extends SimpleChannelInboundHandler<SocksPlusCommand> {

    private final ConnectionManager connectionManager;
    private final Bootstrap b = new Bootstrap();

    public SocksPlusServerCommandHandler(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final SocksPlusCommand message) {
        final Channel parentChannel = ctx.channel();
        if (message instanceof SocksPlusConnectRequestCommand) {
            final SocksPlusConnectRequestCommand command = (SocksPlusConnectRequestCommand)message;
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener(
                    (FutureListener<Channel>) future -> {
                        final Channel outboundChannel = future.getNow();
                        if (future.isSuccess()) {
                            connectionManager.addChildChannel(parentChannel, message.connectionId(), outboundChannel);

                            ChannelFuture responseFuture =
                                    ctx.channel().writeAndFlush(new DefaultSocksPlusConnectResponseCommand(
                                            Socks5CommandStatus.SUCCESS,
                                            command.dstAddrType(),
                                            command.dstAddr(),
                                            command.dstPort(),
                                            command.connectionId()));

                            responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                                //TODO: Figure out what handlers to remove here
/*                                ctx.pipeline().remove(SocksServerConnectHandler.this);
                                ctx.pipeline().remove(Socks5CommandRequestDecoder.class);
                                ctx.pipeline().remove(Socks5InitialRequestDecoder.class);
                                ctx.pipeline().remove(Socks5ServerEncoder.class);*/

                                //TODO: Figure out how to establish a relay here
                                //TODO: ClientBackToParentRelayHandler for backward traffic channel
                                //TODO: forward traffic will be handled by this handler: branch [~if (message instanceof SocksPlusDataCommand)~]
                                outboundChannel.pipeline().addLast(new ChildConnectionBackToParentRelayHandler(ctx.channel(), command.connectionId(), connectionManager));
/*                                outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                ctx.pipeline().addLast(new RelayHandler(outboundChannel));*/

                                System.out.println("CHILD CONNECTED TO " + command.dstAddr() + ":" + command.dstPort() + " connectionId " + command.connectionId());

//                                System.out.println(showPipeline(ctx.pipeline()));
//                                System.out.println(showPipeline(outboundChannel.pipeline()));
                            });
                        } else {
                            ctx.channel().writeAndFlush(new DefaultSocksPlusConnectResponseCommand(
                                    Socks5CommandStatus.FAILURE,
                                    command.dstAddrType(),
                                    command.dstAddr(),
                                    command.dstPort(),
                                    command.connectionId()));
                            ServerUtil.closeOnFlush(ctx.channel());
                        }
                    });

            final Channel inboundChannel = ctx.channel();
            b.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new DirectClientHandler(promise));

            b.connect(command.dstAddr(), command.dstPort()).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    // Connection established use handler provided results
                } else {
                    // Close the connection if the connection attempt has failed.
                    ctx.channel().writeAndFlush(
                            new DefaultSocksPlusConnectResponseCommand(
                                    Socks5CommandStatus.FAILURE,
                                    command.dstAddrType(),
                                    command.dstAddr(),
                                    command.dstPort(),
                                    command.connectionId()));
                    ServerUtil.closeOnFlush(ctx.channel());
                }
            });
        } else if (message instanceof SocksPlusConnectResponseCommand) {
            throw new IllegalStateException("Server can't process SocksPlusConnectResponseCommand");
        } else if (message instanceof SocksPlusDisconnectCommand) {
            SocksPlusDisconnectCommand command = (SocksPlusDisconnectCommand)message;
            Channel childChannel = connectionManager.removeChildChannel(parentChannel, command.connectionId());
            childChannel.close();
        } else if (message instanceof SocksPlusDataCommand) {
            //TODO: if channel not found, send Disconnect command back to the client
            SocksPlusDataCommand command = (SocksPlusDataCommand)message;
            Channel childChannel = connectionManager.getChildChannel(parentChannel, command.connectionId());
            childChannel.writeAndFlush(command.data());
        } else {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ServerUtil.closeOnFlush(ctx.channel());
    }
}
