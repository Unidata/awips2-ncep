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

/**
 * Interface specifying a strategy for performing GEMPAK data processing.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 04, 2018 7417       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IGempakProcessingStrategy {

    /**
     * Given the {@link GempakDataInput} specifying the data to process and
     * necessary parameters, process the data through GEMPAK and return the
     * {@link GempakDataRecord} result.
     *
     * @param dataInput
     *            the data to process
     * @return the processed data
     */
    GempakDataRecord getDataRecord(GempakDataInput dataInput);
}
