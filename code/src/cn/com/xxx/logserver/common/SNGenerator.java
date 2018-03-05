package cn.com.xxx.logserver.common;

import cn.com.xxx.logserver.filter.auditlog.AuditLogDaoImpl;
import cn.com.xxx.logserver.filter.cdplog.CDPLogDaoImpl;
import cn.com.xxx.logserver.filter.jobhistory.JobHistoryDaoImpl;
import cn.com.xxx.logserver.filter.rsynclog.RsyncLogDaoImpl;

/**
 * 日志流水号生成器
 */
public class SNGenerator
{
    private static SNGenerator INSTANCE = new SNGenerator();

    private int jobHistoryNextSN;

    private int jobHistoryInfoNextSN;

    private int auditNextSN;

    private int cdpNextSN;

    private int rsyncNextSN;

    public static SNGenerator getInstance()
    {
        return INSTANCE;
    }

    private SNGenerator()
    {
        init();
    }

    private void init()
    {

        this.auditNextSN = AuditLogDaoImpl.getInstance().getMaxSN() + 1;

        this.cdpNextSN = CDPLogDaoImpl.getInstance().getMaxSN() + 1;

        this.rsyncNextSN = RsyncLogDaoImpl.getInstance().getMaxSN() + 1;

        this.jobHistoryInfoNextSN = JobHistoryDaoImpl.getInstance().getInfoMaxSN() + 1;

        this.jobHistoryNextSN = JobHistoryDaoImpl.getInstance().getMaxSN() + 1;
    }

    public synchronized int getJobHistorySN()
    {
        if (this.jobHistoryNextSN >= Integer.MAX_VALUE)
        {
            this.jobHistoryNextSN = 0;
        }
        return this.jobHistoryNextSN++;
    }

    public synchronized int getJobHistoryInfoSN()
    {
        if (this.jobHistoryInfoNextSN >= Integer.MAX_VALUE)
        {
            this.jobHistoryInfoNextSN = 0;
        }
        return this.jobHistoryInfoNextSN++;
    }

    public synchronized int getAuditSN()
    {
        if (this.auditNextSN >= Integer.MAX_VALUE)
        {
            this.auditNextSN = 0;
        }
        return this.auditNextSN++;
    }

    public synchronized int getCDPSN()
    {
        if (this.cdpNextSN >= Integer.MAX_VALUE)
        {
            this.cdpNextSN = 0;
        }
        return this.cdpNextSN++;
    }

    public synchronized int getRsyncSN()
    {
        if (this.rsyncNextSN >= Integer.MAX_VALUE)
        {
            this.rsyncNextSN = 0;
        }
        return this.rsyncNextSN++;
    }

}
