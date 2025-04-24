package cn.com.vortexa.web3.dto;


import cn.com.vortexa.common.constants.ChainType;
import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Web3ChainInfo implements Serializable {
    /**
     * 链类型
     */
    private ChainType chainType;

    private List<String> rpcUrls;

    private String name;

    private Integer chainId;

    private String originTokenSymbol;

    private String blockExploreUrl;

    public String getRpcUrl() {
        return CollUtil.isEmpty(rpcUrls) ? null : rpcUrls.getFirst();
    }
}
