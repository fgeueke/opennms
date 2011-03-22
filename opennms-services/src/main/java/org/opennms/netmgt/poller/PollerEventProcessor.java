//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2009 May 14: added threshold config change handler for in-line thresholds processing
// 2004 Jan 06: Added support for SUSPEND_POLLING_SERVICE_EVENT_UEI and
// 		RESUME_POLLING_SERVICE_EVENT_UEI
// 2003 Nov 11: Merged changes from Rackspace project
// 2003 Jan 31: Cleaned up some unused imports.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
// Tab Size = 8
//

package org.opennms.netmgt.poller;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.utils.LogUtils;
import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.capsd.EventUtils;
import org.opennms.netmgt.capsd.InsufficientInformationException;
import org.opennms.netmgt.config.PollerConfig;
import org.opennms.netmgt.dao.DemandPollDao;
import org.opennms.netmgt.eventd.EventIpcManager;
import org.opennms.netmgt.model.events.EventListener;
import org.opennms.netmgt.poller.pollables.PollableInterface;
import org.opennms.netmgt.poller.pollables.PollableNetwork;
import org.opennms.netmgt.poller.pollables.PollableNode;
import org.opennms.netmgt.poller.pollables.PollableService;
import org.opennms.netmgt.utils.XmlrpcUtil;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Parm;
import org.opennms.netmgt.xml.event.Parms;
import org.opennms.netmgt.xml.event.Value;

/**
 * 
 * @author <a href="mailto:jamesz@opennms.com">James Zuo </a>
 * @author <a href="mailto:weave@oculan.com">Brian Weaver </a>
 * @author <a href="http://www.opennms.org/">OpenNMS </a>
 */
final class PollerEventProcessor implements EventListener {

    private final Poller m_poller;
	private volatile DemandPollDao m_demandPollDao;
	
	

    /**
     * Create message selector to set to the subscription
     */
    private void createMessageSelectorAndSubscribe() {
        // Create the selector for the UEIs this service is interested in
        //
        List<String> ueiList = new ArrayList<String>();

        // nodeGainedService
        ueiList.add(EventConstants.NODE_GAINED_SERVICE_EVENT_UEI);

        // serviceDeleted
        // deleteService
        /*
         * NOTE: deleteService is only generated by the PollableService itself.
         * Therefore, we ignore it. If future implementations allow other
         * subsystems to generate this event, we may have to listen for it as
         * well. 'serviceDeleted' is the response event that the outage manager
         * generates. We ignore this as well, since the PollableService has
         * already taken action at the time it generated 'deleteService'
         */
        ueiList.add(EventConstants.SERVICE_DELETED_EVENT_UEI);
        // ueiList.add(EventConstants.DELETE_SERVICE_EVENT_UEI);

        // serviceManaged
        // serviceUnmanaged
        // interfaceManaged
        // interfaceUnmanaged
        /*
         * NOTE: These are all ignored because the responsibility is currently
         * on the class generating the event to restart the poller service. If
         * that implementation is ever changed, this message selector should
         * listen for these and act on them.
         */
        // ueiList.add(EventConstants.SERVICE_MANAGED_EVENT_UEI);
        // ueiList.add(EventConstants.SERVICE_UNMANAGED_EVENT_UEI);
        // ueiList.add(EventConstants.INTERFACE_MANAGED_EVENT_UEI);
        // ueiList.add(EventConstants.INTERFACE_UNMANAGED_EVENT_UEI);
        // interfaceIndexChanged
        // NOTE: No longer interested in this event...if Capsd detects
        // that in interface's index has changed a
        // 'reinitializePrimarySnmpInterface' event is generated.
        // ueiList.add(EventConstants.INTERFACE_INDEX_CHANGED_EVENT_UEI);
        // interfaceReparented
        ueiList.add(EventConstants.INTERFACE_REPARENTED_EVENT_UEI);

        // reloadPollerConfig
        /*
         * NOTE: This is ignored because the reload is handled through an
         * autoaction.
         */
        // ueiList.add(EventConstants.RELOAD_POLLER_CONFIG_EVENT_UEI);
        // NODE OUTAGE RELATED EVENTS
        // 
        // nodeAdded
        /*
         * NOTE: This is ignored. The real trigger will be the first
         * nodeGainedService event, at which time the interface and node will be
         * created
         */
        // ueiList.add(EventConstants.NODE_ADDED_EVENT_UEI);
        // nodeDeleted
        ueiList.add(EventConstants.NODE_DELETED_EVENT_UEI);

        // duplicateNodeDeleted
        ueiList.add(EventConstants.DUP_NODE_DELETED_EVENT_UEI);

        // nodeGainedInterface
        /*
         * NOTE: This is ignored. The real trigger will be the first
         * nodeGainedService event, at which time the interface and node will be
         * created
         */
        // ueiList.add(EventConstants.NODE_GAINED_INTERFACE_EVENT_UEI);
        // interfaceDeleted
        ueiList.add(EventConstants.INTERFACE_DELETED_EVENT_UEI);

        // suspendPollingService
        ueiList.add(EventConstants.SUSPEND_POLLING_SERVICE_EVENT_UEI);

        // resumePollingService
        ueiList.add(EventConstants.RESUME_POLLING_SERVICE_EVENT_UEI);
	
        // scheduled outage configuration change
        ueiList.add(EventConstants.SCHEDOUTAGES_CHANGED_EVENT_UEI);
        
        // demand poll
        ueiList.add(EventConstants.DEMAND_POLL_SERVICE_EVENT_UEI);
        
        // update threshold configuration
        ueiList.add(EventConstants.THRESHOLDCONFIG_CHANGED_EVENT_UEI);

        // asset information updated
        ueiList.add(EventConstants.ASSET_INFO_CHANGED_EVENT_UEI);
        
        // categories updated
        ueiList.add(EventConstants.NODE_CATEGORY_MEMBERSHIP_CHANGED_EVENT_UEI);
        
        
        // Subscribe to eventd
        getEventManager().addEventListener(this, ueiList);
    }

