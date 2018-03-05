package cn.com.xxx.logserver.filter.oms.model;

public class SysConfig
{
    private String s_key;
    private String s_value;

    public SysConfig()
    {

    }

    public SysConfig(final String s_key, final String s_value)
    {
        this.s_key = s_key;
        this.s_value = s_value;
    }

    public String getS_key()
    {
        return s_key;
    }

    public void setS_key(final String s_key)
    {
        this.s_key = s_key;
    }

    public String getS_value()
    {
        return s_value;
    }

    public void setS_value(final String s_value)
    {
        this.s_value = s_value;
    }

    @Override
    public String toString()
    {
        return s_key + "--" + s_value;
    }

}
