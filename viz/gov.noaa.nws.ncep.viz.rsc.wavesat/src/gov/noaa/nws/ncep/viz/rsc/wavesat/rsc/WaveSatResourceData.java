package gov.noaa.nws.ncep.viz.rsc.wavesat.rsc;

import gov.noaa.nws.ncep.viz.common.RGBColorAdapter;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.IDataLoader;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.ui.display.ColorBar;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractNameGenerator;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

/**
 * Resource data for WaveSat data.
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *  09/21/2011    #248      Greg Hull    Initial creation.
 *  04/23/2015   R6281      B. Hebbard   Support both WaveSat and WaveSatV
 *  06/01/2015   R6281      B. Hebbard   Add Jason-3; update satelliteId for CryoSat-2
 *  06/16/2015   R6281      B. Hebbard   Update per code review comments
 *  12/03/2015   R11819     S. Russell   Added the getDataLoader() funciton.
 *                                       Modified constructResource() to
 *                                       compensate for the deletion of
 *                                       WaveSatResource and WaveSatVResource,
 *                                       as well as the renaming of
 *                                       AbstractWaveSatResource to just
 *                                       WaveSatResource
 *  11/05/2015    5070      randerso     Adjust font sizes for dpi scaling
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1.0
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "NC-WaveSatResourceData")
public class WaveSatResourceData extends
        AbstractNatlCntrsRequestableResourceData implements
        INatlCntrsResourceData {

    @XmlElement
    private boolean useFeetInsteadOfMeters = false;

    @XmlElement
    private ColorBar colorBarForMeters = null;

    @XmlElement
    private ColorBar colorBarForFeet = null;

    @XmlElement
    private Integer timeDisplayInterval = null; // in minutes

    @XmlElement
    @XmlJavaTypeAdapter(RGBColorAdapter.class)
    private RGB timeDisplayColor;

    @XmlElement
    private String fontName = "Times";

    @XmlElement
    private Integer fontSize = 10;

    @XmlElement
    private String latParam = null;
    @XmlElement
    private String latParamInPdo = null;
    @XmlElement
    private String lonParam = null;
    @XmlElement
    private String lonParamInPdo = null;
    @XmlElement
    private String satIdParam = null;
    @XmlElement
    private String satIdParamInPdo = null;
    @XmlElement
    private String waveHeightParam = null;
    @XmlElement
    private String windSpeedParam = null;

    private static final IUFStatusHandler logger = UFStatus
            .getHandler(WaveSatResourceData.class);

    // Note that the following are also in WaveSatResource & WaveSatVResource
    private final static String[] satelliteIdParamNames = { "satelliteId",
            "said" };

    private enum Satellite {
        // Consider moving this to common if useful elsewhere
        // @formatter:off
        CRYOSAT_2(47, "CryoSat-2"), ENVISAT(60, "Envisat"), JASON_1(260,
                "Jason-1"), JASON_2(261, "Jason-2"), JASON_3(262, "Jason-3"), ALTIKA(
                441, "AltiKa"),
        // TODO: Check decoder. Stores following as ID for CryoSat-2; expecting
        // 47 instead
        CRYOSAT_2_AS_UNKNOWN(-9999998, "CryoSat-2");
        // @formatter:on

        private int id;

        private String displayName;

        private Satellite(int id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public static String getDisplayNameForId(int id) {
            for (Satellite sat : values()) {
                if (sat.id == id) {
                    return sat.displayName;
                }
            }
            return "SatelliteId=" + Integer.toString(id);
        }
    }

    private WaveSatResource resource = null;

    public WaveSatResourceData() {
        super();
        this.nameGenerator = new AbstractNameGenerator() {
            @Override
            public String getName(AbstractVizResource<?, ?> resource) {
                return "Significant Wave Height ("
                        + (useFeetInsteadOfMeters ? "feet" : "meters") + ") "
                        + getSatelliteName();
            }
        };

        if (colorBarForMeters == null) {
            colorBarForMeters = new ColorBar();
            colorBarForMeters.addColorBarInterval(0.0f,
                    Float.POSITIVE_INFINITY, new RGB(0, 255, 0));
        }

        if (colorBarForFeet == null) {
            colorBarForFeet = new ColorBar();
            colorBarForFeet.addColorBarInterval(0.0f, Float.POSITIVE_INFINITY,
                    new RGB(0, 255, 0));
        }

    }

    public String getLatParam() {
        return latParam;
    }

    public void setLatParam(String latParam) {
        this.latParam = latParam;
    }

    @Override
    public void update(Object updateData) {
    }

    public int getSatelliteId() {
        for (String paramName : satelliteIdParamNames) {
            if (getMetadataMap().containsKey(paramName)) {
                return Integer.parseInt(getMetadataMap().get(paramName)
                        .getConstraintValue());
            }
        }
        return 0;
    }

    public String getSatelliteName() {
        return Satellite.getDisplayNameForId(getSatelliteId());
    }

    public RGB getTimeDisplayColor() {
        return timeDisplayColor;
    }

    public void setTimeDisplayColor(RGB timeDisplayColor) {
        this.timeDisplayColor = timeDisplayColor;
    }

    public Integer getTimeDisplayInterval() {
        return timeDisplayInterval;
    }

    public void setTimeDisplayInterval(Integer timeDisplayInterval) {
        this.timeDisplayInterval = timeDisplayInterval;
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public ColorBar getColorBarForMeters() {
        return colorBarForMeters;
    }

    public void setColorBarForMeters(ColorBar colorBar) {
        this.colorBarForMeters = colorBar;
    }

    public ColorBar getColorBarForFeet() {
        return colorBarForFeet;
    }

    public void setColorBarForFeet(ColorBar colorBar) {
        this.colorBarForFeet = colorBar;
    }

    public Boolean getUseFeetInsteadOfMeters() {
        return useFeetInsteadOfMeters;
    }

    public void setUseFeetInsteadOfMeters(Boolean useFeetInsteadOfMeters) {
        this.useFeetInsteadOfMeters = useFeetInsteadOfMeters;
    }

    public IDataLoader getDataLoader() {

        IDataLoader waveSatDataLoader = new WaveSatDataLoader(this.latParam,
                this.lonParam, this.satIdParam, this.waveHeightParam,
                this.windSpeedParam);

        return waveSatDataLoader;

    }

    @Override
    protected AbstractVizResource<?, ?> constructResource(
            LoadProperties loadProperties, PluginDataObject[] objects)
            throws VizException {

        String pluginName = "";
        if (getMetadataMap().containsKey("pluginName")) {
            pluginName = getMetadataMap().get("pluginName")
                    .getConstraintValue();
        }

        if ((pluginName.equals("sgwh")) || (pluginName.equals("sgwhv"))) {
            logger.debug("Creating a WaveSatResourse object with plugin: "
                    + pluginName);
            resource = new WaveSatResource(this, loadProperties);
        } else {
            logger.debug("Unrecognized EDEX pluginName: " + pluginName);
        }

        if (resource != null) {
            // These values come from WaveSat.xml or WaveSatV.xml depending
            // on the plugin named used. sgwhv is mostly defunct.

            // By the time WaveSatResourceData is created it is initialized
            // with the correct WaveSat*.xml per the appropriate plugin in
            // use.

            // This conditional replaces the existence of WaveSatResource.java
            // WaveSatVResource.java which were both children of
            // AbstractWaveSatResource.java. Those classes were empty of
            // everything except a constructor where these string values
            // were set. After those classes were removed
            // AbstractWaveSatResource got renamed to just WaveSatResoure.
            // Folling are comments from the constructors of those delted
            // classes in regards to these strings:

            // "Param" strings must match those in:
            // gov.noaa.nws.ncep.edex.plugin.sgwh/res/pointdata/sgwh.xml

            // "ParamInPdo" strings must match those in PDO class:
            // gov.noaa.nws.ncep.common.dataplugin.sgwh.SgwhRecord

            // Mappings between the two are defined in class:
            // gov.noaa.nws.ncep.common.dataplugin.sgwh.SgwhPointDataTransform

            resource.setLatParam(this.latParam);
            resource.setLatParamInPdo(this.latParamInPdo);
            resource.setLonParam(this.lonParam);
            resource.setLonParamInPdo(this.lonParamInPdo);
            resource.setSatIdParam(this.satIdParam);
            resource.setSatIdParamInPdo(this.satIdParamInPdo);
            resource.setWaveHeightParam(this.waveHeightParam);
            resource.setWindSpeedParam(this.windSpeedParam);
        }

        return resource;
    }

    public WaveSatResource getResource() {
        return resource;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (obj instanceof WaveSatResourceData == false) {
            return false;
        }

        WaveSatResourceData other = (WaveSatResourceData) obj;

        if (!colorBarForMeters.equals(other.colorBarForMeters)
                || !colorBarForFeet.equals(other.colorBarForFeet)
                || useFeetInsteadOfMeters != other.useFeetInsteadOfMeters
                || !fontName.equals(other.fontName)
                || fontSize != other.fontSize
                || !timeDisplayColor.equals(other.timeDisplayColor)
                || timeDisplayInterval != other.timeDisplayInterval) {
            return false;
        }

        return true;
    }

    public String getLatParamInPdo() {
        return latParamInPdo;
    }

    public void setLatParamInPdo(String latParamInPdo) {
        this.latParamInPdo = latParamInPdo;
    }

    public String getLonParam() {
        return lonParam;
    }

    public void setLonParam(String lonParam) {
        this.lonParam = lonParam;
    }

    public String getLonParamInPdo() {
        return lonParamInPdo;
    }

    public void setLonParamInPdo(String lonParamInPdo) {
        this.lonParamInPdo = lonParamInPdo;
    }

    public String getSatIdParam() {
        return satIdParam;
    }

    public void setSatIdParam(String satIdParam) {
        this.satIdParam = satIdParam;
    }

    public String getSatIdParamInPdo() {
        return satIdParamInPdo;
    }

    public void setSatIdParamInPdo(String satIdParamInPdo) {
        this.satIdParamInPdo = satIdParamInPdo;
    }

    public String getWaveHeightParam() {
        return waveHeightParam;
    }

    public void setWaveHeightParam(String waveHeightParam) {
        this.waveHeightParam = waveHeightParam;
    }

    public String getWindSpeedParam() {
        return windSpeedParam;
    }

    public void setWindSpeedParam(String windSpeedParam) {
        this.windSpeedParam = windSpeedParam;
    }

}
