package com.xhxi.photobooker.config;

import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.service.PhotographerGeoService;
import com.xhxi.photobooker.service.PhotographerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** 启动时全量同步 DB → Redis GEO，兜底历史数据与绕过接口的 DB 直改 */
@Component
public class GeoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GeoDataInitializer.class);

    @Autowired
    private PhotographerService photographerService;

    @Autowired
    private PhotographerGeoService photographerGeoService;

    private int syncedCount;

    @Override
    public void run(String... args) {
        try {
            List<Photographer> all = photographerService.getAllPhotographers();
            syncedCount = photographerGeoService.syncFromDb(all);
            log.info("摄影师地理位置索引初始化完成，共同步 {} 个摄影师", syncedCount);
        } catch (Exception e) {
            // Redis 不可达或数据异常时降级启动：不中断启动，GEO key 缺失时附近检索接口天然返回空列表
            log.error("摄影师地理位置索引初始化失败，本次跳过全量同步，附近检索将降级为空结果: {}", e.getMessage(), e);
        }
    }

    /** 测试观察用：本次同步的摄影师数 */
    public int getSyncedCount() {
        return syncedCount;
    }
}
