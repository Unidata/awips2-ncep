/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.viz.common.tsScaleMngr;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * UI for temporarily editing X-axis scaling attribute of a Point Data Resource
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Sep 18, 2014  R4875      sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class EditXAxisScaleAttrDialog extends Dialog {

    protected Shell shell;

    protected String dlgTitle = "Edit X-axis Scale";

    protected boolean ok = false;

    private XAxisScale xAxisScale = null;

    public EditXAxisScaleAttrDialog(Shell parentShell, XAxisScale cf) {
        super(parentShell);
        dlgTitle = "Edit X-axis scale " + cf.getName();
        xAxisScale = new XAxisScale(cf);
    }

    public void createShell(int x, int y) {

        shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE);
        shell.setText(dlgTitle);
        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 1;
        mainLayout.marginWidth = 1;
        shell.setLayout(mainLayout);
        shell.setLocation(x, y);

        Composite editxScaleComp = new EditXAxisScaleComposite(shell, SWT.NONE,
                xAxisScale);

        GridData gd = new GridData();

        Composite okCanComp = new Composite(shell, SWT.NONE);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        okCanComp.setLayoutData(gd);

        okCanComp.setLayout(new FormLayout());

        Button canBtn = new Button(okCanComp, SWT.PUSH);
        canBtn.setText(" Cancel ");
        FormData fd = new FormData();
        fd.width = 80;
        fd.bottom = new FormAttachment(100, -5);
        fd.left = new FormAttachment(20, -40);
        canBtn.setLayoutData(fd);

        canBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ok = false;
                shell.dispose();
            }
        });

        Button applyBtn = new Button(okCanComp, SWT.PUSH);
        applyBtn.setText("  OK  ");
        fd = new FormData();
        fd.width = 80;
        fd.bottom = new FormAttachment(100, -5);
        fd.left = new FormAttachment(40, -40);
        applyBtn.setLayoutData(fd);
        // if ("".equals(xAxisScale.getName()))
        // applyBtn.setEnabled(false);

        applyBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ok = true;
                shell.dispose();
            }
        });

        Button revertBtn = new Button(okCanComp, SWT.PUSH);
        revertBtn.setText("  Revert  ");
        fd = new FormData();
        fd.width = 90;
        fd.bottom = new FormAttachment(100, -5);
        fd.left = new FormAttachment(60, -40);
        revertBtn.setLayoutData(fd);

        revertBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                xAxisScale = XAxisScaleMngr.getInstance().getXAxisScale(
                        xAxisScale.getName());
                ok = true;
                shell.dispose();
            }
        });

        final Button helpBtn = new Button(okCanComp, SWT.PUSH);
        helpBtn.setText(" Help... ");
        helpBtn.setToolTipText("Help for X-axis Scale");

        fd = new FormData();
        fd.width = 80;
        fd.bottom = new FormAttachment(100, -5);
        fd.left = new FormAttachment(80, -40);
        helpBtn.setLayoutData(fd);

        helpBtn.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {

                XAxisScaleHelpDialog xScaleDlg = new XAxisScaleHelpDialog(shell
                        .getShell());
                helpBtn.setEnabled(false);
                xScaleDlg.open();
                if (!helpBtn.isDisposed())
                    helpBtn.setEnabled(true);
            }

        });

    }

    public void open() {
        open(getParent().getLocation().x + 10, getParent().getLocation().y + 10);
    }

    public Object open(int x, int y) {
        Display display = getParent().getDisplay();

        createShell(x, y);

        initWidgets();

        shell.pack();
        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        return (ok ? xAxisScale : null);
    }

    public void initWidgets() {
    }
}
