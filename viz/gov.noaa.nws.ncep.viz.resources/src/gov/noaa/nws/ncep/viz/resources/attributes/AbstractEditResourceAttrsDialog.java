package gov.noaa.nws.ncep.viz.resources.attributes;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;
import com.raytheon.viz.ui.editor.AbstractEditor;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

/**
 * an interface to edit resource attributes
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 29 May 2009   #115        Greg Hull  Initial Creation.
 * 19 Nov 2009   #192        M. Li      Added new constructor
 * 15 Dec 2009               Greg Hull  removed new constructor 
 * 04 Apr 2010   #259        Greg Hull  add dispose()
 * 27 Apr 2010   #245        Greg Hull  Added Apply Button
 * 22 Nov 2011   #495        Greg Hull  Make Application Modal when from ResourceManager.
 * 13 Mar 2012   #651        Archana    Updated the code to reuse editedRscAttrSet in case of a roll-back  
 * 28 March 2012 #651        S. Gurung  Removed changes made by Archana. 
 *                                      Moved the changes to a new class AbstractEditResourceAttrsInteractiveDialog.
 * 03/22/2016    R10366      bkowal     Cleanup and added {@link #handleOK()}.
 * 04/05/2016    R15715      dgilling   Pass Capabilites object through to concrete classes.
 * 04/24/2019    #62919      ksunil     changes to support NCP's locator tool in D2D
 * </pre>
 *
 * @author ghull
 * @version 1
 */

public abstract class AbstractEditResourceAttrsDialog extends Dialog {

    protected final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    /*
     * Associate with each line style a list of segment lengths (in pixels) of
     * the repeating pattern. Numbers are pixels on, pixels off, on, off, ...
     * 
     * (Derived from similar structure in NMAP NxmLineA.c) CAUTION: Duplication
     * (of a sort). This governs only local display of line patterns in this
     * dialog (preview and line style selector buttons).
     * 
     * Actual drawing of lines with these styles is up to the implementation of
     * IGraphicsTarget being used.
     */
    protected static final Map<LineStyle, int[]> STYLE_MAP = Collections
            .unmodifiableMap(new EnumMap<LineStyle, int[]>(LineStyle.class) {
                {
                    // GEMPAK line type 1
                    put(LineStyle.SOLID, new int[] { 4 });
                    // GEMPAK line type 2
                    put(LineStyle.SHORT_DASHED, new int[] { 4, 4 });
                    // GEMPAK line type 3
                    put(LineStyle.MEDIUM_DASHED, new int[] { 8, 8 });
                    // GEMPAK line type 4
                    put(LineStyle.LONG_DASH_SHORT_DASH,
                            new int[] { 16, 8, 4, 8 });
                    // GEMPAK line type 5
                    put(LineStyle.LONG_DASHED, new int[] { 16, 8 });
                    // GEMPAK line type 6
                    put(LineStyle.LONG_DASH_THREE_SHORT_DASHES,
                            new int[] { 16, 8, 4, 8, 4, 8, 4, 8 });
                    // GEMPAK line type 7
                    put(LineStyle.LONG_DASH_DOT, new int[] { 16, 8, 2, 8 });
                    // GEMPAK line type 8
                    put(LineStyle.LONG_DASH_THREE_DOTS,
                            new int[] { 16, 8, 2, 8, 2, 8, 2, 8 });
                    // GEMPAK line type 9
                    put(LineStyle.MEDIUM_DASH_DOT, new int[] { 8, 8, 2, 8 });
                    // GEMPAK line type 10
                    put(LineStyle.DOTS, new int[] { 2, 4 });
                }
            });

    protected final INatlCntrsResourceData rscData;

    protected final Capabilities capabilities;

    protected Shell shell;

    protected String dlgTitle;

    protected ResourceAttrSet editedRscAttrSet = null;

    protected boolean hasApplyBtn = false;

    protected boolean ok = false;

    public AbstractEditResourceAttrsDialog(Shell parentShell,
            INatlCntrsResourceData r, Capabilities capabilities,
            Boolean apply) {
        super(parentShell);

        this.rscData = r;
        this.capabilities = capabilities;
        ResourceName rscName = this.rscData.getResourceName();
        this.dlgTitle = String.format("Edit %s Attributes", rscName);

        hasApplyBtn = apply;
    }

    public abstract Composite createDialog(Composite topComp);

    // initialize the GUI from editedRscAttrSet
    public abstract void initWidgets();

    public void createShell() {
        int style = SWT.DIALOG_TRIM | SWT.RESIZE;
        if (!hasApplyBtn) {
            style |= SWT.APPLICATION_MODAL;
        }

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
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        sep.setLayoutData(gd);

        Composite okCanComp = new Composite(shell, SWT.NONE);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.CENTER;
        okCanComp.setLayoutData(gd);

        okCanComp.setLayout(new GridLayout((hasApplyBtn ? 3 : 2), true));

        Button canBtn = new Button(okCanComp, SWT.PUSH);
        canBtn.setText(" Cancel ");

        canBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ok = false;
                shell.dispose();
            }
        });

        if (hasApplyBtn) {
            Button applyBtn = new Button(okCanComp, SWT.PUSH);
            applyBtn.setText(" Apply ");

            applyBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    rscData.setRscAttrSet(editedRscAttrSet);
                    rscData.setIsEdited(true);
                    AbstractEditor ed = NcDisplayMngr
                            .getActiveNatlCntrsEditor();
                    if (ed != null) {
                        ed.refresh();
                    }
                }
            });
        }

        Button okBtn = new Button(okCanComp, SWT.PUSH);
        okBtn.setText("    OK    ");

        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleOK();
            }
        });
    }

    protected void handleOK() {
        ok = true;
        shell.dispose();
    }

    public boolean isOpen() {
        return shell != null && !shell.isDisposed();
    }

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
        // Uses Java Bean utils to set the attributes on the resource
        if (ok) {

            rscData.setRscAttrSet(editedRscAttrSet);
            rscData.setIsEdited(true);
        }

        dispose();

        return null;
    }

    // allow to override
    protected void dispose() {
    }
}
