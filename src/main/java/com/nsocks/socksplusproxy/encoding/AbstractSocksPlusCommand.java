package com.nsocks.socksplusproxy.encoding;

import io.netty.handler.codec.DecoderResult;
import io.netty.util.internal.ObjectUtil;

public abstract class AbstractSocksPlusCommand implements SocksPlusCommand {
    private DecoderResult decoderResult = DecoderResult.SUCCESS;

    @Override
    public DecoderResult decoderResult() {
        return decoderResult;
    }

    @Override
    public void setDecoderResult(DecoderResult decoderResult) {
        this.decoderResult = ObjectUtil.checkNotNull(decoderResult, "decoderResult");
    }

    private final int connectionId;
    private final SocksPlusCommandType type;

    public AbstractSocksPlusCommand(SocksPlusCommandType type, int connectionId) {
        this.type = ObjectUtil.checkNotNull(type, "type");
        this.connectionId = connectionId;
    }

    @Override
    public SocksPlusCommandType type() { return type; }

    @Override
    public int connectionId() { return connectionId; }
}
