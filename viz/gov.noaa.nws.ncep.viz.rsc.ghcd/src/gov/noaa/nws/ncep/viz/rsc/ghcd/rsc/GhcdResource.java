/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.viz.rsc.ghcd.rsc;

import gov.noaa.nws.ncep.common.dataplugin.ghcd.GenericHighCadenceDataRecord;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataField;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataItem;
import gov.noaa.nws.ncep.viz.common.tsScaleMngr.XAxisScale;
import gov.noaa.nws.ncep.viz.common.tsScaleMngr.XAxisScaleElement;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet;
import gov.noaa.nws.ncep.viz.resources.time_match.GraphTimelineUtil;
import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcher;
import gov.noaa.nws.ncep.viz.rsc.ghcd.GhcdDescriptor;
import gov.noaa.nws.ncep.viz.rsc.ghcd.GhcdGraph;
import gov.noaa.nws.ncep.viz.rsc.ghcd.util.GhcdUtil;
import gov.noaa.nws.ncep.viz.rsc.timeseries.rsc.RetrieveUtils;
import gov.noaa.nws.ncep.viz.ui.display.NCTimeSeriesDescriptor;
import gov.noaa.nws.ncep.viz.ui.display.NCTimeSeriesRenderableDisplay;
import gov.noaa.nws.ncep.viz.ui.display.NcPaneID;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.catalog.LayerProperty;
import com.raytheon.uf.viz.core.catalog.ScriptCreator;
import com.raytheon.uf.viz.core.comm.Connector;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.ResourceType;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.MagnificationCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.OutlineCapability;
import com.raytheon.uf.viz.xy.graph.IGraph;
import com.raytheon.uf.viz.xy.graph.labeling.DataTimeLabel;
import com.raytheon.uf.viz.xy.graph.labeling.DoubleLabel;
import com.raytheon.uf.viz.xy.graph.labeling.IGraphLabel;
import com.raytheon.uf.viz.xy.map.rsc.IGraphableResource;
import com.raytheon.viz.core.graphing.xy.XYData;
import com.raytheon.viz.core.graphing.xy.XYDataList;

