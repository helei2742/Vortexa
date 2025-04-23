package cn.com.vortexa.bot_platform.controller;

import cn.com.vortexa.bot_platform.service.IWeb3WalletService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.web3.SignatureMessage;
import cn.com.vortexa.web3.dto.SCInvokeParams;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author com.helei
 * @since 2025-04-21
 */
@RestController
@RequestMapping("/web3")
public class Web3WalletController {
    @Autowired
    public IWeb3WalletService web3WalletService;

    @PostMapping("/signature")
    public Result signatureWalletMessage(SignatureMessage message) {
        return Result.ok(web3WalletService.signatureMessage(message));
    }

    @PostMapping("/smart_contract_invoke")
    public Result smartContractInvoke(@RequestBody SCInvokeParams invokeParams) {
        try {
            return web3WalletService.smartContractInvoke(invokeParams);
        } catch (Exception e) {
            return Result.fail(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        }
    }
}
