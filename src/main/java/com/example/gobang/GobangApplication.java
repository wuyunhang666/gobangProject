package com.example.gobang;

import com.example.gobang.game.Room;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class GobangApplication {
    public static ConfigurableApplicationContext context;

    public static  void main(String[] args) {
        context = SpringApplication.run(GobangApplication.class, args);
        System.out.println(new Room().getRoomId());
    }


}
