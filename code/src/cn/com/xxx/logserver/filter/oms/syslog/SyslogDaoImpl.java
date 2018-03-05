package cn.com.xxx.logserver.filter.oms.syslog;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import cn.com.xxx.logserver.filter.oms.model.SysConfig;
import cn.com.xxx.logserver.filter.oms.syslog.model.Syslog;

public final class SyslogDaoImpl
{
    private static final String SQL_QUERY_SYSLOG = "oms.query";

    private static final String SQL_UPDATE_SYSLOG = "oms.update";

    private static final Logger LOGGER = LoggerFactory.getLogger(SyslogDaoImpl.class);

    private static final SyslogDaoImpl INSTANCE = new SyslogDaoImpl();

    private SqlMapClientTemplate daoTemplate;

    private PlatformTransactionManager transactionManager;

    private static final String SYSLOG_ENABLE = "syslog_enable";
    private static final String SYSLOG_PROTOCOL = "syslog_protocol";
    private static final String SYSLOG_HOST = "syslog_host";
    private static final String SYSLOG_PORT = "syslog_port";
    private static final String SYSLOG_CHARSET = "syslog_charset";

    private SyslogDaoImpl()
    {

    }

    public static SyslogDaoImpl getInstance()
    {
        return INSTANCE;
    }

    public SqlMapClientTemplate getDaoTemplate()
    {
        return daoTemplate;
    }

    public void setDaoTemplate(final SqlMapClientTemplate daoTemplate)
    {
        this.daoTemplate = daoTemplate;
    }

    public PlatformTransactionManager getTransactionManager()
    {
        return transactionManager;
    }

    public void setTransactionManager(final PlatformTransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }

    /**
     * 从数据库获取syslog的配置
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public Syslog getSyslogConfig() throws Exception
    {
        try
        {
            final List<String> list = new ArrayList<String>();
            list.add(SYSLOG_ENABLE);
            list.add(SYSLOG_PROTOCOL);
            list.add(SYSLOG_HOST);
            list.add(SYSLOG_PORT);
            list.add(SYSLOG_CHARSET);
            final List<SysConfig> configs = daoTemplate.queryForList(SQL_QUERY_SYSLOG,
                    list);
            final Syslog syslog = new Syslog();
            if (null != configs)
            {
                for (final SysConfig config : configs)
                {
                    if (SYSLOG_ENABLE.equals(config.getS_key()))
                    {
                        syslog.setEnable(config.getS_value());
                    }
                    if (SYSLOG_PROTOCOL.equals(config.getS_key()))
                    {
                        syslog.setProtocol(config.getS_value());
                    }
                    if (SYSLOG_HOST.equals(config.getS_key()))
                    {
                        syslog.setHost(config.getS_value());
                    }
                    if (SYSLOG_PORT.equals(config.getS_key()))
                    {
                        syslog.setPort(config.getS_value());
                    }
                    if (SYSLOG_CHARSET.equals(config.getS_key()))
                    {
                        syslog.setCharset(config.getS_value());
                    }
                }
            }
            return syslog;
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to query syslog config.");
            LOGGER.error(e.getLocalizedMessage(), e);
            throw e;
        }
    }

    /**
     * 更新数据库的syslog配置
     * @param sysConfig
     * @throws Exception
     */
    public void updateSyslogConfig(final SysConfig sysConfig) throws Exception
    {
        final TransactionDefinition def = new DefaultTransactionDefinition();
        final TransactionStatus status = transactionManager.getTransaction(def);
        try
        {
            daoTemplate.update(SQL_UPDATE_SYSLOG, sysConfig);
            transactionManager.commit(status);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to update syslog. condition={}", sysConfig);
            LOGGER.error(e.getLocalizedMessage(), e);
            try
            {
                transactionManager.rollback(status);
            }
            catch (final Exception ex)
            {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }
            throw e;
        }
    }

}