    /**
     * Process the event, construct a new PollableService object representing
     * the node/interface/service/pkg combination, and schedule the service for
     * polling.
     * 
     * If any errors occur scheduling the interface no error is returned.
     * 
     * @param event
     *            The event to process.
     * 
     */
    private void nodeGainedServiceHandler(Event event) {
        // Is this the result of a resumePollingService event?
        @SuppressWarnings("unused")
        String whichEvent = "Unexpected Event: " + event.getUei() + ": ";
        if (event.getUei().equals(EventConstants.NODE_GAINED_SERVICE_EVENT_UEI)) {
            whichEvent = "nodeGainedService: ";
        } else if (event.getUei().equals(EventConstants.RESUME_POLLING_SERVICE_EVENT_UEI)) {
            whichEvent = "resumePollingService: ";
        }
        

        // First make sure the service gained is in active state before trying to schedule

        String ipAddr = InetAddressUtils.str(event.getInterface());
        Long nodeId = event.getNodeid();
        String svcName = event.getService();
        
        String nodeLabel = EventUtils.getParm(event, EventConstants.PARM_NODE_LABEL);
        
        try {
            nodeLabel = getPoller().getQueryManager().getNodeLabel(nodeId.intValue());
        } catch (final Exception e) {
            LogUtils.errorf(this, e, "Unable to retrieve nodeLabel for node %d", nodeId);
        }

        getPoller().scheduleService(nodeId.intValue(), nodeLabel, ipAddr, svcName);
        
    }

