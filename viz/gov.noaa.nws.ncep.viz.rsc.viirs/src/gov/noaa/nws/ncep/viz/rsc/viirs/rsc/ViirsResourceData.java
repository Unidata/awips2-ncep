package gov.noaa.nws.ncep.viz.rsc.viirs.rsc;

import gov.noaa.nws.ncep.viz.common.area.AreaName.AreaSource;
import gov.noaa.nws.ncep.viz.rsc.satellite.rsc.SatelliteResourceData;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
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
 * 06/01/2016      R18511  kbugenhagen Initial creation.  This is only
 *                                     needed to satisfy a cyclical RPM build
 *                                     dependency on the npp viirs project and nsharp.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "NcViirsResourceData")
public class ViirsResourceData extends SatelliteResourceData {

    @Override
    protected AbstractVizResource<?, ?> constructResource(
            LoadProperties loadProperties, PluginDataObject[] objects) {

        return new ViirsSatResource(this, loadProperties);
    }

    @Override
    public AreaSource getSourceProvider() {
        return AreaSource.getAreaSource("UNKNOWN");
    }

}