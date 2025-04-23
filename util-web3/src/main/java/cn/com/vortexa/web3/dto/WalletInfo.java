package cn.com.vortexa.web3.dto;


import cn.com.vortexa.common.constants.ChainType;
import cn.com.vortexa.common.entity.Web3Wallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author helei
 * @since 2025-04-05
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletInfo implements Serializable {
    private String address;
    private String privateKey;
    private String publicKey;
    private String mnemonic;

    public WalletInfo(ChainType chainType, Web3Wallet web3Wallet) {
        switch (chainType) {
            case ETH -> {
                this.address = web3Wallet.getEthAddress();
                this.privateKey = web3Wallet.getEthPrivateKey();
            }
            case SOL -> {
                this.address = web3Wallet.getSolAddress();
                this.privateKey = web3Wallet.getSolPrivateKey();
            }
        }
        this.mnemonic = web3Wallet.getMnemonic();
    }
}
