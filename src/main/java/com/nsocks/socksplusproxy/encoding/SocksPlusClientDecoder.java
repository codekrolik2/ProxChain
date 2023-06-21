package com.nsocks.socksplusproxy.encoding;

import com.nsocks.socksplusproxy.SocksPlusVersion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;

import java.util.List;

/**
 * Decodes a single {@link SocksPlusClientDecoder} from the inbound {@link ByteBuf}s.
 * On successful decode, this decoder will forward the received data to the next handler, so that
 * other handler can remove or replace this decoder later.  On failed decode, this decoder will
 * discard the received data, so that other handler closes the connection later.
 */
public final class SocksPlusClientDecoder extends ReplayingDecoder<SocksPlusClientDecoder.State> {

    enum State {
        INIT,
        COMMAND,
        FAILURE
    }

    private final SocksPlusCommandDecoder commandDecoder;

    public SocksPlusClientDecoder() {
        super(State.INIT);
        commandDecoder = new SocksPlusCommandDecoder();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            switch (state()) {
                case INIT: {
                    final byte version = in.readByte();
                    if (version != SocksPlusVersion.SOCKS_PLUS.byteValue()) {
                        throw new DecoderException(
                                "unsupported version: " + version + " (expected: " + SocksPlusVersion.SOCKS_PLUS.byteValue() + ')');
                    }

                    final Socks5CommandStatus status = Socks5CommandStatus.valueOf(in.readByte());
                    out.add(new DefaultSocksPlusInitResponse(status));
                    checkpoint(State.COMMAND);
                    break;
                }
                case COMMAND: {
                    commandDecoder.decode(ctx, in, out);
                    break;
                }
                case FAILURE: {
                    in.skipBytes(actualReadableBytes());
                    break;
                }
            }
        } catch (Exception e) {
            fail(out, e);
        }
    }

    private void fail(List<Object> out, Exception cause) {
        if (!(cause instanceof DecoderException)) {
            cause = new DecoderException(cause);
        }

        checkpoint(State.FAILURE);

        SocksPlusMessage m = new DefaultSocksPlusInitRequest();
        m.setDecoderResult(DecoderResult.failure(cause));
        out.add(m);
    }
}
