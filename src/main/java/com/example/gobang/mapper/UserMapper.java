package com.example.gobang.mapper;

import com.example.gobang.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    void insert(User user);
    User selectByUsername(String username);
    void userWin(int userId);//赢一把+50
    void userLose(int userId);//输一把-30
}
