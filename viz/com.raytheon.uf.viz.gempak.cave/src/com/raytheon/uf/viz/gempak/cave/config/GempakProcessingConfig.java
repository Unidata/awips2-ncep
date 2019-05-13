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
package com.raytheon.uf.viz.gempak.cave.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * POJO container of GEMPAK processing configuration.
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
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class GempakProcessingConfig {

    @XmlElement
    private boolean runInSubprocess;

    @XmlElement
    private int subprocessLimit;

    /**
     * @return whether to do GEMPAK processing in subprocesses or in the CAVE
     *         process
     */
    public boolean isRunInSubprocess() {
        return runInSubprocess;
    }

    /**
     * @param runInSubprocess
     *            whether GEMPAK processing should be done in subprocesses or in
     *            the CAVE process
     */
    public void setRunInSubprocess(boolean runInSubprocess) {
        this.runInSubprocess = runInSubprocess;
    }

    /**
     * @return the subprocessLimit
     */
    public int getSubprocessLimit() {
        return subprocessLimit;
    }

    /**
     * @param subprocessLimit
     *            the subprocessLimit to set
     */
    public void setSubprocessLimit(int subprocessLimit) {
        this.subprocessLimit = subprocessLimit;
    }
}
