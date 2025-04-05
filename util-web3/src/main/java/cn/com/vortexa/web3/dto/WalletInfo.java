package cn.com.vortexa.web3.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author helei
 * @since 2025-04-05
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletInfo {
    private String address;
    private String privateKey;
    private String publicKey;
    private String mnemonic;
}
