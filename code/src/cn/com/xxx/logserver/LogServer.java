package cn.com.xxx.logserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import sun.misc.Signal;
import sun.misc.SignalHandler;
import cn.com.xxx.common.common.CommonUtil;
import cn.com.xxx.common.file.FileOper;
import cn.com.xxx.common.locale.ResourceUtil;
import cn.com.xxx.common.log.LogInitiator;
import cn.com.xxx.common.msg.MessageConstructor;
import cn.com.xxx.common.msg.RequestNode;
import cn.com.xxx.common.msg.XMLNode;
import cn.com.xxx.jobschedule.JobManager;
import cn.com.xxx.logserver.common.MessageName;
import cn.com.xxx.logserver.filter.alarm.AlarmManager;
import cn.com.xxx.logserver.filter.alarm.AlarmNotifier;
import cn.com.xxx.logserver.filter.model.LogLevel;
import cn.com.xxx.logserver.filter.oms.snmp.SNMPManager;
import cn.com.xxx.logserver.filter.oms.syslog.SyslogManager;
import cn.com.xxx.logserver.filter.report.AlarmReportCleanJob;
import cn.com.xxx.logserver.receive.LogDispatcher;
import cn.com.xxx.logserver.receive.LogReceiver;
import cn.com.xxx.logserver.tablemgr.TableManager;
import cn.com.xxx.mdp.MdpFactory;
import cn.com.xxx.mdp.impl.MdpInstance;
import cn.com.xxx.mdp.model.MdpParams;
import cn.com.xxx.mdp.model.Message;

/**
 * LogServer
 * @author wy
 * 
 */
public final class LogServer
{

    private static final Logger LOGGER = LoggerFactory.getLogger(LogServer.class);

    private static final AtomicBoolean RUNNING = new AtomicBoolean(true);

    private static volatile MdpInstance mdpInst = null;

