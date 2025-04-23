package cn.com.vortexa.web3.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author helei
 * @since 2025/4/23 10:05
 */
@Data
public class ContractABI implements Serializable {

    /**
     * 链的类型
     */
    private String chainType;

    /**
     * 合约地址
     */
    private String contractAddress;

    /**
     * abi内容的字符串
     */
    private String abiContent;
}
