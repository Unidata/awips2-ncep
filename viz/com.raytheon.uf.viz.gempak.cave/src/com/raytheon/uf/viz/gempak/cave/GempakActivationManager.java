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
package com.raytheon.uf.viz.gempak.cave;

import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;

import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;

import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;

/**
 * Manager for controlling the activation/deactivation of GEMPAK data
 * processing.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Oct 16, 2018  54483    mapeters  Initial creation
 * Dec 09, 2019  7991     randerso  Fix NPE in earlyStartup()
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakActivationManager implements IStartup {

    @Override
    public void earlyStartup() {
        /*
         * Activate GEMPAK immediately if in NCP perspective, as the perspective
         * listener below isn't triggered on start-up
         */
        AbstractVizPerspectiveManager perspectiveManager = VizPerspectiveListener
                .getCurrentPerspectiveManager();
        if (perspectiveManager != null && NmapCommon.NatlCntrsPerspectiveID
                .equals(perspectiveManager.getPerspectiveId())) {
            activate();
        }

        // Activate/deactivate GEMPAK when activating/closing GEMPAK
        IWorkbenchWindow workbenchWindow = VizWorkbenchManager.getInstance()
                .getCurrentWindow();
        if (workbenchWindow != null) {
            workbenchWindow.addPerspectiveListener(new PerspectiveAdapter() {

                @Override
                public void perspectiveActivated(IWorkbenchPage page,
                        IPerspectiveDescriptor perspective) {
                    /*
                     * Listen for activate instead of open since we may start
                     * with NCP open but not activated (and this listener isn't
                     * triggered on start-up)
                     */
                    if (NmapCommon.NatlCntrsPerspectiveID
                            .equals(perspective.getId())) {
                        activate();
                    }
                }

                @Override
                public void perspectiveClosed(IWorkbenchPage page,
                        IPerspectiveDescriptor perspective) {
                    if (NmapCommon.NatlCntrsPerspectiveID
                            .equals(perspective.getId())) {
                        shutdown(false);
                    }
                }
            });
        }

        // Permanently shutdown GEMPAK on workbench shutdown
        PlatformUI.getWorkbench()
                .addWorkbenchListener(new IWorkbenchListener() {

                    @Override
                    public boolean preShutdown(IWorkbench workbench,
                            boolean forced) {
                        return true;
                    }

                    @Override
                    public void postShutdown(IWorkbench workbench) {
                        shutdown(true);
                    }
                });
    }

    private void activate() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                GempakProcessingManager.getInstance().activate();
            }
        }).start();
    }

    private void shutdown(boolean permanent) {
        if (permanent) {
            // We need to block to ensure this completes on CAVE shutdown
            GempakProcessingManager.getInstance().shutdown(permanent);
        } else {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    GempakProcessingManager.getInstance().shutdown(permanent);
                }
            }).start();
        }
    }
}
