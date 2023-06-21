package com.nsocks.socksplusproxy.encoding;


import io.netty.handler.codec.DecoderResult;

import io.netty.util.internal.ObjectUtil;

/**
 * An abstract {@link SocksPlusMessage}.
 */
public abstract class AbstractSocksPlusMessage implements SocksPlusMessage {
    private DecoderResult decoderResult = DecoderResult.SUCCESS;

    @Override
    public DecoderResult decoderResult() {
        return decoderResult;
    }

    @Override
    public void setDecoderResult(DecoderResult decoderResult) {
        this.decoderResult = ObjectUtil.checkNotNull(decoderResult, "decoderResult");
    }
}
