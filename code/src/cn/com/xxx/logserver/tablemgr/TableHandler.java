package cn.com.xxx.logserver.tablemgr;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.xxx.common.common.CommonUtil;
import cn.com.xxx.logserver.common.ConfigParamsHolder;
import cn.com.xxx.logserver.common.Constants;
import cn.com.xxx.logserver.filter.jobhistory.JobHistoryDaoImpl;
import cn.com.xxx.logserver.filter.jobhistory.model.JobHistory;

public final class TableHandler
{

    private static final Logger LOGGER = LoggerFactory.getLogger(TableHandler.class);

    private static final String JOBHISTORY_TABLE = "tbl_jobhistory";

    private static final String JOBHISTORY_INFO_TABLE = "tbl_jobhistory_info";

    /**
     * 处理日志表
     * @param tableNames 表名称列表
     */
    public static void handle(final List<String> tableNames)
    {
        if (CommonUtil.isEmpty(tableNames))
        {
            LOGGER.error("tableNames is null.");
            return;
        }

        LOGGER.info("Begin to handle log table. tableNames={}", tableNames);

        for (final String tableName : tableNames)
        {
            buildMonthTable(tableName);
            delExpireTable(tableName);
        }
        moveJobToNewTable();
        LOGGER.info("End to handle log table.");
    }

    private static int comExpireDate(final String tableName)
    {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH,
                -ConfigParamsHolder.getInstance().getTableMaxKeepMonth(tableName));

        final StringBuilder strBuilder = new StringBuilder("");
        strBuilder.append(cal.get(Calendar.YEAR));
        if (cal.get(Calendar.MONTH) + 1 < 10)
        {
            strBuilder.append("0");
        }
        strBuilder.append(cal.get(Calendar.MONTH) + 1);

