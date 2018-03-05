package cn.com.xxx.logserver.filter.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "main", "sub" })
@XmlRootElement(name = "type")
public class LogType
{

    private int main;

    private int sub;

    @XmlAttribute(name = "main")
    public int getMain()
    {
        return main;
    }

    public void setMain(final int main)
    {
        this.main = main;
    }

    @XmlAttribute(name = "sub")
    public int getSub()
    {
        return sub;
    }

    public void setSub(final int sub)
    {
        this.sub = sub;
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("LogType [main=")
                .append(main)
                .append(", sub=")
                .append(sub)
                .append("]");
        return builder.toString();
    }

}
