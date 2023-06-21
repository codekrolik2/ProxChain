package com.nsocks.socksplusproxy.encoding;

public interface SocksPlusCommand extends SocksPlusMessage {
    SocksPlusCommandType type();
    int connectionId();
}
