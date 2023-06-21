package com.nsocks.socksplusproxy.encoding;

import com.nsocks.socksplusproxy.SocksPlusVersion;

public interface SocksPlusInitRequest extends SocksPlusMessage {
    /**
     * Returns the protocol version of this message.
     */
    SocksPlusVersion version();
}
