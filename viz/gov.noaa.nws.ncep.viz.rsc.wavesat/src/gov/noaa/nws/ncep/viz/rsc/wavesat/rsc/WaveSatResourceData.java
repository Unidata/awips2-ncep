package gov.noaa.nws.ncep.viz.rsc.wavesat.rsc;

import gov.noaa.nws.ncep.viz.common.RGBColorAdapter;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.ui.display.ColorBar;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
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

    // Note that the following are also in WaveSatResource & WaveSatVResource
    private final static String[] satelliteIdParamNames = { "satelliteId",
            "said" };

    private enum Satellite {
        // Consider moving this to common if useful elsewhere
        // @formatter:off
        CRYOSAT_2 ( 47, "CryoSat-2"),
        ENVISAT   ( 60, "Envisat"),
        JASON_1   (260, "Jason-1"),
        JASON_2   (261, "Jason-2"),
        JASON_3   (262, "Jason-3"),
        ALTIKA    (441, "AltiKa"),
        // TODO: Check decoder. Stores following as ID for CryoSat-2; expecting 47 instead
        CRYOSAT_2_AS_UNKNOWN (-9999998, "CryoSat-2");
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

    private AbstractWaveSatResource resource = null;

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

    @Override
    protected AbstractVizResource<?, ?> constructResource(
            LoadProperties loadProperties, PluginDataObject[] objects)
            throws VizException {

        String pluginName = "";
        if (getMetadataMap().containsKey("pluginName")) {
            pluginName = getMetadataMap().get("pluginName")
                    .getConstraintValue();
        }

        if (pluginName.equals("sgwh")) {
            resource = new WaveSatResource(this, loadProperties);
        } else if (pluginName.equals("sgwhv")) {
            resource = new WaveSatVResource(this, loadProperties);
        } else {
            System.out.println("Unrecognized EDEX pluginName: " + pluginName);
        }
        return resource;
    }

    public AbstractWaveSatResource getResource() {
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
}
