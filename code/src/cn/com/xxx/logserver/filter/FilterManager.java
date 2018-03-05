package cn.com.xxx.logserver.filter;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.ibatis.common.jdbc.exception.NestedSQLException;
import com.mysql.jdbc.MysqlDataTruncation;

import cn.com.xxx.common.common.CommonUtil;
import cn.com.xxx.common.file.XMLOper;
import cn.com.xxx.logserver.LogServer;
import cn.com.xxx.logserver.common.ConfigParamsHolder;
import cn.com.xxx.logserver.filter.model.BaseLog;
import cn.com.xxx.logserver.receive.LogReceiver;
import cn.com.xxx.mdp.model.Message;

/**
 * 过滤器管理
 * @author wy
 *
 */
public final class FilterManager
{

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterManager.class);

    private static final FilterManager INSTANCE = new FilterManager();

    private final int dbfailureMaxretry = ConfigParamsHolder.getInstance()
            .getLogfilterDbfailureMaxretry();

    private int dbfailureNum = 0;

    private List<LogFilter> filters;

    private PlatformTransactionManager transactionManager;

    private FilterManager()
    {

    }

    public static FilterManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * 处理日志消息
     * @param message 日志消息
     */
    public void dispose(final Message message)
    {
        TransactionStatus status = null;
        BaseLog log = null;
        final boolean[] ack = { false };
        try
        {
            // 解析日志消息
            log = parseLog(message);
            if (null == log)
            {
                ack[0] = true;
                return;
            }

            final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            status = transactionManager.getTransaction(def);

            // 过滤器按顺序处理
            for (final LogFilter filter : filters)
            {
                filter.dispose(log);
            }

            transactionManager.commit(status);
            
            // 确认消息
            ack[0] = true;

            // 失败次数归零
            dbfailureNum = 0;

            LOGGER.info("Success to dispose log. log={}", log);
        }
        catch (final DataIntegrityViolationException e)
        {
            dealExecuteSQLException(e, status, log, message, ack);
        }
        catch (final NestedSQLException e)
        {
            dealExecuteSQLException(e, status, log, message, ack);
        }
        catch (final MysqlDataTruncation e)
        {
            dealExecuteSQLException(e, status, log, message, ack);
        }
        catch (final SQLException e)
        {
            dealDatabaseException(e, status, log);
        }
        catch (final DataAccessException e)
        {
            dealDatabaseException(e, status, log);
        }
        catch (final TransactionException e)
        {
            dealDatabaseException(e, status, log);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to dispose log. log={}", log);
            LOGGER.error(e.getLocalizedMessage(), e);
            rollback(status);
            // 未知异常时直接丢弃消息
            ack[0] = true;
        }
        finally
        {
            LogReceiver.getInstance().releaseMessage(message, ack[0]);
        }
    }

    private void dealExecuteSQLException(final Exception e,
            final TransactionStatus status, final BaseLog log, final Message message,
            final boolean[] ack)
    {
        LOGGER.error("Failed to dispose log, discard log message. log={}", log);
        LOGGER.error(e.getLocalizedMessage());
        rollback(status);

        // SQL执行出错时直接丢弃消息
        ack[0] = true;
    }

    private void dealDatabaseException(final Exception e, final TransactionStatus status,
            final BaseLog log)
    {
        LOGGER.error("Failed to dispose log. log={}", log);
        LOGGER.error(e.getLocalizedMessage());
        rollback(status);

        Throwable t = e;
        do
        {
            if (t instanceof com.mysql.jdbc.exceptions.jdbc4.CommunicationsException
                    || t instanceof com.mysql.jdbc.CommunicationsException
                    || t instanceof DataAccessResourceFailureException)
            {
                // 连续失败次数达到logfilter.dbfailure.maxretry时，停止进程
                if (++dbfailureNum >= dbfailureMaxretry)
                {
                    LOGGER.error("The number of database operation failures to reach the maximum number.");
                    LogServer.stop();
                }

                break;
            }
        }
        while (null != (t = t.getCause()));
    }

    private BaseLog parseLog(final Message message)
    {
        if (null == message)
        {
            LOGGER.error("message is null.");
            return null;
        }

        if (!(message.getContent() instanceof String))
        {
            LOGGER.error("Invalid message type.");
            return null;
        }

        BaseLog log = null;
        try
        {
            final String msgContent = (String) message.getContent();
            if (null == msgContent)
            {
                LOGGER.error("Invalid message content.");
                return null;
            }

            log = XMLOper.unmarshalFromStr(msgContent, BaseLog.class);
            if (null == log)
            {
                LOGGER.error("Failed to unmarshal log message.");
                return null;
            }

            // desc长度超过500时截断
            final String logDesc = log.getDesc();
            if (!CommonUtil.isEmpty(logDesc) && logDesc.length() > 500)
            {
                log.setDesc(logDesc.substring(0, 497).concat("..."));
            }
        }
        catch (final Exception e)
        {
            LOGGER.error(e.getLocalizedMessage(), e);
            return null;
        }

        return log;
    }

    private void rollback(final TransactionStatus status)
    {
        if (null == status)
        {
            return;
        }

        try
        {
            transactionManager.rollback(status);
        }
        catch (final Exception ex)
        {
            LOGGER.error(ex.getLocalizedMessage(), ex);
        }
    }

    public List<LogFilter> getFilters()
    {
        return filters;
    }

    public void setFilters(final List<LogFilter> filters)
    {
        this.filters = filters;
    }

    public PlatformTransactionManager getTransactionManager()
    {
        return transactionManager;
    }

    public void setTransactionManager(final PlatformTransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }

}
