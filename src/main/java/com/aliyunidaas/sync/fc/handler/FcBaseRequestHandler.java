package com.aliyunidaas.sync.fc.handler;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.FunctionComputeLogger;
import com.aliyun.fc.runtime.HttpRequestHandler;
import com.aliyunidaas.sync.event.objects.ErrorResponseObject;
import com.aliyunidaas.sync.event.objects.RequestObject;
import com.aliyunidaas.sync.event.objects.ResponseObject;
import com.aliyunidaas.sync.event.objects.SuccessResponseObject;
import com.aliyunidaas.sync.fc.util.ServletUtil;
import com.aliyunidaas.sync.internal.util.ExceptionUtil;
import com.aliyunidaas.sync.internal.util.IpMatcher;
import com.aliyunidaas.sync.internal.util.JsonUtil;
import com.aliyunidaas.sync.internal.util.StringUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 基础函数计算处理Handler
 *
 * @author hatterjiang
 * @see com.aliyunidaas.sync.event.runner.EventDataRunner
 */
public abstract class FcBaseRequestHandler implements HttpRequestHandler {

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, Context context)
            throws IOException, ServletException {
        final FunctionComputeLogger logger = context.getLogger();

        // 检查IP地址是否被允许访问，支持CIDR表示，如 10.0.0.0/16
        final String remoteAddress = getRemoteAddress(request, logger);
        logger.debug("Remote address: " + remoteAddress);
        final IpMatcher ipMatcher = getIpMatcher(logger);
        if (ipMatcher != null) {
            if (!ipMatcher.matches(remoteAddress)) {
                final ErrorResponseObject errorResponseObject = new ErrorResponseObject();
                errorResponseObject.setError("bad_request");
                errorResponseObject.setErrorDescription("Blocked ip address: " + remoteAddress);
                ServletUtil.writeErrorResponse(response, logger, 400, errorResponseObject);
                return;
            }
        }

        String requestBody = null;
        try {
            requestBody = ServletUtil.readRequestBody(request);
            if ((requestBody == null) || requestBody.trim().isEmpty()) {
                final ErrorResponseObject errorResponseObject = new ErrorResponseObject();
                errorResponseObject.setError("bad_request");
                errorResponseObject.setErrorDescription("Empty request body");
                ServletUtil.writeErrorResponse(response, logger, 400, errorResponseObject);
                return;
            }
            final RequestObject requestObject = JsonUtil.fromJson(requestBody, RequestObject.class);
            logger.debug("Receive event object: " + JsonUtil.toJson(requestObject));

            final ResponseObject responseObject = innerHandleRequest(context, requestObject);
            if (responseObject instanceof SuccessResponseObject) {
                ServletUtil.writeSuccessResponse(response, logger, (SuccessResponseObject)responseObject);
            } else {
                ServletUtil.writeErrorResponse(response, logger, 500, (ErrorResponseObject)responseObject);
            }
        } catch (Throwable t) {
            logger.error("Unknown error: " + t.getMessage()
                    + ", requestURI: " + request.getRequestURI()
                    + ", requestBody: " + requestBody
                    + " :: " + ExceptionUtil.printStacktrace(t));

            final ErrorResponseObject errorResponseObject = new ErrorResponseObject();
            errorResponseObject.setError("internal_error");
            errorResponseObject.setErrorDescription("Error: " + t.getMessage());
            ServletUtil.writeErrorResponse(response, logger, 500, errorResponseObject);
        }
    }

    protected String getRemoteAddress(HttpServletRequest request, FunctionComputeLogger logger) {
        final String xFcClientIp = request.getHeader("X-Fc-Client-Ip");
        if (StringUtil.isNotEmpty(xFcClientIp)) {
            return xFcClientIp;
        }

        final String xFcHttpParams = request.getHeader("X-Fc-Http-Params");
        if (StringUtil.isNotEmpty(xFcHttpParams)) {
            try {
                final String xFcHttpParamsJson = new String(Base64.getDecoder().decode(xFcHttpParams), StandardCharsets.UTF_8);
                @SuppressWarnings("unchecked")
                final Map<String, Object> xFcHttpParamsObject = JsonUtil.fromJson(xFcHttpParamsJson, Map.class);
                final Object clientIp = xFcHttpParamsObject.get("clientIP");
                if (clientIp instanceof String) {
                    return (String)clientIp;
                }
            } catch (Exception e) {
                logger.warn("Get and parse X-Fc-Http-Params failed: " + xFcHttpParams);
            }
        }

        return request.getRemoteAddr();
    }

    protected IpMatcher getIpMatcher(FunctionComputeLogger logger) {return null;}

    protected abstract ResponseObject innerHandleRequest(Context context, RequestObject requestObject) throws Exception;
}