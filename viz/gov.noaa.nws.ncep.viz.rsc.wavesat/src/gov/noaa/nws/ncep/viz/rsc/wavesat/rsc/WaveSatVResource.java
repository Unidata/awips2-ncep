package gov.noaa.nws.ncep.viz.rsc.wavesat.rsc;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;

import com.raytheon.uf.viz.core.rsc.LoadProperties;

/**
 * WaveSatVResource - Display SGWHV data.
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *  04/27/2015   R6281     B. Hebbard  Initial creation.  New class to support
 *                                     'sgwhv' EDEX plugin, which differs from
 *                                     'sgwh' only in a few IDataStore field names
 *  06/01/2015   R6281     B. Hebbard  Need separate names for fields in PDO vs.
 *                                     IDataStore (latter usually uppercase of former)
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
public class WaveSatVResource extends AbstractWaveSatResource implements
        INatlCntrsResource {

    public WaveSatVResource(WaveSatResourceData ncresourceData,
            LoadProperties loadProperties) {
        super(ncresourceData, loadProperties);

        // "Param" strings must match those in:
        // gov.noaa.nws.ncep.edex.plugin.sgwhv/res/pointdata/sgwhv.xml

        // "ParamInPdo" strings must match those in PDO class:
        // gov.noaa.nws.ncep.common.dataplugin.sgwhv.SgwhvRecord

        // Mappings between the two are defined in class:
        // gov.noaa.nws.ncep.common.dataplugin.sgwhv.dao.SgwhvPointDataTransform

        latParam = "LAT";
        latParamInPdo = "lat";

        lonParam = "LON";
        lonParamInPdo = "lon";

        satIdParam = "SATELLITEID";
        satIdParamInPdo = "satelliteId";

        waveHeightParam = "HTWAVES";

        windSpeedParam = "WSPD10";

    }
}
