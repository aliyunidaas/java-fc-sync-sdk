package com.aliyunidaas.sync.fc.log;

import com.aliyun.fc.runtime.FunctionComputeLogger;
import com.aliyunidaas.sync.log.SimpleLogger;

/**
 * 将日志适配到函数计算日志
 *
 * @author hatterjiang
 */
public class FcSimpleLogger implements SimpleLogger {
    private final FunctionComputeLogger logger;

    public FcSimpleLogger(FunctionComputeLogger logger) {
        this.logger = logger;
    }

    @Override
    public void trace(String message) {
        logger.trace(message);
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void fatal(String message) {
        logger.fatal(message);
    }
}
