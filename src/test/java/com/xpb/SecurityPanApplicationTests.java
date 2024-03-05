package com.xpb;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xpb.entities.User;
import com.xpb.mapper.UserMapper;
import com.xpb.utils.FileUtil;
import com.xpb.utils.StreamMediaUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class SecurityPanApplicationTests {
    @Resource
    RedisTemplate redisTemplate;
    @Autowired
    UserMapper userMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;
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
    void mqTest(){
        String queueName="simple.queue";
        rabbitTemplate.convertAndSend(queueName,"hello world!");
    }
    @Test
    void deleteCache(){
        redisTemplate.opsForValue().set("1","1");
        System.out.println(redisTemplate.opsForValue().get("1"));
    }
    @Test
    void LazyQueueTest(){
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        Message message=new Message("hello".getBytes(),messageProperties);
        for (int i = 0; i < 1000000; i++) {
            rabbitTemplate.convertAndSend("lazy.queue",message);
        }
    }
    @Test
    void ffmpegTest() throws IOException {
        /*String targetPath="E:/PanStorage/file/202402/professional.mp4";
        StreamMediaUtil.transferVideo("114514",targetPath);*/
        /*String sourcePath="E:/PanStorage/file/202402/professional.mp4";
        String targetPath="E:/PanStorage/file/202402/114514-cover.png";
        StreamMediaUtil.generateThumbnailForVideo(sourcePath,150,targetPath);*/
        StreamMediaUtil.generateThumbnailForImage("E:/PanStorage/file/202402/xpb.jpg",150,"E:/PanStorage/file/202402/xpb_.jpg");

    }
}
