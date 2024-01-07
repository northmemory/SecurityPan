package com.xpb;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xpb.entities.User;
import com.xpb.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.UUID;

@SpringBootTest
class SecurityPanApplicationTests {
    @Autowired
    UserMapper userMapper;
    @Test
    void userMapperAllTest() {
        System.out.println("--------selectAll method test");
        List<User> users = userMapper.selectList(null);
        users.forEach(System.out::println);
    }

    @Test
    void userInsertTest() {
        User user=new User();
        user.setNickname("xpb2");
        user.setPassword("$2a$10$CG0WroijPY13A2uW/B3dcOKKDR7vDVbes0x8UcweZHpp6pAnVAxoO");
        user.setEmail("1597511@qq.com");
        userMapper.insert(user);
    }
    @Test
    void userMapperIdTest() {
        User user = userMapper.selectById("1716444862600695809");
        System.out.println(user);
        LambdaQueryWrapper<User> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(User::getNickname,"xiaoming");
        userMapper.selectObjs(wrapper);
    }
    @Test
    void encodeTest(){
        BCryptPasswordEncoder passwordEncoder=new BCryptPasswordEncoder();
        String encode= passwordEncoder.encode("123456");
        System.out.println(encode);
    }
    @Test
    void uuidTest(){
        UUID uuid = UUID.randomUUID();
        System.out.println("生成的UUID为：" + uuid.toString());
        return;
    }
    @Test
    void IDworkerTest(){

    }
}