    public static void main(final String[] args)
    {
        File pidFile = null;
        ClassPathXmlApplicationContext mainContext = null;
        try
        {
            if (!LogInitiator.init("../conf/log.properties", "logserver"))
            {
                return;
            }

            final MdpParams mdpParams = new MdpParams();
            mdpParams.setLocalId("logserver");
            mdpInst = MdpFactory.create(mdpParams);
            mainContext = new ClassPathXmlApplicationContext("classpath:spring.xml");

            ResourceUtil.init("locale");
            JobManager.startScheduler();
            TableManager.getInstance().init();
            LogDispatcher.getInstance().start();
            LogReceiver.getInstance().start();
            AlarmNotifier.getInstance().init();
            AlarmManager.getInstance().init();
            AlarmReportCleanJob.init();
            SNMPManager.init();
            SyslogManager.init();

            BufferedWriter writer = null;
            try
            {
                pidFile = FileOper.createFile("../work/logserver.pid");
                if (null == pidFile)
                {
                    return;
                }

                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                        pidFile)));
                writer.write(String.valueOf(CommonUtil.getCurrentProcessId()));
            }
            finally
            {
                if (null != writer)
                {
                    writer.close();
                }
            }

            // 注册SIGTERM信号
            Signal.handle(new Signal("TERM"), new SystemSignalHandler(pidFile,
                    mainContext));

            final Thread thread = new Thread(new ConsoleInput());
            thread.setDaemon(true);
            thread.start();

            while (RUNNING.get())
            {
                Thread.sleep(300);
            }
        }
        catch (final Exception e)
        {
            LOGGER.error(e.getLocalizedMessage(), e);
            System.out.println("ExceptionInfo: " + e.getLocalizedMessage());
        }
        finally
        {
            destroy(pidFile, mainContext);
        }
    }

    public static void stop()
    {
        RUNNING.set(false);
    }

    private static void destroy(final File pidFile,
            final AbstractApplicationContext context)
    {
        try
        {
            if (null != pidFile)
            {
                pidFile.delete();
            }
        }
        catch (final Exception e)
        {
            LOGGER.error(e.getLocalizedMessage(), e);
        }

        LogInitiator.destroy();

        AlarmReportCleanJob.destroy();
        AlarmManager.getInstance().destroy();
        AlarmNotifier.getInstance().destroy();
        LogReceiver.getInstance().stop();
        LogDispatcher.getInstance().stop();
        TableManager.getInstance().destroy();
        JobManager.shutdownScheduler();

        if (null != context)
        {
            context.close();
        }
        mdpInst = null;
        MdpFactory.destroy();
    }

    private static class SystemSignalHandler implements SignalHandler
    {

        private File pidFile = null;

        private AbstractApplicationContext context = null;

        public SystemSignalHandler(final File pidFile,
                final AbstractApplicationContext context)
        {
            this.pidFile = pidFile;
            this.context = context;
        }

        @Override
        public void handle(final Signal signal)
        {
            LOGGER.info("SIG{} is received.", signal.getName());
            try
            {
                destroy(pidFile, context);
            }
            catch (final Throwable e)
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
            System.exit(1);
        }

    }

    public static MdpInstance getMdpInst()
    {
        return mdpInst;
    }

    private static class ConsoleInput implements Runnable
    {

        @Override
        public void run()
        {
            final Scanner scanner = new Scanner(System.in);
            String line = null;

            try
            {
                while (true)
                {
                    System.out.println();
                    System.out.println("wait input (type 'quit' to stop):");

                    line = scanner.nextLine();
                    try
                    {
                        // final String[] params = line.split(" ");
                        if ("quit".equalsIgnoreCase(line))
                        {
                            stop();
                            break;
                        }
                        else if (line.startsWith("alarm"))
                        {
                            final XMLNode type = new XMLNode("type");
                            type.addAttr("main", 1);
                            type.addAttr("sub", 1);

                            final XMLNode component = new XMLNode("component");
                            component.addChild(new XMLNode("id", 12345));
                            component.addChild(new XMLNode("name", "Agent"));

                            final XMLNode policy = new XMLNode("policy");
                            policy.addChild(new XMLNode("id", "001"));
                            policy.addChild(new XMLNode("name", "policy01"));

                            final XMLNode jobinfo = new XMLNode("jobinfo");
                            jobinfo.addChild(new XMLNode("status", "run"));
                            jobinfo.addChild(new XMLNode("taskid", "ABC"));
                            jobinfo.addChild(policy);

                            final XMLNode yyy = new XMLNode("yyy");
                            yyy.addChild(new XMLNode("a", "a"));
                            yyy.addChild(new XMLNode("b", "b"));

                            final XMLNode content = new XMLNode("content");
                            content.addChild(new XMLNode("xxx", "xxx"));
                            content.addChild(yyy);

                            // // send auditlog
                            // for (int i = 1; i <= 200; ++i)
                            // {
                            // final XMLNode auditlog = new XMLNode("auditlog");
                            // auditlog.addChild(new XMLNode("ver", i));
                            // auditlog.addChild(new XMLNode("tm",
                            // System.currentTimeMillis()));
                            // auditlog.addChild(new XMLNode("level",
                            // LogLevel.WARN.getValue()));
                            // auditlog.addChild(type);
                            // auditlog.addChild(new XMLNode("user", "admin"));
                            // auditlog.addChild(new XMLNode("ip", "10.10.10.10"));
                            // auditlog.addChild(component);
                            // auditlog.addChild(new XMLNode("desc", "删除Agent"));
                            // mdpInst.inform("logrecorder",
                            // "logrecorder.write",
                            // MessageConstructor.constructMsg(auditlog),
                            // true);
                            // }

                            // send joblog
                            final XMLNode joblog = new XMLNode("joblog");
                            joblog.addChild(new XMLNode("ver", "1"));
                            joblog.addChild(new XMLNode("tm", System.currentTimeMillis()));
                            joblog.addChild(new XMLNode("level", LogLevel.WARN.getValue()));
                            joblog.addChild(type);
                            joblog.addChild(new XMLNode("user", "admin"));
                            joblog.addChild(new XMLNode("ip", "10.10.10.10"));
                            joblog.addChild(component);
                            joblog.addChild(new XMLNode("desc", "删除Agent"));
                            joblog.addChild(jobinfo);
                            joblog.addChild(content);
                            mdpInst.inform("logrecorder",
                                    "logrecorder.write",
                                    MessageConstructor.constructMsg(joblog),
                                    true);
                        }
                        else if (line.startsWith("createrule"))
                        {
                            final XMLNode filter = new XMLNode("filter");
                            filter.addChild(new XMLNode("level", 2));
                            final XMLNode type = new XMLNode("type");
                            type.addAttr("main", 0);
                            type.addAttr("main", 0);
                            filter.addChild(type);

                            final RequestNode rule = new RequestNode("createalarmrule");
                            rule.addChild(new XMLNode("id", "ruleid1"));
                            rule.addChild(new XMLNode("name", "name"));
                            rule.addChild(filter);

                            final XMLNode suppress = new XMLNode("suppress");
                            suppress.addChild(new XMLNode("interval", 1));
                            rule.addChild(suppress);

                            final XMLNode notify = new XMLNode("notify");
                            notify.addAttr("mode", 0);
                            notify.addChild(new XMLNode("phone", "15150511533"));
                            notify.addChild(new XMLNode("email",
                                    "zjr@xxx.com.cn;347060103@qq.com"));
                            rule.addChild(notify);

                            final Message retMsg = mdpInst.request("logserver",
                                    MessageName.MSG_CREATEALARMRULE,
                                    MessageConstructor.constructMsg(rule));
                            System.out.println(retMsg);
                        }
                        else if (line.startsWith("modifyrule"))
                        {
                            final XMLNode filter = new XMLNode("filter");
                            filter.addChild(new XMLNode("level", 2));
                            final XMLNode type = new XMLNode("type");
                            type.addAttr("main", 0);
                            type.addAttr("main", 0);
                            filter.addChild(type);

                            final RequestNode rule = new RequestNode("modifyalarmrule");
                            rule.addChild(new XMLNode("id", "ruleid1"));
                            rule.addChild(new XMLNode("name", "rule name"));
                            rule.addChild(filter);

                            final XMLNode suppress = new XMLNode("suppress");
                            suppress.addChild(new XMLNode("interval", 1));
                            rule.addChild(suppress);

                            final XMLNode notify = new XMLNode("notify");
                            notify.addAttr("mode", 0);
                            notify.addChild(new XMLNode("phone", "15150511533"));
                            notify.addChild(new XMLNode("email",
                                    "zjr@xxx.com.cn;347060103@qq.com"));
                            rule.addChild(notify);

                            final Message retMsg = mdpInst.request("logserver",
                                    MessageName.MSG_MODIFYALARMRULE,
                                    MessageConstructor.constructMsg(rule));
                            System.out.println(retMsg);
                        }
                        else if (line.startsWith("deleterule"))
                        {
                            final RequestNode delete = new RequestNode("deletealarmrule");
                            final XMLNode ids = new XMLNode("ids");
                            ids.addAttr("total", 1);
                            ids.addChild(new XMLNode("id", "ruleid1"));
                            delete.addChild(ids);
                            final Message retMsg = mdpInst.request("logserver",
                                    MessageName.MSG_DELETEALARMRULE,
                                    MessageConstructor.constructMsg(delete));
                            System.out.println(retMsg);
                        }
                        else if ("query".equalsIgnoreCase(line))
                        {
                            final Message reponse = mdpInst.request("logserver",
                                    "logserver.querysize",
                                    MessageConstructor.constructMsg(new RequestNode(
                                            "querysize")));
                            if (null == reponse)
                            {
                                System.out.println("Request timeout!");
                                return;
                            }

                            System.out.println(reponse.getContent());
                        }
                        else if (line.startsWith("test"))
                        {
                            System.out.println("input params:");

                            final XMLNode type = new XMLNode("type");
                            type.addAttr("main", 2);
                            type.addAttr("sub", 1);

                            final XMLNode component = new XMLNode("component");
                            component.addChild(new XMLNode("id",
                                    "e02bfebd345694b9f8731a11d5303e67"));
                            component.addChild(new XMLNode("name",
                                    "xxxhost.xxxdomain"));

                            final XMLNode policy = new XMLNode("policy");
                            policy.addChild(new XMLNode("id",
                                    "56743ccf-c0f0-42c4-b34c-d3442e1714cf@1506787214747"));
                            policy.addChild(new XMLNode("name", "again_again"));

                            System.out.println("taskid：(Task_***)");
                            final String taskid = scanner.nextLine();
                            String desc = "";
                            String status = "begin";
                            String con = "";
                            for (int i = 0; i < 3; i++)
                            {
                                if (i == 0)
                                {
                                    desc = "create task";
                                    status = "begin";
                                    con = "task_type=s_mysql bk_type=1 shduler_exec_type=right_now";
                                }
                                else if (i == 1)
                                {
                                    desc = "doing..";
                                    status = "run";
                                    con = "";
                                }
                                else if (i == 2)
                                {
                                    desc = "task success";
                                    status = "end";
                                    con = "status=success";
                                }
                                System.out.println("time_" + (i + 1)
                                        + "：(yyyy-MM-dd HH:mm:ss)");
                                final String time = scanner.nextLine();
                                final Calendar logtm = Calendar.getInstance();
                                final SimpleDateFormat format = new SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss");
                                final Date date = format.parse(time);
                                logtm.setTime(date);
                                final Long tm = logtm.getTimeInMillis();

                                final XMLNode jobinfo = new XMLNode("jobinfo");
                                jobinfo.addChild(new XMLNode("status", status));
                                jobinfo.addChild(new XMLNode("taskid", taskid));
                                jobinfo.addChild(policy);

                                final XMLNode content = new XMLNode("content");
                                content.addChild(new XMLNode("con", con));

                                final XMLNode joblog = new XMLNode("joblog");
                                joblog.addChild(new XMLNode("ver", "1"));
                                joblog.addChild(new XMLNode("tm", tm));
                                joblog.addChild(new XMLNode("level",
                                        LogLevel.INFO.getValue()));
                                joblog.addChild(type);
                                joblog.addChild(new XMLNode("user", "SYSTEM"));
                                joblog.addChild(new XMLNode("ip", "10.10.1.244"));
                                joblog.addChild(component);
                                joblog.addChild(new XMLNode("desc", desc));
                                joblog.addChild(jobinfo);
                                joblog.addChild(content);
                                mdpInst.inform("logrecorder",
                                        "logrecorder.write",
                                        MessageConstructor.constructMsg(joblog),
                                        true);
                            }
                        }
                        else
                        {
                            System.out.println("Invalid command.");
                        }
                    }
                    catch (final Exception e)
                    {
                        LOGGER.error(e.getLocalizedMessage(), e);
                        System.out.println("ExceptionInfo: " + e.getLocalizedMessage());
                    }
                }
            }
            catch (final NoSuchElementException e)
            {
                LOGGER.warn(e.getLocalizedMessage());
            }
            catch (final Exception e)
            {
                LOGGER.error(e.getLocalizedMessage(), e);
                System.out.println("ExceptionInfo: " + e.getLocalizedMessage());
            }
            finally
            {
                scanner.close();
            }
        }
    }

}
