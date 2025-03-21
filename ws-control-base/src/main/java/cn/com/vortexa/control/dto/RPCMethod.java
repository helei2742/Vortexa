package cn.com.vortexa.control.dto;

import lombok.Data;

import java.util.List;

/**
 * @author helei
 * @since 2025/3/21 11:25
 */
@Data
public class RPCMethod {

    private String interfaceClassReference;

    private String methodName;

    private List<String> typeName;
}
