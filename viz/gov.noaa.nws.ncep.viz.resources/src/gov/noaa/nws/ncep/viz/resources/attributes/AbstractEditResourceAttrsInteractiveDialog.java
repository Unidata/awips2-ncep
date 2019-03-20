package gov.noaa.nws.ncep.viz.resources.attributes;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

/**
 * An interface to edit resource attributes interactively.
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#    Engineer     Description
 * ------------  ---------- -----------  --------------------------
 * 03/29/2012    #651       S. Gurung    Initial Creation.
 * 06/21/2012    #569       G. Hull      call refreshGUIElements() to update the
 *                                       Fade Display
 * 03/08/2016    R15519     RC Reynolds  Changed modality from APPLICATION_MODAL
 *                                       to PRIMARY_MODAL (default)
 * 04/05/2016    R15715     dgilling     Pass Capabilites object through to
 *                                       concrete classes.
 * Mar 20, 2019  7569       tgurney      Clean up properly when closed via trim
 *                                       button
 *
 * </pre>
 *
 * @author sgurung
 */

public class AbstractEditResourceAttrsInteractiveDialog
        extends AbstractEditResourceAttrsDialog {

    protected boolean isRscAttrSetChanged = false;

    public AbstractEditResourceAttrsInteractiveDialog(Shell parentShell,
            INatlCntrsResourceData r, Capabilities capabilities,
            Boolean apply) {
        super(parentShell, r, capabilities, apply);
    }

    @Override
    public Composite createDialog(Composite topComp) {
        return null;
    }

    @Override
    public void initWidgets() {
    }

    @Override
    public void createShell() {
        int style = SWT.DIALOG_TRIM | SWT.RESIZE;

        shell = new Shell(getParent(), style);
        shell.setText(dlgTitle);

        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 1;
        mainLayout.marginWidth = 1;
        shell.setLayout(mainLayout);

        Composite topComp = new Composite(shell, SWT.NONE);
        topComp.setLayout(new GridLayout());
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        topComp.setLayoutData(gd);

        createDialog(topComp);

        Label sep = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        sep.setLayoutData(gd);

        Composite okCanComp = new Composite(shell, SWT.NONE);
        okCanComp.setLayout(new GridLayout(hasApplyBtn ? 3 : 2, true));
        gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        okCanComp.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        Button canBtn = new Button(okCanComp, SWT.PUSH);
        canBtn.setText("Cancel");
        canBtn.setLayoutData(gd);
        canBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                rscData.setRscAttrSet(new ResourceAttrSet(editedRscAttrSet));
                close(false);
            }
        });

        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        Button okBtn = new Button(okCanComp, SWT.PUSH);
        okBtn.setText("OK");
        okBtn.setLayoutData(gd);
        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                close(true);
            }
        });
    }

    private void close(boolean ok) {
        NcEditorUtil
                .refreshGUIElements(NcDisplayMngr.getActiveNatlCntrsEditor());
        this.ok = ok;
        shell.dispose();
    }

    @Override
    public Object open() {
        Display display = getParent().getDisplay();

        // copy the attrSet
        editedRscAttrSet = new ResourceAttrSet(rscData.getRscAttrSet());

        createShell();

        initWidgets();

        shell.pack();
        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        editedRscAttrSet = new ResourceAttrSet(rscData.getRscAttrSet());
        rscData.setRscAttrSet(editedRscAttrSet);
        rscData.setIsEdited(ok);

        dispose();

        return null;
    }

}
