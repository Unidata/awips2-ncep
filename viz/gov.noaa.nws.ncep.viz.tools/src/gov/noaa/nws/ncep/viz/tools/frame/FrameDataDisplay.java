/* FrameDataDisplay
 * 
 * Date Created (16 March 2010)
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 
 */
package gov.noaa.nws.ncep.viz.tools.frame;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.AbstractTimeMatcher;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor.IFrameChangedListener;
import com.raytheon.viz.ui.editor.AbstractEditor;

import gov.noaa.nws.ncep.viz.common.display.INatlCntrsDescriptor;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcher;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

/**
 * Contribution item added to the status bar in the National Centers Perspective
 * to display the frame counter and the time of the displayed frame
 * <p>
 * 
 * <pre>
 * SOFTWARE HISTORY
 *    Date       Ticket#	 Engineer	    Description
 * -----------------------------------------------------------
 * 16-Mar-2010   238, 239    Archana       Initial Creation.
 * 08-Apr-2010   239         Archana      Changed the method name
 *                                        from getCurrentTimeFrame(int)
 *                                        to getValidTime(int).
 * 20-May-2010   238, 239    Archana      Increased the spacing for the labels                                                
 * 07/15/11                  C Chen       fixed frame number not updated while looping problem
 * 11/11/11                  G. Hull      implement IVizEditorChangedListener, use raytheon's IFrameChangedListener 
 * 11/11/11                  G. Hull      change to GridLayout, resize and pack Labels on frame change.
 * 11/11/11                  G. Hull      create frameChangelistener in constructor. (ie new instance after a dispose)    
 * 11/22/11      #514        G. Hull      remove editorChangeListener now that this is set by the PerspeciveManager 
 *                                        and this gets updated via refreshGUIElements()
 * 02/11/13      #972        G. Hull      INatlCntrsDescriptor instead of NCMapDescriptor
 * 01/10/2017    R17976      A. Su        Added two methods, isDominantResourceAForecastResource() &
 *                                        getAddedForecastTimeString(), to display forecast time 
 *                                        (VHHH or VHHHmm) when forecast data is loaded.
 * 
 * </pre>
 * 
 * @author Archana
 * @version 1.0
 */
public class FrameDataDisplay extends ContributionItem {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(FrameDataDisplay.class);

    private Shell shell = null;

    private static FrameDataDisplay instance = null;

    /**
     * Composite to display the frame counter as well as the time for the
     * current frame
     */
    private Composite frameDataComposite;

    private Label frameCounterLabel;

    private Label frameTimeLabel;

    private Font font;

    private INatlCntrsDescriptor ncDescriptor;

    private IFrameChangedListener frameChangelistener = null;

    private final SimpleDateFormat CYCLE_TIME_DATE_FORMAT = new SimpleDateFormat(
            "yyMMdd_HHmm");

    private final SimpleDateFormat VALID_TIME_DATE_FORMAT = new SimpleDateFormat(
            "EEE yyMMdd/HHmm");

    private INatlCntrsResourceData dominantResource = null;

    public static FrameDataDisplay createInstance() {
        if (instance == null) {
            instance = new FrameDataDisplay();
        }
        return instance;
    }

