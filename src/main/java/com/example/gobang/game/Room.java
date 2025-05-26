package com.example.gobang.game;

import com.example.gobang.GobangApplication;
import com.example.gobang.mapper.UserMapper;
import com.example.gobang.model.User;
import com.example.gobang.util.SpringContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.UUID;

import static com.example.gobang.util.SpringContextUtil.getBean;
//表示一个游戏房间
@Data
//@Component
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
//使用原型作用域来处理依赖注入问题
public class Room {
    private String roomId;
    private User user1;
    private User user2;
    //先手玩家的id
    private int whiteUser;

    private static final int MAX_ROW = 15;
    private static final int MAX_COL = 15;
    //使用二位数组表示棋盘
    //1 表示user1的落子位置，2表示user2的落子位置，0表示空
    private int[][] board = new int[MAX_ROW][MAX_COL];
    private ObjectMapper objectMapper = new ObjectMapper();
    //@Autowired
    private OnlineUserManager onlineUserManager;
    //引入RoomManger来管理room对象
   //@Autowired
    private RoomManager roomManager;
   //@Autowired
    private UserMapper userMapper;
    //处理落子请求
    public void putChess(String reqJson) throws IOException {
        GameRequest request = objectMapper.readValue(reqJson, GameRequest.class);
        GameResponse response = new GameResponse();
        //根据request中的信息判断是1还是2
        int chesser = request.getUserId() == user1.getUserId() ?1:2;
        int row = request.getRow();
        int col = request.getCol();
        if(board[row][col] != 0){
            System.out.println("当前位置（"+row+","+col+"）已经有子了");
            return;
        }
        board[row][col] = chesser;
        //打印当前的棋盘信息，方便调试和观察
        printBoard();
        //进行胜负判定
        int winner = checkWinner(row,col,chesser);
        //胜负已分后，通知room中的对象

        response.setMessage("putChess");
        response.setUserId(request.getUserId());
        response.setRow(row);
        response.setCol(col);
        response.setWinner(winner);
        //获取每个用户的WebsocketSession
        WebSocketSession session1 = onlineUserManager.getFromGameRoom(user1.getUserId());
        System.out.println("玩家1的会话："+session1);
        WebSocketSession session2 = onlineUserManager.getFromGameRoom(user2.getUserId());
        System.out.println("玩家2的会话："+session2);
        //处理会话为空的情况
        if(session1==null){
            //1下线，认为2直接获胜
            response.setWinner(user2.getUserId());
            System.out.println("玩家1掉线！");
        }
        if(session2==null){
            response.setWinner(user1.getUserId());
            System.out.println("玩家2掉线！");
        }
        //把响应转为json再传输
        String respJson = objectMapper.writeValueAsString(response);
        if(session1!=null){
            session1.sendMessage(new TextMessage(respJson));
        }
        if(session2!=null){
            session2.sendMessage(new TextMessage(respJson));
        }
        //胜负已分，room就可以销毁了
        if(response.getWinner()!=0){
            System.out.println("游戏结束！房间："+roomId+"将进行销毁"+"获胜方为："+response.getWinner());
            int winnerId = response.getWinner();
            int loserId = response.getWinner() == user1.getUserId() ? user2.getUserId() : user1.getUserId();
            userMapper.userWin(winnerId);
            userMapper.userLose(loserId);
            //销毁room
            roomManager.remove(roomId,user1.getUserId(),user2.getUserId());
        }
    }

    private int checkWinner(int row, int col, int chesser) {
        //主要负责判断棋子是否五子连珠
        //1.行的情况
        for(int i = col-4; i <= col; i++){
            try{
                if(board[row][i] == chesser && board[row][i+1] == chesser &&
                        board[row][i+2] == chesser && board[row][i+3] == chesser
                && board[row][i+4] == chesser){
                    return chesser == 1 ? user1.getUserId() : user2.getUserId();
                }
            }catch(ArrayIndexOutOfBoundsException e){
                continue;
                //越界了不需要处理，直接判断下一个就行了
            }
        }
        //2.列的情况
        for(int i = row-4; i <= row; i++){
            try{
                if(board[i][col]==chesser && board[i+1][col]==chesser&& board[i+2][col]==chesser
                        && board[i+3][col]==chesser && board[i+4][col]==chesser){
                    return chesser == 1 ? user1.getUserId() : user2.getUserId();
                }
            }catch(ArrayIndexOutOfBoundsException e){
                continue;
            }
        }
        //3.左对角线
        for(int r = row - 4,c = col + 4; r <= row && c >= col; r++,c--){
            try{
                if(board[r][c]==chesser && board[r+1][c-1]==chesser && board[r+2][c-2]==chesser
                        && board[r+3][c-3]==chesser && board[r+4][c-4]==chesser){
                    return chesser == 1 ? user1.getUserId() : user2.getUserId();
                }
            }catch(ArrayIndexOutOfBoundsException e){
                continue;
            }
        }
        //4.右对角线
        for(int r = row - 4,c = col - 4; r <= row && c <= col; r++,c++){
            try{
                if(board[r][c]==chesser && board[r+1][c+1]==chesser && board[r+2][c+2]==chesser
                        && board[r+3][c+3]==chesser && board[r+4][c+4]==chesser){
                    return chesser == 1 ? user1.getUserId() : user2.getUserId();
                }
            }catch(ArrayIndexOutOfBoundsException e){
                continue;
            }
        }
        return 0;//0表示胜负未分
    }

    private void printBoard() {
        System.out.println("打印棋盘信息"+roomId);
        System.out.println("=====================================");
        for (int i = 0; i < MAX_ROW; i++) {
            for (int j = 0; j < MAX_COL; j++) {
                System.out.print(board[i][j]+"\t");
            }
            System.out.println();
        }
        System.out.println("=====================================");
    }

    public Room(){
        //使用UUID来作为房间id
        roomId = UUID.randomUUID().toString();
        onlineUserManager = GobangApplication.context.getBean(OnlineUserManager.class);
        roomManager = GobangApplication.context.getBean(RoomManager.class);
        userMapper = GobangApplication.context.getBean(UserMapper.class);

    }

    public static void main(String[] args) {
        Room room = new Room();
        System.out.println(room.getRoomId());
    }
}
