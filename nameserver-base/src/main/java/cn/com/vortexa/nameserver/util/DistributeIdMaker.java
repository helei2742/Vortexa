package cn.com.vortexa.nameserver.util;

import cn.hutool.core.net.NetUtil;

import java.nio.ByteBuffer;
import java.util.UUID;

public interface DistributeIdMaker {
    Algorithm DEFAULT = Algorithm.SnowFlake;

    String nextId(String serviceName);

    enum Algorithm implements DistributeIdMaker {
        SnowFlake{
            private volatile SnowFlakeShortUtil snowFlakeShortUtil;
            @Override
            public String nextId(String serviceName) {
                if(snowFlakeShortUtil == null) {
                    synchronized (this) {
                        long c = serviceName.hashCode()%100000;
                        long m = ByteBuffer.wrap(NetUtil.getLocalMacAddress().getBytes()).getLong()%100000;
                        snowFlakeShortUtil = new SnowFlakeShortUtil(Math.abs(c), Math.abs(m));
                    }
                }
                return String.valueOf(snowFlakeShortUtil.nextId());
            }
        },
        RandomUUID {
            @Override
            public String nextId(String serviceName) {
                return UUID.randomUUID().toString();
            }
        }
    }
}
