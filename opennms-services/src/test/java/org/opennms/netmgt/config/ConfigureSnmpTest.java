/*
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 2007 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a derivative work, containing both original code, included code and modified
 * code that was published under the GNU General Public License. Copyrights for modified
 * and included code are below.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Modifications:
 * 
 * Created: June 19, 2007
 *
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * For more information contact:
 *      OpenNMS Licensing       <license@opennms.org>
 *      http://www.opennms.org/
 *      http://www.opennms.com/
 */
package org.opennms.netmgt.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.test.ConfigurationTestUtils;
import org.springframework.core.io.Resource;

/**
 * 
 * @author <a href="mailto:david@opennms.org">David Hustace</a>
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 */
public class ConfigureSnmpTest extends TestCase {
    final private int m_startingDefCount = 5;

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
    	super.setUp();
    	
        Resource rsrc = ConfigurationTestUtils.getSpringResourceForResource(this, "snmp-config-configureSnmpTest.xml");
    	SnmpPeerFactory.setInstance(new SnmpPeerFactory(rsrc));
    }

    /**
     * Tests creating a string representation of an IP address that is converted to an InetAddress and then
     * a long and back to an IP address.
     * 
     * @throws UnknownHostException 
     */
    public void testToIpAddrString() throws UnknownHostException {
        String addr = "192.168.1.1";
        assertEquals(addr, InetAddressUtils.toIpAddrString(InetAddressUtils.addr(addr).getAddress()));
    }

    /**
     * Test method for {@link org.opennms.netmgt.config.SnmpPeerFactory#createSnmpEventInfo(org.opennms.netmgt.xml.event.Event)}.
     * Tests creating an SNMP config definition from a configureSNMP event.
     * 
     * @throws UnknownHostException 
     */
    public void testCreateSnmpEventInfo() throws UnknownHostException {
        EventBuilder bldr = createConfigureSnmpEventBuilder("192.168.1.1", null);
        addCommunityStringToEvent(bldr, "seemore");
        
        SnmpEventInfo info = new SnmpEventInfo(bldr.getEvent());
        
        assertNotNull(info);
        assertEquals("192.168.1.1", info.getFirstIPAddress());
        assertNull(info.getLastIPAddress());
        assertTrue(info.isSpecific());
    }
    
    /**
     * Tests getting the correct SNMP Peer after a configureSNMP event and merge to the running config.
     * @throws UnknownHostException
     */
    public void testSnmpEventInfoClassWithSpecific() throws UnknownHostException {
        final String addr = "192.168.0.5";
        EventBuilder bldr = createConfigureSnmpEventBuilder(addr, null);
        addCommunityStringToEvent(bldr, "abc");
        SnmpEventInfo info = new SnmpEventInfo(bldr.getEvent());
        
        SnmpConfigManager.mergeIntoConfig(SnmpPeerFactory.getSnmpConfig(), info.createDef());

        SnmpAgentConfig agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(addr));
        assertEquals(InetAddressUtils.str(agent.getAddress()), addr);
        assertEquals("abc", agent.getReadCommunity());
    }
    
    /**
     * This test should remove the specific 192.168.0.5 from the first definition and
     * replace it with a range 192.168.0.5 - 192.168.0.7.
     * 
     * @throws UnknownHostException
     */
    public void testSnmpEventInfoClassWithRangeReplacingSpecific() throws UnknownHostException {
        final String addr1 = "192.168.0.5";
        final String addr2 = "192.168.0.7";
        
        SnmpAgentConfig agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(addr1));
        assertEquals(SnmpAgentConfig.VERSION2C, agent.getVersion());
        
        EventBuilder bldr = createConfigureSnmpEventBuilder(addr1, addr2);
        SnmpEventInfo info = new SnmpEventInfo(bldr.getEvent());
        info.setVersion("v2c");
        
        SnmpConfigManager.mergeIntoConfig(SnmpPeerFactory.getSnmpConfig(), info.createDef());
        
        agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(addr1));
        assertEquals(InetAddressUtils.str(agent.getAddress()), addr1);
        assertEquals(SnmpAgentConfig.VERSION2C, agent.getVersion());
        assertEquals(m_startingDefCount, SnmpPeerFactory.getSnmpConfig().getDefinitionCount());
    }

    /**
     * Tests getting the correct SNMP Peer after merging a new range that super sets a current range.
     * 
     * @throws UnknownHostException
     */
    public void testSnmpEventInfoClassWithRangeSuperSettingDefRanges() throws UnknownHostException {
        final String addr1 = "192.168.99.1";
        final String addr2 = "192.168.108.254";
        
        SnmpAgentConfig agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(addr1));
        assertEquals(SnmpAgentConfig.VERSION1, agent.getVersion());
        
        EventBuilder bldr = createConfigureSnmpEventBuilder(addr1, addr2);
        SnmpEventInfo info = new SnmpEventInfo(bldr.getEvent());
        info.setCommunityString("opennmsrules");
        
        SnmpConfigManager.mergeIntoConfig(SnmpPeerFactory.getSnmpConfig(), info.createDef());
        
        agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(addr1));
        assertEquals(InetAddressUtils.str(agent.getAddress()), addr1);
        assertEquals(SnmpAgentConfig.VERSION1, agent.getVersion());
        assertEquals(m_startingDefCount, SnmpPeerFactory.getSnmpConfig().getDefinitionCount());
    }

    /**
     * Tests getting the correct SNMP Peer after receiving a configureSNMP event that moves a
     * specific from one definition into another.
     * 
     * @throws UnknownHostException
     */
    public void testSplicingSpecificsIntoRanges() throws UnknownHostException {
        assertEquals(3, SnmpPeerFactory.getSnmpConfig().getDefinition(2).getRangeCount());
        assertEquals(6, SnmpPeerFactory.getSnmpConfig().getDefinition(2).getSpecificCount());
        
        final String specificAddr = "10.1.1.7";
        final EventBuilder bldr = createConfigureSnmpEventBuilder(specificAddr, null);
        final SnmpEventInfo info = new SnmpEventInfo(bldr.getEvent());
        info.setCommunityString("splice-test");
        info.setVersion("v2c");
        
        SnmpConfigManager.mergeIntoConfig(SnmpPeerFactory.getSnmpConfig(), info.createDef());
        
        assertEquals(5, SnmpPeerFactory.getSnmpConfig().getDefinition(2).getRangeCount());
        
        assertEquals("10.1.1.10", SnmpPeerFactory.getSnmpConfig().getDefinition(2).getSpecific(0));
        assertEquals(1, SnmpPeerFactory.getSnmpConfig().getDefinition(2).getSpecificCount());
        assertEquals(m_startingDefCount, SnmpPeerFactory.getSnmpConfig().getDefinitionCount());
    }
    
    /**
     * This test should show that a specific is added to the definition and the current
     * single definition should become the beginning address in the adjacent range.
     * 
     * @throws UnknownHostException
     */
    public void testSplice2() throws UnknownHostException {
        assertEquals(3, SnmpPeerFactory.getSnmpConfig().getDefinition(3).getRangeCount());
        assertEquals(1, SnmpPeerFactory.getSnmpConfig().getDefinition(3).getSpecificCount());
        assertEquals("10.1.1.10", SnmpPeerFactory.getSnmpConfig().getDefinition(3).getSpecific(0));
        assertEquals("10.1.1.11", SnmpPeerFactory.getSnmpConfig().getDefinition(3).getRange(0).getBegin());
        
        final String specificAddr = "10.1.1.7";
        final EventBuilder bldr = createConfigureSnmpEventBuilder(specificAddr, null);
        final SnmpEventInfo info = new SnmpEventInfo(bldr.getEvent());
        info.setCommunityString("splice2-test");

        SnmpConfigManager.mergeIntoConfig(SnmpPeerFactory.getSnmpConfig(), info.createDef());
        
        assertEquals(3, SnmpPeerFactory.getSnmpConfig().getDefinition(3).getRangeCount());
        assertEquals(1, SnmpPeerFactory.getSnmpConfig().getDefinition(3).getSpecificCount());
        assertEquals("10.1.1.7", SnmpPeerFactory.getSnmpConfig().getDefinition(3).getSpecific(0));
        assertEquals("10.1.1.10", SnmpPeerFactory.getSnmpConfig().getDefinition(3).getRange(0).getBegin());

        String marshalledConfig = SnmpPeerFactory.marshallConfig();
        assertNotNull(marshalledConfig);
        
    }

    private EventBuilder createConfigureSnmpEventBuilder(final String firstIp, final String lastIp) {
        
        EventBuilder bldr = new EventBuilder(EventConstants.CONFIGURE_SNMP_EVENT_UEI, "ConfigureSnmpTest");

        bldr.addParam(EventConstants.PARM_FIRST_IP_ADDRESS, firstIp);
        bldr.addParam(EventConstants.PARM_LAST_IP_ADDRESS, lastIp);
        
        return bldr;
    }

    private void addCommunityStringToEvent(final EventBuilder bldr, final String commStr) {
        bldr.addParam(EventConstants.PARM_COMMUNITY_STRING, commStr);
    }

}
