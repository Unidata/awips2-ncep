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
package com.raytheon.uf.viz.gempak.common.data;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.viz.gempak.common.request.GempakNavigationRequest;

/**
 * Response object corresponding to a {@link GempakNavigationRequest}.
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
public class GempakNavigationResponse {

    @DynamicSerializeElement
    private String navigation;

    @DynamicSerializeElement
    private boolean enableFlip;

    /**
     * Empty constructor for serialization.
     */
    public GempakNavigationResponse() {
    }

    /**
     * Constructor.
     *
     * @param navigation
     *            the grid navigation string
     * @param enableFlip
     *            true if data should now be flipped from CAVE order to GEMPAK
     *            order, false if whether or not we flip should remain unchanged
     */
    public GempakNavigationResponse(String navigation, boolean enableFlip) {
        this.navigation = navigation;
        this.enableFlip = enableFlip;
    }

    /**
     * @return the navigation
     */
    public String getNavigation() {
        return navigation;
    }

    /**
     * @param navigation
     *            the navigation to set
     */
    public void setNavigation(String navigation) {
        this.navigation = navigation;
    }

    /**
     * @return true if data should now be flipped from CAVE order to GEMPAK
     *         order, false if whether or not we flip should remain unchanged
     */
    public boolean isEnableFlip() {
        return enableFlip;
    }

    /**
     * @param enableFlip
     *            the enableFlip to set
     */
    public void setEnableFlip(boolean enableFlip) {
        this.enableFlip = enableFlip;
    }
}