    /**
     * This method is responsible for processing 'interfacReparented' events. An
     * 'interfaceReparented' event will have old and new nodeId parms associated
     * with it. Node outage processing hierarchy will be updated to reflect the
     * new associations.
     * 
     * @param event
     *            The event to process.
     * 
     */
    private void interfaceReparentedHandler(Event event) { 
        LogUtils.debugf(this, "interfaceReparentedHandler: processing interfaceReparented event for %s", event.getInterface());

        // Verify that the event has an interface associated with it
        if (event.getInterface() == null)
            return;
        
        InetAddress ipAddr = event.getInterface();

        // Extract the old and new nodeId's from the event parms
        String oldNodeIdStr = null;
        String newNodeIdStr = null;
        Parms parms = event.getParms();
        if (parms != null) {
            String parmName = null;
            Value parmValue = null;
            String parmContent = null;

            for (Parm parm : parms.getParmCollection()) {
                parmName = parm.getParmName();
                parmValue = parm.getValue();
                if (parmValue == null)
                    continue;
                else
                    parmContent = parmValue.getContent();

                // old nodeid
                if (parmName.equals(EventConstants.PARM_OLD_NODEID)) {
                    oldNodeIdStr = parmContent;
                }

                // new nodeid
                else if (parmName.equals(EventConstants.PARM_NEW_NODEID)) {
                    newNodeIdStr = parmContent;
                }
            }
        }

        // Only proceed provided we have both an old and a new nodeId
        //
        if (oldNodeIdStr == null || newNodeIdStr == null) {
            LogUtils.errorf(this, "interfaceReparentedHandler: old and new nodeId parms are required, unable to process.");
            return;
        }
        
        PollableNode oldNode;
        PollableNode newNode;
        try {
            oldNode = getNetwork().getNode(Integer.parseInt(oldNodeIdStr));
            if (oldNode == null) {
                LogUtils.errorf(this, "interfaceReparentedHandler: Cannot locate old node %s belonging to interface %s", oldNodeIdStr, ipAddr);
                return;
            }
            newNode = getNetwork().getNode(Integer.parseInt(newNodeIdStr));
            if (newNode == null) {
                LogUtils.errorf(this, "interfaceReparentedHandler: Cannot locate new node %s to move interface to.  Also, grammar error: ended a sentence with a preposition.", newNodeIdStr);
                return;
            }
            
            PollableInterface iface = oldNode.getInterface(ipAddr);
            if (iface == null) {
                LogUtils.errorf(this, "interfaceReparentedHandler: Cannot locate interface with ipAddr %s to reparent.", ipAddr);
                return;
            }
            
            iface.reparentTo(newNode);
            
            
        } catch (final NumberFormatException nfe) {
            LogUtils.errorf(this, "interfaceReparentedHandler: failed converting old/new nodeid parm to integer, unable to process.");
            return;
        } 
        
    }

    /**
     * This method is responsible for removing a node's pollable service from
     * the pollable services list
     */
    private void nodeRemovePollableServiceHandler(Event event) {
        Long nodeId = event.getNodeid();
        InetAddress ipAddr = event.getInterface();
        String svcName = event.getService();
        
        if (svcName == null) {
            LogUtils.errorf(this, "nodeRemovePollableServiceHandler: service name is null, ignoring event");
            return;
        }
        
        PollableService svc = getNetwork().getService(nodeId.intValue(), ipAddr, svcName);
        svc.delete();

    }

    /**
     * This method is responsible for removing the node specified in the
     * nodeDeleted event from the Poller's pollable node map.
     */
    private void nodeDeletedHandler(Event event) {
        Long nodeId = event.getNodeid();
        final String sourceUei = event.getUei();

        // Extract node label and transaction No. from the event parms
        long txNo = -1L;
        Parms parms = event.getParms();
        if (parms != null) {
            String parmName = null;
            Value parmValue = null;
            String parmContent = null;

            for (Parm parm : parms.getParmCollection()) {
                parmName = parm.getParmName();
                parmValue = parm.getValue();
                if (parmValue == null)
                    continue;
                else
                    parmContent = parmValue.getContent();

                // get the external transaction number
                if (parmName.equals(EventConstants.PARM_TRANSACTION_NO)) {
                    String temp = parmContent;
                    LogUtils.debugf(this, "nodeDeletedHandler:  parmName: %s /parmContent: %s", parmName, parmContent);
                    try {
                        txNo = Long.valueOf(temp).longValue();
                    } catch (final NumberFormatException nfe) {
                        LogUtils.warnf(this, nfe, "nodeDeletedHandler: Parameter %s cannot be non-numeric", EventConstants.PARM_TRANSACTION_NO);
                        txNo = -1;
                    }
                }
            }
        }
        
        Date closeDate;
        try {
            closeDate = EventConstants.parseToDate(event.getTime());
        } catch (ParseException e) {
            closeDate = new Date();
        }
        
        getPoller().closeOutagesForNode(closeDate, event.getDbid(), nodeId.intValue());

        
        PollableNode node = getNetwork().getNode(nodeId.intValue());
        if (node == null) {
          LogUtils.errorf(this, "Nodeid %d does not exist in pollable node map, unable to delete node.", nodeId);
          if (isXmlRPCEnabled()) {
              int status = EventConstants.XMLRPC_NOTIFY_FAILURE;
              XmlrpcUtil.createAndSendXmlrpcNotificationEvent(txNo, sourceUei, "Node does not exist in pollable node map.", status, "OpenNMS.Poller");
          }
          return;
        }
        node.delete();
       
    }

