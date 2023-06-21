package com.nsocks.socksplusproxy.encoding;

import java.util.List;

import com.nsocks.socksplusproxy.SocksPlusVersion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;

/**
 * Decodes a single {@link SocksPlusServerDecoder} from the inbound {@link ByteBuf}s.
 * On successful decode, this decoder will forward the received data to the next handler, so that
 * other handler can remove or replace this decoder later.  On failed decode, this decoder will
 * discard the received data, so that other handler closes the connection later.
 */
public final class SocksPlusServerDecoder extends ReplayingDecoder<SocksPlusServerDecoder.State> {

    enum State {
        INIT,
        COMMAND,
        FAILURE
    }

    private final SocksPlusCommandDecoder commandDecoder;

    public SocksPlusServerDecoder() {
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

                    out.add(new DefaultSocksPlusInitRequest());
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
