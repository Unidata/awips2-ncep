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
 */
package com.raytheon.uf.viz.gempak.common.request;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Request object for GEMPAK subgrid coverage.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 25, 2018 54483      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
@DynamicSerialize
public class GempakSubgridCoverageRequest implements IGempakRequest {

    @DynamicSerializeElement
    private ISpatialObject spatialObj;

    @DynamicSerializeElement
    private String subgGempakFormat;

    /**
     * Empty constructor for serialization.
     */
    public GempakSubgridCoverageRequest() {
    }

    /**
     * Constructor.
     *
     * @param spatialObj
     * @param subgGempakFormat
     */
    public GempakSubgridCoverageRequest(ISpatialObject spatialObj,
            String subgGempakFormat) {
        this.spatialObj = spatialObj;
        this.subgGempakFormat = subgGempakFormat;
    }

    /**
     * @return the spatialObj
     */
    public ISpatialObject getSpatialObj() {
        return spatialObj;
    }

    /**
     * @param spatialObj
     *            the spatialObj to set
     */
    public void setSpatialObj(ISpatialObject spatialObj) {
        this.spatialObj = spatialObj;
    }

    /**
     * @return the subgGempakFormat:
     *         prj;nx;ny;lllat;lllon;urlat;urlon;angle1;angle2;angle3
     */
    public String getSubgGempakFormat() {
        return subgGempakFormat;
    }

    /**
     * @param subgGempakFormat
     *            the subgGempakFormat to set
     */
    public void setSubgGempakFormat(String subgGempakFormat) {
        this.subgGempakFormat = subgGempakFormat;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
