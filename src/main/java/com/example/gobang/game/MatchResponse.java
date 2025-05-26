package com.example.gobang.game;

import lombok.Data;
//表示Websocket的匹配响应
@Data
public class MatchResponse {
    private boolean ok;
    private String message;
    private String reason;
}
