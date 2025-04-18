package cn.com.vortexa.browser_control.dto;

import cn.com.vortexa.common.constants.ProxyProtocol;
import cn.com.vortexa.common.entity.ProxyInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeleniumProxy {

    private ProxyProtocol proxyProtocol;

    private String host;

    private int port;

    private String username;

    private String password;

    public SeleniumProxy(ProxyInfo proxy) {
        this.proxyProtocol = proxy.getProxyProtocol();
        this.host = proxy.getHost();
        this.port = proxy.getPort();
        this.username = proxy.getUsername();
        this.password = proxy.getPassword();
    }
}
