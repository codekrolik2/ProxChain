package com.nsocks.socksplusproxy.encoding;

import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.NetUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

import java.net.IDN;

public class DefaultSocksPlusConnectResponseCommand extends AbstractSocksPlusCommand implements SocksPlusConnectResponseCommand {

    private final Socks5CommandStatus status;

    private final Socks5AddressType dstAddrType;
    private final String dstAddr;
    private final int dstPort;

    public DefaultSocksPlusConnectResponseCommand(Socks5CommandStatus status, Socks5AddressType dstAddrType, String dstAddr, int dstPort, int connectionId) {
        super(SocksPlusCommandType.CONNECT_RESPONSE, connectionId);

        this.status = ObjectUtil.checkNotNull(status, "status");

        ObjectUtil.checkNotNull(dstAddrType, "dstAddrType");
        ObjectUtil.checkNotNull(dstAddr, "dstAddr");

        if (dstAddrType == Socks5AddressType.IPv4) {
            if (!NetUtil.isValidIpV4Address(dstAddr)) {
                throw new IllegalArgumentException("dstAddr: " + dstAddr + " (expected: a valid IPv4 address)");
            }
        } else if (dstAddrType == Socks5AddressType.DOMAIN) {
            dstAddr = IDN.toASCII(dstAddr);
            if (dstAddr.length() > 255) {
                throw new IllegalArgumentException("dstAddr: " + dstAddr + " (expected: less than 256 chars)");
            }
        } else if (dstAddrType == Socks5AddressType.IPv6) {
            if (!NetUtil.isValidIpV6Address(dstAddr)) {
                throw new IllegalArgumentException("dstAddr: " + dstAddr + " (expected: a valid IPv6 address");
            }
        }

        if (dstPort < 0 || dstPort > 65535) {
            throw new IllegalArgumentException("dstPort: " + dstPort + " (expected: 0~65535)");
        }

        this.dstAddrType = dstAddrType;
        this.dstAddr = dstAddr;
        this.dstPort = dstPort;
    }

    @Override
    public Socks5CommandStatus status() { return status; }

    @Override
    public Socks5AddressType dstAddrType() { return dstAddrType; }

    @Override
    public String dstAddr() { return dstAddr; }

    @Override
    public int dstPort() { return dstPort; }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(128);
        buf.append(StringUtil.simpleClassName(this));

        DecoderResult decoderResult = decoderResult();
        if (!decoderResult.isSuccess()) {
            buf.append("(decoderResult: ");
            buf.append(decoderResult);
            buf.append(", type: ");
        } else {
            buf.append("(type: ");
        }
        buf.append(type());
        buf.append(", status: ");
        buf.append(status());
        buf.append(", dstAddrType: ");
        buf.append(dstAddrType());
        buf.append(", dstAddr: ");
        buf.append(dstAddr());
        buf.append(", dstPort: ");
        buf.append(dstPort());
        buf.append(", connectionId: ");
        buf.append(connectionId());
        buf.append(')');

        return buf.toString();
    }
}
