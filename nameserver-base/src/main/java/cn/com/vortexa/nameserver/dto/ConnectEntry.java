package cn.com.vortexa.nameserver.dto;


import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;
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
    private String type;

    public void close() {
        if (channel != null && channel.isActive()) {
            channel.close();
        }
        usable = false;
    }
}
