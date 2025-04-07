package cn.com.vortexa.db_layer.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MybatisConfig implements MetaObjectHandler {


    /**
     * 使用mp做添加操作时候，这个方法执行
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        //设置属性值
        this.setFieldValByName("insertDatetime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("updateDatetime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("valid", true, metaObject);
    }

    /**
     * 使用mp做修改操作时候，这个方法执行
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateDatetime", LocalDateTime.now(), metaObject);
    }

}
