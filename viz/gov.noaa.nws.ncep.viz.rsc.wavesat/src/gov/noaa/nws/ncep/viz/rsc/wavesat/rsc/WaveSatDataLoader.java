package gov.noaa.nws.ncep.viz.rsc.wavesat.rsc;

import gov.noaa.nws.ncep.viz.resources.AbstractDataLoader;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.rsc.wavesat.rsc.WaveSatResource.FrameData;
import gov.noaa.nws.ncep.viz.rsc.wavesat.rsc.WaveSatResource.WaveSatRscDataObj;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.pointdata.PointDataContainer;
import com.raytheon.uf.common.pointdata.PointDataDescription.Type;
import com.raytheon.uf.common.pointdata.PointDataView;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.pointdata.PointDataRequest;

/**
 * This class implements preloading of data for the AbstractWaveSatResource
 * class.  That is, instead of loading data frame by frame, as each frame is
 * advanced to in CAVE, this class will load all of the data for 
 * AbstractWaveSatResource after the user hits the "Load And Close" button
 * from the Resource Manager.
 * 
 * This class is also part of a redesign of the Resource family of classes
 * meant to separate data loading as much as possible out of the Resource 
 * family leaving that class to focus on presentation.
 * 
 * For more information see the title block comment at
 * {@link gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 20,2015  RM 11819   srussell    Initial creation
 * 
 * </pre>
 * 
 * @author srussell
 * @version 1.0
 */

public class WaveSatDataLoader extends AbstractDataLoader {

    private static final IUFStatusHandler logger = UFStatus
            .getHandler(WaveSatDataLoader.class);

    private WaveSatResourceData waveSatRscData;

    protected String REFTIME_PARAM = "REFTIME";

    protected String latParam;
    protected String lonParam;
    protected String satIdParam;
    protected String waveHeightParam;
    protected String windSpeedParam;

    public WaveSatDataLoader() {
    }

    public WaveSatDataLoader(String latParam, String lonParam,
            String satIdParam, String waveHeightParam, String windSpeedParam) {

        this.latParam = latParam;
        this.lonParam = lonParam;
        this.satIdParam = satIdParam;
        this.waveHeightParam = waveHeightParam;
        this.windSpeedParam = windSpeedParam;

    }

    @Override
    public void loadData() {

        List<String> params = new ArrayList<String>();

        params.add(REFTIME_PARAM);
        params.add(latParam);
        params.add(lonParam);
        params.add(satIdParam);
        params.add(waveHeightParam);
        params.add(windSpeedParam);

        try {
            this.queryRecords(prepareConstraints(), params);
        } catch (VizException ve) {
            logger.error("Error Querying Records", ve);
        }

    }

    private HashMap<String, RequestConstraint> prepareConstraints() {
        long headKey = 0;
        long tailKey = 0;

        // Get the startTime of the first frame and the endTime of the last
        // frame
        headKey = frameDataMap.firstKey();
        tailKey = frameDataMap.lastKey();
        FrameData firstFrame = (FrameData) frameDataMap.get(headKey);
        FrameData lastFrame = (FrameData) frameDataMap.get(tailKey);
        DataTime startTime = firstFrame.getFrameStartTime();
        DataTime endTime = lastFrame.getFrameEndTime();

        // Get the standard constraints
        HashMap<String, RequestConstraint> requestConstraints = new HashMap<String, RequestConstraint>(
                waveSatRscData.getMetadataMap());

        RequestConstraint timeConstraint = new RequestConstraint();

        // Add a constraint for all data BETWEEN the starting time of the
        // first frame and ending time of the last frame
        String[] constraintList = { startTime.toString(), endTime.toString() };
        timeConstraint.setBetweenValueList(constraintList);
        timeConstraint
                .setConstraintType(RequestConstraint.ConstraintType.BETWEEN);
        requestConstraints.put("dataTime", timeConstraint);

        return requestConstraints;

    }

    private void queryRecords(
            HashMap<String, RequestConstraint> requestConstraints,
            List<String> params) throws VizException {

        PointDataContainer pdc = null;

        try {
            // Run the query
            pdc = PointDataRequest.requestPointDataAllLevels(
                    waveSatRscData.getPluginName(),
                    params.toArray(new String[params.size()]), null,
                    requestConstraints);

        } catch (VizException e1) {
            logger.error("Error querying for sgwh point data", e1);
        }

        if (pdc == null) {
            return;
        } else {
            pdc.setCurrentSz(pdc.getAllocatedSz());
        }

        for (int uriCounter = 0; uriCounter < pdc.getAllocatedSz(); uriCounter++) {
            PointDataView pdv = pdc.readRandom(uriCounter);
            if (pdv != null) {
                for (gov.noaa.nws.ncep.viz.resources.IRscDataObject dataObject : processRecord(pdv)) {
                    newRscDataObjsQueue.add(dataObject);
                }
            }
        }

        this.processNewRscDataList();
        this.setAllFramesAsPopulated();

    }

    @Override
    protected gov.noaa.nws.ncep.viz.resources.IRscDataObject[] processRecord(
            Object pdo) {

        if (pdo instanceof WaveSatRscDataObj) {
            WaveSatRscDataObj waveSatData = (WaveSatRscDataObj) pdo;
            if (waveSatData.createdFromAutoUpdate == false) {
                System.out.println("WaveSat processRecord() : sanity check #1");
                return null;
            }
            return new WaveSatRscDataObj[] { waveSatData };
        }
        if (!(pdo instanceof PointDataView)) {
            System.out
                    .println("WaveSatDataLoader processRecord() : Expecting PointDataView object instead of: "
                            + pdo.getClass().getName());
            return new WaveSatRscDataObj[] {};
        }

        WaveSatRscDataObj waveSatData = new WaveSatRscDataObj();
        PointDataView pdv = (PointDataView) pdo;

        if (pdv.getType(REFTIME_PARAM) != Type.LONG
                || pdv.getType(latParam) != Type.FLOAT
                || pdv.getType(lonParam) != Type.FLOAT
                || pdv.getType(satIdParam) != Type.LONG
                || pdv.getType(waveHeightParam) != Type.FLOAT
                || pdv.getType(windSpeedParam) != Type.FLOAT) {
            System.out.println("SGWHV parameter not found.");
            return new WaveSatRscDataObj[] {};
        }

        waveSatData.dataTime = new DataTime(new Date(
                (Long) pdv.getNumber(REFTIME_PARAM)));
        waveSatData.satelliteId = Long.toString((Long) pdv
                .getNumber(satIdParam).longValue());
        waveSatData.lat = pdv.getNumber(latParam).doubleValue();
        waveSatData.lon = pdv.getNumber(lonParam).doubleValue();
        waveSatData.waveHeight = pdv.getNumber(waveHeightParam).doubleValue();
        waveSatData.windSpeed = pdv.getNumber(windSpeedParam).doubleValue();

        return new WaveSatRscDataObj[] { waveSatData };
    }

    @Override
    public void setResourceData(
            AbstractNatlCntrsRequestableResourceData waveSatRscData) {
        this.waveSatRscData = (WaveSatResourceData) waveSatRscData;
    }

}
