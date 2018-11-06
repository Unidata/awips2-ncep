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
package com.raytheon.uf.viz.ncep.grid;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Handles enabling/disabling various types of logging for ncgrib data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *                                     Initial creation
 * Sep 13, 2018 54483      mapeters    Sync {@link #getInstance()}
 * Oct 25, 2018 54483      mapeters    Add dynamic serialize support
 *
 * </pre>
 *
 * @author mapeters
 */
@DynamicSerialize
public class NcgribLogger {

    private static final Object INSTANCE_LOCK = new Object();

    private static NcgribLogger instance = null;

    @DynamicSerializeElement
    private boolean enableRscLogs = false;

    @DynamicSerializeElement
    private boolean enableDiagnosticLogs = false;

    @DynamicSerializeElement
    private boolean enableContourLogs = false;

    @DynamicSerializeElement
    private boolean enableTotalTimeLogs = false;

    /**
     * @return the singleton {@link NcgribLogger} instance, creating it if
     *         necessary
     */
    public static NcgribLogger getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new NcgribLogger();
            }
            return instance;
        }
    }

    /**
     * Empty constructor for serialization. This should never be called
     * externally as this is a singleton, use {@link #getInstance()} instead.
     */
    public NcgribLogger() {
    }

    /**
     * @return the enableRscLogs
     */
    public boolean isEnableRscLogs() {
        return enableRscLogs;
    }

    /**
     * @param enableRscLogs
     *            the enableRscLogs to set
     */
    public void setEnableRscLogs(boolean enableRscLogs) {
        this.enableRscLogs = enableRscLogs;
    }

    /**
     * @return the enableDiagnosticLogs
     */
    public boolean isEnableDiagnosticLogs() {
        return enableDiagnosticLogs;
    }

    /**
     * @param enableDiagnosticLogs
     *            the enableDiagnosticLogs to set
     */
    public void setEnableDiagnosticLogs(boolean enableDiagnosticLogs) {
        this.enableDiagnosticLogs = enableDiagnosticLogs;
    }

    /**
     * @return the enableContourLogs
     */
    public boolean isEnableContourLogs() {
        return enableContourLogs;
    }

    /**
     * @param enableContourLogs
     *            the enableContourLogs to set
     */
    public void setEnableContourLogs(boolean enableContourLogs) {
        this.enableContourLogs = enableContourLogs;
    }

    /**
     * @return the enableTotalTimeLogs
     */
    public boolean isEnableTotalTimeLogs() {
        return enableTotalTimeLogs;
    }

    /**
     * @param enableTotalTimeLogs
     *            the enableTotalTimeLogs to set
     */
    public void setEnableTotalTimeLogs(boolean enableTotalTimeLogs) {
        this.enableTotalTimeLogs = enableTotalTimeLogs;
    }

    /**
     * Update this {@link NcgribLogger}'s settings from other's. Note that since
     * this is a singleton, this should only be used for updating the singleton
     * instance in one process after serializing the singleton instance from
     * another process.
     *
     * @param other
     *            the {@link NcgribLogger} to copy the settings from
     */
    public void update(NcgribLogger other) {
        setEnableRscLogs(other.isEnableRscLogs());
        setEnableDiagnosticLogs(other.isEnableDiagnosticLogs());
        setEnableContourLogs(other.isEnableContourLogs());
        setEnableTotalTimeLogs(other.isEnableTotalTimeLogs());
    }
}
