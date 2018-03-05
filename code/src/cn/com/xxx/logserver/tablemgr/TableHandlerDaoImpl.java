package cn.com.xxx.logserver.tablemgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import cn.com.xxx.logserver.filter.jobhistory.JobHistoryDaoImpl;

public final class TableHandlerDaoImpl
{

    private static final String SQL_QUERY_TABLESIZE = "tablemgr.queryTableSize";

    private static final String SQL_QUERY_TABLE = "tablemgr.queryTable";

    private static final String SQL_QUERY_TABLE_BY_REGEX = "tablemgr.queryTableByRegex";

    private static final String SQL_QUERY_LOGDATE = "tablemgr.queryLogDate";

    private static final String SQL_QUERY_EXPIRETABLE = "tablemgr.queryExpireTable";

    private static final String SQL_DROP_EXPIRETABLE = "tablemgr.dropExpireTable";

    private static final String SQL_CREATE_NEWTABLE = "tablemgr.createNewTable";

    private static final String SQL_RENAME_CURTABLE = "tablemgr.renameTable";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobHistoryDaoImpl.class);

    private static final TableHandlerDaoImpl INSTANCE = new TableHandlerDaoImpl();

    private SqlMapClientTemplate daoTemplate;

    private PlatformTransactionManager transactionManager;

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

    private TableHandlerDaoImpl()
    {

    }

    public static TableHandlerDaoImpl getInstance()
    {
        return INSTANCE;
    }

    public float queryTableSize(final String tableName)
    {
        try
        {
            final Object result = daoTemplate.queryForObject(SQL_QUERY_TABLESIZE,
                    tableName);
            return result == null ? 0L : (Float) result;
        }
        catch (final RuntimeException e)
        {
            LOGGER.error(e.getLocalizedMessage(), e);
            throw e;
        }
    }

    public String queryTable(final String tableName)
    {
        try
        {
            return (String) daoTemplate.queryForObject(SQL_QUERY_TABLE, tableName);
        }
        catch (final RuntimeException e)
        {
            LOGGER.error(e.getLocalizedMessage(), e);
            throw e;
        }
    }

    /**
     * 根据正则表达式查询表名称
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public List<String> queryTablesByRegex(final String regex)
    {
        List<String> tables = new ArrayList<String>();
        try
        {
            tables = daoTemplate.queryForList(SQL_QUERY_TABLE_BY_REGEX, regex);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to query table by regex. regex={}", regex);
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return tables;
    }

    @SuppressWarnings("unchecked")
    public void delExpireTable(final String tableName, final int expDate)
    {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("tablename", tableName);
        params.put("expiredate", expDate);

        TransactionStatus status = null;
        List<String> expireTables = null;
        try
        {
            expireTables = daoTemplate.queryForList(SQL_QUERY_EXPIRETABLE, params);
            if (null == expireTables || expireTables.isEmpty())
            {
                return;
            }

            final TransactionDefinition def = new DefaultTransactionDefinition();
            status = transactionManager.getTransaction(def);

            for (final String table : expireTables)
            {
                daoTemplate.update(SQL_DROP_EXPIRETABLE, table);
            }

            transactionManager.commit(status);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to delete expire log table. tableNames={}", expireTables);
            LOGGER.error(e.getLocalizedMessage(), e);
            try
            {
                transactionManager.rollback(status);
            }
            catch (final Exception ex)
            {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }
            return;
        }

        LOGGER.info("Success to delete expire log table. tableNames={}", expireTables);
    }

    public long queryLogDate(final String tableName)
    {
        try
        {
            final Object result = daoTemplate.queryForObject(SQL_QUERY_LOGDATE, tableName);
            return result == null ? 0L : (Long) result;
        }
        catch (final RuntimeException e)
        {
            LOGGER.error(e.getLocalizedMessage(), e);
            throw e;
        }
    }

    public void buildNewTable(final String prefix, final String tableName)
    {
        final TransactionDefinition def = new DefaultTransactionDefinition();
        final TransactionStatus status = transactionManager.getTransaction(def);
        final Map<String, String> params = new HashMap<String, String>();
        params.put("prefix", prefix);
        params.put("tablename", tableName);

        try
        {
            daoTemplate.update(SQL_CREATE_NEWTABLE, params);
            transactionManager.commit(status);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to create log table. tableName={}", tableName);
            LOGGER.error(e.getLocalizedMessage(), e);
            try
            {
                transactionManager.rollback(status);
            }
            catch (final Exception ex)
            {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }
            return;
        }

        LOGGER.info("Success to create log table. tableName={}", tableName);
    }

    public void renameCurTable(final String oldName, final String newName)
    {
        final TransactionDefinition def = new DefaultTransactionDefinition();
        final TransactionStatus status = transactionManager.getTransaction(def);
        final Map<String, String> params = new HashMap<String, String>();
        params.put("oldname", oldName);
        params.put("newname", newName);

        try
        {
            daoTemplate.update(SQL_RENAME_CURTABLE, params);
            transactionManager.commit(status);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to rename log table. oldName={}, newName={}",
                    oldName,
                    newName);
            LOGGER.error(e.getLocalizedMessage(), e);
            try
            {
                transactionManager.rollback(status);
            }
            catch (final Exception ex)
            {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }
            return;
        }

        LOGGER.info("Success to rename log table. oldName={}, newName={}",
                oldName,
                newName);
    }
}
