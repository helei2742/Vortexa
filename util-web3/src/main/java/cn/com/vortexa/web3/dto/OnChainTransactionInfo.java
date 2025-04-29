package cn.com.vortexa.web3.dto;


import cn.com.vortexa.common.util.tableprinter.CommandTableField;
import lombok.Data;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;

/**
 * @author helei
 * @since 2025-04-29
 */
@Data
public class OnChainTransactionInfo {
    public static final OnChainTransactionInfo FAIL;
    static {
        FAIL = new OnChainTransactionInfo();
        FAIL.status = Status.FAILED;
    }

    @CommandTableField
    private String transactionHash; // 交易hash
    @CommandTableField
    private String from;    // 发送方
    @CommandTableField
    private String to;  // 接收方
    @CommandTableField
    private Status status;  // 交易结果
    @CommandTableField
    private String cumulativeGasUsed;   //  累计消耗 Gas
    @CommandTableField
    private Integer gasUsed;    // gas 消耗
    @CommandTableField
    private Integer effectiveGasPrice;  // 有效 Gas 单价

    private BigInteger transactionIndex;    // 交易索引
    private String blockHash;   //  区块哈希 (
    private BigInteger blockNumber; //  区块高度
    private String contractAddress; //  部署合约的交易地址
    // status is only present on Byzantium transactions onwards
    // see EIP 658 https://github.com/ethereum/EIPs/pull/658
    private List<Log> logs; //  日志信息
    private String logsBloom;   //  用于快速索引日志的布隆过滤器（一般普通开发不会直接用到）。
    private String revertReason;    //  交易 revert（回滚）信息。
    private TransactionType type;    //  交易类型，
    private String root;    //

    public enum Status {
        SUCCESS,
        FAILED
    }

    public enum TransactionType {
        Legacy_Transaction, //  传统交易，固定gas
        Access_List,    //  带访问列表交易
        Dynamic_Fee //  动态gas交易
    }

    public static OnChainTransactionInfo fromReceipt(TransactionReceipt receipt) {
        OnChainTransactionInfo info = new OnChainTransactionInfo();
        info.setTransactionHash(receipt.getTransactionHash());
        info.setFrom(receipt.getFrom());
        info.setTo(receipt.getTo());
        info.setTransactionIndex(receipt.getTransactionIndex());
        info.setBlockHash(receipt.getBlockHash());
        info.setBlockNumber(receipt.getBlockNumber());
        info.setContractAddress(receipt.getContractAddress());
        info.setLogs(receipt.getLogs());
        info.setLogsBloom(receipt.getLogsBloom());
        info.setRevertReason(receipt.getRevertReason());
        info.setRoot(receipt.getRoot());

        // 状态解析
        if ("0x1".equals(receipt.getStatus())) {
            info.setStatus(Status.SUCCESS);
        } else {
            info.setStatus(Status.FAILED);
        }
        info.setType(switch (receipt.getType()) {
            case "0x0" -> TransactionType.Legacy_Transaction;
            case "0x1" -> TransactionType.Access_List;
            case "0x2" -> TransactionType.Dynamic_Fee;
            default -> null;
        });

        // GasUsed 和 GasPrice 转为 Integer（注意溢出）
        info.setGasUsed(safeToInt(receipt.getGasUsed()));
        info.setEffectiveGasPrice(parseHexToInt(receipt.getEffectiveGasPrice()));

        return info;
    }

    private static Integer safeToInt(BigInteger bigInt) {
        if (bigInt == null) return null;
        if (bigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            return Integer.MAX_VALUE;
        }
        return bigInt.intValue();
    }

    private static Integer parseHexToInt(String hex) {
        if (hex == null) return null;
        try {
            BigInteger value = new BigInteger(hex.replace("0x", ""), 16);
            return value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0
                    ? Integer.MAX_VALUE
                    : value.intValue();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex string: " + hex, e);
        }
    }
}