    private FrameDataDisplay() {
        super();

        frameChangelistener = new IFrameChangedListener() {
            @Override
            public void frameChanged(IDescriptor descriptor, DataTime oldTime,
                    DataTime newTime) {

                AbstractEditor activeEd = NcDisplayMngr
                        .getActiveNatlCntrsEditor();

                // if this is the active editor
                if (activeEd == descriptor.getRenderableDisplay()
                        .getContainer()) {

                    // If the widgets are disposed, it means that this listener
                    // is no longer valid (the
                    // perspective has been deactivated and status bar
                    // disposed),So we need to remove this listner.
                    // NOTE: it would be nice to remove the listener's when the
                    // status bar is disposed but by this
                    // time the editors have been removed.
                    if (frameCounterLabel == null
                            || frameCounterLabel.isDisposed()) {
                        NcEditorUtil.removeFrameChangedListener(activeEd, this);
                        return;
                    }

                    // chin, fixed issue that GUI component can not be changed
                    // by other thread (worker thread)
                    try {
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                updateFrameDataDisplay(NcDisplayMngr
                                        .getActiveNatlCntrsEditor());
                            }
                        });
                    } catch (SWTException e) {
                        statusHandler.handle(Priority.WARN,
                                "updateFrameDataFromWorkerThread: can not run asyncExec()",
                                e);
                    }
                }
            }
        };
    }

    /***
     * Creates the composite to display the frame counter and the current time
     * of the frame.
     * 
     * @param the
     *            parent status bar manager, where the contribution item should
     *            be added.
     */
    @Override
    public void fill(Composite parent) {

        shell = parent.getShell();

        font = new Font(parent.getDisplay(), "Monospace", 11, SWT.BOLD);
        frameDataComposite = new Composite(parent, SWT.NONE);
        frameDataComposite.setLayout(new GridLayout(2, false));
        frameCounterLabel = new Label(frameDataComposite, SWT.NONE);

        frameCounterLabel
                .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        frameCounterLabel.setFont(font);
        frameCounterLabel.setText("");

        frameTimeLabel = new Label(frameDataComposite, SWT.NONE);
        frameTimeLabel
                .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        frameTimeLabel.setFont(font);
        frameTimeLabel.setText("");

        setVisible(true);
    }

    /***
     * Updates the frame number and the the time of the frame, each time the
     * current frame changes.
     * 
     * @param nmapEditor
     *            - the active editor in which the frames are loaded or
     *            animated.
     */
    private void updateFrameDataDisplay(final AbstractEditor nmapEditor) {

        String frameCountString = "";
        String frameTimeString = "";

        if (frameCounterLabel.isDisposed()) {
            statusHandler.handle(Priority.WARN,
                    "Frame Counter Widget has been disposed!");
            return;
        }

        if (nmapEditor != null) {

            ncDescriptor = ((INatlCntrsDescriptor) (nmapEditor
                    .getActiveDisplayPane().getRenderableDisplay()
                    .getDescriptor()));

            if (ncDescriptor != null) {
                int currentFrame = ncDescriptor.getCurrentFrame();
                int totalFrames = ncDescriptor.getFrameCount();

                if (totalFrames > 0) {
                    frameCountString = (currentFrame + 1) + " of "
                            + totalFrames;

                    frameTimeString = " "
                            + ncDescriptor.getValidTime(currentFrame);

                    if (isDominantResourceAForecastResource()) {
                        frameTimeString += getAddedForecastTimeString();
                    }
                }
            }
        }

        frameCounterLabel.setText(frameCountString);
        frameTimeLabel.setText(frameTimeString);

        shell.layout(true, true);
        frameCounterLabel.pack(true);
        frameTimeLabel.pack(true);
    }

    @Override
    public void update() {
        AbstractEditor activeEd = NcDisplayMngr.getActiveNatlCntrsEditor();

        if (activeEd != null) {
            NcEditorUtil.addFrameChangedListener(activeEd, frameChangelistener);

            updateFrameDataDisplay(NcDisplayMngr.getActiveNatlCntrsEditor());
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        instance = null;
    }

    private boolean isDominantResourceAForecastResource() {
        boolean returnValue = false;

        AbstractTimeMatcher timeMatcher = ncDescriptor.getTimeMatcher();

        if (timeMatcher != null && timeMatcher instanceof NCTimeMatcher) {
            dominantResource = ((NCTimeMatcher) timeMatcher)
                    .getDominantResource();

            if (dominantResource != null && dominantResource.getResourceName()
                    .isForecastResource()) {
                returnValue = true;
            } else {
                dominantResource = null;
            }
        }
        return returnValue;
    }

    private String getAddedForecastTimeString() {

        boolean isMinuteTextIncluded = false;

        int currentFrame = ncDescriptor.getCurrentFrame();
        int totalFrames = ncDescriptor.getFrameCount();
        int[] hours = new int[totalFrames];
        int[] minutes = new int[totalFrames];

        String cycleTimeString = dominantResource.getResourceName()
                .getCycleTimeString();
        try {
            Date cycleTime = CYCLE_TIME_DATE_FORMAT.parse(cycleTimeString);

            Date validTime;
            String validTimeString;

            for (int i = 0; i < totalFrames; i++) {
                validTimeString = ncDescriptor.getValidTime(i);
                validTime = VALID_TIME_DATE_FORMAT.parse(validTimeString);

                int totalMinDiff = (int) ((validTime.getTime()
                        - cycleTime.getTime()) / 1000 / 60);

                hours[i] = totalMinDiff / 60;
                minutes[i] = totalMinDiff % 60;

                if (minutes[i] != 0) {
                    isMinuteTextIncluded = true;
                }
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.WARN, "Date format mismatch!", e);
        }

        String addedString = String.format("V%03d", hours[currentFrame]);
        if (isMinuteTextIncluded) {
            addedString += String.format("%02d", minutes[currentFrame]);
        }
        return addedString;
    }
}