package cn.com.helei.browser_control;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeleniumProxy {

    private String host;

    private int port;

    private String username;

    private String password;
}

