package com.nsocks.socksplusproxy.encoding;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderResult;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

public class DefaultSocksPlusDataCommand extends AbstractSocksPlusCommand implements SocksPlusDataCommand {

    private final ByteBuf data;

    public DefaultSocksPlusDataCommand(int connectionId, ByteBuf data) {
        super(SocksPlusCommandType.DATA, connectionId);

        this.data = ObjectUtil.checkNotNull(data, "data");
    }

    @Override
    public ByteBuf data() { return data; }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(128);
        buf.append(StringUtil.simpleClassName(this));

        DecoderResult decoderResult = decoderResult();
        if (!decoderResult.isSuccess()) {
            buf.append("(decoderResult: ");
            buf.append(decoderResult);
            buf.append(", type: ");
        } else {
            buf.append("(type: ");
        }
        buf.append(type());
        buf.append(", data: ");
        buf.append(data());
        buf.append(", connectionId: ");
        buf.append(connectionId());
        buf.append(')');

        return buf.toString();
    }
}
