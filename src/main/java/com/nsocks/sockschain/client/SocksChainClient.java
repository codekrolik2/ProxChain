package com.nsocks.sockschain.client;

import com.google.common.base.Preconditions;
import com.nsocks.sockschain.config.SocksNode;
import com.nsocks.sockschain.config.SocksProtocolVersion;
import com.nsocks.socksproxy.RelayHandler;
import com.nsocks.util.ServerUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.nsocks.util.ServerUtil.showPipeline;

public class SocksChainClient {
    private final ChannelHandlerContext inboundCtx;
    private final Channel inboundChannel;
    private final SocksMessage inboundMessage;
    private final List<SocksNode> socksProxyChain;

    private final SocksMessage downstreamResponseSuccess;
    private final SocksMessage downstreamResponseFailure;

    private final Bootstrap b = new Bootstrap();
    private final AtomicInteger nodeIndex = new AtomicInteger(-1);
    private final AtomicReference<Channel> outgoingChannel = new AtomicReference<>();

    private Channel outgoingChannel() {
        return Preconditions.checkNotNull(outgoingChannel.get());
    }

    public SslContext sslCtx() throws SSLException {
        return SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    }

    public SocksChainClient(final ChannelHandlerContext inboundCtx, final SocksMessage inboundMessage, List<SocksNode> socksProxyChain) {
        this.inboundCtx = inboundCtx;
        this.inboundChannel = inboundCtx.channel();
        this.inboundMessage = inboundMessage;
        this.socksProxyChain = socksProxyChain;

        if (socksProxyChain.size() < 1) {
            throw new IllegalStateException("Proxy chain can't be empty.");
        }

        if (inboundMessage instanceof Socks5CommandRequest) {
            Socks5CommandRequest incomingRequest = (Socks5CommandRequest) inboundMessage;
            downstreamResponseSuccess = new DefaultSocks5CommandResponse(
                    Socks5CommandStatus.SUCCESS,
                    incomingRequest.dstAddrType(),
                    incomingRequest.dstAddr(),
                    incomingRequest.dstPort());
            downstreamResponseFailure = new DefaultSocks5CommandResponse(
                    Socks5CommandStatus.FAILURE, incomingRequest.dstAddrType());
//        } else if (inboundMessage instanceof Socks4CommandRequest) {
                //TODO: support Socks4 responses
        } else {
            throw new UnsupportedOperationException("Non-Socks5 not supported at this time");
        }
    }

    public void connect() {
        if (nodeIndex.compareAndSet(-1, 0)) {
            connectFirstNode(socksProxyChain.get(0));
        } else {
            throw new IllegalStateException("Can't connect, already connecting");
        }
    }

