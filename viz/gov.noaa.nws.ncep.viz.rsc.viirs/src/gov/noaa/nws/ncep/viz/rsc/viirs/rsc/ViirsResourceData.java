package gov.noaa.nws.ncep.viz.rsc.viirs.rsc;

import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractNameGenerator;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

/**
 * 
 * ViirsResourceData - Class for display of VIIRS satellite data
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 23, 2014 R4644      Yukuan Song     Initial creation
 * 
 * </pre>
 * 
 * @author yusong
 * @version 1.0
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "NcViirsResourceData")
public class ViirsResourceData extends AbstractNatlCntrsRequestableResourceData
        implements INatlCntrsResourceData {

    // ============================
    enum VSatelliteType {
        GINI, MCIDAS, VIIRS
    }

    @XmlElement
    private Float alpha;

    @XmlElement
    private Float brightness;

    @XmlElement
    private Float contrast;

    @XmlElement
    private String colorMapName;

    @XmlElement
    private ColorBarFromColormap colorBar;

    @XmlElement
    private VSatelliteType satelliteType;

    @XmlElement
    private String displayUnitStr;

    public Float getAlpha() {
        return alpha;
    }

    public void setAlpha(Float alpha) {
        this.alpha = alpha;
    }

    public Float getBrightness() {
        return brightness;
    }

    public void setBrightness(Float brightness) {
        this.brightness = brightness;
    }

    public Float getContrast() {
        return contrast;
    }

    public void setContrast(Float contrast) {
        this.contrast = contrast;
    }

    public String getColorMapName() {
        return colorMapName;
    }

    public void setColorMapName(String colorMapName) {
        this.colorMapName = colorMapName;
    }

    public ColorBarFromColormap getColorBar() {
        return colorBar;
    }

    public void setColorBar(ColorBarFromColormap colorBar) {
        this.colorBar = colorBar;
    }

    public VSatelliteType getSatelliteType() {
        return satelliteType;
    }

    public void setSatelliteType(VSatelliteType satelliteType) {
        this.satelliteType = satelliteType;
    }

    public String getDisplayUnitStr() {
        return displayUnitStr;
    }

    public void setDisplayUnitStr(String displayUnitStr) {
        this.displayUnitStr = displayUnitStr;
    }

    /**
     * Create a VIIRS resource.
     * 
     * @throws VizException
     */
    public ViirsResourceData() throws VizException {
        super();
        this.nameGenerator = new AbstractNameGenerator() {

            @Override
            public String getName(AbstractVizResource<?, ?> resource) {
                String s = "NC VIIRS Resource";
                return s;
            }
        };
    }

    @Override
    protected AbstractVizResource<?, ?> constructResource(
            LoadProperties loadProperties, PluginDataObject[] objects)
            throws VizException {
        return new ViirsResource(this, loadProperties);
    }
}