    /**
     * 
     */
    private void interfaceDeletedHandler(Event event) {
        Long nodeId = event.getNodeid();
        String sourceUei = event.getUei();
        InetAddress ipAddr = event.getInterface();
        
        // Extract node label and transaction No. from the event parms
        long txNo = -1L;
        Parms parms = event.getParms();
        if (parms != null) {
            String parmName = null;
            Value parmValue = null;
            String parmContent = null;
            
            for (Parm parm : parms.getParmCollection()) {
                parmName = parm.getParmName();
                parmValue = parm.getValue();
                if (parmValue == null)
                    continue;
                else
                    parmContent = parmValue.getContent();

                // get the external transaction number
                if (parmName.equals(EventConstants.PARM_TRANSACTION_NO)) {
                    String temp = parmContent;
                    LogUtils.debugf(this, "interfaceDeletedHandlerHandler:  parmName: %s /parmContent: %s", parmName, parmContent);
                    try {
                        txNo = Long.valueOf(temp).longValue();
                    } catch (final NumberFormatException nfe) {
                        LogUtils.warnf(this, nfe, "interfaceDeletedHandlerHandler: Parameter %s cannot be non-numberic", EventConstants.PARM_TRANSACTION_NO);
                        txNo = -1;
                    }
                }
            }
        }
        
        Date closeDate;
        try {
            closeDate = EventConstants.parseToDate(event.getTime());
        } catch (ParseException e) {
            closeDate = new Date();
        }
        
        getPoller().closeOutagesForInterface(closeDate, event.getDbid(), nodeId.intValue(), InetAddressUtils.str(ipAddr));

        
        PollableInterface iface = getNetwork().getInterface(nodeId.intValue(), ipAddr);
        if (iface == null) {
          LogUtils.errorf(this, "Interface %d/%s does not exist in pollable node map, unable to delete node.", nodeId, event.getInterface());
          if (isXmlRPCEnabled()) {
              int status = EventConstants.XMLRPC_NOTIFY_FAILURE;
              XmlrpcUtil.createAndSendXmlrpcNotificationEvent(txNo, sourceUei, "Interface does not exist in pollable node map.", status, "OpenNMS.Poller");
          }
          return;
        }
        iface.delete();

    }

    /**
     * <p>
     * This method remove a deleted service from the pollable service list of
     * the specified interface, so that it will not be scheduled by the poller.
     * </p>
     */
    private void serviceDeletedHandler(Event event) {
        Long nodeId = event.getNodeid();
        InetAddress ipAddr = event.getInterface();
        String service = event.getService();
        
        Date closeDate;
        try {
            closeDate = EventConstants.parseToDate(event.getTime());
        } catch (ParseException e) {
            closeDate = new Date();
        }
        
        getPoller().closeOutagesForService(closeDate, event.getDbid(), nodeId.intValue(), InetAddressUtils.str(ipAddr), service);
        
        PollableService svc = getNetwork().getService(nodeId.intValue(), ipAddr, service);
        if (svc == null) {
          LogUtils.errorf(this, "Interface %d/%s does not exist in pollable node map, unable to delete node.", nodeId, event.getInterface());
          return;
        }
        
        svc.delete();

    }
    
