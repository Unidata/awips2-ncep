package gov.noaa.nws.ncep.viz.rsc.plotdata;

import gov.noaa.nws.ncep.viz.common.Activator;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * 
 * PointDataDisplayPreferences
 * 
 * This class defines a CAVE Preferences page for the point data display
 * (plotdata) resource.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 01 Sep 2015  R7757       B. Hebbard  Initial creation.
 * 
 * </pre>
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS system.
 */

public class PointDataDisplayPreferences extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    public final static String ENABLE_FRAME_STATUS_DISPLAY = "ENABLE_FRAME_STATUS_DISPLAY";

    public final static String ENABLE_VERBOSE_FRAME_STATUS = "ENABLE_VERBOSE_FRAME_STATES";

    private BooleanFieldEditor enableFrameStatusDisplay;

    private BooleanFieldEditor enableVerboseFrameStatus;

    public PointDataDisplayPreferences() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    @Override
    public void createFieldEditors() {
        Composite composite = getFieldEditorParent();

        enableFrameStatusDisplay = new BooleanFieldEditor(
                ENABLE_FRAME_STATUS_DISPLAY,
                "&Monitor Frame Status (Diagnostic)",
                BooleanFieldEditor.DEFAULT, composite);
        this.addField(enableFrameStatusDisplay);

        enableVerboseFrameStatus = new BooleanFieldEditor(
                ENABLE_VERBOSE_FRAME_STATUS, "Use Verbose Frame Status",
                BooleanFieldEditor.DEFAULT, composite);
        this.addField(enableVerboseFrameStatus);

        boolean enabled = this.getPreferenceStore().getBoolean(
                ENABLE_FRAME_STATUS_DISPLAY);
        enableVerboseFrameStatus.setEnabled(enabled, getFieldEditorParent());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.preference.FieldEditorPreferencePage#propertyChange
     * (org.eclipse.jface.util.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getSource().equals(this.enableFrameStatusDisplay)) {
            boolean enabled = (Boolean) event.getNewValue();
            enableVerboseFrameStatus
                    .setEnabled(enabled, getFieldEditorParent());
        } else if (event.getSource().equals(this.enableVerboseFrameStatus)) {
            if (enableFrameStatusDisplay.getBooleanValue()) {
                super.propertyChange(event);
            }
        } else {
            super.propertyChange(event);
        }
    }

}
