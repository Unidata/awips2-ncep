package gov.noaa.nws.ncep.viz.rsc.viirs.rsc;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.AbstractEditResourceAttrsInteractiveDialog;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarFromColorMapAttrsEditorComposite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class EditViirsAttrsDialog extends
        AbstractEditResourceAttrsInteractiveDialog {

    public EditViirsAttrsDialog(Shell parentShell, INatlCntrsResourceData r,
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
