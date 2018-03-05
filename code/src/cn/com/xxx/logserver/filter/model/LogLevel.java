package cn.com.xxx.logserver.filter.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum(Integer.class)
public enum LogLevel
{

    @XmlEnumValue("1")
    INFO(1),

    @XmlEnumValue("2")
    WARN(2),

    @XmlEnumValue("3")
    ERROR(3);

    private int value;

    LogLevel(final int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

}
