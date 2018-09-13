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

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

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
 *
 * </pre>
 *
 * @author mapeters
 */
@DynamicSerialize
public class GempakDataRecordRequest implements IGempakRequest {

    @DynamicSerializeElement
    private GempakDataInput dataInput;

    public GempakDataRecordRequest() {
        // For serialization
    }

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
}
