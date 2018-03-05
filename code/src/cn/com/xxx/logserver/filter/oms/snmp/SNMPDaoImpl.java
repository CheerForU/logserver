package cn.com.xxx.logserver.filter.oms.snmp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import cn.com.xxx.logserver.filter.oms.model.SysConfig;

public final class SNMPDaoImpl
{

    private static final String SQL_UPDATE_SNMP = "oms.update";

    private static final Logger LOGGER = LoggerFactory.getLogger(SNMPDaoImpl.class);

    private static final SNMPDaoImpl INSTANCE = new SNMPDaoImpl();

    private SqlMapClientTemplate daoTemplate;

    private PlatformTransactionManager transactionManager = null;

    private SNMPDaoImpl()
    {

    }

    public static SNMPDaoImpl getInstance()
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

    public void updateTrapConfig(final SysConfig sysconfig) throws Exception
    {
        final TransactionDefinition def = new DefaultTransactionDefinition();
        final TransactionStatus status = transactionManager.getTransaction(def);
        try
        {
            daoTemplate.update(SQL_UPDATE_SNMP, sysconfig);
            transactionManager.commit(status);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to update trap. condition={}", sysconfig);
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

    public void updateMonitorConfig(final SysConfig sysConfig) throws Exception
    {
        final TransactionDefinition def = new DefaultTransactionDefinition();
        final TransactionStatus status = transactionManager.getTransaction(def);
        try
        {
            daoTemplate.update(SQL_UPDATE_SNMP, sysConfig);
            transactionManager.commit(status);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to update monitor. condition={}", sysConfig);
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