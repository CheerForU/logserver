package cn.com.xxx.logserver.filter.model;

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import cn.com.xxx.logserver.filter.auditlog.model.AuditLog;
import cn.com.xxx.logserver.filter.cdplog.model.CDPLog;
import cn.com.xxx.logserver.filter.jobhistory.model.JobLog;
import cn.com.xxx.logserver.filter.rsynclog.model.RsyncLog;

@XmlType(propOrder = { "sn", "ver", "tm", "level", "type", "user", "ip", "component",
        "desc" })
@XmlSeeAlso({ AuditLog.class, CDPLog.class, JobLog.class, RsyncLog.class })
public abstract class BaseLog
{

    private int sn;

    private int ver = 1;

    private long tm;

    private LogLevel level = LogLevel.INFO;

    private LogType type;

    private String user;

    private String ip;

    private Component component;

    private String desc;

    public int getSn()
    {
        return sn;
    }

    public void setSn(final int sn)
    {
        this.sn = sn;
    }

    public int getVer()
    {
        return ver;
    }

    public void setVer(final int ver)
    {
        this.ver = ver;
    }

    public long getTm()
    {
        return tm;
    }

    public void setTm(final long tm)
    {
        this.tm = tm;
    }

    public LogLevel getLevel()
    {
        return level;
    }

    public void setLevel(final LogLevel level)
    {
        this.level = level;
    }

    public LogType getType()
    {
        return type;
    }

    public void setType(final LogType type)
    {
        this.type = type;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(final String user)
    {
        this.user = user;
    }

    public String getIp()
    {
        return ip;
    }

    public void setIp(final String ip)
    {
        this.ip = ip;
    }

    public Component getComponent()
    {
        return component;
    }

    public void setComponent(final Component component)
    {
        this.component = component;
    }

    public String getDesc()
    {
        return desc;
    }

    public void setDesc(final String desc)
    {
        this.desc = desc;
    }

}