        return Integer.parseInt(strBuilder.toString());
    }

    private static void delExpireTable(final String tableName)
    {
        try
        {
            TableHandlerDaoImpl.getInstance().delExpireTable(tableName,
                    comExpireDate(tableName));
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to delete expired table, table name={}", tableName);
            LOGGER.error(e.getLocalizedMessage(), e);
        }
    }

    private static void buildMonthTable(final String tableName)
    {
        try
        {
            if (TableHandlerDaoImpl.getInstance().queryTable(tableName) == null)// 当前表不存在创建新表
            {
                TableHandlerDaoImpl.getInstance().buildNewTable(tableName, tableName);
            }
            else
            {
                final long logDate = TableHandlerDaoImpl.getInstance()
                        .queryLogDate(tableName);
                if (logDate == 0)
                {
                    return;
                }
                else
                {
                    final Calendar logCal = Calendar.getInstance();// 数据库数据最早日期
                    final Calendar ctCal = Calendar.getInstance();// 当前表日期
                    logCal.setTimeInMillis(logDate);
                    ctCal.set(Calendar.DAY_OF_MONTH, 1); // 设置日期为当月1日00:00:00:000
                    ctCal.set(Calendar.HOUR, 0);
                    ctCal.set(Calendar.MINUTE, 0);
                    ctCal.set(Calendar.SECOND, 0);
                    ctCal.set(Calendar.MILLISECOND, 0);
                    final Calendar pmCal = Calendar.getInstance(); // 上月日期
                    pmCal.setTimeInMillis(ctCal.getTimeInMillis());
                    pmCal.add(Calendar.MONTH, -1);
                    String curTable = tableName;

                    while ((logCal.get(Calendar.YEAR) < ctCal.get(Calendar.YEAR))
                            || (logCal.get(Calendar.YEAR) == ctCal.get(Calendar.YEAR) && logCal.get(Calendar.MONTH) < ctCal.get(Calendar.MONTH)))
                    {
                        // 数据库数据最早日期早于当前表日期
                        // 更改表名
                        final String pmTable = constructTableName(tableName, pmCal);
                        if (null == TableHandlerDaoImpl.getInstance().queryTable(pmTable))
                        {
                            TableHandlerDaoImpl.getInstance().renameCurTable(curTable,
                                    pmTable);
                            // 创建当前月表
                            TableHandlerDaoImpl.getInstance().buildNewTable(tableName,
                                    curTable);
                        }
                        ctCal.add(Calendar.MONTH, -1);
                        pmCal.add(Calendar.MONTH, -1);
                        curTable = pmTable;
                    }
                }
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to build new table, table name={}", tableName);
            LOGGER.error(e.getLocalizedMessage(), e);
        }

    }

    private static String constructTableName(final String prefix, final Calendar cal)
    {
        final StringBuilder strBuilder = new StringBuilder(prefix);
        strBuilder.append(Constants.UNDERLINE);
        strBuilder.append(cal.get(Calendar.YEAR));
        if (cal.get(Calendar.MONTH) + 1 < 10)
        {
            strBuilder.append("0");
        }
        strBuilder.append(cal.get(Calendar.MONTH) + 1);

        return strBuilder.toString();
    }

    /**
     * 检测是否存在历史月的作业记录表，若没有则根据配置文件设置的保留时间创建表
     */
    public static void checkJobHistoryTab()
    {
        String tablename;
        String infotablename;
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        Map<String, String> tablenames = getJobHistoryTabNameByCal(cal);
        tablename = tablenames.get("tablename");
        infotablename = tablenames.get("infotablename");
        final int keepTime = ConfigParamsHolder.getInstance()
                .getTableMaxKeepMonth(JOBHISTORY_TABLE);
        if ((TableHandlerDaoImpl.getInstance().queryTable(tablename) == null)
                || (TableHandlerDaoImpl.getInstance().queryTable(infotablename) == null))
        {
            for (int i = 0; i < keepTime - 1; i++)
            {
                if (TableHandlerDaoImpl.getInstance().queryTable(tablename) == null)
                {
                    TableHandlerDaoImpl.getInstance().buildNewTable(JOBHISTORY_TABLE,
                            tablename);
                }
                if (TableHandlerDaoImpl.getInstance().queryTable(infotablename) == null)
                {
                    TableHandlerDaoImpl.getInstance()
                            .buildNewTable(JOBHISTORY_INFO_TABLE, infotablename);
                }
                cal.add(Calendar.MONTH, -1);
                tablenames = getJobHistoryTabNameByCal(cal);
                tablename = tablenames.get("tablename");
                infotablename = tablenames.get("infotablename");
            }
        }
    }

    /**
     * 根据Calendar对象获取对应月份的job_history和job_history_info的表名称
     * @param cal
     * @return
     */
    private static Map<String, String> getJobHistoryTabNameByCal(final Calendar cal)
    {
        final Map<String, String> tableNames = new HashMap<String, String>();
        final StringBuffer tablename = new StringBuffer();
        final StringBuffer infotablename = new StringBuffer();

        final Calendar cur_cal = Calendar.getInstance();
        if ((cur_cal.get(Calendar.YEAR) == cal.get(Calendar.YEAR))
                && (cur_cal.get(Calendar.MONTH) == cal.get(Calendar.MONTH)))
        {
            tablename.append(JOBHISTORY_TABLE);
            infotablename.append(JOBHISTORY_INFO_TABLE);
        }
        else
        {
            final int month = cal.get(Calendar.MONTH) + 1;
            tablename.append(JOBHISTORY_TABLE)
                    .append(Constants.UNDERLINE)
                    .append(cal.get(Calendar.YEAR));
            infotablename.append(JOBHISTORY_INFO_TABLE)
                    .append(Constants.UNDERLINE)
                    .append(cal.get(Calendar.YEAR));
            if (month < 10)
            {
                tablename.append("0");
                infotablename.append("0");
            }
            tablename.append(month);
            infotablename.append(month);
        }
        tableNames.put("tablename", tablename.toString());
        tableNames.put("infotablename", infotablename.toString());
        return tableNames;
    }

    /**
     * 跨月的时候，将暂时存放在上个月表中的本月或将来月的作业日志记录移动至新表中
     */
    private static void moveJobToNewTable()
    {
        // 获取作业能够保存的时间范围内的倒数第二个月，设置日期为当月1日00:00:00:000
        final int keepTime = ConfigParamsHolder.getInstance()
                .getTableMaxKeepMonth(JOBHISTORY_TABLE.toString());
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -keepTime + 2);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        String lastTablename = "";
        String lastInfotablename = "";
        String tablename = "";
        String infotablename = "";

        do
        {
            // 获取前一个月的表名
            final Calendar lastMonthCal = Calendar.getInstance();
            lastMonthCal.setTimeInMillis(cal.getTimeInMillis());
            lastMonthCal.add(Calendar.MONTH, -1);
            final Map<String, String> lastTablenames = getJobHistoryTabNameByCal(lastMonthCal);
            lastTablename = lastTablenames.get("tablename");
            lastInfotablename = lastTablenames.get("infotablename");

            // 获取后一个月的表名
            final Map<String, String> tablenames = getJobHistoryTabNameByCal(cal);
            tablename = tablenames.get("tablename");
            infotablename = tablenames.get("infotablename");

            // 若前一个月的历史记录表不存在，则不进行移动
            if ((TableHandlerDaoImpl.getInstance().queryTable(lastTablename) == null)
                    || (TableHandlerDaoImpl.getInstance().queryTable(lastInfotablename) == null))
            {
                cal.add(Calendar.MONTH, +1);
                continue;
            }

            final Long tm = cal.getTimeInMillis();
            try
            {
                // 从数据库中查询前一个月表中发生在后一个月或者以后的数据，并把数据从前一个月的表中转移到后一个月的表中
                final List<JobHistory> list = JobHistoryDaoImpl.getInstance()
                        .queryJobHistoryList(lastTablename, tm);
                if (null == list || list.size() == 0)
                {
                    cal.add(Calendar.MONTH, +1);
                    continue;
                }

                for (final JobHistory job : list)
                {
                    final String taskid = job.getJobinfo().getTaskid();
                    JobHistoryDaoImpl.getInstance().moveJobLog(lastTablename,
                            lastInfotablename,
                            taskid,
                            job,
                            tablename,
                            infotablename,
                            false);
                }
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to move job to new table, oldtable name={},newtable={}",
                        lastTablename,
                        tablename);
                LOGGER.error(e.getLocalizedMessage(), e);
            }

            cal.add(Calendar.MONTH, +1);
        }
        while (!JOBHISTORY_TABLE.equals(tablename));
    }
}
