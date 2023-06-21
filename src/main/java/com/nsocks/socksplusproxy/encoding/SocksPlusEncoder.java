package com.nsocks.socksplusproxy.encoding;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

@ChannelHandler.Sharable
public final class SocksPlusEncoder extends MessageToByteEncoder<SocksPlusMessage> {
    public static final SocksPlusEncoder DEFAULT = new SocksPlusEncoder(SocksPlusAddressEncoder.DEFAULT);

    public static int COMMAND_HEADER_SIZE = 4 + 1 + 4;

    private final SocksPlusAddressEncoder addressEncoder;

    /**
     * Creates a new instance with the default {@link Socks5AddressEncoder}.
     */
    private SocksPlusEncoder() {
        this(SocksPlusAddressEncoder.DEFAULT);
    }

    /**
     * Creates a new instance with the specified {@link Socks5AddressEncoder}.
     */
    public SocksPlusEncoder(SocksPlusAddressEncoder addressEncoder) {
        this.addressEncoder = ObjectUtil.checkNotNull(addressEncoder, "addressEncoder");
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, SocksPlusMessage msg, ByteBuf out) throws Exception {
        if (msg instanceof SocksPlusInitRequest) {
            encodeInitRequest((SocksPlusInitRequest) msg, out);
        } else if (msg instanceof SocksPlusInitResponse) {
            encodeInitResponse((SocksPlusInitResponse) msg, out);
        } else if (msg instanceof SocksPlusConnectRequestCommand) {
            encodeConnectRequestCommand((SocksPlusConnectRequestCommand) msg, out);
        } else if (msg instanceof SocksPlusConnectResponseCommand) {
            encodeConnectResponseCommand((SocksPlusConnectResponseCommand) msg, out);
        } else if (msg instanceof SocksPlusDisconnectCommand) {
            encodeDisconnectCommand((SocksPlusDisconnectCommand) msg, out);
        } else if (msg instanceof SocksPlusDataCommand) {
            encodeDataCommand((SocksPlusDataCommand) msg, out);
        } else {
            throw new EncoderException("unsupported message type: " + StringUtil.simpleClassName(msg));
        }
    }

    private static void encodeInitRequest(SocksPlusInitRequest msg, ByteBuf out) {
        out.writeByte(msg.version().byteValue());
    }

    private static void encodeInitResponse(SocksPlusInitResponse msg, ByteBuf out) {
        out.writeByte(msg.version().byteValue());
        out.writeByte(msg.status().byteValue());
    }

    private static void encodeCommand(int size, SocksPlusCommand msg, ByteBuf out) {
        out.writeInt(size);
        out.writeByte(msg.type().byteValue());
        out.writeInt(msg.connectionId());
    }

    private void encodeConnectRequestCommand(SocksPlusConnectRequestCommand msg, ByteBuf out) throws Exception {
        encodeCommand(COMMAND_HEADER_SIZE + 1 + addressEncoder.addressSize(msg.dstAddrType(), msg.dstAddr()) + 2, msg, out);

        out.writeByte(msg.dstAddrType().byteValue());
        addressEncoder.encodeAddress(msg.dstAddrType(), msg.dstAddr(), out);
        out.writeShort(msg.dstPort());
    }

    private void encodeConnectResponseCommand(SocksPlusConnectResponseCommand msg, ByteBuf out) throws Exception {
        encodeCommand(COMMAND_HEADER_SIZE + 1 + 1 + addressEncoder.addressSize(msg.dstAddrType(), msg.dstAddr()) + 2, msg, out);

        out.writeByte(msg.status().byteValue());
        out.writeByte(msg.dstAddrType().byteValue());
        addressEncoder.encodeAddress(msg.dstAddrType(), msg.dstAddr(), out);
        out.writeShort(msg.dstPort());
    }

    private void encodeDisconnectCommand(SocksPlusCommand msg, ByteBuf out) {
        encodeCommand(COMMAND_HEADER_SIZE, msg, out);
    }

    private void encodeDataCommand(SocksPlusDataCommand msg, ByteBuf out) {
        encodeCommand(COMMAND_HEADER_SIZE + msg.data().readableBytes(), msg, out);

        out.writeBytes(msg.data());
    }
}
