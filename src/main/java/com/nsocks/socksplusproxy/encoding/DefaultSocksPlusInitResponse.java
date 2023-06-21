package com.nsocks.socksplusproxy.encoding;

import com.nsocks.socksplusproxy.SocksPlusVersion;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.internal.StringUtil;

public class DefaultSocksPlusInitResponse extends AbstractSocksPlusMessage implements SocksPlusInitResponse {
    @Override
    public final SocksPlusVersion version() {
        return SocksPlusVersion.SOCKS_PLUS;
    }

    private final Socks5CommandStatus status;

    public DefaultSocksPlusInitResponse(Socks5CommandStatus status) {
        this.status = status;
    }

    public Socks5CommandStatus status() {
        return status;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(StringUtil.simpleClassName(this));
        DecoderResult decoderResult = this.decoderResult();
        if (!decoderResult.isSuccess()) {
            buf.append("(decoderResult: ");
            buf.append(decoderResult);
        }
        buf.append(')');
        return buf.toString();
    }
}
