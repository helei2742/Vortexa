package cn.com.vortexa.nameserver.server;

import cn.com.vortexa.nameserver.config.NameserverServerConfig;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.dto.RequestHandleResult;
import cn.com.vortexa.nameserver.dto.ServiceInstance;
import cn.com.vortexa.nameserver.exception.CustomCommandException;
import cn.com.vortexa.nameserver.exception.NameserverException;
import cn.com.vortexa.nameserver.handler.CustomRequestHandler;
import cn.com.vortexa.nameserver.protocol.Serializer;
import cn.com.vortexa.nameserver.service.impl.FileRegistryService;
import cn.com.vortexa.nameserver.service.impl.MemoryConnectionService;

import cn.com.vortexa.nameserver.util.DistributeIdMaker;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

class NameserverServiceTest {

    private static final Logger log = LoggerFactory.getLogger(NameserverServiceTest.class);
    static NameserverServerConfig nameserverServerConfig;
    static NameserverService nameserverService;
    static Random random = new Random();
    static ServiceInstance clientInstance = ServiceInstance
            .builder()
            .group("test1")
            .serviceId("client")
            .instanceId("client-1")
            .build();
    static int command_client = 3001;
    static int command_service = 3002;

    @BeforeAll
    public static void setUp() throws NameserverException {
        nameserverServerConfig = new NameserverServerConfig();

        ServiceInstance address = ServiceInstance.builder()
                .group("default")
                .serviceId("sahara")
                .instanceId("sahara_test")
                .host("127.0.0.1")
                .port(8080)
                .build();

        nameserverServerConfig.setServiceInstance(address);
        nameserverService = new NameserverService(NameserverServerConfig.DEFAULT);
        nameserverService.init(new FileRegistryService(), new MemoryConnectionService());
    }

    @Test
    public void test() throws NameserverException, InterruptedException, CustomCommandException, ExecutionException {
        nameserverService.addCustomCommandHandler(command_client, new CustomRequestHandler() {
            @Override
            public RequestHandleResult handlerRequest(RemotingCommand request) {
                log.warn("收到客户端自定义命令[{}]", request);

                return RequestHandleResult.success("test-" + random.nextInt());
            }
        });

        nameserverService.start().get();

        while (true) {
            RemotingCommand remotingCommand = new RemotingCommand();
            remotingCommand.setTransactionId(
                    DistributeIdMaker.DEFAULT.nextId(nameserverServerConfig.getServiceInstance().toString())
            );
            remotingCommand.setFlag(command_service);
            TimeUnit.SECONDS.sleep(5);

            log.info("发送服务端自定义命令.{}", remotingCommand);
            nameserverService.sendCommandToServiceInstance(
                    clientInstance.getGroup(),
                    clientInstance.getServiceId(),
                    clientInstance.getInstanceId(),
                    remotingCommand
            ).thenAccept(response->{
                String deserialize = Serializer.Algorithm.Protostuff.deserialize(response.getBody(), String.class);
                log.info("收到服务端自定义命令响应. {}\n{}", response, deserialize);
            });
        }
    }
}
