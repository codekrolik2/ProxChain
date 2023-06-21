package com.nsocks.socksplusproxy.encoding;

import com.nsocks.socksplusproxy.SocksPlusVersion;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;

public interface SocksPlusInitResponse extends SocksPlusMessage {
    /**
     * Returns the protocol version of this message.
     */
    SocksPlusVersion version();

    Socks5CommandStatus status();
}
