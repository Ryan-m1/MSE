package cn.wormholestack.mse.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @description： MseBootApplication
 * @Author MRyan
 * @Date 2021/11/7 17:41
 * @Version 1.0
 */
@SpringBootApplication(scanBasePackages = {
        "cn.wormholestack.mse.boot.*",
        "cn.wormholestack.mse.facade.*",
        "cn.wormholestack.mse.facadeImpl.*",
        "cn.wormholestack.mse.biz.*",
        "cn.wormholestack.mse.common.*",
        "cn.wormholestack.mse.web.*",
        "cn.wormholestack.mse.dal.*"})
public class MseBootApplication {

    static Logger logger = LoggerFactory.getLogger(MseBootApplication.class);

    public static void main(String[] args) {
        logger.info("MSE Service container is starting");
        SpringApplication.run(MseBootApplication.class, args);
    }

}
