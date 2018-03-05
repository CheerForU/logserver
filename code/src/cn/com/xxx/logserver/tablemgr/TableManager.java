package cn.com.xxx.logserver.tablemgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.xxx.common.msg.MessageConstructor;
import cn.com.xxx.common.msg.ResponseNode;
import cn.com.xxx.common.msg.XMLNode;
import cn.com.xxx.common.msg.XmlMessageListener;
import cn.com.xxx.jobschedule.JobManager;
import cn.com.xxx.logserver.LogServer;
import cn.com.xxx.logserver.common.ErrorNumber;
import cn.com.xxx.logserver.common.MessageName;
import cn.com.xxx.logserver.filter.auditlog.AuditlogFilter;
import cn.com.xxx.logserver.filter.cdplog.CDPLogFilter;
import cn.com.xxx.logserver.filter.jobhistory.JobLogFilter;
import cn.com.xxx.logserver.filter.rsynclog.RsyncLogFilter;
import cn.com.xxx.mdp.model.Message;

/**
 * 日志表管理
 */
public final class TableManager
{

    private static final Logger LOGGER = LoggerFactory.getLogger(TableManager.class);

    private static final TableManager INSTANCE = new TableManager();

    private String jobId = null;

    private TableManager()
    {

    }

    public static TableManager getInstance()
    {
        return INSTANCE;
    }

    public synchronized void init()
    {
        // 订阅消息
        LogServer.getMdpInst().subscribe(MessageName.MSG_QUERYSIZE, new QuerySize());

        // 处理日志表
        final List<String> tableNames = new ArrayList<String>();
        tableNames.add(AuditlogFilter.TABLENAME);
        tableNames.add(CDPLogFilter.TABLENAME);
        tableNames.add(JobLogFilter.TABLENAME);
        tableNames.add(JobLogFilter.INFOTABLENAME);
        tableNames.add(RsyncLogFilter.TABLENAME);
        TableHandler.handle(tableNames);
        TableHandler.checkJobHistoryTab();
        // 启动定时作业
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put(TableCheckJob.TABLENAME, tableNames);
        if (null == jobId)
        {
            jobId = JobManager.addCycleJobByMonth(1,
                    "00:00",
                    null,
                    null,
                    1,
                    TableCheckJob.class,
                    params);
        }
    }

    public synchronized void destroy()
    {
        // 停止定时作业
        if (null != jobId)
        {
            JobManager.deleteJob(jobId);
            jobId = null;
        }

        // 取消订阅消息
        if (null != LogServer.getMdpInst())
        {
            LogServer.getMdpInst().unsubscribe(MessageName.MSG_QUERYSIZE);
        }
    }

    private static class QuerySize extends XmlMessageListener
    {

        @Override
        protected void handle(final Message message, final Document document)
        {
            try
            {
                final float auditSize = TableHandlerDaoImpl.getInstance()
                        .queryTableSize(AuditlogFilter.TABLENAME);

                final float cdpSize = TableHandlerDaoImpl.getInstance()
                        .queryTableSize(CDPLogFilter.TABLENAME);

                final float jobHisSize = TableHandlerDaoImpl.getInstance()
                        .queryTableSize(JobLogFilter.TABLENAME);

                final float rsyncSize = TableHandlerDaoImpl.getInstance()
                        .queryTableSize(RsyncLogFilter.TABLENAME);

                final ResponseNode response = new ResponseNode("querysize");
                response.addChild(new XMLNode("auditlog", auditSize));
                response.addChild(new XMLNode("cdplog", cdpSize));
                response.addChild(new XMLNode("jobhistory", jobHisSize));
                response.addChild(new XMLNode("rsynclog", rsyncSize));
                LogServer.getMdpInst().reply(message,
                        MessageConstructor.constructMsg(response));
            }
            catch (final Exception e)
            {
                LOGGER.error(e.getLocalizedMessage(), e);
                LogServer.getMdpInst().reply(message,
                        MessageConstructor.constructMsg(new ResponseNode("querysize",
                                ErrorNumber.ERRNO_QUERYSIZE_FAILURE,
                                "Exception occured when process the query.")));
            }
        }

    }

}
