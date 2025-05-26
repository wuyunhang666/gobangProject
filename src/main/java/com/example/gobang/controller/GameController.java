package com.example.gobang.controller;

import com.example.gobang.game.*;
import com.example.gobang.mapper.UserMapper;
import com.example.gobang.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
public class GameController extends TextWebSocketHandler {
    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private RoomManager roomManager;
    @Autowired
    private OnlineUserManager onlineUserManager;
    @Resource
    private UserMapper userMapper;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        //1.先获取玩家信息
        User user = (User)session.getAttributes().get("user");
        if(user == null) {
            System.out.println("handleTextMessage: 当前玩家未登录！");
            return;
        }
       //2.根据玩家id获取room对象
       Room room = roomManager.getRoomByUserId(user.getUserId());
        //3.通过room对象来具体处理请求
        System.out.println("数据为：  "+message.getPayload());
        room.putChess(message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        User user = (User)session.getAttributes().get("user");
        if(user == null) {
            return;
        }
        WebSocketSession exitSession = onlineUserManager.getFromGameRoom(user.getUserId());
        if(session == exitSession) {
            //这是避免重复登录时，第二个用户断开连接导致第一个用户会话被删除
            onlineUserManager.exitGameRoom(user.getUserId());
        }
        System.out.println("当前用户"+user.getUsername()+"游戏房间连接异常！");
        //这时对手获胜
        noticeThatUserWin(user);
    }

    private void noticeThatUserWin(User user) throws IOException {
        //根据玩家找到room
        Room room = roomManager.getRoomByUserId(user.getUserId());
        if(room == null) {
            System.out.println("当前房间已不存在！");
            return;
        }
        //根据房间找对手
        User thatUser = user == room.getUser1() ? room.getUser2() : room.getUser1();
        //找到对手的在线状态
        WebSocketSession session = onlineUserManager.getFromGameRoom(thatUser.getUserId());
        if(session==null){
            System.out.println("双方都掉线了！");
            return;
        }
        //构造一个响应，通知对方你是获胜方
        GameResponse resp = new GameResponse();
        resp.setMessage("putChess");
        resp.setUserId(thatUser.getUserId());
        resp.setWinner(thatUser.getUserId());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
        //更新玩家分数信息
        int winUserId = thatUser.getUserId();
        int loseUserId = user.getUserId();
        userMapper.userWin(winUserId);
        userMapper.userLose(loseUserId);
        //释放房间对象
        roomManager.remove(room.getRoomId(),room.getUser1().getUserId(),room.getUser2().getUserId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        User user = (User)session.getAttributes().get("user");
        if(user == null) {
            return;
        }
        WebSocketSession exitSession = onlineUserManager.getFromGameRoom(user.getUserId());
        if(session == exitSession) {
            //这是避免重复登录时，第二个用户断开连接导致第一个用户会话被删除
            onlineUserManager.exitGameRoom(user.getUserId());
        }
        System.out.println("当前用户"+user.getUsername()+"游戏房间连接异常！");
        //这时对手获胜
        noticeThatUserWin(user);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
       GameReadyResponse resp = new GameReadyResponse();
       //1.先获取到用户的身份信息
        User user = (User) session.getAttributes().get("user");
        if(user == null) {
            resp.setOk(false);
            resp.setReason("用户尚未登录！");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            return;
        }
        //2.判断当前用户是否进入room
        Room room = roomManager.getRoomByUserId(user.getUserId());
        if(room == null) {
            //表示没有找到对应的房间
            resp.setOk(false);
            resp.setReason("用户尚未匹配到！");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            return;
        }
        //3.判断是不是重复登录
        if(onlineUserManager.getFromGameHall(user.getUserId()) != null || onlineUserManager.getFromGameRoom(user.getUserId()) != null) {
            resp.setOk(true);
            resp.setReason("不要重复登录哟！");
            resp.setMessage("repeatConnection");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            return;
        }
        //4.设置当前玩家上线
        onlineUserManager.enterGameRoom(user.getUserId(),session);
        //5.把两个玩家加入到游戏房间中
        synchronized (room) {
            if(room.getUser1()==null){
                //1没有准备好，就将当前连接的玩家作为玩家1
                room.setUser1(user);
                //并且把先连上的作为先手方
                room.setWhiteUser(user.getUserId());
                System.out.println("玩家"+user.getUsername()+"已经准备就绪，并作为玩家1");
                return;
            }
            if(room.getUser2()==null){
                room.setUser2(user);
                System.out.println("玩家"+user.getUsername()+"已经准备就绪，并作为玩家2");
                //通知玩家1
                noticeGameReady(room,room.getUser1(),room.getUser2());
                //通知玩家2
                noticeGameReady(room,room.getUser2(),room.getUser1());
                return;
            }
        }
        //6.如果又有一个玩家想要加入，就提醒错误
        resp.setOk(false);
        resp.setReason("当前房间已满，您不能加入");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
    }
    private void noticeGameReady(Room room, User thisUser, User thatUser) throws IOException {
        GameReadyResponse resp = new GameReadyResponse();
        resp.setOk(true);
        resp.setMessage("gameReady");
        resp.setReason("");
        resp.setRoomId(room.getRoomId());
        resp.setThisUserId(thisUser.getUserId());
        resp.setThatUserId(thatUser.getUserId());
        resp.setWhiteUser(room.getWhiteUser());
        System.out.println("数据设置好了： "+resp);
        //把响应数据传回给玩家
        WebSocketSession webSocketSession = onlineUserManager.getFromGameRoom(thisUser.getUserId());
        // 说明当前用户没有在线
        if(webSocketSession == null) {
            resp.setOk(false);
            return;
        }
        webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
    }
}
