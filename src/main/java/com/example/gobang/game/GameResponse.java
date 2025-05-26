package com.example.gobang.game;

import lombok.Data;
//表示落子响应
@Data
public class GameResponse {
    private String message;
    private int userId;
    private int row;
    private int col;
    private int winner;
}
