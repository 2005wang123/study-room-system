//定时任务（自动释放超时预约）
// config/ScheduledTasks.java
package com.example.studyroom.config;

import com.example.studyroom.service.IReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final IReservationService reservationService;

    // 每小时执行一次，清理超时预约
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanExpiredReservations() {
        log.info("开始清理超时预约");
        int count = reservationService.cleanExpiredReservations();
        log.info("清理了 {} 条超时预约", count);
    }
}