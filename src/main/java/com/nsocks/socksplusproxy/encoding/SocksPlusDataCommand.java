package com.nsocks.socksplusproxy.encoding;

import io.netty.buffer.ByteBuf;

public interface SocksPlusDataCommand extends SocksPlusCommand {
    ByteBuf data();
}
