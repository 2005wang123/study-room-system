package com.example.studyroom;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.example.studyroom.mapper") // 指向mapper包路径
@SpringBootApplication
@EnableScheduling // 启用定时任务（自动清理超时/未签退的预约）
public class StudyRoomApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudyRoomApplication.class, args);
        System.out.println("=================================");
        System.out.println("项目启动成功！");
        System.out.println("=================================");
    }
}