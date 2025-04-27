package cn.com.vortexa.rpc.api.platform;

import cn.com.vortexa.common.entity.RewordInfo;

import java.util.List;

/**
 * @author helei
 * @since 2025/4/27 15:37
 */
public interface IRewordInfoRPC {

    /**
     * 批量保存收益
     *
     * @param botId       botId
     * @param botKey      botKey
     * @param rewordInfos rewordInfos
     */
    void saveBatchRPC(Integer botId, String botKey, List<RewordInfo> rewordInfos);
}
