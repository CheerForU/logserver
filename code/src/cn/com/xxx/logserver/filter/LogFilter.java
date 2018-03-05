package cn.com.xxx.logserver.filter;

import cn.com.xxx.logserver.filter.model.BaseLog;

/**
 * 日志过滤接口
 * @author wy
 *
 */
public interface LogFilter
{

    /**
     * 日志处理接口
     * @param log 日志
     * @throws Exception 抛出异常
     */
    void dispose(final BaseLog log) throws Exception;

}
