package cn.com.helei.bot_father.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BotApplication {

    /**
     * bot 名字
     *
     * @return name
     */
    String name();

    /**
     * 描述
     *
     * @return 描述
     */
    String describe() default "";

    /**
     * 图片url
     *
     * @return url
     */
    String image() default "";

    /**
     * 适用项目的id
     *
     * @return id
     */
    int[] limitProjectIds() default {};

    /**
     * 设置参数
     *
     * @return String
     */
    String[] configParams() default {};

    /**
     * 账户需要的参数 AccountContext.params
     *
     * @return String
     */
    String[] accountParams() default {};
}
