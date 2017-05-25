package gov.noaa.nws.ncep.viz.rsc.asdi.rsc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.time.DataTime;

import gov.noaa.nws.ncep.viz.resources.AbstractDataLoader;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.IDataLoader;
import gov.noaa.nws.ncep.viz.resources.IRscDataObject;
import gov.noaa.nws.ncep.viz.rsc.asdi.rsc.AsdiResource.AsdiRscDataObject;

/**
 * TODO Add Description
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 21, 2016 R15954     SRussell    Initial creation
 * 06/07/2017   R28579     RCReynolds  Modified for ASDI
 * 
 * </pre>
 * 
 * @author srussell
 * @version 1.0
 */

public class AsdiDataLoader extends AbstractDataLoader implements IDataLoader {

    protected static final String FIELD_REFTIME = "dataTime.refTime";

    public AsdiDataLoader() {
    }

    public AsdiDataLoader(
            AbstractNatlCntrsRequestableResourceData resourceData) {
        super();
        this.resourceData = resourceData;
    }

    @Override
    public void setResourceData(
            AbstractNatlCntrsRequestableResourceData resourceData) {
        this.resourceData = resourceData;
    }

    @Override
    protected IRscDataObject[] processRecord(Object pdo) {
        if (!(pdo instanceof PluginDataObject)) {
            System.out.println(
                    "Resource Impl " + getClass().getName() + " must override "
                            + "the processRecord method to process data objects of class: "
                            + pdo.getClass().getName());
            return null;
        }

        AsdiResource.AsdiRscDataObject asdiDataObj = new AsdiResource.AsdiRscDataObject(
                (PluginDataObject) pdo);

        return new AsdiRscDataObject[] { asdiDataObj };
    }

    @Override
    public void loadData() {

        frameDataMap.keySet();
        ArrayList<DataTime> frameTimes = new ArrayList<>();
        for (long t : frameDataMap.keySet()) {
            frameTimes.add(new DataTime(new Date(t)));
        }
        Collections.sort(frameTimes);
        Calendar frameStartTime = Calendar.getInstance();

        DataTime startTime = null;
        DataTime endTime = null;

        RequestConstraint timeConstraint = new RequestConstraint();

        HashMap<String, RequestConstraint> requestConstraints = new HashMap<>();

        if (frameTimes != null && frameTimes.size() > 0) {
            int frameTimeListSize = frameTimes.size();
            long frameIntrvlMs = (long) resourceData.getFrameSpan() * 60 * 1000;
            long startTimeOffsetMs = 30 * 60 * 1000;

            for (int i = 0; i < frameTimeListSize; i++) {

                // adjust the start and end times to include the
                // frameIntervals. If this isn't dominant, we need to query
                // everything that may match the first/last frames.

                Calendar validTime = frameTimes.get(i).getValidTime();
                String validTimeString = new DataTime(validTime).toString();
                frameStartTime.setTime(
                        new Date(validTime.getTimeInMillis() - frameIntrvlMs));
                startTime = new DataTime(frameStartTime);
                String[] constraintList = { startTime.toString(),
                        validTimeString };

                timeConstraint.setBetweenValueList(constraintList);

                timeConstraint.setConstraintType(
                        RequestConstraint.ConstraintType.BETWEEN);

                requestConstraints.put("dataTime", timeConstraint);

                requestConstraints.put("pluginName",
                        new RequestConstraint(resourceData.getPluginName()));

                try {
                    queryRecords(requestConstraints);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        processNewRscDataList();
    }

    @Override
    protected boolean postProcessFrameUpdate() {

        resourceData.getResourceName();

        return true;
    }

}
