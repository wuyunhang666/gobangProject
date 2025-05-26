package com.example.gobang.game;

import lombok.Data;

//客户端连接到游戏房间后，服务器返回的响应
@Data
public class GameReadyResponse {
    private String message;
    private boolean ok;
    private String reason;
    private String roomId;
    private int thisUserId;
    private int thatUserId;
    private int whiteUser;//白方先手
}