    /**
     * Constructor
     * 
     * @param pollableServices
     *            List of all the PollableService objects scheduled for polling
     */
    PollerEventProcessor(Poller poller) {

        m_poller = poller;

        createMessageSelectorAndSubscribe();

        LogUtils.debugf(this, "Subscribed to eventd");
    }

    /**
     * Unsubscribe from eventd
     */
    public void close() {
        getEventManager().removeEventListener(this);
    }

    /**
     * @return
     */
    private EventIpcManager getEventManager() {
        return getPoller().getEventManager();
    }

    /**
     * This method is invoked by the EventIpcManager when a new event is
     * available for processing. Each message is examined for its Universal
     * Event Identifier and the appropriate action is taking based on each UEI.
     * 
     * @param event
     *            The event
     */
    public void onEvent(Event event) {
        if (event == null)
            return;

        // print out the uei
        LogUtils.debugf(this, "PollerEventProcessor: received event, uei = %s", event.getUei());

	if(event.getUei().equals(EventConstants.SCHEDOUTAGES_CHANGED_EVENT_UEI)) {
		LogUtils.infof(this, "Reloading poller config factory and polloutages config factory");
        
		scheduledOutagesChangeHandler();
	} else if(event.getUei().equals(EventConstants.THRESHOLDCONFIG_CHANGED_EVENT_UEI)) {
        LogUtils.infof(this, "Reloading thresholding configuration in pollerd");
        
	    thresholdsConfigChangeHandler();

	} else if(!event.hasNodeid()) {
	    // For all other events, if the event doesn't have a nodeId it can't be processed.

            LogUtils.infof(this, "PollerEventProcessor: no database node id found, discarding event");
        } else if (event.getUei().equals(EventConstants.NODE_GAINED_SERVICE_EVENT_UEI)) {
            // If there is no interface then it cannot be processed
            //
            if (event.getInterface() == null) {
                LogUtils.infof(this, "PollerEventProcessor: no interface found, discarding event");
            } else {
                nodeGainedServiceHandler(event);
            }
        } else if (event.getUei().equals(EventConstants.RESUME_POLLING_SERVICE_EVENT_UEI)) {
            // If there is no interface then it cannot be processed
            //
            if (event.getInterface() == null) {
                LogUtils.infof(this, "PollerEventProcessor: no interface found, cannot resume polling service, discarding event");
            } else {
                nodeGainedServiceHandler(event);
            }
        } else if (event.getUei().equals(EventConstants.SUSPEND_POLLING_SERVICE_EVENT_UEI)) {
            // If there is no interface then it cannot be processed
            //
            if (event.getInterface() == null) {
                LogUtils.infof(this, "PollerEventProcessor: no interface found, cannot suspend polling service, discarding event");
            } else {
                nodeRemovePollableServiceHandler(event);
            }
        } else if (event.getUei().equals(EventConstants.INTERFACE_REPARENTED_EVENT_UEI)) {
            // If there is no interface then it cannot be processed
            //
            if (event.getInterface() == null) {
                LogUtils.infof(this, "PollerEventProcessor: no interface found, discarding event");
            } else {
                interfaceReparentedHandler(event);
            }
        } else if (event.getUei().equals(EventConstants.NODE_DELETED_EVENT_UEI) || event.getUei().equals(EventConstants.DUP_NODE_DELETED_EVENT_UEI)) {
            if (event.getNodeid() < 0) {
                LogUtils.infof(this, "PollerEventProcessor: no node or interface found, discarding event");
            }
            // NEW NODE OUTAGE EVENTS
            nodeDeletedHandler(event);
        } else if (event.getUei().equals(EventConstants.INTERFACE_DELETED_EVENT_UEI)) {
            // If there is no interface then it cannot be processed
            //
            if (event.getNodeid() < 0 || event.getInterface() == null) {
                LogUtils.infof(this, "PollerEventProcessor: invalid nodeid or no interface found, discarding event");
            } else {
                interfaceDeletedHandler(event);
            }
        } else if (event.getUei().equals(EventConstants.SERVICE_DELETED_EVENT_UEI)) {
            // If there is no interface then it cannot be processed
            //
            if ((event.getNodeid() < 0) || (event.getInterface() == null) || (event.getService() == null)) {
                LogUtils.infof(this, "PollerEventProcessor: invalid nodeid or no nodeinterface " + "or service found, discarding event");
            } else {
                serviceDeletedHandler(event);
            }
        } else if (event.getUei().equals(EventConstants.NODE_CATEGORY_MEMBERSHIP_CHANGED_EVENT_UEI)){
            if (!(event.getNodeid() < 0)) { 
                
                serviceReschedule(event);
            }
        } else if (event.getUei().equals(EventConstants.ASSET_INFO_CHANGED_EVENT_UEI)){
            if (!(event.getNodeid() < 0)) { 
                serviceReschedule(event);
            }
            
        } // end single event process

    } // end onEvent()
    
