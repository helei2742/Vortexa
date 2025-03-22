package cn.com.vortexa.control;

import cn.com.vortexa.control.config.ScriptAgentConfig;
import cn.com.vortexa.control.constant.ExtFieldsConstants;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.control.dto.ArgsWrapper;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.control.util.DistributeIdMaker;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
class ScriptAgentTest {

    private static ScriptAgent scriptAgent;

    static int command_client = 3001;
    static int command_service = 3002;
    static Random random = new Random();

    @BeforeAll
    static void setUpBeforeClass() throws Exception {

        scriptAgent = new ScriptAgent(ScriptAgentConfig.defaultConfig());
    }

    @Test
    public void test() throws ExecutionException, InterruptedException, CustomCommandException, NoSuchMethodException {
        Class<TestRpc> rpcClass = TestRpc.class;
        String name = rpcClass.getName();
        Method testMethod = rpcClass.getMethod("test", String.class);
        String value = name + "-" + testMethod.getName();
//
//        nameserverClient.addCustomCommandHandler(
//                command_service,
//                request -> {
//                    log.info("收到服务端自定义命令");
//                    return RequestHandleResult.success("客户端响应-" + random.nextInt());
//                }
//        );

        scriptAgent.setAfterRegistryHandler(response -> {
            log.info("send 发送客户端自定义命令 {}", value);

            try {
                RemotingCommand remotingCommand = new RemotingCommand();
                remotingCommand.setFlag(RemotingCommandFlagConstants.CUSTOM_COMMAND);
                remotingCommand.setCode(RemotingCommandCodeConstants.SUCCESS);
                remotingCommand.setTransactionId(
                        DistributeIdMaker.DEFAULT.nextId(scriptAgent.getName())
                );
                remotingCommand.addExtField(
                        ExtFieldsConstants.CUSTOM_COMMAND_HANDLER_KEY,
                        value
                );

                remotingCommand.setBody(
                        Serializer.Algorithm.Protostuff.serialize(
                                new ArgsWrapper(new Object[]{"参数1", "参数2"})
                        )
                );
                scriptAgent.sendRequest(remotingCommand).thenApply(customResponse -> {
                    try {
                        if (customResponse.getCode() == RemotingCommandCodeConstants.SUCCESS) {
                            String deserialize = Serializer.Algorithm.Protostuff.deserialize(customResponse.getBody(),
                                    String.class);
                            log.info("收到客户端自定义命令响应[{}]\n{}", customResponse, deserialize);
                        } else {
                            String deserialize = Serializer.Algorithm.Protostuff.deserialize(customResponse.getBody(),
                                    String.class);
                            log.error(deserialize);
                        }

                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                    return customResponse;
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });

        CompletableFuture<Boolean> connect = scriptAgent.connect();
        connect.get();

        TimeUnit.SECONDS.sleep(10000000);
    }
}
