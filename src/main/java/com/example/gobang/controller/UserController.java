package com.example.gobang.controller;

import com.example.gobang.mapper.UserMapper;
import com.example.gobang.model.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;
@CrossOrigin("*")
@RestController
public class UserController {
    @Resource
    private UserMapper userMapper;

   @PostMapping("/login")
   @ResponseBody
    public Object login(String username, String password, HttpServletRequest request) {
       User user = userMapper.selectByUsername(username);
       if (user == null) {
           System.out.println("用户"+username+"不存在");
           return new User();
       }
       if(!user.getPassword().equals(password)) {
           System.out.println("用户"+username+"密码错误");
           return new User();
       }
       HttpSession session = request.getSession(true);
       session.setAttribute("user", user);
       return user;
   }
   @PostMapping("/register")
    @ResponseBody
    public Object register(String username, String password) {
       try{
           User user = new User();
           user.setUsername(username);
           user.setPassword(password);
           userMapper.insert(user);
           return  user;
       }catch (DuplicateKeyException e){//判断名字是否和已经存在的名字重复，DuplicateKeyException是判断主键是否重复的
           User user = new User();
           return user;
       }
   }
    @GetMapping("/userInfo")
    @ResponseBody
    public Object getUserInfo(HttpServletRequest request) {
       try{
           HttpSession session = request.getSession(false);
           User user = (User)session.getAttribute("user");
           //要再加一步，保证拿到的是最新的，不然比赛信息不会改变
           User newUser = userMapper.selectByUsername(user.getUsername());
           return newUser;
       }catch (NullPointerException e){
           return new User();
       }
    }
}
