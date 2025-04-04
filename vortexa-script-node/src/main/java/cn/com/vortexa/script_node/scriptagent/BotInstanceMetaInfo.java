package cn.com.vortexa.script_node.scriptagent;


import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author helei
 * @since 2025-04-04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class BotInstanceMetaInfo {
    private final AutoLaunchBot<?> bot;
    private boolean exposed = false;

    public BotInstanceMetaInfo(AutoLaunchBot<?> bot) {
        this.bot = bot;
    }
}
