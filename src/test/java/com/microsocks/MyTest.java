package com.microsocks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class MyTest {
    @Test
    public void testRetainedSlice() {
        ByteBuf buf1 = PooledByteBufAllocator.DEFAULT.directBuffer();
        buf1.writeInt(1);

        ByteBuf slice1 = buf1.slice();
        slice1.retain();
        Assert.assertEquals(2, buf1.refCnt());
        Assert.assertEquals(2, slice1.refCnt());


        ByteBuf buf2 = PooledByteBufAllocator.DEFAULT.directBuffer();
        buf2.writeInt(1);

        ByteBuf slice2 = buf2.retainedSlice();
        Assert.assertEquals(2, buf2.refCnt());
        Assert.assertEquals(1, slice2.refCnt());
    }
}
