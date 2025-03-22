package cn.com.vortexa.rpc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author helei
 * @since 2025/3/21 11:05
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RPCServiceInfo<T> {

    private Class<T> interfaces;

    private Object ref;
}
