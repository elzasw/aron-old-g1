package cz.aron.transfagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {"cz.aron.transfagent", "cz.aron.customizations"})
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        System.setProperty("spring.config.name", "transfagent");
        SpringApplication.run(Application.class, args);
    }

}