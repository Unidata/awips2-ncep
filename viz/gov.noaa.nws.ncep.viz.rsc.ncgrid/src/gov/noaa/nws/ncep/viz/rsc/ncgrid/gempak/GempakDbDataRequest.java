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
 * Request object for DB data needed to perform GEMPAK processing.
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
public class GempakDbDataRequest implements IGempakRequest {

    @DynamicSerializeElement
    private String dataURI;

    @DynamicSerializeElement
    private int nx;

    @DynamicSerializeElement
    private int ny;

    @DynamicSerializeElement
    private boolean flip;

    /**
     * Empty constructor for serialization.
     */
    public GempakDbDataRequest() {
    }

    /**
     * Constructor.
     *
     * @param dataURI
     *            the URI of the data to retrieve
     * @param nx
     *            the expected x-dimension of the grid data
     * @param ny
     *            the expected y-dimension of the grid data
     * @param flip
     *            whether or not the grid data needs to be flipped from CAVE
     *            order
     */
    public GempakDbDataRequest(String dataURI, int nx, int ny, boolean flip) {
        this.dataURI = dataURI;
        this.nx = nx;
        this.ny = ny;
        this.flip = flip;
    }

    /**
     * @return the dataURI
     */
    public String getDataURI() {
        return dataURI;
    }

    /**
     * @param dataURI
     *            the dataURI to set
     */
    public void setDataURI(String dataURI) {
        this.dataURI = dataURI;
    }

    /**
     * @return the nx
     */
    public int getNx() {
        return nx;
    }

    /**
     * @param nx
     *            the nx to set
     */
    public void setNx(int nx) {
        this.nx = nx;
    }

    /**
     * @return the ny
     */
    public int getNy() {
        return ny;
    }

    /**
     * @param ny
     *            the ny to set
     */
    public void setNy(int ny) {
        this.ny = ny;
    }

    /**
     * @return the flip
     */
    public boolean isFlip() {
        return flip;
    }

    /**
     * @param flip
     *            the flip to set
     */
    public void setFlip(boolean flip) {
        this.flip = flip;
    }
}
