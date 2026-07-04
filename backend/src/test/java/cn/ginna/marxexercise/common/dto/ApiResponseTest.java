package cn.ginna.marxexercise.common.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiResponse 单元测试
 */
class ApiResponseTest {

    @Test
    void testSuccess() {
        ApiResponse<String> resp = ApiResponse.success("ok");
        assertEquals(200, resp.getCode());
        assertEquals("ok", resp.getData());
        assertEquals("success", resp.getMessage());
    }

    @Test
    void testError() {
        ApiResponse<?> resp = ApiResponse.error(400, "bad request");
        assertEquals(400, resp.getCode());
        assertEquals("bad request", resp.getMessage());
        assertNull(resp.getData());
    }

    @Test
    void testSuccessWithNullData() {
        ApiResponse<Object> resp = ApiResponse.success(null);
        assertEquals(200, resp.getCode());
        assertNull(resp.getData());
    }
}
