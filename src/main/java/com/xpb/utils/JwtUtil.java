package com.xpb.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {

    //有效期为
    @Value("${Jwt.expire_time}")
    public  Long JWT_TTL ;// 60 * 60 *1000  一个小时
    //设置秘密
    @Value("${Jwt.secret}")
    public String secret;
    //设置密钥
    private SecretKey KEY;

    @PostConstruct
    public void init() {
        KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String creatJWT(String subject, Map<String,String> payload) {
        //创建一个Jwt构造器
        JwtBuilder builder = Jwts.builder();
        //设置签发时间
        builder.setIssuedAt(new Date());
        //设置过期时间
        builder.setExpiration(new Date(System.currentTimeMillis() + JWT_TTL*1000));
        //设置Id
        builder.setId(UUID.randomUUID().toString());
        //设置主题
        builder.setSubject(subject);
        //设置自定义信息
        if (payload!=null){
            payload.forEach((String key, String val)->{
                builder.claim(key,val);
            });
        }
        //设置签名
        builder.signWith(KEY);
        //生成token字符串
        String token = builder.compact();
        return token;
    }

    public Claims parseJWT(String jwt) throws Exception{
        JwtParserBuilder jwtParserBuilder = Jwts.parserBuilder();  //jwt解析器
        jwtParserBuilder.setSigningKey(KEY);  //设置签名的密钥
        Jws<Claims> claimsJws = jwtParserBuilder.build().parseClaimsJws(jwt);//解析内容

        JwsHeader header = claimsJws.getHeader();
        Claims body = claimsJws.getBody();
        return body;
    }

}