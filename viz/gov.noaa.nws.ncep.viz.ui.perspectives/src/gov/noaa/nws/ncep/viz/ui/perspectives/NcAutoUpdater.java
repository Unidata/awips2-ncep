package gov.noaa.nws.ncep.viz.ui.perspectives;

import gov.noaa.nws.ncep.viz.common.dbQuery.NcDBUtils;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.RecordFactory;
import com.raytheon.uf.viz.core.alerts.AbstractAlertMessageParser;
import com.raytheon.uf.viz.core.alerts.AlertMessage;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractRequestableResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.updater.DataUpdateTree;
import com.raytheon.viz.alerts.IAlertObserver;

/**
 * NcAutoUpdater
 * 
 * Updates the Natl Cntrs resources as data comes in. This was copied from
 * Raytheon's AutoUpdater to fix a couple of bugs in their code.
 * 
 * 06/13 : We could probably use Raytheon's AutoUpdater now but we would need to
 * override the AbstractRequestableResources update() method
 * 
 * 
 * <pre>
 * 
 *    SOFTWARE HISTORY
 *   
 *    Date         Ticket#     Engineer    Description
 *    ------------ ----------  ----------- --------------------------
 *    10/22/10      #307       ghull       Initial Creation based on AutoUpdater
 *    06/07/11      #445       xGuo        Data Manager Performance Improvements
 *                                         process alert message to update data resource
 *    03/21/12      #606       ghull       resources no longer need to be updated
 *    06/16/13      #768       ghull       don't replace URI spaces with '_'s
 *    04/20/2016    R15954     SRussell    Updated method alertArrived() to
 *                                         also handle AbstractNatlCntrsResource2.
 *                                         Replaced deprecated call 
 *                                         Loader.loadData() in parseAlertMessage
 * 
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
public class NcAutoUpdater implements IAlertObserver {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(NcAutoUpdater.class);

    private static final int MAX_ERRORS = 10;

    private static class ParseNatlCntrsAlertMessage extends
            AbstractAlertMessageParser {

        @Override
        public Object parseAlertMessage(AlertMessage message,
                AbstractRequestableResourceData reqResourceData)
                throws VizException {
            Object objectToSend = null;
            String dataURI = message.dataURI;
            if (reqResourceData.isUpdatingOnMetadataOnly()) {
                PluginDataObject record = RecordFactory.getInstance()
                        .loadRecordFromUri(dataURI);
                objectToSend = record;

            } else {
                objectToSend = NcDBUtils.loadDataURI2PDO(dataURI);
            }
            return objectToSend;
        }
    };

    private static ParseNatlCntrsAlertMessage defaultParser = new ParseNatlCntrsAlertMessage();

    protected NcAutoUpdater() {
    }

    @Override
    public void alertArrived(Collection<AlertMessage> alertMessages) {

        int errors = 0;

        for (AlertMessage message : alertMessages) {
            Map<String, Object> attribs = new HashMap<>(message.decodedAlert);

            try {
                java.util.List<AbstractVizResource<?, ?>> rscList = DataUpdateTree
                        .getInstance().searchTree(attribs);

                if (rscList != null && rscList.size() > 0) {

                    // For each resource
                    for (AbstractVizResource<?, ?> r1 : rscList) {

                        // Skip processing if the resource is NOT
                        // AbstractNatlCntrsResource or
                        // AbstractNatlCntrsResource2
                        //
                        // AbstractNatlCntrsResource is deprecated and will
                        // eventually be replaced by AbstractNatlCntrsResource2
                        if (!(r1 instanceof AbstractNatlCntrsResource<?, ?>)
                                && !(r1 instanceof AbstractNatlCntrsResource2<?, ?>)) {
                            continue;
                        }

                        if (!(r1.getResourceData() instanceof AbstractNatlCntrsRequestableResourceData)
                                || r1.getResourceData().isFrozen())
                            continue;

                        AbstractNatlCntrsRequestableResourceData reqResourceData = (AbstractNatlCntrsRequestableResourceData) r1
                                .getResourceData();

                        AbstractAlertMessageParser parserToUse = null;

                        if ((parserToUse = reqResourceData.getAlertParser()) == null) {
                            parserToUse = defaultParser;
                        }

                        Object objectToSend = parserToUse.parseAlertMessage(
                                message, reqResourceData);

                        if (objectToSend != null) {
                            reqResourceData.autoUpdate(objectToSend);
                            r1.issueRefresh();
                        }
                    }
                }

            } catch (final Throwable e) {
                if (errors < MAX_ERRORS) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Error performing NC autoupdate", e);
                }
                errors++;
            }
        }
    }
}
