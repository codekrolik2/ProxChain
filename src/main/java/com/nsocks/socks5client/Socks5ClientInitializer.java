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

import com.nsocks.securechat.SecureChatClient;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponseDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
public class Socks5ClientInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final String dstAddr;
    private final int dstPort;

    public Socks5ClientInitializer(SslContext sslCtx, String dstAddr, int dstPort) {
        this.sslCtx = sslCtx;
        this.dstAddr = dstAddr;
        this.dstPort = dstPort;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(sslCtx.newHandler(ch.alloc()));

        pipeline.addLast(new Socks5InitialResponseDecoder());
        pipeline.addLast(Socks5ClientEncoder.DEFAULT);

        // and then business logic.
        pipeline.addLast(new Socks5ClientHandler(dstAddr, dstPort));
    }
}
