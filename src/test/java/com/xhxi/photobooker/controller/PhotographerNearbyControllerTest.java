package com.xhxi.photobooker.controller;

import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.PhotographerService;
import com.xhxi.photobooker.vo.PhotographerNearbyVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotographerNearbyControllerTest {

    private PhotographerController controller;
    private PhotographerService photographerService;

    @BeforeEach
    void setUp() {
        controller = new PhotographerController();
        photographerService = Mockito.mock(PhotographerService.class);
        // 通过反射注入（controller 字段是 @Autowired private，无 setter）
        try {
            java.lang.reflect.Field field = PhotographerController.class.getDeclaredField("photographerService");
            field.setAccessible(true);
            field.set(controller, photographerService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void nearby_rejectsInvalidLatitude() {
        Result<?> result = controller.nearby(200.0, 113.28, 5.0, 20);

        assertEquals(0, result.getCode());
        assertTrue(result.getMsg().contains("latitude"));
    }

    @Test
    void nearby_rejectsInvalidLongitude() {
        Result<?> result = controller.nearby(23.12, 200.0, 5.0, 20);

        assertEquals(0, result.getCode());
        assertTrue(result.getMsg().contains("longitude"));
    }

    @Test
    void nearby_rejectsInvalidRadius() {
        Result<?> result = controller.nearby(23.12, 113.28, 0.0, 20);

        assertEquals(0, result.getCode());
        assertTrue(result.getMsg().contains("radius"));
    }

    @Test
    void nearby_rejectsInvalidLimit() {
        Result<?> result = controller.nearby(23.12, 113.28, 5.0, 100);

        assertEquals(0, result.getCode());
        assertTrue(result.getMsg().contains("limit"));
    }

    @Test
    void nearby_returnsPhotographerListOnValidParams() {
        Mockito.when(photographerService.searchNearbyPhotographers(23.12, 113.28, 5.0, 20))
                .thenReturn(List.of(PhotographerNearbyVO.builder().id(1L).name("张三").distanceKm(1.5).build()));

        Result<List<PhotographerNearbyVO>> result = controller.nearby(23.12, 113.28, 5.0, 20);

        assertEquals(1, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("张三", result.getData().get(0).getName());
        assertNull(result.getMsg());
    }
}
