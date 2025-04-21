package cn.com.vortexa.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 *
 * </p>
 *
 * @author com.helei
 * @since 2025-04-21
 */
@Getter
@Setter
@TableName("t_web3_wallet")
public class Web3Wallet implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("mnemonic")
    private String mnemonic;

    @TableField("privateKey")
    private String privateKey;

    @TableField("eth_address")
    private String ethAddress;

    @TableField("sol_address")
    private String solAddress;

    @TableField("bsc_address")
    private String bscAddress;

    @TableField("btc_address")
    private String btcAddress;

    @TableField("insert_datetime")
    private LocalDateTime insertDatetime;

    @TableField("update_datetime")
    private LocalDateTime updateDatetime;

    @TableField("valid")
    private Boolean valid;
}
