package cn.com.vortexa.bot_platform.wsController;

import jakarta.websocket.Session;
import lombok.Data;

@Data
public class SocketDomain {
    private Session session;
    private String uri;
}
