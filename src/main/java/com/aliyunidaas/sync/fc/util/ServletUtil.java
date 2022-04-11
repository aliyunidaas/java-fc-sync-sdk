package com.aliyunidaas.sync.fc.util;

import com.aliyun.fc.runtime.FunctionComputeLogger;
import com.aliyunidaas.sync.event.objects.ErrorResponseObject;
import com.aliyunidaas.sync.event.objects.SuccessResponseObject;
import com.aliyunidaas.sync.internal.util.IoUtil;
import com.aliyunidaas.sync.internal.util.JsonUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Servlet工具类
 *
 * @author hatterjiang
 */
public class ServletUtil {

    @SuppressWarnings("SameParameterValue")
    public static <T> T readRequestObject(HttpServletRequest request, Class<T> requestObjectClass) throws IOException {
        final InputStream inputStream = request.getInputStream();
        final String body = new String(IoUtil.readAll(inputStream), StandardCharsets.UTF_8);
        return JsonUtil.fromJson(body, requestObjectClass);
    }

    @SuppressWarnings("SameParameterValue")
    public static String readRequestBody(HttpServletRequest request) throws IOException {
        final InputStream inputStream = request.getInputStream();
        final byte[] inputBytes = IoUtil.readAll(inputStream);
        if (inputBytes.length == 0) {
            return null;
        }
        return new String(inputBytes, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("SameParameterValue")
    public static void writeErrorResponse(HttpServletResponse response, FunctionComputeLogger logger, int status, ErrorResponseObject responseObject)
            throws IOException {
        writeResponse(response, logger, status, responseObject);
    }

    public static void writeSuccessResponse(HttpServletResponse response, FunctionComputeLogger logger, SuccessResponseObject responseObject)
            throws IOException {
        writeResponse(response, logger, 200, responseObject);
    }

    public static void writeResponse(HttpServletResponse response, FunctionComputeLogger logger, int status, Object responseObject)
            throws IOException {
        logger.debug("Response, status: " + status + ", body: " + JsonUtil.toJson(responseObject));
        response.setStatus(status);
        response.setContentType("application/json;charset=utf8");
        final OutputStream out = response.getOutputStream();
        out.write(JsonUtil.toJson(responseObject).getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }
}
