package cn.iyque;


import com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.sql.SQLException;

@SpringBootApplication(exclude = {PageHelperAutoConfiguration.class})
@MapperScan("cn.iyque.mapper")
public class IyQueApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(IyQueApplication.class)
                .build().run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ 集智销伴 启动成功 ლ(´ڡ`ლ)ﾞ");
    }
}
