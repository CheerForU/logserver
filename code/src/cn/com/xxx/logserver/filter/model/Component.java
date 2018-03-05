package cn.com.xxx.logserver.filter.model;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "id", "name" })
@XmlRootElement(name = "component")
public class Component
{

    private String id;

    private String name;

    public String getId()
    {
        return id;
    }

    public void setId(final String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("Component [id=")
                .append(id)
                .append(", name=")
                .append(name)
                .append("]");
        return builder.toString();
    }

}
