package gov.noaa.nws.ncep.viz.rsc.viirs.rsc;

import gov.noaa.nws.ncep.viz.common.area.AreaName.AreaSource;
import gov.noaa.nws.ncep.viz.rsc.satellite.rsc.SatelliteResourceData;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
 * 07/06/2016      R17376  kbugenhagen Overrode getMethods method to get list
 *                                     of methods for this class and its superclass.
 * 
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

    /*
     * Since this class subclasses the SatelliteResourceData class, this method
     * needs to get not only its methods but those of its superclass.
     * 
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData
     * #getMethods()
     */
    @Override
    protected Method[] getMethods() {
        Set<Method> allMethods = new HashSet<>();
        Method[] superMethods = this.getClass().getSuperclass()
                .getDeclaredMethods();
        allMethods.addAll(Arrays.asList(superMethods));
        Method[] methods = this.getClass().getDeclaredMethods();
        allMethods.addAll(Arrays.asList(methods));
        return allMethods.toArray(new Method[allMethods.size()]);
    }

}