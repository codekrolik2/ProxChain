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
import com.nsocks.socksplusproxy.encoding.SocksPlusClientDecoder;
import com.nsocks.socksplusproxy.encoding.SocksPlusConnectResponseCommand;
import com.nsocks.socksplusproxy.encoding.SocksPlusEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;

import java.util.function.Consumer;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
public class SocksPlusClientInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final ConnectionManager connectionManager;
    private final Consumer<SocksPlusConnectResponseCommand> connectResponseConsumer;

    public SocksPlusClientInitializer(SslContext sslCtx, ConnectionManager connectionManager, Consumer<SocksPlusConnectResponseCommand> connectResponseConsumer) {
        this.sslCtx = sslCtx;
        this.connectionManager = connectionManager;
        this.connectResponseConsumer = connectResponseConsumer;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(sslCtx.newHandler(ch.alloc()));

        pipeline.addLast(SocksPlusEncoder.DEFAULT);
        pipeline.addLast(new SocksPlusClientDecoder());

        // and then business logic.
        pipeline.addLast(new SocksPlusClientHandler(connectionManager, connectResponseConsumer));
    }
}
