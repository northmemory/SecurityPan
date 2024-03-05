package com.xpb.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class CommandUtil {
    public static void exec(String command) throws IOException {
        Runtime runtime=Runtime.getRuntime();
        try {
            runtime.exec(command);
        } catch (IOException e) {
            log.error(command+"：命令执行失败");
            throw e;
        }
    }
}
