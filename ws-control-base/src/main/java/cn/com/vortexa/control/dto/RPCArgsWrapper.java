package cn.com.vortexa.control.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RPCArgsWrapper implements Serializable {
    @Serial
    private static final long serialVersionUID = 281739182378912372L;

    private Object[] args;
}
