package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import java.io.File;
import java.text.ParsePosition;

import javax.measure.Unit;
import javax.measure.format.ParserException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.viz.core.rsc.AbstractNameGenerator;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

import gov.noaa.nws.ncep.viz.common.area.AreaName.AreaSource;
import gov.noaa.nws.ncep.viz.common.area.IAreaProviderCapable;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.IDataLoader;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;
import tec.uom.se.format.SimpleUnitFormat;

/**
 * Resource data for satellite data
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 07/10/2009              mgao        Initial creation
 * 07/31/2009              ghull       to11 migration
 * 08/26/2009              ghull       Integrate with AbstractNatlCntrsResource
 * 04/15/2010      #259    ghull       Added ColorBar
 * 11/20/2012      #630    ghull       implement IGridGeometryProvider
 * 05/08/2013      #892    ghull       change to IAreaProviderCapable
 * 09/28/2015      11385   njensen     construct NcSatelliteResource instead of GiniSatResource
 * 10/15/2015      R7190   R. Reynolds Added support for Mcidas
 * 10/28/2015      R7190   kbugenhagen Added support for Himawari
 * 04/12/2016      R16367  kbugenhagen Added support for SIMGOESR
 * 04/13/2016      R15954  S Russell   Added method getDataLoader()
 * 06/01/2016      R18511  kbugenhagen Added support for MODIS, removed system.out's
 * 
 * This class is copied from com.raytheon.viz.satellite.rsc.SatResourceData
 * for TO 11 integration
 * 
 * </pre>
 * 
 * @author mgao
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "NcSatelliteResourceData")
public class SatelliteResourceData extends
        AbstractNatlCntrsRequestableResourceData implements
        IAreaProviderCapable {

    enum SatelliteType {
        GINI, MCIDAS, HIMAWARI, SIMGOESR, MODIS
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
    protected SatelliteType satelliteType;

    @XmlElement
    private String displayUnitStr;

    private Unit<?> displayUnit;

    public SatelliteResourceData() {
        super();

        displayUnit = null;
        displayUnitStr = null;

        this.nameGenerator = new AbstractNameGenerator() {
            @Override
            public String getName(AbstractVizResource<?, ?> resource) {
                return resource.getName();
            }
        };

    }

    @Override
    protected AbstractVizResource<?, ?> constructResource(
            LoadProperties loadProperties, PluginDataObject[] objects) {
        if (satelliteType == SatelliteType.GINI) {
            return new GiniSatResource(this, loadProperties);
        } else if (satelliteType == SatelliteType.MODIS) {
            return new ModisSatResource(this, loadProperties);
        } else if (satelliteType == SatelliteType.MCIDAS) {
            return new McidasSatResource(this, loadProperties);
        } else if (satelliteType == SatelliteType.HIMAWARI) {
            return new NcSatelliteResource(this, loadProperties);
        } else if (satelliteType == SatelliteType.SIMGOESR) {
            return new NcSatelliteResource(this, loadProperties);
        } else {
            statusHandler.error("Unrecognized satellite type: "
                    + satelliteType.toString());
            return null;
        }

    }

    public Unit<?> getDisplayUnit() {
        if (displayUnit == null) {
            setDisplayUnitStr(displayUnitStr);
        }
        return displayUnit;
    }

    public String getDisplayUnitStr() {
        return displayUnitStr;
    }

    public void setDisplayUnitStr(String dispUnitStr) {
        this.displayUnitStr = dispUnitStr;

        if (displayUnit == null) {
            if (displayUnitStr != null) {
                try {
                    displayUnit = SimpleUnitFormat.getInstance(SimpleUnitFormat.Flavor.ASCII).parseSingleUnit(
                            displayUnitStr, new ParsePosition(0));
                } catch (ParserException e) {
                    statusHandler.error("Unable parse display units : "
                            + displayUnitStr, e);
                }
            }
        }
    }

    public String getColorMapName() {
        return colorMapName;
    }

    public void setColorMapName(String cmapName) {
        colorMapName = cmapName;
    }

    public ColorBarFromColormap getColorBar() {
        return colorBar;
    }

    public void setColorBar(ColorBarFromColormap cBar) {
        this.colorBar = cBar;
    }

    public Float getAlpha() {
        return alpha;
    }

    public void setAlpha(Float alpha) {
        this.alpha = alpha;
    }

    public SatelliteType getSatelliteType() {
        return satelliteType;
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

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (obj instanceof SatelliteResourceData == false) {
            return false;
        }

        SatelliteResourceData other = (SatelliteResourceData) obj;

        if (this.colorMapName != null && other.colorMapName == null) {
            return false;
        } else if (this.colorMapName == null && other.colorMapName != null) {
            return false;
        } else if (this.colorMapName != null
                && this.colorMapName.equals(other.colorMapName) == false) {
            return false;
        }

        if (this.displayUnitStr != null && other.displayUnitStr == null) {
            return false;
        } else if (this.displayUnitStr == null && other.displayUnitStr != null) {
            return false;
        } else if (this.displayUnitStr != null
                && this.displayUnitStr.equals(other.displayUnitStr) == false) {
            return false;
        }

        if (this.satelliteType != other.satelliteType) {
            return false;
        }

        if ((this.alpha != null && other.alpha == null)
                || (this.alpha == null && other.alpha != null)
                || (this.alpha != null && this.alpha.equals(other.alpha) == false)) {
            return false;

        }

        if ((this.brightness != null && other.brightness == null)
                || (this.brightness == null && other.brightness != null)
                || (this.brightness != null && this.brightness
                        .equals(other.brightness) == false)) {
            return false;

        }

        if ((this.contrast != null && other.contrast == null)
                || (this.contrast == null && other.contrast != null)
                || (this.contrast != null && this.contrast
                        .equals(other.contrast) == false)) {
            return false;
        }

        return true;
    }

    // this needs to match the source names in the areaProvider ext point.
    @Override
    public AreaSource getSourceProvider() {
        switch (satelliteType) {
        case MCIDAS:
            return AreaSource.getAreaSource("MCIDAS_AREA_NAME");
        case GINI:
            return AreaSource.getAreaSource("GINI_SECTOR_ID");
        default:
            return AreaSource.getAreaSource("UNKNOWN");
        }
    }

    /*
     * NOTE : if reading from the areaNames table then the VAAC satellite is
     * part of the areaname in the table but if reading the main table then the
     * area is just the area name.
     */
    @Override
    public String getAreaName() {
        String areaName = "xxx";
        if (satelliteType == SatelliteType.MCIDAS) {
            return metadataMap.get("satelliteId").getConstraintValue()
                    + File.separator
                    + metadataMap.get("areaId").getConstraintValue();
        } else if (satelliteType == SatelliteType.GINI) {
            return metadataMap.get("creatingEntity").getConstraintValue()
                    + File.separator
                    + metadataMap.get("sectorID").getConstraintValue();
        }
        return areaName;
    }

    public IDataLoader getDataLoader() {
        return new NcSatelliteDataLoader(this);
    }

}
