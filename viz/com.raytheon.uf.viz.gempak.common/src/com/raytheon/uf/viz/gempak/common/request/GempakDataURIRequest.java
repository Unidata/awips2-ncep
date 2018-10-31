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

/**
 * Request object for a GEMPAK data URI.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 10, 2018 54483      mapeters    Initial creation
 * Oct 30, 2018 54483      mapeters    Add {@link #toString()}
 *
 * </pre>
 *
 * @author mapeters
 */
@DynamicSerialize
public class GempakDataURIRequest implements IGempakRequest {

    @DynamicSerializeElement
    private String parameters;

    @DynamicSerializeElement
    private boolean ensCategory;

    @DynamicSerializeElement
    private String ensembleMember;

    /**
     * Empty constructor for serialization.
     */
    public GempakDataURIRequest() {
    }

    /**
     * Constructor.
     *
     * @param parameters
     *            '|'-delimited parameters specifying the data URI to retrieve
     * @param ensCategory
     *            whether or not the data URI we are requesting is for an
     *            ENSEMBLE grid
     * @param ensembleMember
     *            the ENSEMBLE member of the requested grid
     */
    public GempakDataURIRequest(String parameters, boolean ensCategory,
            String ensembleMember) {
        this.parameters = parameters;
        this.ensCategory = ensCategory;
        this.ensembleMember = ensembleMember;
    }

    /**
     * @return the parameters
     */
    public String getParameters() {
        return parameters;
    }

    /**
     * @param parameters
     *            the parameters to set
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the ensCategory
     */
    public boolean isEnsCategory() {
        return ensCategory;
    }

    /**
     * @param ensCategory
     *            the ensCategory to set
     */
    public void setEnsCategory(boolean ensCategory) {
        this.ensCategory = ensCategory;
    }

    /**
     * @return the ensembleMember
     */
    public String getEnsembleMember() {
        return ensembleMember;
    }

    /**
     * @param ensembleMember
     *            the ensembleMember to set
     */
    public void setEnsembleMember(String ensembleMember) {
        this.ensembleMember = ensembleMember;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
