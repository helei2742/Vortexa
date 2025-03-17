package cn.com.vortexa.nameserver;

import cn.com.vortexa.nameserver.config.NameserverClientConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NameserverClientTest {

    private static NameserverClient nameserverClient;


    @BeforeAll
    static void setUpBeforeClass() throws Exception {

        nameserverClient = new NameserverClient(NameserverClientConfig.DEFAULT);
    }


    @Test
    public void test() throws ExecutionException, InterruptedException {
        CompletableFuture<Boolean> connect = nameserverClient.connect();
        connect.get();

        TimeUnit.SECONDS.sleep(100);
    }
}
