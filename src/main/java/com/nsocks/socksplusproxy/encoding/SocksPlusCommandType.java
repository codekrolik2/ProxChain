package com.nsocks.socksplusproxy.encoding;

import io.netty.util.internal.ObjectUtil;

import javax.annotation.Nullable;

public class SocksPlusCommandType implements Comparable<SocksPlusCommandType> {
    public static final byte CONNECT_BYTE = (byte)1;
    public static final byte DATA_BYTE = (byte)2;
    public static final byte DISCONNECT_BYTE = (byte)3;
    public static final byte CONNECT_RESPONSE_BYTE = (byte)4;

    public static final SocksPlusCommandType CONNECT = new SocksPlusCommandType(CONNECT_BYTE, "CONNECT");
    public static final SocksPlusCommandType DATA = new SocksPlusCommandType(DATA_BYTE, "DATA");
    public static final SocksPlusCommandType DISCONNECT = new SocksPlusCommandType(DISCONNECT_BYTE, "DISCONNECT");
    public static final SocksPlusCommandType CONNECT_RESPONSE = new SocksPlusCommandType(CONNECT_RESPONSE_BYTE, "CONNECT_RESPONSE");

    private final byte byteValue;
    private final String name;
    private @Nullable String text;

    public static SocksPlusCommandType valueOf(byte b) {
        switch(b) {
            case 1:
                return CONNECT;
            case 2:
                return DATA;
            case 3:
                return DISCONNECT;
            case 4:
                return CONNECT_RESPONSE;
            default:
                return new SocksPlusCommandType(b);
        }
    }

    public SocksPlusCommandType(int byteValue) {
        this(byteValue, "UNKNOWN");
    }

    public SocksPlusCommandType(int byteValue, String name) {
        this.name = (String) ObjectUtil.checkNotNull(name, "name");
        this.byteValue = (byte)byteValue;
    }

    public byte byteValue() {
        return this.byteValue;
    }

    public int hashCode() {
        return this.byteValue;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SocksPlusCommandType)) {
            return false;
        } else {
            return this.byteValue == ((SocksPlusCommandType)obj).byteValue;
        }
    }

    public int compareTo(SocksPlusCommandType o) {
        return this.byteValue - o.byteValue;
    }

    public String toString() {
        String text = this.text;
        if (text == null) {
            this.text = text = this.name + '(' + (this.byteValue & 255) + ')';
        }

        return text;
    }
}