package cn.com.vortexa.web3.dto;


import cn.com.vortexa.common.constants.ChainType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Web3ChainInfo implements Serializable {
    /**
     * 链类型
     */
    private ChainType chainType;

    private String rpcUrl;

    private String name;

    private Integer chainId;

    private String originTokenSymbol;

    private String blockExploreUrl;
}
