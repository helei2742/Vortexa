package cn.com.vortexa.common.dto.control;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author helei
 * @since 2025-03-12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisteredService {

    private ServiceInstance address;

    private Map<Object, Object> props;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisteredService that = (RegisteredService) o;
        return Objects.equals(address, that.address);
    }

    public void addProps(Object key, Object value) {
        if (props == null) props = new HashMap<>();
        props.put(key, value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
