package com.example.gobang.controller;

import com.example.gobang.game.MatchRequest;
import com.example.gobang.game.MatchResponse;
import com.example.gobang.game.Matcher;
import com.example.gobang.game.OnlineUserManager;
import com.example.gobang.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

//处理匹配的websocket请求
@Slf4j
@Component
public class MatchController extends TextWebSocketHandler {
    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private OnlineUserManager onlineUserManager;
    @Autowired
    private Matcher matcher;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try{
            User user = (User) session.getAttributes().get("user");
            //判断当前用户是否已经登录，如果登录，则不能进行后续操作（重复登录）
            if(onlineUserManager.getFromGameHall(user.getUserId())!=null || onlineUserManager.getFromGameRoom(user.getUserId())!=null){
                //告知用户已经登录了
                MatchResponse response = new MatchResponse();
                response.setOk(true);
                response.setReason("当前重复登录！");
                response.setMessage("repeatConnection");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                //关闭的操作交给客户端处理
                return;
            }
            onlineUserManager.enterGameHall(user.getUserId(), session);
            System.out.println("查看玩家："+user.getUsername()+"是否添加至哈希表gameHall："+onlineUserManager.getFromGameHall(user.getUserId()));
            System.out.println("玩家"+user.getUsername()+"进入游戏大厅！");
        }catch(NullPointerException e){
            System.out.println("MatchController:afterConnectionEstablished: 当前用户未登录！");
            MatchResponse response = new MatchResponse();
            response.setOk(false);
            response.setReason("您尚未登录！不能进行后续匹配功能！");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        User user = (User) session.getAttributes().get("user");
        String text = message.getPayload();
        MatchRequest request = objectMapper.readValue(text, MatchRequest.class);//将JSON对象转为Java对象
        MatchResponse response = new MatchResponse();
        if(request.getMessage().equals("startMatch")){
            //进入匹配队列
            matcher.add(user);
            //给客户端返回成功信息
            response.setOk(true);
            response.setMessage("startMatch");
        }else if(request.getMessage().equals("stopMatch")){
           // System.out.println("错误可能一：request的message为stopMatch");
            matcher.remove(user);
            response.setOk(true);
            response.setMessage("stopMatch");
        }else{
            response.setOk(false);
            response.setReason("非法的请求！");
        }
        String json = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(json));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        try{
            //玩家下线，从onlineUserManager中删除
            User user = (User) session.getAttributes().get("user");
            WebSocketSession tmpsession = onlineUserManager.getFromGameHall(user.getUserId());
            if(tmpsession==session){
                onlineUserManager.exitGameHall(user.getUserId());
            }
            //System.out.println("错误可能2：触发handleTransportError");
            matcher.remove(user);
        }catch(NullPointerException e){
            System.out.println("MatchController:handleTransportError: 当前用户未登录！");
        }
    }

    @Override
    //处理逻辑和handleTransportError一样
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try{
            //玩家下线，从onlineUserManager中删除
            User user = (User) session.getAttributes().get("user");
            WebSocketSession tmpSession = onlineUserManager.getFromGameHall(user.getUserId());
            if(tmpSession==session){
                onlineUserManager.exitGameHall(user.getUserId());
            }
            //System.out.println("错误可能3：提前触发afterConnectionClosed");
            log.info("会话关闭: {}, 状态码: {}", session.getId(), status);
            matcher.remove(user);
        }catch(NullPointerException e){
            System.out.println("MatchController:afterConnectionClosed: 当前用户未登录！");
        }
    }
}
