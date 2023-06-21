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
import com.nsocks.socksplusproxy.encoding.SocksPlusCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusConnectRequestCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusConnectResponseCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusDataCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusDisconnectCommand;
import com.nsocks.util.ServerUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@ChannelHandler.Sharable
public final class SocksPlusClientCommandHandler extends SimpleChannelInboundHandler<SocksPlusCommand> {
    private final ConnectionManager connectionManager;
    private final Consumer<SocksPlusConnectResponseCommand> connectResponseConsumer;

    public SocksPlusClientCommandHandler(ConnectionManager connectionManager,
                                         Consumer<SocksPlusConnectResponseCommand> connectResponseConsumer) {
        this.connectionManager = connectionManager;
        this.connectResponseConsumer = connectResponseConsumer;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final SocksPlusCommand message) {
        final Channel parentChannel = ctx.channel();
        if (message instanceof SocksPlusConnectRequestCommand) {
            throw new IllegalStateException("Client can't process SocksPlusConnectResponseCommand");
        } else if (message instanceof SocksPlusConnectResponseCommand) {
            SocksPlusConnectResponseCommand command = (SocksPlusConnectResponseCommand)message;
            connectResponseConsumer.accept(command);
            if (command.status() == Socks5CommandStatus.SUCCESS) {
                //TODO: child channel registration confirmed, no-op?

            } else {
                //TODO: unregister and close child channel
                Channel childChannel = connectionManager.removeChildChannel(parentChannel, command.connectionId());
                childChannel.close();
            }
        } else if (message instanceof SocksPlusDisconnectCommand) {
            SocksPlusDisconnectCommand command = (SocksPlusDisconnectCommand)message;
            Channel childChannel = connectionManager.removeChildChannel(parentChannel, command.connectionId());
            childChannel.close();
        } else if (message instanceof SocksPlusDataCommand) {
            //TODO: if channel not found, send Disconnect command back to the server
            SocksPlusDataCommand command = (SocksPlusDataCommand)message;

            System.out.println(command.data().toString(StandardCharsets.US_ASCII));
            command.data().release();

            //TODO: do pumping
/*            Channel childChannel = connectionManager.getChildChannel(parentChannel, command.connectionId());
            childChannel.writeAndFlush(command.data()).addListener(future -> command.data().release());*/
        } else {
            ctx.close();
        }


/*
        if (message instanceof Socks5CommandResponse) {
            final Socks5CommandResponse response = (Socks5CommandResponse)message;
            if (response.status() == Socks5CommandStatus.SUCCESS) {
                //ALL GOOD, connection successful

//                System.out.println(showPipeline(ctx.pipeline()));

                ctx.pipeline().remove(SocksPlusClientCommandHandler.this);
                ctx.pipeline().remove(Socks5CommandResponseDecoder.class);
                ctx.pipeline().remove(Socks5InitialResponseDecoder.class);
                ctx.pipeline().remove(Socks5ClientEncoder.class);

                ctx.pipeline().addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                ctx.pipeline().addLast(new StringDecoder());
                ctx.pipeline().addLast(new StringEncoder());

                // and then business logic.
                ctx.pipeline().addLast(new SecureChatClientHandler());

//                System.out.println(showPipeline(ctx.pipeline()));

                //outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                //ctx.pipeline().addLast(new RelayHandler(outboundChannel));
            } else {
                ctx.close();
            }
        } else {
            ctx.close();
        }*/
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ServerUtil.closeOnFlush(ctx.channel());
    }
}
