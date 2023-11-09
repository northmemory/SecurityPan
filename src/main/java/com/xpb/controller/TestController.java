package com.xpb.controller;

import com.xpb.aop.annotation.CurrentLimiting;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/hello")
    @CurrentLimiting
    public String sayhello(){
        return "hello";
    }
}
