package cn.com.vortexa.web3.dto;

import cn.com.vortexa.web3.constants.Web3jFunctionType;
import cn.hutool.core.lang.Pair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

/**
 * @author com.helei
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
     * gas limit
     */
    private BigInteger gasLimit;

    /**
     * 交易金额
     */
    private BigInteger value;

    /**
     * 只读方法
     */
    private Boolean readFunction;

    /**
     * 含交易信息的数据, 有这个，就不会在用functionName、paramsTypes、resultTypes构建交易data了
     */
    private String data;

    /**
     * abi方法名
     */
    private String functionName;

    /**
     * 参数类型
     */
    private List<Pair<Web3jFunctionType, Object>> paramsTypes;

    /**
     * 返回值类型
     */
    private List<Web3jFunctionType> resultTypes;

    private Integer retryTimes;

    public Integer getRetryTimes() {
        if (retryTimes == null || retryTimes <= 0) { return 1; }
        return retryTimes;
    }
}
