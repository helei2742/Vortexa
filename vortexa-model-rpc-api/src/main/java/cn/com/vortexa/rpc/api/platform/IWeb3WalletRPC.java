package cn.com.vortexa.rpc.api.platform;

import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.web3.SignatureMessage;
import cn.com.vortexa.common.entity.Web3Wallet;

import java.io.Serializable;
import java.util.List;

/**
 * @author helei
 * @since 2025/4/22 11:38
 */
public interface IWeb3WalletRPC {

    /**
     * 对消息签名
     * 必须使用已注册的钱包，使用钱包id标识签名的钱包
     *
     * @param message message
     * @return Result
     */
    Result signatureMessageRPC(SignatureMessage message);

    /**
     * id批量查
     *
     * @param ids   ids
     * @return  TwitterAccount
     */
    List<Web3Wallet> batchQueryByIdsRPC(List<Serializable> ids);
}
