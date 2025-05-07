package cn.com.vortexa.common.dto.job;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class AutoBotJobWSParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 984984151765154986L;


    private Boolean isRefreshWSConnection;

    private Boolean wsUnlimitedRetry;

    private Integer nioEventLoopGroupThreads;

    private Integer wsConnectCount;

    private Integer reconnectLimit;

    private Integer heartBeatIntervalSecond;

    private Integer reconnectCountDownSecond;

    public void merge(AutoBotJobWSParam autoBotJobWSParam) {
        if (autoBotJobWSParam == null) return;
        if (autoBotJobWSParam.getIsRefreshWSConnection() != null) { this.isRefreshWSConnection = autoBotJobWSParam.getIsRefreshWSConnection();}
        if (autoBotJobWSParam.getWsUnlimitedRetry() != null) { this.wsUnlimitedRetry = autoBotJobWSParam.getWsUnlimitedRetry(); }
        if (autoBotJobWSParam.getNioEventLoopGroupThreads() != null) { this.nioEventLoopGroupThreads = autoBotJobWSParam.getNioEventLoopGroupThreads(); }
        if (autoBotJobWSParam.getWsConnectCount() != null) { this.wsConnectCount = autoBotJobWSParam.getWsConnectCount(); }
        if (autoBotJobWSParam.getReconnectLimit() != null) { this.reconnectLimit = autoBotJobWSParam.getReconnectLimit(); }
        if (autoBotJobWSParam.getHeartBeatIntervalSecond() != null) { this.heartBeatIntervalSecond = autoBotJobWSParam.getHeartBeatIntervalSecond(); }
        if (autoBotJobWSParam.getReconnectCountDownSecond() != null) { this.reconnectCountDownSecond = autoBotJobWSParam.getReconnectCountDownSecond(); }
    }
}
