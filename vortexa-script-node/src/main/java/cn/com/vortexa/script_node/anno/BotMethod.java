package cn.com.vortexa.script_node.anno;

import cn.com.vortexa.common.constants.BotJobType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BotMethod {

    /**
     * job类型
     *
     * @return BotJobType
     */
    BotJobType jobType();

    /**
     * jobName
     *
     * @return string
     */
    String jobName() default "";

    /**
     * 描述
     *
     * @return String
     */
    String description() default "";

    /**
     * 时间表达式
     *
     * @return String
     */
    String cronExpression() default "";

    /**
     * 运行间隔
     *
     * @return int
     */
    int intervalInSecond() default 0;

    /**
     * 并发数
     *
     * @return int
     */
    int concurrentCount() default 3;

    /**
     * 是否区分账户
     *
     * @return boolean
     */
    boolean uniqueAccount() default false;

    boolean dynamicTrigger() default false;

    boolean syncExecute() default false;

    int dynamicTimeWindowMinute() default 120;

    BotWSMethodConfig bowWsConfig() default @BotWSMethodConfig();
}
