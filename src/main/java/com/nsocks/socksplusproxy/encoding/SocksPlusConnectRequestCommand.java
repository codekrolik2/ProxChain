package com.nsocks.socksplusproxy.encoding;

import io.netty.handler.codec.socksx.v5.Socks5AddressType;

public interface SocksPlusConnectRequestCommand extends SocksPlusCommand {
    Socks5AddressType dstAddrType();

    String dstAddr();

    int dstPort();
}
