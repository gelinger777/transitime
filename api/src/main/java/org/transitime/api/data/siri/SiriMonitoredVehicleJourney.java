/*
 * This file is part of Transitime.org
 * 
 * Transitime.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL) as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Transitime.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transitime.org .  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transitime.api.data.siri;

import javax.xml.bind.annotation.XmlElement;

import org.transitime.ipc.data.IpcExtVehicle;
import org.transitime.ipc.data.IpcPrediction;

/**
 * For SIRI MonitoredVehicleJourney element
 *
 * @author SkiBu Smith
 *
 */
public class SiriMonitoredVehicleJourney {

    // Vehicle Id
    @XmlElement(name="VehicleRef")
    private String vehicleRef;
    
    // Location of vehicle
    @XmlElement(name="VehicleLocation")
    private SiriLocation vehicleLocation;
    
    // Vehicle bearing: 0 is East, increments counter-clockwise.
    // This of course is different from heading, where 0 is north
    // and it goes clockwise.
    @XmlElement(name="Bearing")
    private String bearingStr;
    
    // Block ID
    @XmlElement(name="BlockRef")
    private String blockRef;
    
    // The route name
    @XmlElement(name="LineRef")
    private String lineRef;
    
    // The GTFS direction
    @XmlElement(name="DirectionRef")
    private String directionRef;
    
    // Describes the trip
    @XmlElement(name="FramedVehicleJourneyRef")
    private SiriFramedVehicleJourneyRef framedVehicleJourneyRef;
    
    // Name of route. Using short name since that is available and is
    // more relevant.
    @XmlElement(name="PublishedLineName")
    private String publishedLineName;
    
    // Name of agency
    @XmlElement(name="OperatorRef")
    private String operatorRef;
    
    @XmlElement(name="OriginRef")
    private String originRef;
    
    @XmlElement(name="DestinationRef")
    private String destinationRef;
    
    @XmlElement(name="DestinationName")
    private String destinationName;
    
    @XmlElement(name="OriginAimedDepartureTime")
    private String originAimedDepartureTime;
    
    // Whether vehicle tracked
    @XmlElement(name="Monitored")
    private String monitored;
    
    // Indicator of whether the bus is making progress (i.e. moving, generally)
    // or not (with value noProgress).
    @XmlElement(name="ProgressRate")
    private String progressRate;
    
    @XmlElement(name="ProgressStatus")
    private String progressStatus;
    
    @XmlElement(name="MonitoredCall")
    private SiriMonitoredCall monitoredCall;
    
    @XmlElement(name="OnwardCalls")
    private String onwardCalls;
    
    /********************** Member Functions **************************/

    /**
     * Constructs that massive MonitoredVehicleJourney element.
     * 
     * @param ipcExtVehicle
     * @param prediction
     *            For when doing stop monitoring. If doing vehicle monitoring
     *            then should be set to null.
     * @param agencyId
     */
    public SiriMonitoredVehicleJourney(IpcExtVehicle ipcExtVehicle,
	    IpcPrediction prediction, String agencyId) {
	vehicleRef = ipcExtVehicle.getId();
	vehicleLocation = 
		new SiriLocation(ipcExtVehicle.getLatitude(), ipcExtVehicle.getLongitude());
	double bearing = 90-ipcExtVehicle.getHeading();
	if (bearing < 0)
	    bearing += 360.0;
	bearingStr = Double.toString(bearing);
	blockRef = ipcExtVehicle.getBlockId();
	lineRef = ipcExtVehicle.getRouteShortName();
	directionRef = ipcExtVehicle.getDirectionId();
	framedVehicleJourneyRef = new SiriFramedVehicleJourneyRef(ipcExtVehicle);
	publishedLineName = ipcExtVehicle.getRouteName();
	operatorRef = agencyId;
	originRef = ipcExtVehicle.getOriginStopId();
	destinationRef = ipcExtVehicle.getDestinationId();
	destinationName = ipcExtVehicle.getDestinationName();
	originAimedDepartureTime = 
		Utils.formattedTime(ipcExtVehicle.getTripStartEpochTime());
	monitored = "true";
	progressRate = "normalProgress";
	progressStatus = ipcExtVehicle.isLayover() ? "true" : null;
	
	monitoredCall = new SiriMonitoredCall(ipcExtVehicle, prediction);
	
	// Not currently implemented but outputting it for completeness
	onwardCalls = "";
    }
}