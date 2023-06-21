package com.nsocks.socksplusproxy.encoding;

import com.nsocks.socksplusproxy.SocksPlusVersion;
import io.netty.handler.codec.DecoderResult;
import io.netty.util.internal.StringUtil;

public class DefaultSocksPlusInitRequest extends AbstractSocksPlusMessage implements SocksPlusInitRequest {
    @Override
    public final SocksPlusVersion version() {
        return SocksPlusVersion.SOCKS_PLUS;
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
