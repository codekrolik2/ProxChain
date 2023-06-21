package com.nsocks.socksplusproxy.encoding;

public class DefaultSocksPlusDisconnectCommand extends AbstractSocksPlusCommand implements SocksPlusDisconnectCommand {
    public DefaultSocksPlusDisconnectCommand(int connectionId) {
        super(SocksPlusCommandType.DISCONNECT, connectionId);
    }
}
