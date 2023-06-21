package com.nsocks.socksplusproxy.encoding;

import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;

public interface SocksPlusConnectResponseCommand extends SocksPlusCommand {
    Socks5CommandStatus status();

    Socks5AddressType dstAddrType();

    String dstAddr();

    int dstPort();
}
