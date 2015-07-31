package gov.noaa.nws.ncep.viz.rsc.modis.rsc;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.AbstractEditResourceAttrsInteractiveDialog;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarFromColorMapAttrsEditorComposite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * An interface to edit MODIS resource attributes.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 01, 2014            kbugenhagen Initial creation
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class EditModisAttrsDialog extends
        AbstractEditResourceAttrsInteractiveDialog {

    public EditModisAttrsDialog(Shell parentShell, INatlCntrsResourceData r,
            Boolean apply) {

        super(parentShell, r, apply);
        resourceData = r;
    }

    private INatlCntrsResourceData resourceData;

    private ColorBarFromColorMapAttrsEditorComposite cBarComposite = null;

    //
    @Override
    public Composite createDialog(Composite topComp) {

        FormLayout layout0 = new FormLayout();
        topComp.setLayout(layout0);

        cBarComposite = new ColorBarFromColorMapAttrsEditorComposite(topComp,
                SWT.NONE, resourceData);

        return topComp;
    }

    @Override
    public void initWidgets() {
        // done in createDialog
    }

    @Override
    protected void dispose() {
        super.dispose();
        // colorBarEditor.dispose();
        if (cBarComposite != null)
            cBarComposite.dispose();
    }

}
