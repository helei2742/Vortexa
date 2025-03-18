package cn.com.vortexa.nameserver.server;

import cn.com.vortexa.nameserver.config.NameserverServerConfig;
import cn.com.vortexa.nameserver.dto.ServiceInstance;
import cn.com.vortexa.nameserver.exception.NameserverException;
import cn.com.vortexa.nameserver.service.impl.FileRegistryService;
import cn.com.vortexa.nameserver.service.impl.MemoryConnectionService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class NameserverServiceTest {

    static NameserverServerConfig nameserverServerConfig;

    static NameserverService nameserverService;

    @BeforeAll
    public static void setUp() throws NameserverException {
        nameserverServerConfig = new NameserverServerConfig();

        ServiceInstance address = ServiceInstance.builder()
                .group("default")
                .serviceId("sahara")
                .clientId("sahara_test")
                .host("127.0.0.1")
                .port(8080)
                .build();

        nameserverServerConfig.setServiceInstance(address);


        nameserverService = new NameserverService(NameserverServerConfig.DEFAULT);

        nameserverService.init(new FileRegistryService(), new MemoryConnectionService());
    }


    @Test
    public void test() throws NameserverException, InterruptedException {
        nameserverService.start();

        TimeUnit.SECONDS.sleep(10000);
    }
}
