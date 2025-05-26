package com.example.gobang.game;

import com.example.gobang.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.MatchResult;

//负责匹配功能
@Component
public class Matcher {
    //三个段位的匹配队列
    private Queue<User> normalQueue = new LinkedList<>();
    private Queue<User> highQueue = new LinkedList<>();
    private Queue<User> veryHighQueue = new LinkedList<>();
    @Autowired
    private OnlineUserManager onlineUserManager;
    @Autowired
    private RoomManager roomManager;

    private ObjectMapper mapper = new ObjectMapper();
    //将玩家放入匹配队列
    public void add(User user) {
        if(user.getScore() < 2000){
            synchronized (normalQueue) {//设计到元素的添加和删除，一定要加锁
                normalQueue.offer(user);
                normalQueue.notify();
            }
            System.out.println("玩家: " + user.getUsername()+"加入了normalQueue中！");
        }else if(user.getScore() >= 2000&&user.getScore() < 3000){
            synchronized (highQueue) {
                highQueue.offer(user);
                highQueue.notify();
            }
            System.out.println("玩家: " + user.getUsername()+"加入了highQueue中！");
        }else {
            synchronized (veryHighQueue) {
                veryHighQueue.offer(user);
                veryHighQueue.notify();
            }
            System.out.println("玩家: " + user.getUsername()+"加入了veryHighQueue中！");
        }
    }
    //玩家停止匹配，将玩家从匹配队列中移除
    public void remove(User user) {
        if(user.getScore() < 2000) {
            synchronized (normalQueue) {
                normalQueue.remove(user);
            }
            System.out.println("玩家：" + user.getUsername() + "已经被移除normalQueue队列");
        }else if(user.getScore() >= 2000&&user.getScore() < 3000) {
            synchronized (highQueue) {
                highQueue.remove(user);
            }
            System.out.println("玩家：" + user.getUsername() + "已经被移除highQueue队列");
        }else {
            synchronized (veryHighQueue) {
                veryHighQueue.remove(user);
            }
            System.out.println("玩家：" + user.getUsername() + "已经被移除veryHighQueue队列");
        }
    }
    public Matcher(){
       //创建三个线程，对三个匹配队列进行操作
       Thread t1 = new Thread(() -> {
           while(true){
               handlerMatch(normalQueue);//多线程中使用while代替if
           }
       });
       t1.start();
        Thread t2 = new Thread(() -> {
            while(true){
                handlerMatch(highQueue);//多线程中使用while代替if
            }
        });
        t2.start();
        Thread t3 = new Thread(() -> {
            while(true){
                handlerMatch(veryHighQueue);//多线程中使用while代替if
            }
        });
        t3.start();
    }

    private void handlerMatch(Queue<User> matchQueue) {
        synchronized (matchQueue) {
            try{
                /*明确任务：1.使用while循环检查元素个数是否>2；
                   2.如果<2,即0和1，要调用wait进行阻塞
                 */
                while (matchQueue.size()<2){
                    matchQueue.wait();
                }
                User user1 = matchQueue.poll();
                User user2 = matchQueue.poll();
                System.out.println("获取两个匹配玩家："+user1.getUsername()+"和"+user2.getUsername());
                 //获取会话，通知玩家匹配成功

                WebSocketSession session1 = onlineUserManager.getFromGameHall(user1.getUserId());
                WebSocketSession session2 = onlineUserManager.getFromGameHall(user2.getUserId());
//                System.out.println("获取session成功");
//                System.out.println(session1+" "+session2);//session存在
                //处理突发情况:玩家在匹配时掉线了,此时就将另一个玩家加入匹配队列
                if(session1==null){
                    System.out.println("玩家1 session为空");
                    matchQueue.offer(user2);
                    return;
                }
                if(session2==null){
                    System.out.println("玩家2 session为空");
                    matchQueue.offer(user1);
                    return;
                }
                //处理一个玩家匹配了两次
                if(session2==session1){
                    System.out.println("两个玩家的session相同！");
                    matchQueue.offer(user1);
                    return;
                }
                //将两个玩家放入游戏房间中

                Room room = new Room();
                roomManager.add(room,user1.getUserId(),user2.getUserId());
                //System.out.println("创建room成功！"+room.getRoomId()+"玩家1："+room.getUser1().getUserId()+"玩家2："+room.getUser2().getUserId());

                // 将用户从游戏大厅放入游戏房间的map中
//                onlineUserManager.enterGameRoom(user1.getUserId(), session1);
//                onlineUserManager.enterGameRoom(user2.getUserId(),session2);

                //返回匹配成功的消息，注意要返回给两个玩家
                MatchResponse response1 = new MatchResponse();
                response1.setOk(true);
                response1.setMessage("matchSuccess");
                String json1 = mapper.writeValueAsString(response1);
                session1.sendMessage(new TextMessage(json1));
                System.out.println("玩家1发送了匹配成功的信息！"+json1);

                MatchResponse response2 = new MatchResponse();
                response2.setOk(true);
                response2.setMessage("matchSuccess");
                String json2 = mapper.writeValueAsString(response2);
                session2.sendMessage(new TextMessage(json2));
                System.out.println("玩家2发送了匹配成功的信息！"+json2);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

}
