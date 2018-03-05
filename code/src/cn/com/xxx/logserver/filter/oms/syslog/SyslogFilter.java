package cn.com.xxx.logserver.filter.oms.syslog;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.xxx.common.common.CommonUtil;
import cn.com.xxx.common.locale.ResourceUtil;
import cn.com.xxx.logserver.common.Constants;
import cn.com.xxx.logserver.common.ResourceKey;
import cn.com.xxx.logserver.filter.LogFilter;
import cn.com.xxx.logserver.filter.model.BaseLog;
import cn.com.xxx.logserver.filter.model.LogLevel;
import cn.com.xxx.logserver.filter.oms.syslog.model.Syslog;
import cn.com.xxx.oms.syslog.SysLogUtil;

/**
 * syslog过滤器
 * @author ch
 * 
 */
public class SyslogFilter implements LogFilter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LogFilter.class);

    private static final SyslogFilter INSTANCE = new SyslogFilter();

    // syslog的启用开关，启用时向syslog发送日志，禁用时不作处理
    private boolean enable = false;

    private SyslogFilter()
    {

    }

    public static SyslogFilter getInstance()
    {
        return INSTANCE;
    }

    public boolean isEnable()
    {
        return enable;
    }

    public void setEnable(final boolean enable)
    {
        this.enable = enable;
    }

    public void init()
    {
        try
        {
            // 从数据库读取syslog配置，并初始化SysLogUtil
            final Syslog syslog = SyslogDaoImpl.getInstance().getSyslogConfig();
            if (!CommonUtil.isEmpty(syslog.getEnable())
                    && "true".equals(syslog.getEnable()))
            {
                if (CommonUtil.hasEmptyString(syslog.getProtocol(),
                        syslog.getHost(),
                        syslog.getPort(),
                        syslog.getCharset()))
                {
                    LOGGER.error("Invalid syslog config params,can't init syslog.");
                }
                else
                {
                    SysLogUtil.init(syslog.getProtocol(),
                            syslog.getHost(),
                            Integer.valueOf(syslog.getPort()),
                            syslog.getCharset());
                    enable = true;
                    LOGGER.info("success init syslog.");
                }
            }
            else
            {
                LOGGER.info("syslog is not enabled.");
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("init syslog filter failed.");
            LOGGER.error(e.getLocalizedMessage(), e);
        }
    }

    public void destroy()
    {
        SysLogUtil.destroy();
    }

    @Override
    public void dispose(final BaseLog log) throws Exception
    {
        if (enable)
        {
            LOGGER.debug("Start to dispose {}.", log);

            // 拼装syslog记录的内容
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            final StringBuffer sb = new StringBuffer();
            sb.append(ResourceUtil.getMessage(ResourceKey.ALARM_PREFIX_TIME));
            sb.append(format.format(new Date(log.getTm())));
            sb.append(Constants.BLANK);
            sb.append(ResourceUtil.getMessage(ResourceKey.ALARM_PREFIX_DESC));
            sb.append(log.getDesc());
            sb.append(Constants.BLANK);
            sb.append(ResourceUtil.getMessage(ResourceKey.ALARM_PREFIX_COMPONENT));
            sb.append(log.getComponent().getName());
            sb.append(Constants.BLANK);
            sb.append(ResourceUtil.getMessage(ResourceKey.ALARM_PREFIX_USER));
            sb.append(log.getUser());
            sb.append(Constants.BLANK);
            sb.append(ResourceUtil.getMessage(ResourceKey.ALARM_PREFIX_IP));
            sb.append(log.getIp());

            // 向syslog发送
            final LogLevel level = log.getLevel();
            if (level.getValue() == 1)
            {
                SysLogUtil.info(sb.toString());
            }
            if (level.getValue() == 2)
            {
                SysLogUtil.warn(sb.toString());
            }
            if (level.getValue() == 3)
            {
                SysLogUtil.error(sb.toString());
            }

            LOGGER.debug("Disposition of {} finished.", log);
        }
    }

}
