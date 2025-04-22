package cn.com.vortexa.common.dto.web3;

import cn.com.vortexa.common.constants.ChainType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author helei
 * @since 2025/4/22 10:16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignatureMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = -2783964872364872364L;

    private Integer walletId;

    private ChainType chainType;

    private String message;
}
