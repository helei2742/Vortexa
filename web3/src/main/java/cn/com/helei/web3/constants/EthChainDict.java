package cn.com.helei.web3.constants;

import cn.com.helei.web3.dto.Web3ChainInfo;
import lombok.Getter;

@Getter
public enum EthChainDict {

    MONAD_TESTNET(
            "https://testnet-rpc.monad.xyz/",
            "Monad Testnet",
            10143,
            "MON",
            "https://testnet.monadexplorer.com"
    )
    ;

    private final Web3ChainInfo chainInfo;

    EthChainDict(String rpcUrl, String name, int chainId, String tokenName, String blockUrl) {
        this.chainInfo = new Web3ChainInfo(
                rpcUrl, name, chainId, tokenName, blockUrl
        );
    }
}
