package cn.com.vortexa;


import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.control.protocol.Serializer;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

/**
 * @author helei
 * @since 2025-03-23
 */
public class SerializeTest {

    @Test
    public void testSerialize() {
        List<BotInfo> botInfoList = List.of(
                BotInfo.builder().id(1).build(),
                BotInfo.builder().id(2).build(),
                BotInfo.builder().id(3).build(),
                BotInfo.builder().id(4).build(),
                BotInfo.builder().id(5).build()
        );


        byte[] serialize = Serializer.Algorithm.JDK.serialize(botInfoList);

        List<?> list = Serializer.Algorithm.JDK.deserialize(serialize,
                List.class);

        System.out.println(list);
    }
}
