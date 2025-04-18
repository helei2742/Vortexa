package cn.com.vortexa.control.dto;


import cn.com.vortexa.common.dto.control.ServiceInstance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private Map<?, ?> props;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisteredService that = (RegisteredService) o;
        return Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
