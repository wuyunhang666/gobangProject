package com.example.gobang.game;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

//房间管理器
@Component
public class RoomManager {
    private ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();//表示room队列
    private ConcurrentHashMap<Integer, String> userIdToRoomId = new ConcurrentHashMap<>();//对应Id的room中对应的玩家的Id
    public void add(Room room,int userId1,int userId2) {
        rooms.put(room.getRoomId(), room);
//        System.out.println("玩家的房间id："+room.getRoomId());
//        System.out.println("传入的userId1："+userId1+"  传入的userId2： "+userId2);
        userIdToRoomId.put(userId1,room.getRoomId());
        userIdToRoomId.put(userId2,room.getRoomId());
    }
    public Room getRoomByUserId(int userId) {
        String result = userIdToRoomId.get(userId);
        if(result == null) {
            return null;
        }
        return rooms.get(result);
    }
    public Room getRoomById(String roomId) {
        return rooms.get(roomId);
    }
    public void remove(String roomId, int userId1, int userId2) {
        rooms.remove(roomId);
        userIdToRoomId.remove(userId2);
        userIdToRoomId.remove(userId1);
    }
}
