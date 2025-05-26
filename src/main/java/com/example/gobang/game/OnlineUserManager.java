package com.example.gobang.game;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.net.http.WebSocket;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineUserManager {
    //创建两个哈希表，一个表示游戏大厅在线状态，一个表示游戏房间在线状态
    //这里使用线程安全的ConcurrentHashMap
    private ConcurrentHashMap<Integer, WebSocketSession> gameHall = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, WebSocketSession> gameRoom = new ConcurrentHashMap<>();

    public void enterGameHall(int userId,WebSocketSession session) {
        gameHall.put(userId, session);
    }
    public void exitGameHall(int userId) {
        gameHall.remove(userId);
    }
    public void enterGameRoom(int userId,WebSocketSession session) {
        gameRoom.put(userId, session);
    }
    public void exitGameRoom(int userId) {
        gameRoom.remove(userId);
    }

    public WebSocketSession getFromGameHall(int userId) {
        System.out.println("查看是否进入哈希表gameHall： "+gameHall.get(userId));
        return gameHall.get(userId);
    }

    public WebSocketSession getFromGameRoom(int userId) {
        return gameRoom.get(userId);
    }
}