/**
 * The resource class for GhcdResource (generic high cadence data).
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Sep 5, 2014   R4508       sgurung     Initial creation
 * Jun 6, 2015	 R5413		 jhuber		 Remove Script Creator and use Thrift Client
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class GhcdResource extends
        AbstractNatlCntrsResource<GhcdResourceData, NCTimeSeriesDescriptor>
        implements IGraphableResource<DataTime, Double>, INatlCntrsResource,
        IResourceDataChanged {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(GhcdResource.class);

    private GhcdResourceData ghcdData;

    protected IDisplayPane currentPane;

    private XAxisScale existingxAxisScale = null;

    private final SimpleDateFormat dateFmt = new SimpleDateFormat(
            "yyyyMMdd/HHmm");

    /** The data in xy form */
    protected volatile XYDataList data = new XYDataList();

    /** The graph to draw to */
    protected IGraph graph = null;

    private Set<DataTime> dataTimes = new TreeSet<DataTime>();

    protected NCTimeMatcher timeMatcher;

    protected DataTime timelineStart;

    protected DataTime timelineEnd;

    protected List<GenericHighCadenceDataRecord> ghcdRecords;

    private HashMap<Integer, XAxisScaleElement> xAxisScaleMap = new HashMap<Integer, XAxisScaleElement>();

    private boolean reconstructGraph = false;

    protected class FrameData extends AbstractFrameData {

        protected FrameData(DataTime time, int interval) {
            super(time, interval);
            dateFmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        @Override
        public void dispose() {
            super.dispose();
        }

        @Override
        public boolean updateFrameData(
                gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource.IRscDataObject rscDataObj) {
            return false;
        }
    }

    protected GhcdResource(GhcdResourceData resData,
            LoadProperties loadProperties) {

        super(resData, loadProperties);
        ghcdData = resData;
        existingxAxisScale = ghcdData.getXAxisScale();
        xAxisScaleMap = existingxAxisScale.getxAxisScaleMap();

        getCapabilities().removeCapability(ColorableCapability.class);
        getCapabilities().removeCapability(OutlineCapability.class);
    }

    @Override
    protected void disposeInternal() {

        if (data != null) {
            data.dispose();
        }
        this.data = null;

        if (currentPane != null)
            currentPane.dispose();

        super.disposeInternal();
    }

    @Override
    public void paintFrame(AbstractFrameData frmData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

    }

    @Override
    public void initResource(IGraphicsTarget target) throws VizException {
        getDescriptor().getResourceList().instantiateResources(getDescriptor(),
                true);
        if (descriptor.getRenderableDisplay().getContainer().getDisplayPanes().length > 1) {
            ((NCTimeSeriesDescriptor) descriptor).getTimeMatcher()
                    .redoTimeMatching(((NCTimeSeriesDescriptor) descriptor));
        }

        timeMatcher = (NCTimeMatcher) ((NCTimeSeriesDescriptor) descriptor)
                .getTimeMatcher();

        setQueryTimes();
        queryRecords();
    }

    public void setQueryTimes() {
        Long stl = Long.MAX_VALUE;
        Long etl = Long.MIN_VALUE;

        for (AbstractFrameData afd : frameDataMap.values()) {
            if (stl > afd.getFrameStartTime().getRefTime().getTime()) {

                stl = afd.getFrameStartTime().getRefTime().getTime();
            }
            if (etl < afd.getFrameEndTime().getRefTime().getTime()) {

                etl = afd.getFrameEndTime().getRefTime().getTime();
            }

        }
        Date start = new Date(stl);
        start.setSeconds(0);
        Date end = new Date(etl);
        end.setSeconds(0);

        timelineStart = new DataTime(start);
        timelineEnd = new DataTime(end);

        // snap to endtime based on snapHour
        if (timelineStart.getRefTime().getMinutes() != 0) {

            Calendar tem = GraphTimelineUtil.snapTimeToClosest(
                    timelineStart.getRefTimeAsCalendar(),
                    timeMatcher.getHourSnap());
            timelineStart = new DataTime(tem);

            tem = GraphTimelineUtil.snapTimeToClosest(
                    timelineEnd.getRefTimeAsCalendar(),
                    timeMatcher.getHourSnap());
            timelineEnd = new DataTime(tem);
        }

        ghcdData.setStartTime(timelineStart);
    }

    // override base version to constrain on the selected timeline
    @Override
    public void queryRecords() throws VizException {

        HashMap<String, RequestConstraint> reqConstraints = new HashMap<String, RequestConstraint>();
        reqConstraints.put("pluginName", new RequestConstraint(resourceData.getPluginName()));
        RequestConstraint reqConstr = new RequestConstraint();

        String startTimeStr = timelineStart.toString().substring(0, 19);
        String endTimeStr = timelineEnd.toString().substring(0, 19);

        String[] constraintList = { startTimeStr, endTimeStr };
        reqConstr.setBetweenValueList(constraintList);
        reqConstr.setConstraintType(RequestConstraint.ConstraintType.BETWEEN);

        reqConstraints.put("dataTime.refTime", reqConstr);
        DbQueryRequest request = new DbQueryRequest();
        request.setConstraints( reqConstraints );
        DbQueryResponse response = (DbQueryResponse) ThriftClient.sendRequest(request);
        ghcdRecords = new ArrayList<GenericHighCadenceDataRecord>();
        
        for( Map<String, Object> result : response.getResults())  {
        	for (Object pdo : result.values()) {
        		for (IRscDataObject dataObject : processRecord(pdo)) {		
        			newRscDataObjsQueue.add((IRscDataObject) dataObject);
        			ghcdRecords.add((GenericHighCadenceDataRecord) ((DfltRecordRscDataObj) dataObject)
                                .getPDO());
        		}
        	}
        }
        sortRecord();
        data = loadInternal(ghcdRecords);

        // Append null to ghcdRecords if data does not exist up to the graph end
        // time.
        int recSize = data.getData().size();
        if (recSize > 0) {
            DataTime last = (DataTime) data.getData().get(recSize - 1).getX();
            Calendar lastCal = last.getRefTimeAsCalendar();
            Calendar cal = (Calendar) lastCal.clone();
            cal = GraphTimelineUtil.snapTimeToNext(cal,
                    timeMatcher.getHourSnap());
            int fill = (int) (cal.getTimeInMillis() - lastCal.getTimeInMillis()) / 60000;

            for (int i = 0; i < fill; i++) {
                if (ghcdData.getDataResolUnits().startsWith("min")) {
                    lastCal.add(Calendar.MINUTE, ghcdData.getDataResolVal());
                } else if (ghcdData.getDataResolUnits().startsWith("hour")) {
                    lastCal.add(Calendar.HOUR, ghcdData.getDataResolVal());
                }

                Calendar appendCal = (Calendar) lastCal.clone();

                XYData d = new XYData(new DataTime(appendCal), null);
                data.getData().add(d);
            }

            dataTimes = new TreeSet<DataTime>();
            for (XYData d : data.getData()) {
                dataTimes.add((DataTime) d.getX());
            }
        }

    }

    private XYDataList loadInternal(
            List<GenericHighCadenceDataRecord> recordsList) throws VizException {

        ArrayList<XYData> data = new ArrayList<XYData>();
        if (recordsList.size() <= 1)
            return new XYDataList();

        // get data from hdf5
        List<Date> refTimes = new ArrayList<Date>();
        for (GenericHighCadenceDataRecord rec : recordsList) {
            refTimes.add(rec.getDataTime().getRefTime());
        }

        List<GenericHighCadenceDataItem> dataItems = GhcdUtil.getGhcdDataItems(
                refTimes, ghcdData.getSource(), ghcdData.getInstrument(),
                ghcdData.getDatatype(), ghcdData.getDataResolUnits(),
                ghcdData.getDataResolVal());

        if (dataItems.size() > 0) {
            int i = 0;
            for (GenericHighCadenceDataItem dataItem : dataItems) {
                DataTime x = recordsList.get(i).getDataTime();
                Float y = 0f;
                for (GenericHighCadenceDataField field : dataItem
                        .getGhcdFields()) {
                    if (field.getName().equalsIgnoreCase(ghcdData.getYData())) {
                        y = Float.parseFloat(field.getValue());

                        data.add(new XYData(x, y));
                    }
                }
                i++;
            }
        }

        XYDataList list = new XYDataList();
        list.setData(data);

        return list;
    }

    public void autoupdateRecords() {

    }

    @Override
    protected AbstractFrameData createNewFrame(DataTime frameTime,
            int frameInterval) {
        return new FrameData(frameTime, frameInterval);
    }

    @Override
    public void resourceAttrsModified() {
        ResourceAttrSet rscAttrSet = ghcdData.getRscAttrSet();
        existingxAxisScale = ghcdData.getXAxisScale();
        xAxisScaleMap = existingxAxisScale.getxAxisScaleMap();
        reconstructGraph = true;
        issueRefresh();

    }

    @Override
    public void propertiesChanged(ResourceProperties updatedProps) {
    }

    @Override
    public void resourceChanged(ChangeType type, Object object) {
        if (type == ChangeType.DATA_UPDATE) {
            PluginDataObject[] objects = (PluginDataObject[]) object;
            // for (PluginDataObject pdo : objects) {
            // addRecord(pdo);
            // }
        }
        issueRefresh();
    }

    private void sortRecord() {
        Collections.sort(ghcdRecords,
                new Comparator<GenericHighCadenceDataRecord>() {

                    @Override
                    public int compare(GenericHighCadenceDataRecord g1,
                            GenericHighCadenceDataRecord g2) {
                        DataTime t1 = (DataTime) g1.getDataTime();
                        DataTime t2 = (DataTime) g2.getDataTime();
                        return t1.compareTo(t2);
                    }
                });
    }

    public String getLegendStr() {
        return getName();
    }

    /**
     * @return the data
     */
    public XYDataList getData() {
        return data;
    }

    /**
     * @param data
     *            the data to set
     */
    public void setData(XYDataList data) {
        this.data = data;
    }

    /*
     * don't use paintFrame since we don't want to updateFrame many times
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource#paintInternal
     * (com.raytheon.uf.viz.core.IGraphicsTarget,
     * com.raytheon.uf.viz.core.drawables.PaintProperties)
     */
    @Override
    public void paintInternal(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {

        Double magnification = getCapability(MagnificationCapability.class)
                .getMagnification();

        if (data == null) {
            return;
        }

        /*
         * if autoupdate, reconstruct graph when latest refTime from data
         * exceeds existing end time. Also, reconstruct graph when resource
         * attributes are modified.
         */
        if ((graph != null && timelineStart != ghcdData.getStartTime())
                || reconstructGraph) {
            graph.reconstruct();
            ghcdData.setStartTime(timelineStart);
            reconstructGraph = false;
        }

        boolean changeExtent = false;
        if (timeMatcher.isAutoUpdateable()) {

            while (!newRscDataObjsQueue.isEmpty()) {
                IRscDataObject rscDataObj = newRscDataObjsQueue.poll();

                if (rscDataObj.getDataTime().compareTo(timelineEnd) > 0) {
                    changeExtent = true;
                    timelineStart = RetrieveUtils.moveToNextSynop(
                            timelineStart, timeMatcher.getHourSnap());

                    timelineEnd = RetrieveUtils.moveToNextSynop(timelineEnd,
                            timeMatcher.getHourSnap());

                    break;
                }
            }

            if (changeExtent) {
                ArrayList<DataTime> newList = new ArrayList<DataTime>();
                for (int i = 0; i < newRscDataObjsQueue.size(); i++) {
                    DataTime dataTime = newRscDataObjsQueue.poll()
                            .getDataTime();

                    if (dataTime.compareTo(timelineStart) >= 0
                            && dataTime.compareTo(timelineEnd) <= 0) {

                        newList.add(dataTime);
                    }
                }
                timeMatcher.updateTimeline(newList);

            }

            newRscDataObjsQueue.clear();
            queryRecords();

            if (changeExtent) {
                issueRefresh();
            }

        }

        /*
         * get gmDescriptor and graph
         */
        IDisplayPane[] pane = GhcdDescriptor.getDisplayPane();

        if (pane[0].getRenderableDisplay() instanceof NCTimeSeriesRenderableDisplay) {
            for (int i = 0; i < pane.length; i++) {
                if (checkPaneId((NCTimeSeriesDescriptor) descriptor, pane[i])) {
                    GhcdDescriptor gmDescriptor = new GhcdDescriptor();
                    gmDescriptor.setResourcePair(gmDescriptor, pane[i]);
                    gmDescriptor.setNCTimeMatcher(gmDescriptor, pane[i]);
                    gmDescriptor.addDescriptor(gmDescriptor, pane[i]);
                    gmDescriptor.setAutoUpdate(true);
                    graph = gmDescriptor.getGraph(this);
                    currentPane = pane[i];
                    break;
                }
            }
        } else {
            GhcdDescriptor gmDescriptor = new GhcdDescriptor(
                    (NCTimeSeriesDescriptor) this.descriptor);
            gmDescriptor.setAutoUpdate(true);
            graph = gmDescriptor.getGraph(this);
        }

        /*
         * Wait for graph to initialize before plotting to it, TODO: better
         */
        if (graph.isReady() == false) {
            return;
        }

        graph.setCurrentMagnification(magnification);
        IExtent extent = ((GhcdGraph) graph).getExtent();
        target.setupClippingPlane(extent);

        /*
         * display data
         */
        double[] prevScreen = null;

        RGB color = ((GhcdResourceData) resourceData).getDataColor();

        for (int i = 0; i < data.getData().size(); i++) {

            XYData point = data.getData().get(i);

            if (point.getY() != null) {

                double[] screen = getScreenPosition(point.getX(), point.getY());

                // Connects adjacent data points with a line
                if (prevScreen != null) {
                    target.drawLine(screen[0], screen[1], 0.0, prevScreen[0],
                            prevScreen[1], 0.0, color, ghcdData.getLineWidth(),
                            ghcdData.getLineStyle());
                }

                prevScreen = screen;
            }

            target.clearClippingPlane();
        }
    }

    protected boolean checkPaneId(NCTimeSeriesDescriptor desc, IDisplayPane pane) {

        NcPaneID paneId = ((NCTimeSeriesRenderableDisplay) desc
                .getRenderableDisplay()).getPaneId();

        NcPaneID newPaneId = ((NCTimeSeriesRenderableDisplay) pane
                .getRenderableDisplay()).getPaneId();

        if (newPaneId.getColumn() == paneId.getColumn()
                && newPaneId.getRow() == paneId.getRow()) {
            return true;

        } else {
            return false;
        }

    }

    private double[] getScreenPosition(Object x, Object y) {
        double valY = ((Number) y).doubleValue();
        double valX = ((DataTime) x).getValidTime().getTimeInMillis();
        return graph.getGridLocation(valX, valY);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.rsc.IVizResource#getName()
     */
    @Override
    public String getName() {

        StringBuilder sb = new StringBuilder(((GhcdResourceData) resourceData)
                .getSource().toUpperCase());
        sb.append(" ");
        sb.append(((GhcdResourceData) resourceData).getInstrument());
        sb.append(" ");
        sb.append(((GhcdResourceData) resourceData).getDatatype());
        sb.append(" ");
        sb.append(((GhcdResourceData) resourceData).getYData());
        sb.append(", ");
        sb.append(((GhcdResourceData) resourceData).getDataResolVal());
        sb.append("-");
        sb.append(((GhcdResourceData) resourceData).getDataResolUnits()
                .substring(0, 3));
        if (data == null || data.getData().size() == 0) {
            sb.append(" - NO DATA");
        } else {
            XYData point = data.getData().get(0);
            DataTime time = (DataTime) point.getX();

            dateFmt.format(time.getValidTimeAsDate());
            sb.append(" - Begin: ");
            sb.append(time);
        }

        return sb.toString();
    }

    public String getTitle() {

        String yTitle = ((GhcdResourceData) this.getResourceData()).getYTitle();
        return (yTitle);
    }

    @Override
    public IGraphLabel<DataTime>[] getXRangeData() {
        DataTimeLabel[] labels = new DataTimeLabel[dataTimes.size()];
        int i = 0;
        for (DataTime time : dataTimes) {
            labels[i++] = new DataTimeLabel(time);
        }
        return labels;
    }

    @Override
    public IGraphLabel<Double>[] getYRangeData() {
        double min = this.getMinDataValue();
        double max = this.getMaxDataValue();
        return new DoubleLabel[] { new DoubleLabel(min), new DoubleLabel(max) };
    }

    @Override
    public DataTime[] getDataTimes() {
        return dataTimes.toArray(new DataTime[0]);
    }

    public DataTime getLastDataTime() {

        if (ghcdRecords != null && ghcdRecords.size() > 0) {
            return ghcdRecords.get(ghcdRecords.size() - 1).getDataTime();
        }

        return null;
    }

    public double getMinDataValue() {
        double min = Double.POSITIVE_INFINITY;
        if (data != null) {
            for (XYData d : data.getData()) {
                if (d.getY() instanceof Number) {
                    double y = ((Number) d.getY()).doubleValue();
                    min = Math.min(min, y);
                }
            }
        }
        return min;
    }

    public double getMaxDataValue() {
        double max = Double.NEGATIVE_INFINITY;
        if (data != null) {
            for (XYData d : data.getData()) {
                if (d.getY() instanceof Number) {
                    double y = ((Number) d.getY()).doubleValue();
                    max = Math.max(max, y);
                }
            }
        }
        return max;
    }

    @Override
    public void redraw() {

    }

    public XAxisScale getExistingxAxisScale() {
        return existingxAxisScale;
    }

    public void setExistingxAxisScale(XAxisScale existingxAxisScale) {
        this.existingxAxisScale = existingxAxisScale;
    }

    public HashMap<Integer, XAxisScaleElement> getxAxisScaleMap() {
        return xAxisScaleMap;
    }

    public void setxAxisScaleMap(
            HashMap<Integer, XAxisScaleElement> xAxisScaleMap) {
        this.xAxisScaleMap = xAxisScaleMap;
    }

    @Override
    public Object getGraphKey() {
        return ghcdData.getGraphKey();
    }

}