package cn.com.xxx.logserver.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.xxx.common.config.Configuration;

public final class ConfigParamsHolder
{

    private static final String TABLE_MAXKEEPMONTH_SUFFIX = ".maxkeepmonth";

    private static final String LOGQUEUE_CAPACITY = "logqueue.capacity";

    private static final String LOGQUEUE_FLOWCONTROL_RADIO = "logqueue.flowcontrol.radio";

    private static final String LOGFILTER_DBFAILURE_MAXRETRY = "logfilter.dbfailure.maxretry";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigParamsHolder.class);

    private static final ConfigParamsHolder INSTANCE = new ConfigParamsHolder();

    private final Configuration configParams = new Configuration();

    private final ConcurrentMap<String, Object> propertyMap = new ConcurrentHashMap<String, Object>();

    private ConfigParamsHolder()
    {
        configParams.loadProperties("/config.properties", false);
    }

    public static ConfigParamsHolder getInstance()
    {
        return INSTANCE;
    }

    private String getProperty(final String key)
    {
        return configParams.getProperty(key);
    }

    /**
     * 获取数据表的最大保留月份
     * @param tableName 最大保留月份（默认值6个月，最小值3个月）
     * @return 最大保留月份
     */
    public int getTableMaxKeepMonth(final String tableName)
    {
        int month = 6;
        String propName = null;

        try
        {
            final String[] values = tableName.split(Constants.UNDERLINE);
            if (null == values || values.length < 2)
            {
                LOGGER.error("Invalid table name. tableName={}", tableName);
                return month;
            }

            propName = values[1] + TABLE_MAXKEEPMONTH_SUFFIX;
            Object value = null;
            if (null != (value = propertyMap.get(propName)))
            {
                return (Integer) value;
            }

            final String maxKeepMonth = getProperty(propName);
            if (null != maxKeepMonth)
            {
                month = Integer.valueOf(maxKeepMonth);
                month = (month < 3) ? 3 : month;
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to get max keep month. tableName={}", tableName);
            LOGGER.error(e.getLocalizedMessage(), e);
        }

        if (null == propName)
        {
            return month;
        }

        propertyMap.put(propName, month);
        LOGGER.debug("{} is set to {}.", propName, month);
        return month;
    }

    /**
     * 获取日志队列的容量
     * @return 容量（默认值500，最小值100）
     */
    public int getLogqueueCapacity()
    {
        int capacity = 500;

        Object value = null;
        if (null != (value = propertyMap.get(LOGQUEUE_CAPACITY)))
        {
            return (Integer) value;
        }

        final String logqueueCapacity = getProperty(LOGQUEUE_CAPACITY);
        if (null != logqueueCapacity)
        {
            try
            {
                capacity = Integer.valueOf(logqueueCapacity);
                capacity = (capacity < 100) ? 100 : capacity;
            }
            catch (final NumberFormatException e)
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }

        propertyMap.put(LOGQUEUE_CAPACITY, capacity);
        LOGGER.debug("{} is set to {}.", LOGQUEUE_CAPACITY, capacity);
        return capacity;
    }

    /**
     * 获取日志队列的流控比率
     * @return 流控比率（默认值0.5，最小值0.2，最大值0.8）
     */
    public float getLogqueueFlowcontrolRadio()
    {
        float radio = 0.5f;

        Object value = null;
        if (null != (value = propertyMap.get(LOGQUEUE_FLOWCONTROL_RADIO)))
        {
            return (Float) value;
        }

        final String logqueueFlowcontrolRadio = getProperty(LOGQUEUE_FLOWCONTROL_RADIO);
        if (null != logqueueFlowcontrolRadio)
        {
            try
            {
                radio = Float.valueOf(logqueueFlowcontrolRadio);
                radio = (radio < 0.2f) ? 0.2f : ((radio > 0.8f) ? 0.8f : radio);
            }
            catch (final NumberFormatException e)
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }

        propertyMap.put(LOGQUEUE_FLOWCONTROL_RADIO, radio);
        LOGGER.debug("{} is set to {}.", LOGQUEUE_FLOWCONTROL_RADIO, radio);
        return radio;
    }

    /**
     * 获取日志过滤器的数据库操作失败的最大重试次数
     * @return 最大重试次数（默认值30，最小值3）
     */
    public int getLogfilterDbfailureMaxretry()
    {
        int retry = 30;

        Object value = null;
        if (null != (value = propertyMap.get(LOGFILTER_DBFAILURE_MAXRETRY)))
        {
            return (Integer) value;
        }

        final String logfilterDbfailureMaxretry = getProperty(LOGFILTER_DBFAILURE_MAXRETRY);
        if (null != logfilterDbfailureMaxretry)
        {
            try
            {
                retry = Integer.valueOf(logfilterDbfailureMaxretry);
                retry = (retry < 3) ? 3 : retry;
            }
            catch (final NumberFormatException e)
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }

        propertyMap.put(LOGFILTER_DBFAILURE_MAXRETRY, retry);
        LOGGER.debug("{} is set to {}.", LOGFILTER_DBFAILURE_MAXRETRY, retry);
        return retry;
    }

}
