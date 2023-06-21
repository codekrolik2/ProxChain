package com.nsocks.socksplusproxy;

import io.netty.handler.codec.socksx.SocksVersion;

public enum SocksPlusVersion {
    /**
     * SOCKS+ protocol version
     */
    SOCKS_PLUS((byte) 0x36),//54 decimal -> 5+
    /**
     * Unknown protocol version
     */
    UNKNOWN((byte) 0xff);

    /**
     * Returns the {@link SocksVersion} that corresponds to the specified version field value,
     * as defined in the protocol specification.
     *
     * @return {@link #UNKNOWN} if the specified value does not represent a known SOCKS+ protocol version
     */
    public static SocksPlusVersion valueOf(byte b) {
        if (b == SOCKS_PLUS.byteValue()) {
            return SOCKS_PLUS;
        }
        return UNKNOWN;
    }

    private final byte b;

    SocksPlusVersion(byte b) {
        this.b = b;
    }

    /**
     * Returns the value of the version field, as defined in the protocol specification.
     */
    public byte byteValue() {
        return b;
    }
}
