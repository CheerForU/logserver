package cn.com.xxx.logserver.tablemgr;

import java.util.List;
import java.util.Map;

import cn.com.xxx.jobschedule.JobTemplate;

/**
 * 日志表检查作业
 */
public final class TableCheckJob extends JobTemplate
{

    public static final String TABLENAME = "tableName";

    public TableCheckJob()
    {

    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(final String id, final Map<String, ?> params)
    {
        final List<String> tableNames = (List<String>) params.get(TABLENAME);
        TableHandler.handle(tableNames);
    }

}