    private void serviceReschedule(Event event)   {       
       PollableNode pnode = getNetwork().getNode(event.getNodeid().intValue());
       Long nodeId = event.getNodeid();
       String nodeLabel = pnode.getNodeLabel();
       
       //pnode.delete();
       //nodeDeletedHandler(event);
       
       /*while(pnode.isDeleted()==false){
           
           LogUtils.debugf(this,"Waiting for node to delete...");
           
       }*/
       
       List<String[]> list = getPoller().getQueryManager().getNodeServices(nodeId.intValue());
       
       for(String[] row : list){
           LogUtils.debugf(this," Removing the following from the list: %s:%s", row[0],row[1]);
           
           InetAddress addr;
           addr = InetAddressUtils.addr(row[0]);
           if (addr == null) {
               LogUtils.warnf(this,"Rescheduler: Could not convert "+row[0]+" to an InetAddress");
               return;
           }
           
           Date closeDate;
           try {
               closeDate = EventConstants.parseToDate(event.getTime());
           } catch (ParseException e) {
               closeDate = new Date();
           }
           
           getPoller().closeOutagesForService(closeDate, event.getDbid(), nodeId.intValue(), row[0], row[1]);
           
           PollableService svc = getNetwork().getService(nodeId.intValue(),addr,row[1]);
           
           if (svc != null) {
           
               svc.delete();
           
               while(svc.isDeleted()==false){
                   LogUtils.debugf(this,"Waiting for the service to delete...");
               }
           
           }
           
           else {
               LogUtils.debugf(this, "Service Not Found");
           }
           
       }
       
       getPoller().getPollerConfig().rebuildPackageIpListMap();
       
       for(String[] row : list){
           LogUtils.debugf(this," Re-adding the following to the list: %s:%s", row[0],row[1]);
           getPoller().scheduleService(nodeId.intValue(),nodeLabel,row[0],row[1]);
       }
    }

    @SuppressWarnings("unused")
    private void demandPollServiceHandler(Event e) throws InsufficientInformationException {
    	EventUtils.checkNodeId(e);
    	EventUtils.checkInterface(e);
    	EventUtils.checkService(e);
    	EventUtils.requireParm(e, EventConstants.PARM_DEMAND_POLL_ID);
    	m_demandPollDao.get(EventUtils.getIntParm(e, EventConstants.PARM_DEMAND_POLL_ID, -1));
    }

    private void scheduledOutagesChangeHandler() {
        try {
            getPollerConfig().update();
            getPoller().getPollOutagesConfig().update();
		} catch (Throwable e) {
			LogUtils.errorf(this, e, "Failed to reload PollerConfigFactory");
		}
        getPoller().refreshServicePackages();
    }
    
    private void thresholdsConfigChangeHandler() {
        getPoller().refreshServiceThresholds();
    }

    /**
     * Return an id for this event listener
     */
    public String getName() {
        return "Poller:PollerEventProcessor";
    }

    /**
     * @return
     */
    private Poller getPoller() {
        return m_poller;
    }

    /**
     * @return
     */
    private PollerConfig getPollerConfig() {
        return getPoller().getPollerConfig();
    }

    private PollableNetwork getNetwork() {
        return getPoller().getNetwork();
    }

    /**
     * @return Returns the XMLRPC.
     */
    private boolean isXmlRPCEnabled() {
        return getPollerConfig().shouldNotifyXmlrpc();
    }

} // end class
