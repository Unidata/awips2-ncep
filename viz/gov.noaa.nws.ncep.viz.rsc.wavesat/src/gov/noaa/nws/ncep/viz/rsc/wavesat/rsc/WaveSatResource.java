package gov.noaa.nws.ncep.viz.rsc.wavesat.rsc;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;

import com.raytheon.uf.viz.core.rsc.LoadProperties;

/**
 * WaveSatResource - Display SGWH data.
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *  09/21/2011    #248     Greg Hull    Initial creation. 
 *  02/16/2012    #555     S. Gurung    Added call to setPopulated(true) in queryData().
 *  05/23/2012    #785     Q. Zhou      Added getName for legend
 *  12/19/2012    #960     Greg Hull    override propertiesChanged() to update colorBar.
 *  04/27/2014   R6281     B. Hebbard   Move former contents upstairs to AbstractWaveSatResource
 *                                      (to allow for new WaveSatVResource); only retain here the
 *                                      few things that differ between 'sgwh' and 'sgwhv' plugins
 *  06/01/2015   R6281     B. Hebbard   Need separate names for fields in PDO vs.
 *                                      IDataStore (latter usually uppercase of former)
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1.0
 */
public class WaveSatResource extends AbstractWaveSatResource implements
        INatlCntrsResource {

    public WaveSatResource(WaveSatResourceData ncresourceData,
            LoadProperties loadProperties) {
        super(ncresourceData, loadProperties);

        // "Param" strings must match those in:
        // gov.noaa.nws.ncep.edex.plugin.sgwh/res/pointdata/sgwh.xml

        // "ParamInPdo" strings must match those in PDO class:
        // gov.noaa.nws.ncep.common.dataplugin.sgwh.SgwhRecord

        // Mappings between the two are defined in class:
        // gov.noaa.nws.ncep.common.dataplugin.sgwh.SgwhPointDataTransform

        latParam = "CLATH";
        latParamInPdo = "clath";

        lonParam = "CLONH";
        lonParamInPdo = "clonh";

        satIdParam = "SAID";
        satIdParamInPdo = "said";

        waveHeightParam = "SGWH";

        windSpeedParam = "WS10G3R1";

    }
}
