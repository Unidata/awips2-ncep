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
 * Request object for GEMPAK grid navigation.
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
public class GempakNavigationRequest implements IGempakRequest {

    @DynamicSerializeElement
    private ISpatialObject spatialObject;

    /**
     * Empty constructor for serialization.
     */
    public GempakNavigationRequest() {
    }

    /**
     * Constructor.
     *
     * @param spatialObject
     */
    public GempakNavigationRequest(ISpatialObject spatialObject) {
        this.spatialObject = spatialObject;
    }

    /**
     * @return the spatialObject
     */
    public ISpatialObject getSpatialObject() {
        return spatialObject;
    }

    /**
     * @param spatialObject
     *            the spatialObject to set
     */
    public void setSpatialObject(ISpatialObject spatialObject) {
        this.spatialObject = spatialObject;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
