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
package com.nsocks.socks5client;

import com.nsocks.securechat.SecureChatClientHandler;
import com.nsocks.util.ServerUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponseDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponseDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import static com.nsocks.util.ServerUtil.showPipeline;

@ChannelHandler.Sharable
public final class Socks5ClientConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) {
        if (message instanceof Socks5CommandResponse) {
            final Socks5CommandResponse response = (Socks5CommandResponse)message;
            if (response.status() == Socks5CommandStatus.SUCCESS) {
                //ALL GOOD, connection successful

//                System.out.println(showPipeline(ctx.pipeline()));

                ctx.pipeline().remove(Socks5ClientConnectHandler.this);
                ctx.pipeline().remove(Socks5CommandResponseDecoder.class);
                ctx.pipeline().remove(Socks5InitialResponseDecoder.class);
                ctx.pipeline().remove(Socks5ClientEncoder.class);

                ctx.pipeline().addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                ctx.pipeline().addLast(new StringDecoder());
                ctx.pipeline().addLast(new StringEncoder());

                // and then business logic.
                ctx.pipeline().addLast(new SecureChatClientHandler());

//                System.out.println(showPipeline(ctx.pipeline()));

                //TODO: do pumping
                //outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                //ctx.pipeline().addLast(new RelayHandler(outboundChannel));
            } else {
                ctx.close();
            }
        } else {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ServerUtil.closeOnFlush(ctx.channel());
    }
}
