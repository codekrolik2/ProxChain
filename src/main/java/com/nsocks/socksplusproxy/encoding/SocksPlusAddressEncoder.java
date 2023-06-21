package com.nsocks.socksplusproxy.encoding;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;

public interface SocksPlusAddressEncoder extends Socks5AddressEncoder {
    SocksPlusAddressEncoder DEFAULT = new SocksPlusAddressEncoder() {
        @Override
        public void encodeAddress(Socks5AddressType addrType, String addrValue, ByteBuf out) {
            byte typeVal = addrType.byteValue();
            if (typeVal == Socks5AddressType.IPv4.byteValue()) {
                if (addrValue != null) {
                    out.writeBytes(NetUtil.createByteArrayFromIpAddressString(addrValue));
                } else {
                    out.writeInt(0);
                }
            } else if (typeVal == Socks5AddressType.DOMAIN.byteValue()) {
                if (addrValue != null) {
                    out.writeByte(addrValue.length());
                    out.writeCharSequence(addrValue, CharsetUtil.US_ASCII);
                } else {
                    out.writeByte(0);
                }
            } else {
                if (typeVal != Socks5AddressType.IPv6.byteValue()) {
                    throw new EncoderException("unsupported addrType: " + (addrType.byteValue() & 255));
                }

                if (addrValue != null) {
                    out.writeBytes(NetUtil.createByteArrayFromIpAddressString(addrValue));
                } else {
                    out.writeLong(0L);
                    out.writeLong(0L);
                }
            }
        }

        @Override
        public int addressSize(Socks5AddressType addrType, String addrValue) {
            int size = 0;

            byte typeVal = addrType.byteValue();
            if (typeVal == Socks5AddressType.IPv4.byteValue()) {
                if (addrValue != null) {
                    size += NetUtil.createByteArrayFromIpAddressString(addrValue).length;
                } else {
                    size += 4;
                }
            } else if (typeVal == Socks5AddressType.DOMAIN.byteValue()) {
                if (addrValue != null) {
                    size += 1 + addrValue.length();
                } else {
                    size += 1;
                }
            } else {
                if (typeVal != Socks5AddressType.IPv6.byteValue()) {
                    throw new EncoderException("unsupported addrType: " + (addrType.byteValue() & 255));
                }

                if (addrValue != null) {
                    size += NetUtil.createByteArrayFromIpAddressString(addrValue).length;
                } else {
                    size += 8 + 8;
                }
            }
            return size;
        }
    };

    int addressSize(Socks5AddressType addrType, String addrValue) throws Exception;
}
