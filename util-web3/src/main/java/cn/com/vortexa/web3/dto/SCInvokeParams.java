package cn.com.vortexa.web3.dto;

import cn.com.vortexa.web3.constants.Web3jFunctionType;
import cn.hutool.core.lang.Pair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.util.List;

/**
 * @author h30069248
 * @since 2025/4/23 11:05
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SCInvokeParams implements Serializable {
    /**
     * 钱包id
     */
    private Integer walletId;

    /**
     * 钱包信息
     */
    private WalletInfo walletInfo;

    /**
     * 链信息
     */
    private Web3ChainInfo chainInfo;

    /**
     * 调用的合约地址
     */
    private String contractAddress;
    /**
     * abi方法名
     */
    private String functionName;
    /**
     * 只读方法
     */
    private Boolean readFunction;
    /**
     * 参数类型
     */
    private List<Pair<Web3jFunctionType, Object>> paramsTypes;
    /**
     * 返回值类型
     */
    private List<Web3jFunctionType> resultTypes;
}
