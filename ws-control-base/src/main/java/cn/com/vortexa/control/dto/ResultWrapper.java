package cn.com.vortexa.control.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author helei
 * @since 2025-03-23
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResultWrapper {
    private Object result;

    @Override
    public String toString() {
        return "ResultWrapper{" +
                "args=" + result +
                '}';
    }
}
