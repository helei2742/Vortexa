package cn.com.vortexa.control_server.dto;


import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author helei
 * @since 2025-03-12
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConnectEntry {
    private Channel channel;
    private boolean usable;
    private long lastActiveTimestamp;

    public void close() {
        if (channel != null && channel.isActive()) {
            channel.close();
        }
        usable = false;
    }

    public boolean isUsable() {
        return usable && channel != null && channel.isActive();
    }
}
