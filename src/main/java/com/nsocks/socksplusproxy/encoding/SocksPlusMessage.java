package com.nsocks.socksplusproxy.encoding;

import io.netty.handler.codec.DecoderResultProvider;

/**
 * A tag interface that all SOCKS+ protocol messages implement.
 */
public interface SocksPlusMessage extends DecoderResultProvider {
}
