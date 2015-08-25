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

import gov.noaa.nws.ncep.viz.common.RGBColorAdapter;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.common.tsScaleMngr.XAxisScale;
import gov.noaa.nws.ncep.viz.common.tsScaleMngr.XAxisScaleMngr;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.rsc.ghcd.util.GhcdUtil;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractNameGenerator;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

/**
 * Resource data for Ghcd from GenericHighCadenceDataRecord.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Sep 11, 2014   R4508     sgurung     Initial creation
 * Oct 16, 2014   R5097     sgurung     Added additional attributes
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "NC-GhcdResourceData")
public class GhcdResourceData extends AbstractNatlCntrsRequestableResourceData
        implements INatlCntrsResourceData {

    @XmlElement
    protected String source;

    @XmlElement
    protected String instrument;

    @XmlElement
    protected String datatype;

    @XmlElement
    protected String dataResolUnits;

    @XmlElement
    protected Integer dataResolVal;

    @XmlElement
    protected String yData;

    @XmlElement
    protected String xTitle;

    @XmlElement
    protected String yTitle;

    @XmlElement
    protected String yTitlePosition;

    @XmlElement
    protected String yScaleType;

    @XmlElement
    protected Float yScaleMin;

    @XmlElement
    protected Float yScaleMax;

    @XmlElement
    protected Float yInterval;

    @XmlElement
    protected Integer yNumTicks;

    @XmlElement
    protected String yUnits;

    @XmlElement
    protected String yUnitsPosition;

    @XmlElement
    @XmlJavaTypeAdapter(RGBColorAdapter.class)
    protected RGB dataColor = new RGB(255, 255, 255);

    // @XmlElement
    // protected String legendColor;

    @XmlElement
    protected XAxisScale xAxisScale = null;

    @XmlElement
    protected String graphKey;

    @XmlElement
    protected String graphTopTitle;

    @XmlElement
    protected Boolean displayXLabels = true;

    @XmlElement
    protected Boolean displayLastDataDate = true;

    @XmlElement
    protected Integer lineWidth = 1;

    @XmlElement
    protected LineStyle lineStyle = LineStyle.DEFAULT;

    @XmlElement
    protected String unitsFontSize = "14";

    @XmlElement
    protected String unitsFont = "Times";

    @XmlElement
    protected String unitsStyle = "Bold";

    @XmlElement
    protected String titleFontSize = "14";

    @XmlElement
    protected String titleFont = "Times";

    @XmlElement
    protected String titleStyle = "Bold";

    protected DataTime startTime;

    protected DataTime endTime;

    /**
     * 
     */
    public GhcdResourceData() {
        super();

        // called by AbstractVizResource.getName()
        // and we delegate back to the resource
        this.nameGenerator = new AbstractNameGenerator() {

            @Override
            public String getName(AbstractVizResource<?, ?> resource) {
                return ((GhcdResource) resource).getLegendStr();
            }
        };
    }

    @Override
    public NcDisplayType[] getSupportedDisplayTypes() {
        return new NcDisplayType[] { NcDisplayType.GRAPH_DISPLAY };
    }

    @Override
    protected AbstractVizResource<?, ?> constructResource(
            LoadProperties loadProperties, PluginDataObject[] objects)
            throws VizException {

        return new GhcdResource(this, loadProperties);
    }

    public String getStation() {
        RequestConstraint rc = getMetadataMap().get("stationCode");
        return rc.getConstraintValue();
    }

    @Override
    public DataTime[] getAvailableTimes() throws VizException {
        DataTime[] times = super.getAvailableTimes();
        // if (updating) {
        // this.startTime = calculateStartTime();
        // }
        // times = filterTimes(times, startTime, getEndTime());
        return times;
    }

    /**
     * Given the times, filter them to only return times between given times
     * 
     * @param times
     * @param start
     * @param end
     * @return
     */
    public DataTime[] filterTimes(DataTime[] times, DataTime startTime,
            DataTime endTime) {
        List<DataTime> validTimes = new ArrayList<DataTime>();
        for (DataTime time : times) {
            if (time.compareTo(startTime) >= 0 && time.compareTo(endTime) <= 0) {
                validTimes.add(time);
            }
        }
        return validTimes.toArray(new DataTime[validTimes.size()]);
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getDataResolUnits() {
        return dataResolUnits;
    }

    public void setDataResolUnits(String dataResolUnits) {
        this.dataResolUnits = dataResolUnits;
    }

    public Integer getDataResolVal() {
        return dataResolVal;
    }

    public void setDataResolVal(Integer dataResolVal) {
        this.dataResolVal = dataResolVal;
    }

    public String getYTitlePosition() {
        if (yTitlePosition == null
                || "".equals(yTitlePosition)
                || (!yTitlePosition
                        .equalsIgnoreCase(GhcdUtil.TITLE_POSITION_LEFT) && !yTitlePosition
                        .equalsIgnoreCase(GhcdUtil.TITLE_POSITION_RIGHT)))
            return GhcdUtil.TITLE_POSITION_LEFT;
        else
            return yTitlePosition;
    }

    public void setYTitlePosition(String yTitleLocation) {
        this.yTitlePosition = yTitleLocation;
    }

    public String getYScaleType() {
        return yScaleType;
    }

    public void setYScaleType(String yScaleType) {
        this.yScaleType = yScaleType;
    }

    public Float getYScaleMin() {
        return yScaleMin;
    }

    public void setYScaleMin(Float yScaleMin) {
        this.yScaleMin = yScaleMin;
    }

    public Float getYScaleMax() {
        return yScaleMax;
    }

    public void setYScaleMax(Float yScaleMax) {
        this.yScaleMax = yScaleMax;
    }

    public Float getYInterval() {
        return yInterval;
    }

    public void setYInterval(Float yInterval) {
        this.yInterval = yInterval;
    }

    public Integer getYNumTicks() {
        return yNumTicks;
    }

    public void setYNumTicks(Integer yNumTicks) {
        this.yNumTicks = yNumTicks;
    }

    public String getYUnits() {
        return yUnits;
    }

    public void setYUnits(String yUnits) {
        this.yUnits = yUnits;
    }

    public String getYUnitsPosition() {
        if (yUnitsPosition == null
                || "".equals(yUnitsPosition)
                || (!yUnitsPosition
                        .equalsIgnoreCase(GhcdUtil.TITLE_POSITION_LEFT) && !yUnitsPosition
                        .equalsIgnoreCase(GhcdUtil.TITLE_POSITION_RIGHT)))
            return GhcdUtil.TITLE_POSITION_LEFT;
        else
            return yUnitsPosition;
    }

    public void setYUnitsPosition(String yUnitsLocation) {

        this.yUnitsPosition = yUnitsLocation;
    }

    public DataTime getEndTime() {
        return endTime;
    }

    public void setEndTime(DataTime endTime) {
        this.endTime = endTime;
    }

    public DataTime getStartTime() {
        return startTime;
    }

    public void setStartTime(DataTime startTime) {
        this.startTime = startTime;
    }

    public RGB getDataColor() {
        return dataColor;
    }

    public void setDataColor(RGB dataColor) {
        this.dataColor = dataColor;
    }

    public RGB getLegendColor() {
        return this.dataColor;
    }

    public XAxisScale getXAxisScale() {
        // Note that the xAxisScale is set directly from either the refd xml
        // file or from
        // the xml in the RBD.
        // if the xAxisScale has not been set yet (from xml file
        // or from the xAxisScaleName attribute) then get it from the manager
        if (xAxisScale == null) {
            xAxisScale = XAxisScaleMngr.getInstance().getDefaultxAxisScale();
            return xAxisScale;
        }
        return new XAxisScale(xAxisScale);
    }

    public void setXAxisScale(XAxisScale xAxisScale) {
        this.xAxisScale = xAxisScale;
    }

    // public XAxisScale getxAxisScale() {
    // // Note that the xAxisScale is set directly from either the refd xml
    // // file or from
    // // the xml in the RBD.
    // // if the xAxisScale has not been set yet (from xml file
    // // or from the xAxisScaleName attribute) then get it from the manager
    // if (xAxisScale == null) {
    // xAxisScale = XAxisScaleMngr.getInstance().getDefaultxAxisScale();
    // return xAxisScale;
    // }
    // return new XAxisScale(xAxisScale);
    // }
    //
    // public void setxAxisScale(XAxisScale xAxisScale) {
    // this.xAxisScale = xAxisScale;
    // }

    public String getGraphKey() {
        return graphKey;
    }

    public void setGraphKey(String graphKey) {
        this.graphKey = graphKey;
    }

    public String getGraphTopTitle() {
        return graphTopTitle;
    }

    public void setGraphTopTitle(String graphTopTitle) {
        this.graphTopTitle = graphTopTitle;
    }

    public Boolean getDisplayXLabels() {
        return displayXLabels;
    }

    public void setDisplayXLabels(Boolean displayXLabels) {
        this.displayXLabels = displayXLabels;
    }

    public Boolean getDisplayLastDataDate() {
        return displayLastDataDate;
    }

    public void setDisplayLastDataDate(Boolean displayLastDataDate) {
        this.displayLastDataDate = displayLastDataDate;
    }

    public String getYData() {
        return yData;
    }

    public void setYData(String yData) {
        this.yData = yData;
    }

    public String getXTitle() {
        return xTitle;
    }

    public void setXTitle(String xTitle) {
        this.xTitle = xTitle;
    }

    public String getYTitle() {
        return yTitle;
    }

    public void setYTitle(String yTitle) {
        this.yTitle = yTitle;
    }

    public Integer getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(Integer lineWidth) {
        this.lineWidth = lineWidth;
    }

    public LineStyle getLineStyle() {
        return lineStyle;
    }

    public void setLineStyle(LineStyle lineStyle) {
        this.lineStyle = lineStyle;
    }

    public String getUnitsFontSize() {
        return unitsFontSize;
    }

    public void setUnitsFontSize(String unitsFontSize) {
        this.unitsFontSize = unitsFontSize;
    }

    public String getUnitsFont() {
        return unitsFont;
    }

    public void setUnitsFont(String unitsFont) {
        this.unitsFont = unitsFont;
    }

    public String getUnitsStyle() {
        return unitsStyle;
    }

    public void setUnitsStyle(String unitsStyle) {
        this.unitsStyle = unitsStyle;
    }

    public String getTitleFontSize() {
        return titleFontSize;
    }

    public void setTitleFontSize(String titleFontSize) {
        this.titleFontSize = titleFontSize;
    }

    public String getTitleFont() {
        return titleFont;
    }

    public void setTitleFont(String titleFont) {
        this.titleFont = titleFont;
    }

    public String getTitleStyle() {
        return titleStyle;
    }

    public void setTitleStyle(String titleStyle) {
        this.titleStyle = titleStyle;
    }

    public GraphAttributes getGraphAttr() {
        GraphAttributes graphAttr = new GraphAttributes(this);
        return graphAttr;
    }

    public void setGraphAttr(GraphAttributes graphAttr) {
        this.graphKey = graphAttr.getGraphKey();
        this.yScaleType = graphAttr.getyScaleType();
        this.yScaleMin = graphAttr.getyScaleMin();
        this.yScaleMax = graphAttr.getyScaleMax();
        this.yInterval = graphAttr.getyInterval();
        this.yNumTicks = graphAttr.getyNumTicks();
        this.graphTopTitle = graphAttr.getGraphTopTitle();
        this.displayLastDataDate = graphAttr.getDisplayLastDataDate();
        this.displayXLabels = graphAttr.getDisplayXLabels();
        this.xTitle = graphAttr.getxTitle();
        this.titleFont = graphAttr.getTitleFont();
        this.titleFontSize = graphAttr.getTitleFontSize();
        this.titleStyle = graphAttr.getTitleStyle();
        this.unitsFont = graphAttr.getUnitsFont();
        this.unitsFontSize = graphAttr.getUnitsFontSize();
        this.unitsStyle = graphAttr.getUnitsStyle();
        this.xAxisScale = graphAttr.getxAxisScale();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (obj instanceof GhcdResourceData == false) {
            return false;
        }

        GhcdResourceData other = (GhcdResourceData) obj;

        if (this.source != null && other.source == null) {
            return false;
        } else if (this.source == null && other.source != null) {
            return false;
        } else if (this.source != null
                && this.source.equals(other.source) == false) {
            return false;
        }

        if (this.instrument != null && other.instrument == null) {
            return false;
        } else if (this.instrument == null && other.instrument != null) {
            return false;
        } else if (this.instrument != null
                && this.instrument.equals(other.instrument) == false) {
            return false;
        }

        if (this.datatype != null && other.datatype == null) {
            return false;
        } else if (this.datatype == null && other.datatype != null) {
            return false;
        } else if (this.datatype != null
                && this.datatype.equals(other.datatype) == false) {
            return false;
        }

        if (this.dataResolUnits != null && other.dataResolUnits == null) {
            return false;
        } else if (this.dataResolUnits == null && other.dataResolUnits != null) {
            return false;
        } else if (this.dataResolUnits != null
                && this.dataResolUnits.equals(other.dataResolUnits) == false) {
            return false;
        }

        if (this.dataResolVal != null && other.dataResolVal == null) {
            return false;
        } else if (this.dataResolVal == null && other.dataResolVal != null) {
            return false;
        } else if (this.dataResolVal != null
                && this.dataResolVal.equals(other.dataResolVal) == false) {
            return false;
        }

        if (this.yData != null && other.yData == null) {
            return false;
        } else if (this.yData == null && other.yData != null) {
            return false;
        } else if (this.yData != null
                && this.yData.equals(other.yData) == false) {
            return false;
        }

        if (this.xTitle != null && other.xTitle == null) {
            return false;
        } else if (this.xTitle == null && other.xTitle != null) {
            return false;
        } else if (this.xTitle != null
                && this.xTitle.equals(other.xTitle) == false) {
            return false;
        }

        if (this.yTitle != null && other.yTitle == null) {
            return false;
        } else if (this.yTitle == null && other.yTitle != null) {
            return false;
        } else if (this.yTitle != null
                && this.yTitle.equals(other.yTitle) == false) {
            return false;
        }

        if (this.yTitlePosition != null && other.yTitlePosition == null) {
            return false;
        } else if (this.yTitlePosition == null && other.yTitlePosition != null) {
            return false;
        } else if (this.yTitlePosition != null
                && this.yTitlePosition.equals(other.yTitlePosition) == false) {
            return false;
        }

        if (this.graphKey != null && other.graphKey == null) {
            return false;
        } else if (this.graphKey == null && other.graphKey != null) {
            return false;
        } else if (this.graphKey != null
                && this.graphKey.equals(other.graphKey) == false) {
            return false;
        }

        if (this.yScaleType != null && other.yScaleType == null) {
            return false;
        } else if (this.yScaleType == null && other.yScaleType != null) {
            return false;
        } else if (this.yScaleType != null
                && this.yScaleType.equals(other.yScaleType) == false) {
            return false;
        }

        if (this.yScaleMin != null && other.yScaleMin == null) {
            return false;
        } else if (this.yScaleMin == null && other.yScaleMin != null) {
            return false;
        } else if (this.yScaleMin != null
                && this.yScaleMin.equals(other.yScaleMin) == false) {
            return false;
        }

        if (this.yScaleMax != null && other.yScaleMax == null) {
            return false;
        } else if (this.yScaleMax == null && other.yScaleMax != null) {
            return false;
        } else if (this.yScaleMax != null
                && this.yScaleMax.equals(other.yScaleMax) == false) {
            return false;
        }

        if (this.yInterval != null && other.yInterval == null) {
            return false;
        } else if (this.yInterval == null && other.yInterval != null) {
            return false;
        } else if (this.yInterval != null
                && this.yInterval.equals(other.yInterval) == false) {
            return false;
        }

        if (this.yNumTicks != null && other.yNumTicks == null) {
            return false;
        } else if (this.yNumTicks == null && other.yNumTicks != null) {
            return false;
        } else if (this.yNumTicks != null
                && this.yNumTicks.equals(other.yNumTicks) == false) {
            return false;
        }

        if (this.yUnits != null && other.yUnits == null) {
            return false;
        } else if (this.yUnits == null && other.yUnits != null) {
            return false;
        } else if (this.yUnits != null
                && this.yUnits.equals(other.yUnits) == false) {
            return false;
        }

        if (this.yUnitsPosition != null && other.yUnitsPosition == null) {
            return false;
        } else if (this.yUnitsPosition == null && other.yUnitsPosition != null) {
            return false;
        } else if (this.yUnitsPosition != null
                && this.yUnitsPosition.equals(other.yUnitsPosition) == false) {
            return false;
        }

        if (this.dataColor != null && other.dataColor == null) {
            return false;
        } else if (this.dataColor == null && other.dataColor != null) {
            return false;
        } else if (this.dataColor != null
                && this.dataColor.equals(other.dataColor) == false) {
            return false;
        }

        if (this.graphTopTitle != null && other.graphTopTitle == null) {
            return false;
        } else if (this.graphTopTitle == null && other.graphTopTitle != null) {
            return false;
        } else if (this.graphTopTitle != null
                && this.graphTopTitle.equals(other.graphTopTitle) == false) {
            return false;
        }

        if (this.displayXLabels != null && other.displayXLabels == null) {
            return false;
        } else if (this.displayXLabels == null && other.displayXLabels != null) {
            return false;
        } else if (this.displayXLabels != null
                && this.displayXLabels.equals(other.displayXLabels) == false) {
            return false;
        }

        if (this.displayLastDataDate != null
                && other.displayLastDataDate == null) {
            return false;
        } else if (this.displayLastDataDate == null
                && other.displayLastDataDate != null) {
            return false;
        } else if (this.displayLastDataDate != null
                && this.displayLastDataDate.equals(other.displayLastDataDate) == false) {
            return false;
        }

        if (this.xAxisScale != null && other.xAxisScale == null) {
            return false;
        } else if (this.xAxisScale == null && other.xAxisScale != null) {
            return false;
        } else if (this.xAxisScale != null
                && this.xAxisScale.equals(other.xAxisScale) == false) {
            return false;
        }

        if (this.lineStyle != null && other.lineStyle == null) {
            return false;
        } else if (this.lineStyle == null && other.lineStyle != null) {
            return false;
        } else if (this.lineStyle != null
                && this.lineStyle.equals(other.lineStyle) == false) {
            return false;
        }

        if (this.lineWidth != null && other.lineWidth == null) {
            return false;
        } else if (this.lineWidth == null && other.lineWidth != null) {
            return false;
        } else if (this.lineWidth != null
                && this.lineWidth.equals(other.lineWidth) == false) {
            return false;
        }

        if (this.unitsFont != null && other.unitsFont == null) {
            return false;
        } else if (this.unitsFont == null && other.unitsFont != null) {
            return false;
        } else if (this.unitsFont != null
                && this.unitsFont.equals(other.unitsFont) == false) {
            return false;
        }

        if (this.unitsFontSize != null && other.unitsFontSize == null) {
            return false;
        } else if (this.unitsFontSize == null && other.unitsFontSize != null) {
            return false;
        } else if (this.unitsFontSize != null
                && this.unitsFontSize.equals(other.unitsFontSize) == false) {
            return false;
        }

        if (this.unitsStyle != null && other.unitsStyle == null) {
            return false;
        } else if (this.unitsStyle == null && other.unitsStyle != null) {
            return false;
        } else if (this.unitsStyle != null
                && this.unitsStyle.equals(other.unitsStyle) == false) {
            return false;
        }

        if (this.titleFont != null && other.titleFont == null) {
            return false;
        } else if (this.titleFont == null && other.titleFont != null) {
            return false;
        } else if (this.titleFont != null
                && this.titleFont.equals(other.titleFont) == false) {
            return false;
        }

        if (this.titleFontSize != null && other.titleFontSize == null) {
            return false;
        } else if (this.titleFontSize == null && other.titleFontSize != null) {
            return false;
        } else if (this.titleFontSize != null
                && this.titleFontSize.equals(other.titleFontSize) == false) {
            return false;
        }

        if (this.titleStyle != null && other.titleStyle == null) {
            return false;
        } else if (this.titleStyle == null && other.titleStyle != null) {
            return false;
        } else if (this.titleStyle != null
                && this.titleStyle.equals(other.titleStyle) == false) {
            return false;
        }

        return true;
    }

    public String getyData() {
        return yData;
    }

    public void setyData(String yData) {
        this.yData = yData;
    }

    public String getxTitle() {
        return xTitle;
    }

    public void setxTitle(String xTitle) {
        this.xTitle = xTitle;
    }

    public String getyTitle() {
        return yTitle;
    }

    public void setyTitle(String yTitle) {
        this.yTitle = yTitle;
    }

    public String getyTitlePosition() {
        return yTitlePosition;
    }

    public void setyTitlePosition(String yTitlePosition) {
        this.yTitlePosition = yTitlePosition;
    }

    public String getyScaleType() {
        return yScaleType;
    }

    public void setyScaleType(String yScaleType) {
        this.yScaleType = yScaleType;
    }

    public Float getyScaleMin() {
        return yScaleMin;
    }

    public void setyScaleMin(Float yScaleMin) {
        this.yScaleMin = yScaleMin;
    }

    public Float getyScaleMax() {
        return yScaleMax;
    }

    public void setyScaleMax(Float yScaleMax) {
        this.yScaleMax = yScaleMax;
    }

    public Float getyInterval() {
        return yInterval;
    }

    public void setyInterval(Float yInterval) {
        this.yInterval = yInterval;
    }

    public Integer getyNumTicks() {
        return yNumTicks;
    }

    public void setyNumTicks(Integer yNumTicks) {
        this.yNumTicks = yNumTicks;
    }

    public String getyUnits() {
        return yUnits;
    }

    public void setyUnits(String yUnits) {
        this.yUnits = yUnits;
    }

    public String getyUnitsPosition() {
        return yUnitsPosition;
    }

    public void setyUnitsPosition(String yUnitsPosition) {
        this.yUnitsPosition = yUnitsPosition;
    }

    public XAxisScale getxAxisScale() {
        return xAxisScale;
    }

    public void setxAxisScale(XAxisScale xAxisScale) {
        this.xAxisScale = xAxisScale;
    }

}
