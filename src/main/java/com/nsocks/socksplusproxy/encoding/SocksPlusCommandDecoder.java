package com.nsocks.socksplusproxy.encoding;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.internal.ObjectUtil;

import java.util.List;

import static com.nsocks.socksplusproxy.encoding.SocksPlusEncoder.COMMAND_HEADER_SIZE;

public final class SocksPlusCommandDecoder extends MessageToMessageDecoder<ByteBuf> {
    private final Socks5AddressDecoder addressDecoder;

    public SocksPlusCommandDecoder() {
        this(Socks5AddressDecoder.DEFAULT);
    }

    public SocksPlusCommandDecoder(Socks5AddressDecoder addressDecoder) {
        this.addressDecoder = ObjectUtil.checkNotNull(addressDecoder, "addressDecoder");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        final int size = msg.readInt();
        final SocksPlusCommandType type = SocksPlusCommandType.valueOf(msg.readByte());
        final int connectionId = msg.readInt();

        switch (type.byteValue()) {
            case SocksPlusCommandType.CONNECT_BYTE:
                final Socks5AddressType dstAddrType1 = Socks5AddressType.valueOf(msg.readByte());
                final String dstAddr1 = addressDecoder.decodeAddress(dstAddrType1, msg);
                final int dstPort1 = msg.readUnsignedShort();
                out.add(new DefaultSocksPlusConnectRequestCommand(dstAddrType1, dstAddr1, dstPort1, connectionId));
                break;
            case SocksPlusCommandType.CONNECT_RESPONSE_BYTE:
                final Socks5CommandStatus status2 = Socks5CommandStatus.valueOf(msg.readByte());
                final Socks5AddressType dstAddrType2 = Socks5AddressType.valueOf(msg.readByte());
                final String dstAddr2 = addressDecoder.decodeAddress(dstAddrType2, msg);
                final int dstPort2 = msg.readUnsignedShort();
                out.add(new DefaultSocksPlusConnectResponseCommand(status2, dstAddrType2, dstAddr2, dstPort2, connectionId));
                break;
            case SocksPlusCommandType.DISCONNECT_BYTE:
                out.add(new DefaultSocksPlusDisconnectCommand(connectionId));
                break;
            case SocksPlusCommandType.DATA_BYTE:
                int dataLength = size - COMMAND_HEADER_SIZE;
                //Make sure not to use `msg.retainedSlice(...)` here, because we're getting ReplayingDecoderByteBuf instance,
                // and in its implementation the method `retainedSlice(...)` doesn't retain the buffer for some reason.
                //Thus, we're using `slice(...).retain()`.
                ByteBuf slice = msg.slice(msg.readerIndex(), dataLength).retain();
                out.add(new DefaultSocksPlusDataCommand(connectionId, slice));
                msg.skipBytes(dataLength);
                break;
            default:
                throw new DecoderException("unsupported command type: " + type);
        }
    }
}
