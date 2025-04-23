package cn.com.vortexa.web3.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author h30069248
 * @since 2025/4/23 11:48
 */
@Data
public class SCInvokeResult implements Serializable {

    /**
     * 交易类型合约交互时返回的交易hash
     */
    private String transactionHash;

    /**
     * 查询类型合约交互时得到的结果
     */
    private List<Object> result;
}
