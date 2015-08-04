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

import gov.noaa.nws.ncep.viz.common.tsScaleMngr.XAxisScale;
import gov.noaa.nws.ncep.viz.common.tsScaleMngr.XAxisScaleMngr;
import gov.noaa.nws.ncep.viz.rsc.ghcd.util.GhcdUtil;

/**
 * Class to represent ghcd graph attributes.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Oct 27, 2014   R5097     sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class GraphAttributes {

    private String graphKey = "";

    private String graphTopTitle = "";

    private String xTitle = "";

    private String yScaleType = GhcdUtil.YSCALE_TYPE_LINEAR;

    private Float yScaleMin = 0.0f;

    private Float yScaleMax = 1000.0f;;

    private Float yInterval = 200.0f;

    private Integer yNumTicks = 11;

    protected XAxisScale xAxisScale = null;

    private Boolean displayXLabels = true;

    private Boolean displayLastDataDate = true;

    private String unitsFontSize = "14";

    private String unitsFont = "Times";

    private String unitsStyle = "Bold";

    private String titleFontSize = "14";

    private String titleFont = "Times";

    private String titleStyle = "Bold";

    public GraphAttributes() {

    }

    public GraphAttributes(GhcdResourceData rscData) {
        this.graphKey = rscData.getGraphKey();
        this.yScaleType = rscData.getYScaleType();
        this.yScaleMin = rscData.getYScaleMin();
        this.yScaleMax = rscData.getYScaleMax();
        this.yInterval = rscData.getYInterval();
        this.yNumTicks = rscData.getYNumTicks();
        this.graphTopTitle = rscData.getGraphTopTitle();
        this.displayLastDataDate = rscData.getDisplayLastDataDate();
        this.displayXLabels = rscData.getDisplayXLabels();
        this.xTitle = rscData.getXTitle();
        this.titleFont = rscData.getTitleFont();
        this.titleFontSize = rscData.getTitleFontSize();
        this.titleStyle = rscData.getTitleStyle();
        this.unitsFont = rscData.getUnitsFont();
        this.unitsFontSize = rscData.getUnitsFontSize();
        this.unitsStyle = rscData.getUnitsStyle();
        this.xAxisScale = rscData.getXAxisScale();
    }

    public String getxTitle() {
        return xTitle;
    }

    public void setxTitle(String xTitle) {
        this.xTitle = xTitle;
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

    public XAxisScale getxAxisScale() {
        if (xAxisScale == null) {
            xAxisScale = XAxisScaleMngr.getInstance().getDefaultxAxisScale();
            return xAxisScale;
        }
        return new XAxisScale(xAxisScale);
    }

    public void setxAxisScale(XAxisScale xAxisScale) {
        this.xAxisScale = xAxisScale;
    }

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

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (obj instanceof GraphAttributes == false) {
            return false;
        }

        GraphAttributes other = (GraphAttributes) obj;

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

        if (this.graphTopTitle != null && other.graphTopTitle == null) {
            return false;
        } else if (this.graphTopTitle == null && other.graphTopTitle != null) {
            return false;
        } else if (this.graphTopTitle != null
                && this.graphTopTitle.equals(other.graphTopTitle) == false) {
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

}
