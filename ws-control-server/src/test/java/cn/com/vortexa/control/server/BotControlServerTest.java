package cn.com.vortexa.control.server;

import cn.com.vortexa.control.BotControlServer;
import cn.com.vortexa.control.TestRpc;
import cn.com.vortexa.control.config.ControlServerConfig;
import cn.com.vortexa.control.dto.RPCArgsWrapper;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.RequestHandleResult;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.exception.ControlServerException;
import cn.com.vortexa.control.handler.CustomRequestHandler;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.control.service.impl.FileRegistryService;
import cn.com.vortexa.control.service.impl.MemoryConnectionService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.ExecutionException;

class BotControlServerTest {

    private static final Logger log = LoggerFactory.getLogger(BotControlServerTest.class);
    static ControlServerConfig controlServerConfig;
    static BotControlServer botControlServer;
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
    public static void setUp() throws ControlServerException, FileNotFoundException {
        controlServerConfig = new ControlServerConfig();

        ServiceInstance address = ServiceInstance.builder()
                .group("default")
                .serviceId("sahara")
                .instanceId("sahara_test")
                .host("127.0.0.1")
                .port(8080)
                .build();


        controlServerConfig.setServiceInstance(address);
        botControlServer = new BotControlServer(ControlServerConfig.defaultConfig());
        botControlServer.init(new FileRegistryService(botControlServer.getExecutorService()), new MemoryConnectionService());
    }

    @Test
    public void test() throws ControlServerException, InterruptedException, CustomCommandException, ExecutionException, NoSuchMethodException {
        Class<TestRpc> rpcClass = TestRpc.class;
        String name = rpcClass.getName();
        Method testMethod = rpcClass.getMethod("test", String.class);
        TestRPCImpl testRPC = new TestRPCImpl();

        botControlServer.addCustomCommandHandler(name + "-" + testMethod.getName(), new CustomRequestHandler() {
            @Override
            public RequestHandleResult handlerRequest(RemotingCommand request) {
                log.warn("收到客户端自定义命令[{}]", request);
                byte[] body = request.getBody();
                RPCArgsWrapper params = Serializer.Algorithm.Protostuff.deserialize(body, RPCArgsWrapper.class);
                try {
                    return RequestHandleResult.success(testMethod.invoke(testRPC, params.getArgs()));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        botControlServer.start().get();

//        while (true) {
//            RemotingCommand remotingCommand = new RemotingCommand();
//            remotingCommand.setTransactionId(
//                    DistributeIdMaker.DEFAULT.nextId(nameserverServerConfig.getServiceInstance().toString())
//            );
//            remotingCommand.setFlag(command_service);
//            TimeUnit.SECONDS.sleep(5);
//
//            log.info("发送服务端自定义命令.{}", remotingCommand);
//            nameserverService.sendCommandToServiceInstance(
//                    clientInstance.getGroup(),
//                    clientInstance.getServiceId(),
//                    clientInstance.getInstanceId(),
//                    remotingCommand
//            ).thenAccept(response -> {
//                String deserialize = Serializer.Algorithm.Protostuff.deserialize(response.getBody(), String.class);
//                log.info("收到服务端自定义命令响应. {}\n{}", response, deserialize);
//            });
//        }

        Thread.sleep(10000000);
    }

    static class TestRPCImpl implements TestRpc {

        @Override
        public String test(String param) {
            return param + "-test-" + random.nextInt();
        }
    }
}
