package cn.com.vortexa.common.dto.config;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AutoBotAccountConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = -8347837647826838273L;

    private String configFilePath;
}
