package cn.com.vortexa.bot_platform.service;

import com.baomidou.mybatisplus.extension.service.IService;

import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.web3.SignatureMessage;
import cn.com.vortexa.common.entity.Web3Wallet;
import cn.com.vortexa.db_layer.service.ImportService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-04-21
 */
public interface IWeb3WalletService extends IService<Web3Wallet>, ImportService {

    Result signatureMessage(SignatureMessage signatureMessage);
}
