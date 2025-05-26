package com.example.gobang.game;

import lombok.Data;

//表示Websocket的匹配请求
@Data
public class MatchRequest {
    private String message="";
}
