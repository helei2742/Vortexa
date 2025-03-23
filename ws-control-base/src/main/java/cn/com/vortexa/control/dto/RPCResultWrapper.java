package cn.com.vortexa.control.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author helei
 * @since 2025-03-23
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RPCResultWrapper<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 9127638927139822378L;

    private T result;

    private Exception exception;

    public RPCResultWrapper(T result) {
        this.result = result;
    }
}
