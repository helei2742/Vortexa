package cn.com.vortexa.common.entity;

import cn.com.vortexa.common.util.typehandler.MapTextTypeHandler;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 *
 * </p>
 *
 * @author com.helei
 * @since 2025-04-21
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_web3_wallet")
public class Web3Wallet implements Serializable {
    public static final List<String> PUBLIC_FIELDS = List.of("id", "eth_address", "sol_address", "btc_address", "insert_datetime", "update_datetime", "valid");

    @Serial
    private static final long serialVersionUID = 938764872364829487L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("mnemonic")
    private String mnemonic;

    @TableField("eth_private_key")
    private String ethPrivateKey;

    @TableField("eth_address")
    private String ethAddress;

    @TableField("sol_private_key")
    private String solPrivateKey;
    @TableField("sol_address")
    private String solAddress;

    @TableField("btc_private_key")
    private String btcPrivateKey;
    @TableField("btc_address")
    private String btcAddress;

    @TableField(value = "params", typeHandler = MapTextTypeHandler.class)
    private Map<String, Object> params;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer valid;
}
