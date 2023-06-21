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

import com.nsocks.socksplusproxy.encoding.SocksPlusCommandDecoder;
import com.nsocks.socksplusproxy.encoding.SocksPlusEncoder;
import com.nsocks.socksplusproxy.encoding.SocksPlusServerDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

public final class SocksPlusServerInitializer extends ChannelInitializer<SocketChannel> {
    private final SslContext sslCtx;
    private final ConnectionManager connectionManager;

    public SocksPlusServerInitializer(SslContext sslCtx, ConnectionManager connectionManager) {
        this.sslCtx = sslCtx;
        this.connectionManager = connectionManager;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(
                new LoggingHandler(LogLevel.DEBUG),
                sslCtx.newHandler(ch.alloc()),
                SocksPlusEncoder.DEFAULT,
                new SocksPlusServerDecoder(),
                new SocksPlusCommandDecoder(),
                new SocksPlusServerHandler(connectionManager));
    }
}
