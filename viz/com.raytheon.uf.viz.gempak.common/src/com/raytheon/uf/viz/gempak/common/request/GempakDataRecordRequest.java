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
package com.raytheon.uf.viz.gempak.common.request;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.viz.gempak.common.data.GempakDataInput;

/**
 * Request object for GEMPAK to process the contained data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 05, 2018 54480      mapeters    Initial creation
 * Oct 23, 2018 54483      mapeters    Add {@link #toString()}
 *
 * </pre>
 *
 * @author mapeters
 */
@DynamicSerialize
public class GempakDataRecordRequest implements IGempakRequest {

    @DynamicSerializeElement
    private GempakDataInput dataInput;

    /**
     * Empty constructor for serialization.
     */
    public GempakDataRecordRequest() {
    }

    /**
     * Constructor.
     *
     * @param dataInput
     *            the input data needed to perform the GEMPAK processing
     */
    public GempakDataRecordRequest(GempakDataInput dataInput) {
        this.dataInput = dataInput;
    }

    /**
     * @return the dataInput
     */
    public GempakDataInput getDataInput() {
        return dataInput;
    }

    /**
     * @param dataInput
     *            the dataInput to set
     */
    public void setDataInput(GempakDataInput dataInput) {
        this.dataInput = dataInput;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
