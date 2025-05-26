package com.example.gobang.config;

import com.example.gobang.controller.GameController;
import com.example.gobang.controller.MatchController;
import com.example.gobang.controller.TestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private GameController gameController;
    @Autowired
    private TestController testController;
    @Autowired
    private MatchController matchController;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(testController, "/test");
        registry.addHandler(matchController,"/findMatch")
                        .addInterceptors(new HttpSessionHandshakeInterceptor());
        registry.addHandler(gameController,"/game")
                        .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
