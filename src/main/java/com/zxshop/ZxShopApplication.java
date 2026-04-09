package com.zxshop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ZX_SHOP 电商+智能Agent系统启动类
 * 
 * @author 谭鹏
 */
@SpringBootApplication
@MapperScan("com.zxshop.mapper")
public class ZxShopApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ZxShopApplication.class, args);
        System.out.println("========================================");
        System.out.println("   ZX_SHOP 电商系统启动成功！");
        System.out.println("   API文档: http://localhost:8080/swagger-ui.html");
        System.out.println("========================================");
    }
}
