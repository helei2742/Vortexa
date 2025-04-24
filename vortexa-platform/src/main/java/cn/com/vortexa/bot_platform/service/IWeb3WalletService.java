package cn.com.vortexa.bot_platform.service;

import com.baomidou.mybatisplus.extension.service.IService;

import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.web3.SignatureMessage;
import cn.com.vortexa.common.entity.Web3Wallet;
import cn.com.vortexa.db_layer.service.ImportService;
import cn.com.vortexa.web3.dto.SCInvokeParams;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-04-21
 */
public interface IWeb3WalletService extends IService<Web3Wallet>, ImportService {

    /**
     * 保存钱包
     *
     * @param rawLines rawLines
     * @return Result
     */
    Result saveWallet(List<Map<String, Object>> rawLines);

    /**
     * 钱包签名消息
     *
     * @param signatureMessage  signatureMessage
     * @return  Result¬
     */
    Result signatureMessage(SignatureMessage signatureMessage);

    /**
     * 智能合约交互
     *
     * @param invokeParams invokeParams
     * @return Result
     */
    Result smartContractInvoke(SCInvokeParams invokeParams) throws IOException;
}