    public void connectNextNode() throws SSLException {
        if (nodeIndex.get() + 1 < socksProxyChain.size()) {
            tunnelCleanup();
            nodeIndex.incrementAndGet();
            tunnelConnection();
        } else {
            //Connected to endpoint via chain, reply success and establish relay
            ChannelFuture responseFuture = inboundChannel.writeAndFlush(downstreamResponseSuccess);

            responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                //Print pipelines before
                System.out.println(showPipeline(outgoingChannel().pipeline()));
                System.out.println(showPipeline(inboundCtx.pipeline()));

                //TODO: replace with `inboundChannel.pipeline`, get rid of ctx
                //TODO: this is SOCKS5-specific, make it support other versions
                inboundCtx.pipeline().remove(Socks5CommandRequestDecoder.class);
                inboundCtx.pipeline().remove(Socks5InitialRequestDecoder.class);
                inboundCtx.pipeline().remove(Socks5ServerEncoder.class);

                tunnelCleanup();

                inboundChannel.pipeline().addLast(new RelayHandler(outgoingChannel()));
                outgoingChannel().pipeline().addLast(new RelayHandler(inboundChannel));

                //Print pipelines after
                System.out.println(showPipeline(outgoingChannel().pipeline()));
                System.out.println(showPipeline(inboundCtx.pipeline()));
            });
        }
    }

    public void connectionFailed() {
        inboundChannel.writeAndFlush(downstreamResponseFailure);
        ServerUtil.closeOnFlush(outgoingChannel());
        if (outgoingChannel.get() != null) {
            outgoingChannel().close();
        }
    }

    /** Establish entry connection */
    private void connectFirstNode(SocksNode entryNode) {
        //TODO: resolve domain names using DNS over TLS/HTTPS and connect IP only (use `connect(InetAddress inetHost, int inetPort)`).
        //TODO: cache resolved domain names locally

        b.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new EmptyPipelineChannelInitializer());

        b.connect(entryNode.serverAddress(), entryNode.serverPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                // Connection established, send initial request
                outgoingChannel.set(future.channel());
                tunnelConnection();
            } else {
                // Close the connection if the connection attempt has failed.
                connectionFailed();
            }
        });
    }

    void tunnelCleanup() {
        SocksNode currentNode = socksProxyChain.get(nodeIndex.get());

        //Print pipelines before
        System.out.println(showPipeline(outgoingChannel().pipeline()));
        System.out.println(showPipeline(inboundCtx.pipeline()));

        if (currentNode.socksProtocolVersion() == SocksProtocolVersion.SOCKS5) {
            SocksChainClientPipelineManager.cleanupSocks5Pipeline(outgoingChannel().pipeline());
        } else {
            connectionFailed();
            throw new IllegalStateException("Non-Socks5 not supported at this time");
        }

        //Print pipelines after
        System.out.println(showPipeline(outgoingChannel().pipeline()));
        System.out.println(showPipeline(inboundCtx.pipeline()));
    }

    /** Establish next nested connection */
    void tunnelConnection() throws SSLException {
        //TODO: support SOCKS versions
        SocksNode currentNode = socksProxyChain.get(nodeIndex.get());

        //Print pipelines before
        System.out.println(showPipeline(outgoingChannel().pipeline()));
        System.out.println(showPipeline(inboundCtx.pipeline()));

        if (nodeIndex.get() + 1 < socksProxyChain.size()) {
            //We have next proxy node in the chain to connect to.
            SocksNode nextNode = socksProxyChain.get(nodeIndex.get() + 1);
            if (currentNode.socksProtocolVersion() == SocksProtocolVersion.SOCKS5) {
                SocksChainClientPipelineManager.initSocks5Pipeline(outgoingChannel(), nextNode.serverAddress(), nextNode.serverPort(), this, sslCtx(), future -> {
                    //System.out.println("HANDSHAKE DONE! PROXY");
                    outgoingChannel().writeAndFlush(new DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH));
                });

            } else {
                connectionFailed();
                throw new IllegalStateException("Non-Socks5 not supported at this time");
            }
        } else {
            //Chain's completed, connect to the endpoint.
            if (currentNode.socksProtocolVersion() == SocksProtocolVersion.SOCKS5) {
                if (inboundMessage instanceof Socks5CommandRequest) {
                    Socks5CommandRequest socks5Request = (Socks5CommandRequest) inboundMessage;
                    SocksChainClientPipelineManager.initSocks5Pipeline(outgoingChannel(), socks5Request.dstAddr(), socks5Request.dstPort(), this, sslCtx(), future -> {
                        //System.out.println("HANDSHAKE DONE! ENDPOINT");
                        outgoingChannel().writeAndFlush(new DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH));
                    });

                } else {
                    connectionFailed();
                    throw new IllegalStateException("Non-Socks5 not supported at this time");
                }
            } else {
                connectionFailed();
                throw new IllegalStateException("Non-Socks5 not supported at this time");
            }
        }

        //Print pipelines after
        System.out.println(showPipeline(outgoingChannel().pipeline()));
        System.out.println(showPipeline(inboundCtx.pipeline()));
    }
}
