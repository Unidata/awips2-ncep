package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import com.raytheon.uf.viz.core.rsc.LoadProperties;

/**
 * Provides GINI satellite raster rendering support.
 * 
 * <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 *  Date         Ticket#     Engineer    Description
 *  ------------ ----------  ----------- --------------------------
 *  05/24/2010    #281      ghull        Initial creation 
 *  06/07/2012    #717      archana      Added the methods getImageTypeNumber(),
 *                                       getParameterList(), getLocFilePathForImageryStyleRule()
 *                                       removed the overridden method resourceChanged(), 
 *                                       since it is implemented in the base class.
 *  02/13/2015    R6345     mkean        add sectorID and getRscAttrSetName 
 *                                       to legendStr
 *  06/01/2016    R18511    kbugenhagen  Refactored to use NcSatelliteResource.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1
 */

public class GiniSatResource extends NcSatelliteResource {

    private final static String GINI_SAT_PHYSICAL_ELEMENT_NAME = "Imager 11 micron IR";

    public GiniSatResource(SatelliteResourceData data, LoadProperties props) {
        super(data, props);
    }

    @Override
    public boolean isCloudHeightCompatible() {
        return GINI_SAT_PHYSICAL_ELEMENT_NAME.equals(physicalElement);
    }

}
