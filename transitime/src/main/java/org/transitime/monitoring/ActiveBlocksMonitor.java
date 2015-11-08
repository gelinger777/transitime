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

package org.transitime.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.transitime.applications.Core;
import org.transitime.ipc.clients.VehiclesInterfaceFactory;
import org.transitime.ipc.data.IpcActiveBlock;
import org.transitime.ipc.interfaces.VehiclesInterface;
import org.transitime.utils.EmailSender;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;

/**
 * For monitoring active blocks.  Unlike the other monitors,
 * this one never triggers an alarm, it simply posts metrics to cloudwatch
 */
public class ActiveBlocksMonitor extends MonitorBase {

    private long reportingIntervalInMillis = 60l * 1000l;

    private Date lastUpdate = new Date();

    private CloudwatchService cloudwatchService;

    private static final Logger logger = LoggerFactory
            .getLogger(ActiveBlocksMonitor.class);

	public ActiveBlocksMonitor(CloudwatchService cloudwatchService, EmailSender emailSender, String agencyId) {
		super(emailSender, agencyId);
        this.cloudwatchService = cloudwatchService;
	}

	/* (non-Javadoc)
	 * @see org.transitime.monitoring.MonitorBase#triggered()
	 */
	@Override
	protected boolean triggered() {
        Date now = new Date();
        if(now.getTime() - lastUpdate.getTime() > reportingIntervalInMillis){
            VehiclesInterface vehiclesInterface = VehiclesInterfaceFactory
                    .get(agencyId);
            try {
                Collection<IpcActiveBlock> blocks = vehiclesInterface.getActiveBlocks(null, 0);
                double activeBlockCount = (blocks != null ? blocks.size() : 0);
                double totalBlockCount = Core.getInstance().getDbConfig().getBlockCount();
                cloudwatchService.saveMetric("ActiveBlockCount", activeBlockCount , 1, CloudwatchService.MetricType.SCALAR, CloudwatchService.ReportingIntervalTimeUnit.IMMEDIATE, false);
                cloudwatchService.saveMetric("TotalBlockCount", activeBlockCount , 1, CloudwatchService.MetricType.SCALAR, CloudwatchService.ReportingIntervalTimeUnit.IMMEDIATE, false);
                double activeBlockCountPercentage = 0;
                if(activeBlockCount > 0){
                    activeBlockCountPercentage = activeBlockCount / totalBlockCount;
                }
                cloudwatchService.saveMetric("PercentageActiveBlockCount", activeBlockCountPercentage , 1,
                        CloudwatchService.MetricType.SCALAR, CloudwatchService.ReportingIntervalTimeUnit.IMMEDIATE, true);

            } catch (RemoteException e) {
                logger.warn("getActiveBlocks threw Remote Exception");
            }
        }
        return false;
	}

	/* (non-Javadoc)
	 * @see org.transitime.monitoring.MonitorBase#type()
	 */
	@Override
	protected String type() {
		return "Database Queue";
	}
}
