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
import com.nsocks.socksplusproxy.encoding.DefaultSocksPlusConnectRequestCommand;
import com.nsocks.socksplusproxy.encoding.DefaultSocksPlusDataCommand;
import com.nsocks.socksplusproxy.encoding.DefaultSocksPlusInitRequest;
import com.nsocks.socksplusproxy.encoding.SocksPlusConnectResponseCommand;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Simple SSL chat client modified from TelnetClient.
 */
public final class SocksPlusClient implements Consumer<SocksPlusConnectResponseCommand> {

    static final String PROXY_HOST = System.getProperty("host", "127.0.0.1");
    static final int PROXY_PORT = Integer.parseInt(System.getProperty("port", "1081"));

    static final String DST_ADDR = "localhost";
    static final int DST_PORT = 8992;

    final AtomicInteger connectionIdCounter = new AtomicInteger();
    final ConnectionManager connectionManager = ConnectionManager.DEFAULT;
    final EventLoopGroup group = new NioEventLoopGroup();
    final Bootstrap b = new Bootstrap();

    final Map<Integer, Consumer<SocksPlusConnectResponseCommand>> connectionListeners = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        SocksPlusClient client = new SocksPlusClient();
        Channel parentChannel = client.connect().sync().channel();
        client.connectChildChannel(parentChannel, DST_ADDR, DST_PORT, (socksPlusConnectResponseCommand) -> {
            System.out.println("socksPlusConnectResponseCommand " + socksPlusConnectResponseCommand);
        });
        Integer connectionId = 1;

        // Read commands from the stdin.
        ChannelFuture lastWriteFuture = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for (;;) {
            String line = in.readLine();
            if (line == null) {
                break;
            }

            // Sends the received line to the server.
            ByteBuf message = Unpooled.copiedBuffer(line + "\r\n", Charset.defaultCharset());
            lastWriteFuture = client.sendMessage(parentChannel, connectionId, message);

            // If user typed the 'bye' command, wait until the server closes
            // the connection.
            if ("bye".equalsIgnoreCase(line)) {
                parentChannel.closeFuture().sync();
                break;
            }
        }

        // Wait until all messages are flushed before closing the channel.
        if (lastWriteFuture != null) {
            lastWriteFuture.sync();
        }
    }

    public SocksPlusClient() {
        // Configure SSL.
    }

    public ChannelFuture connect() throws InterruptedException, SSLException {
        final SslContext sslCtx = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        b.group(group)
         .channel(NioSocketChannel.class)
         .handler(new SocksPlusClientInitializer(sslCtx, connectionManager, this));

        // Start the connection attempt.
        return connectProxy(PROXY_HOST, PROXY_PORT, b);
    }

    public ChannelFuture connectProxy(String host, int port, Bootstrap b) throws InterruptedException {
        Channel ch = b.connect(host, port).sync().channel();
        return ch.writeAndFlush(new DefaultSocksPlusInitRequest());
    }

    public ChannelFuture connectChildChannel(Channel ch, String host, int port, Consumer<SocksPlusConnectResponseCommand> responseListener) {
        int connectionId = connectionIdCounter.incrementAndGet();
        connectionListeners.put(connectionId, responseListener);
        return ch.writeAndFlush(new DefaultSocksPlusConnectRequestCommand(
                Socks5AddressType.DOMAIN, host, port, connectionId));
    }

    public ChannelFuture sendMessage(Channel parentChannel, Integer connectionId, ByteBuf message) {
        return parentChannel.writeAndFlush(new DefaultSocksPlusDataCommand(connectionId, message));
    }

    @Override
    public void accept(SocksPlusConnectResponseCommand socksPlusConnectResponseCommand) {
        Consumer<SocksPlusConnectResponseCommand> listener = connectionListeners.remove(socksPlusConnectResponseCommand.connectionId());
        if (listener != null) {
            listener.accept(socksPlusConnectResponseCommand);
        }
    }
}
