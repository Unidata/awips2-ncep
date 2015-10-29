package gov.noaa.nws.ncep.viz.rsc.modis.rsc;

import gov.noaa.nws.ncep.common.dataplugin.modis.ModisRecord;
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
 * Class for display of MODIS satellite data
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 10/01/2014   R5116      kbugenhagen  Initial creation.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 * @version 1.0
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "NcModisResourceData")
public class ModisResourceData extends AbstractNatlCntrsRequestableResourceData
        implements INatlCntrsResourceData {

    public ModisRecord[] records;

    // ============================
    enum MSatelliteType {
        MODIS
    }

    @XmlElement
    private Float alpha;

    @XmlElement
    private Float brightness;

    @XmlElement
    private Float contrast;

    @XmlElement
    private Float startTime;

    @XmlElement
    private String colorMapName;

    @XmlElement
    private ColorBarFromColormap colorBar;

    @XmlElement
    private MSatelliteType satelliteType;

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

    public Float getStartTime() {
        return startTime;
    }

    public void setStartTime(Float startTime) {
        this.startTime = startTime;
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

    public MSatelliteType getSatelliteType() {
        return satelliteType;
    }

    public void setSatelliteType(MSatelliteType satelliteType) {
        this.satelliteType = satelliteType;
    }

    public String getDisplayUnitStr() {
        return displayUnitStr;
    }

    public void setDisplayUnitStr(String displayUnitStr) {
        this.displayUnitStr = displayUnitStr;
    }

    /**
     * Create a MODIS resource.
     * 
     * @throws VizException
     */
    public ModisResourceData() throws VizException {
        super();
        this.nameGenerator = new AbstractNameGenerator() {

            @Override
            public String getName(AbstractVizResource<?, ?> resource) {
                String s = "NC MODIS Resource";
                return s;
            }
        };
    }

    @Override
    protected AbstractVizResource<?, ?> constructResource(
            LoadProperties loadProperties, PluginDataObject[] objects)
            throws VizException {
        return new ModisResource(this, loadProperties);
    }

    public ModisRecord[] getRecords() {
        return records;
    }

    public void setRecords(ModisRecord[] records) {
        this.records = records;
    }

}