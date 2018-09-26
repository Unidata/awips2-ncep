/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak;

import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Response object corresponding to a {@link GempakDbDataRequest}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 10, 2018 54483      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
@DynamicSerialize
public class GempakDbDataResponse {

    @DynamicSerializeElement
    private float[] data;

    @DynamicSerializeElement
    private ISpatialObject subgSpatialObj;

    /**
     * Empty constructor for serialization.
     */
    public GempakDbDataResponse() {
    }

    /**
     * Constructor.
     *
     * @param data
     *            the float data
     * @param subgSpatialObj
     *            the sub-grid coverage
     */
    public GempakDbDataResponse(float[] data, ISpatialObject subgSpatialObj) {
        this.data = data;
        this.subgSpatialObj = subgSpatialObj;
    }

    /**
     * @return the data
     */
    public float[] getData() {
        return data;
    }

    /**
     * @param data
     *            the data to set
     */
    public void setData(float[] data) {
        this.data = data;
    }

    /**
     * @return the subgSpatialObj
     */
    public ISpatialObject getSubgSpatialObj() {
        return subgSpatialObj;
    }

    /**
     * @param subgSpatialObj
     *            the subgSpatialObj to set
     */
    public void setSubgSpatialObj(ISpatialObject subgSpatialObj) {
        this.subgSpatialObj = subgSpatialObj;
    }
}
