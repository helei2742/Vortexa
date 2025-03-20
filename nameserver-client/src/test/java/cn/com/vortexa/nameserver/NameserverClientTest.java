package cn.com.vortexa.nameserver;

import cn.com.vortexa.nameserver.config.NameserverClientConfig;
import cn.com.vortexa.nameserver.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.dto.RequestHandleResult;
import cn.com.vortexa.nameserver.exception.CustomCommandException;
import cn.com.vortexa.nameserver.protocol.Serializer;
import cn.com.vortexa.nameserver.util.DistributeIdMaker;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
class NameserverClientTest {

    private static NameserverClient nameserverClient;

    static int command_client = 3001;
    static int command_service = 3002;
    static Random random = new Random();

    @BeforeAll
    static void setUpBeforeClass() throws Exception {

        nameserverClient = new NameserverClient(NameserverClientConfig.DEFAULT);
    }

    @Test
    public void test() throws ExecutionException, InterruptedException, CustomCommandException {
        nameserverClient.addCustomCommandHandler(
                command_service,
                request -> {
                    log.info("收到服务端自定义命令");
                    return RequestHandleResult.success("客户端响应-" + random.nextInt());
                }
        );

        nameserverClient.setAfterRegistryHandler(response -> {
            log.info("send 发送客户端自定义命令 {}", command_client);

            RemotingCommand remotingCommand = new RemotingCommand();
            remotingCommand.setFlag(command_client);
            remotingCommand.setCode(RemotingCommandCodeConstants.SUCCESS);
            remotingCommand.setTransactionId(
                    DistributeIdMaker.DEFAULT.nextId(nameserverClient.getName())
            );
            nameserverClient.sendRequest(remotingCommand).thenAccept(customResponse -> {
                String deserialize = Serializer.Algorithm.Protostuff.deserialize(customResponse.getBody(),
                        String.class);
                log.info("收到客户端自定义命令响应[{}]\n{}", customResponse, deserialize);
            });
        });

        CompletableFuture<Boolean> connect = nameserverClient.connect();
        connect.get();

        TimeUnit.SECONDS.sleep(10000000);
    }
}
