package cn.com.xxx.logserver.filter.oms.snmp;

import java.util.ArrayList;

import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.xxx.common.msg.MessageConstructor;
import cn.com.xxx.common.msg.ResponseNode;
import cn.com.xxx.common.msg.XmlMessageListener;
import cn.com.xxx.logserver.LogServer;
import cn.com.xxx.logserver.common.ErrorNumber;
import cn.com.xxx.logserver.common.MessageName;
import cn.com.xxx.logserver.filter.oms.model.SysConfig;
import cn.com.xxx.logserver.filter.oms.snmp.model.Monitor;
import cn.com.xxx.logserver.filter.oms.snmp.model.Trap;
import cn.com.xxx.mdp.model.Message;

public class SNMPManager
{
    static SNMPDaoImpl daoImpl = SNMPDaoImpl.getInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger(SNMPManager.class);

    static Trap trap;
    static Monitor monitor;

    /**
     * 消息初始化（消息订阅）
     */
    public static void init()
    {
        LogServer.getMdpInst().subscribe(MessageName.MSG_UPDATETRAP, new UpdateTrap());
        LogServer.getMdpInst().subscribe(MessageName.MSG_UPDATEMONITOR,
                new UpdateMonitor());
    }

    /**
     * 消息销毁（消息反订阅）
     */
    public static void destroy()
    {
        LogServer.getMdpInst().unsubscribe(MessageName.MSG_UPDATETRAP);
        LogServer.getMdpInst().unsubscribe(MessageName.MSG_UPDATEMONITOR);
    }

    /**
     * Trap消息操作类
     */
    private static final class UpdateTrap extends XmlMessageListener
    {
        // 接收到消息进行handle操作
        @Override
        protected void handle(final Message message, final Document document)
        {
            try
            {
                final Element root = document.getRootElement();
                final Element protocol = root.element("protocol");

                trap = new Trap(Integer.parseInt(root.element("enable").getText()),
                        root.element("host").getText(), root.element("port").getText(),
                        root.element("charset").getText(),
                        Integer.parseInt(protocol.attributeValue("value")),
                        Integer.parseInt(protocol.element("type").getText()),
                        protocol.element("community").getText(),
                        Integer.parseInt(protocol.element("level").getText()),
                        protocol.element("username").getText(),
                        Integer.parseInt(protocol.element("digest").getText()),
                        protocol.element("password").getText(),
                        Integer.parseInt(protocol.element("encrypt").getText()),
                        protocol.element("priv").getText(), protocol.element("context")
                                .getText());

                final ArrayList<SysConfig> list = new ArrayList<SysConfig>();
                list.add(new SysConfig("snmp_enable", String.valueOf(trap.getEnable())));
                list.add(new SysConfig("snmp_host", trap.getHost()));
                list.add(new SysConfig("snmp_port", trap.getPort()));
                list.add(new SysConfig("snmp_charset", trap.getCharset()));
                list.add(new SysConfig("snmp_protocol",
                        String.valueOf(trap.getProtocol())));
                list.add(new SysConfig("snmp_type", String.valueOf(trap.getType())));
                list.add(new SysConfig("snmp_community", trap.getCommunity()));
                list.add(new SysConfig("snmp_sec_level", String.valueOf(trap.getLevel())));
                list.add(new SysConfig("snmp_username", trap.getUsername()));
                list.add(new SysConfig("snmp_digest", String.valueOf(trap.getDigest())));
                list.add(new SysConfig("snmp_password", trap.getPassword()));
                list.add(new SysConfig("snmp_encrypt", String.valueOf(trap.getEncrypt())));
                list.add(new SysConfig("snmp_priv", trap.getPriv()));
                list.add(new SysConfig("snmp_context", trap.getContext()));

                for (int i = 0; i < list.size(); i++)
                {
                    daoImpl.updateTrapConfig(list.get(i));
                }
                final ResponseNode response = new ResponseNode("updateconfig");
                LogServer.getMdpInst().reply(message,
                        MessageConstructor.constructMsg(response));
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to verify update trap, trap={}", trap);
                LOGGER.error(e.toString());
                final ResponseNode response = new ResponseNode("updateconfig",
                        ErrorNumber.ERRNO_UPDATE_TRAP_FAILURE, e.toString());
                LogServer.getMdpInst().reply(message,
                        MessageConstructor.constructMsg(response));
            }
        }
    }

    /**
     * Monitor消息操作类
     */
    private static final class UpdateMonitor extends XmlMessageListener
    {
        // 接收到消息进行handle操作
        @Override
        protected void handle(final Message message, final Document document)
        {
            try
            {
                final Element e = document.getRootElement();
                final Element root = e.element("updateconfig");
                final Element snmpV1 = root.element("snmpV1");
                final Element snmpV2c = root.element("snmpV2c");
                final Element snmpV3 = root.element("snmpV3");

                monitor = new Monitor(Integer.parseInt(root.element("enable").getText()),
                        root.element("source").getText(), snmpV1.attributeValue("value"),
                        snmpV1.element("community").getText(),
                        snmpV2c.attributeValue("value"), snmpV2c.element("community")
                                .getText(), snmpV3.attributeValue("value"),
                        Integer.parseInt(snmpV3.element("level").getText()),
                        snmpV3.element("username").getText(),
                        Integer.parseInt(snmpV3.element("digest").getText()),
                        snmpV3.element("password").getText(),
                        Integer.parseInt(snmpV3.element("encrypt").getText()),
                        snmpV3.element("priv").getText(), snmpV3.element("context")
                                .getText());

                final ArrayList<SysConfig> list = new ArrayList<SysConfig>();
                list.add(new SysConfig("snmp_monitor_enable",
                        String.valueOf(monitor.getEnable())));
                list.add(new SysConfig("snmp_source", monitor.getSource()));
                list.add(new SysConfig("snmp_v1", monitor.getSnmpV1()));
                list.add(new SysConfig("snmp_v1_community", monitor.getV1community()));
                list.add(new SysConfig("snmp_v2c", monitor.getSnmpV2c()));
                list.add(new SysConfig("snmp_v2c_community", monitor.getV2community()));
                list.add(new SysConfig("snmp_v3", monitor.getSnmpV3()));
                list.add(new SysConfig("snmp_v3_sec_level",
                        String.valueOf(monitor.getLevel())));
                list.add(new SysConfig("snmp_v3_username", monitor.getUsername()));
                list.add(new SysConfig("snmp_v3_digest",
                        String.valueOf(monitor.getDigest())));
                list.add(new SysConfig("snmp_v3_password", monitor.getPassword()));
                list.add(new SysConfig("snmp_v3_encrypt",
                        String.valueOf(monitor.getEncrypt())));
                list.add(new SysConfig("snmp_v3_priv", monitor.getPriv()));
                list.add(new SysConfig("snmp_v3_context", monitor.getContext()));

                for (int i = 0; i < list.size(); i++)
                {
                    daoImpl.updateMonitorConfig(list.get(i));
                }
                final ResponseNode response = new ResponseNode("updateconfig");
                LogServer.getMdpInst().reply(message,
                        MessageConstructor.constructMsg(response));
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to verify update monitor, monitor={}", monitor);
                LOGGER.error(e.toString());
                final ResponseNode response = new ResponseNode("updateconfig",
                        ErrorNumber.ERRNO_UPDATE_MONITOR_FAILURE, e.toString());
                LogServer.getMdpInst().reply(message,
                        MessageConstructor.constructMsg(response));
            }
        }
    }
}
