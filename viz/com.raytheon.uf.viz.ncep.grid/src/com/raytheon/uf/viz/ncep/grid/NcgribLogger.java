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
package com.raytheon.uf.viz.ncep.grid;

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
 *
 * </pre>
 *
 * @author mapeters
 */
public class NcgribLogger {

    private static final Object INSTANCE_LOCK = new Object();

    private boolean enableRscLogs;

    private boolean enableDiagnosticLogs;

    private boolean enableContourLogs;

    private boolean enableTotalTimeLogs;

    private static NcgribLogger instance = null;

    public static NcgribLogger getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new NcgribLogger();
            }
            return instance;
        }
    }

    private NcgribLogger() {
        this.enableRscLogs = false;
        this.enableDiagnosticLogs = false;
        this.enableContourLogs = false;
        this.enableTotalTimeLogs = false;
    }

    public void setEnableRscLogs(boolean enable) {
        this.enableRscLogs = enable;
    }

    public boolean enableRscLogs() {
        return this.enableRscLogs;
    }

    public void setEnableDiagnosticLogs(boolean enable) {
        this.enableDiagnosticLogs = enable;
    }

    public boolean enableDiagnosticLogs() {
        return this.enableDiagnosticLogs;
    }

    public void setEnableCntrLogs(boolean enable) {
        this.enableContourLogs = enable;
    }

    public boolean enableCntrLogs() {
        return this.enableContourLogs;
    }

    public void setEnableTotalTimeLogs(boolean enable) {
        this.enableTotalTimeLogs = enable;
    }

    public boolean enableTotalTimeLogs() {
        return this.enableTotalTimeLogs;
    }
}
