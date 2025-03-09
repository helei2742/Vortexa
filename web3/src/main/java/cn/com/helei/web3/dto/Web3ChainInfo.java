package cn.com.helei.web3.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Web3ChainInfo {

    private String rpcUrl;

    private String name;

    private Integer chainId;

    private String originTokenSymbol;

    private String blockExploreUrl;
}
