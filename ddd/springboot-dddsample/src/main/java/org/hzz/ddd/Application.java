package org.hzz.ddd;

import org.hzz.pathfinder.config.PathfinderApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({PathfinderApplicationContext.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
