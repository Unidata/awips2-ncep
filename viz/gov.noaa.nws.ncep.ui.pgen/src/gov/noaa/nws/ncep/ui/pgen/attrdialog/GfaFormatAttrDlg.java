/*
 * gov.noaa.nws.ncep.ui.pgen.tools.GfaFormatAttrDlg
 *
 * June 2010
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.gfa.Gfa;
import gov.noaa.nws.ncep.ui.pgen.store.PgenStorageException;
import gov.noaa.nws.ncep.ui.pgen.store.StorageUtils;
import gov.noaa.nws.ncep.ui.pgen.tools.PgenGfaFormatTool;

/**
 * Create a dialog for PGEN format action.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 06/10        #223        M.Laryukhin Initial creation
 * 07/11        ?           B. Yin      Use fixed font for text message.
 * 04/29        #977        S. Gilbert  PGEN Database support
 * 07/13        ?           J. Wu       Reposition "Generate" and "Cancel".
 * 03/20/2019   #7572       dgilling    Don't enable Generate button unless all necessary
 *                                      options have been selected.
 *
 * </pre>
 *
 * @author M.Laryukhin
 */
public class GfaFormatAttrDlg extends AttrDlg {

    private static final String ZULU = "ZULU";

    private static final String TANGO = "TANGO";

    private static final String SIERRA = "SIERRA";

    private static final String EAST = "EAST";

    private static final String CENTRAL = "CENTRAL";

    private static final String WEST = "WEST";

    private static GfaFormatAttrDlg instance;

    // radio buttons
    private Button nrmlBtn;

    private Button testBtn;

    private static final String SAVE_LABEL = "Generate/Save";

    // checkboxes
    private Button westBtn, slcBtn, sfoBtn;

    private Button centralBtn, chiBtn, dfwBtn;

    private Button eastBtn, bosBtn, miaBtn;

    private Button sierraBtn, tangoBtn, zuluBtn;

    // last used
    private boolean lastNrml = true;

    private boolean lastWest, lastSlc, lastSfo, lastCentral, lastChi, lastDfw;

    private boolean lastEast, lastBos, lastMia, lastSierra, lastTango,
            lastZulu;

    // text area
    private Text text;

    private PgenGfaFormatTool pgenGfaFormatTool;

    /**
     * Private constructor
     *
     * @param parShell
     * @throws VizException
     */
    private GfaFormatAttrDlg(Shell parShell) {
        super(parShell);
    }

    /**
     * Creates an extrapolation dialog if the dialog does not exist and returns
     * the instance. If the dialog exists, return the instance.
     *
     * @param parShell
     * @return
     */
    public static synchronized GfaFormatAttrDlg getInstance(Shell parShell) {
        if (instance == null) {
            instance = new GfaFormatAttrDlg(parShell);
        }

        return instance;
    }

    @Override
    public Control createDialogArea(Composite parent) {
        Composite top = (Composite) super.createDialogArea(parent);

        // Create the main layout for the shell.
        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        top.setLayout(mainLayout);

        // Initialize all of the menus, controls, and layouts
        initializeComponents(top);

        return top;
    }

    /**
     * Creates buttons, menus, and other controls in the dialog area
     *
     * @param parent
     */
    private void initializeComponents(Composite parent) {
        getShell().setText("AIRMET Format");

        createFirstRowBtns(parent);

        createWCEBtns(parent);

        createSierraTangoZuluBtns(parent);

        createTextArea(parent);

        addSelectionlisteners();
    }

    private void createFirstRowBtns(Composite parent) {
        Composite comp = createComposite(parent, 2);

        nrmlBtn = new Button(comp, SWT.RADIO);
        nrmlBtn.setSelection(lastNrml);
        nrmlBtn.setText("NRML");
        testBtn = new Button(comp, SWT.RADIO);
        testBtn.setSelection(!lastNrml);
        testBtn.setText("TEST");
    }

    private Composite createComposite(Composite parent, int columns) {
        Composite borderComp = new Composite(parent, SWT.BORDER);
        borderComp.setLayout(new GridLayout());
        borderComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        Composite comp = new Composite(borderComp, SWT.NONE);
        GridLayout layout = new GridLayout(columns, true);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        comp.setLayout(layout);
        comp.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, false));
        return comp;
    }

    private void createWCEBtns(Composite parent) {
        Composite comp = createComposite(parent, 3);

        westBtn = createCheckBtn(comp, WEST, lastWest);
        centralBtn = createCheckBtn(comp, CENTRAL, lastCentral);
        eastBtn = createCheckBtn(comp, EAST, lastEast);
        slcBtn = createCheckBtn(comp, Gfa.SLC, lastSlc);
        chiBtn = createCheckBtn(comp, Gfa.CHI, lastChi);
        bosBtn = createCheckBtn(comp, Gfa.BOS, lastBos);
        sfoBtn = createCheckBtn(comp, Gfa.SFO, lastSfo);
        dfwBtn = createCheckBtn(comp, Gfa.DFW, lastDfw);
        miaBtn = createCheckBtn(comp, Gfa.MIA, lastMia);
    }

    private Button createCheckBtn(Composite comp, String str, boolean lastUsed) {
        Button btn = new Button(comp, SWT.CHECK);
        btn.setText(str);
        btn.setSelection(lastUsed);
        btn.setLayoutData(new GridData());
        return btn;
    }

    private void createSierraTangoZuluBtns(Composite parent) {
        Composite comp = createComposite(parent, 3);

        sierraBtn = createCheckBtn(comp, SIERRA, lastSierra);
        tangoBtn = createCheckBtn(comp, TANGO, lastTango);
        zuluBtn = createCheckBtn(comp, ZULU, lastZulu);
    }

    private void createTextArea(Composite parent) {
        text = new Text(parent,
                SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
        text.setEditable(false);
        Font txtFt = new Font(this.getShell().getDisplay(), "Monospace", 12,
                SWT.NORMAL);
        text.setFont(txtFt);

        GC gc = new GC(text);
        int charWidth = gc.getFontMetrics().getAverageCharWidth();
        int charHeight = text.getLineHeight();
        gc.dispose();
        Rectangle size = text.computeTrim(0, 0, 65 * charWidth,
                15 * charHeight);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = size.width;
        gd.heightHint = size.height;
        text.setLayoutData(gd);

        text.addDisposeListener((e) -> {
            txtFt.dispose();
        });
    }

    private void addSelectionlisteners() {
        new ChkBtnSelectionListener(westBtn, slcBtn, sfoBtn);
        new ChkBtnSelectionListener(centralBtn, chiBtn, dfwBtn);
        new ChkBtnSelectionListener(eastBtn, bosBtn, miaBtn);

        // updateLastUsedListener
        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateLastUsed();
            }
        };

        nrmlBtn.addSelectionListener(listener);
        testBtn.addSelectionListener(listener);

        sierraBtn.addSelectionListener(listener);
        tangoBtn.addSelectionListener(listener);
        zuluBtn.addSelectionListener(listener);
    }

    @Override
    public void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setText(SAVE_LABEL);
        getButton(IDialogConstants.CANCEL_ID)
                .setText(IDialogConstants.CANCEL_LABEL);
    }

    @Override
    public void okPressed() {
        // do not close here, just generate

        List<AbstractDrawableComponent> all = new ArrayList<>();
        Product prod = null;
        if (drawingLayer != null) {
            prod = drawingLayer.getActiveProduct();
            for (Layer layer : prod.getLayers()) {
                // formatting each layer separately
                all.addAll(layer.getDrawables());
            }
        }

        String dataURI = null;
        if (prod != null) {
            try {
                prod.setOutputFile(drawingLayer.buildActivityLabel(prod));
                dataURI = StorageUtils.storeProduct(prod);
            } catch (PgenStorageException e) {
                StorageUtils.showError(e);
                return;
            }
        }

        ArrayList<Gfa> allGfa = new ArrayList<>();
        for (AbstractDrawableComponent adc : all) {
            if ((adc instanceof Gfa) && !((Gfa) adc).isSnapshot()) {
                allGfa.add((Gfa) adc);
            }
        }

        // tool
        try {
            StringBuilder sb = pgenGfaFormatTool.generate(drawingLayer, allGfa,
                    getChecked(), getSelectedCategories(), dataURI);
            text.setText(sb.toString());
        } catch (IOException e) {
            text.setText("I/O Error");
            statusHandler.error("I/O error generating AIRMET.", e);
        } catch (JAXBException e) {
            text.setText("Serialization Error");
            statusHandler.error("Serialization error generating AIRMET.", e);
        }

        enableGenerateButton(false);
    }

    private List<String> getChecked() {
        List<String> checked = new ArrayList<>();
        if (slcBtn.getSelection()) {
            checked.add(Gfa.SLC);
        }
        if (sfoBtn.getSelection()) {
            checked.add(Gfa.SFO);
        }
        if (chiBtn.getSelection()) {
            checked.add(Gfa.CHI);
        }
        if (dfwBtn.getSelection()) {
            checked.add(Gfa.DFW);
        }
        if (bosBtn.getSelection()) {
            checked.add(Gfa.BOS);
        }
        if (miaBtn.getSelection()) {
            checked.add(Gfa.MIA);
        }
        return checked;
    }

    private List<String> getSelectedCategories() {
        List<String> cats = new ArrayList<>();
        if (sierraBtn.getSelection()) {
            cats.add(SIERRA);
        }
        if (tangoBtn.getSelection()) {
            cats.add(TANGO);
        }
        if (zuluBtn.getSelection()) {
            cats.add(ZULU);
        }
        return cats;
    }

    @Override
    public void cancelPressed() {
        super.cancelPressed();
        PgenUtil.setSelectingMode();
    }

    /**
     * Set the location of the dialog
     */
    @Override
    public int open() {

        if (this.getShell() == null) {
            this.create();
        }
        if (shellLocation == null) {
            shellLocation = centerOfParent();
        }

        int op = super.open();
        super.enableButtons();
        enableGenerateButton(true);

        return op;
    }

    public Point centerOfParent() {
        Rectangle parentSize = getParentShell().getBounds();
        Rectangle mySize = getShell().getBounds();

        int locationX, locationY;
        locationX = (parentSize.width - mySize.width) / 2 + parentSize.x;
        locationY = (parentSize.height - mySize.height) / 2 + parentSize.y;

        return new Point(locationX, locationY);
    }

    /**
     * Gets values of all attributes of the dialog.
     */
    public Map<String, Object> getAttrFromDlg() {
        return Collections.emptyMap();
    }

    /**
     * Sets values of all attributes of the dialog.
     */
    @Override
    public void setAttrForDlg(IAttribute attr) {
    }

    private void updateLastUsed() {
        lastNrml = nrmlBtn.getSelection();
        lastWest = westBtn.getSelection();
        lastSlc = slcBtn.getSelection();
        lastSfo = sfoBtn.getSelection();
        lastCentral = centralBtn.getSelection();
        lastChi = chiBtn.getSelection();
        lastDfw = dfwBtn.getSelection();
        lastEast = eastBtn.getSelection();
        lastBos = bosBtn.getSelection();
        lastMia = miaBtn.getSelection();
        lastSierra = sierraBtn.getSelection();
        lastTango = tangoBtn.getSelection();
        lastZulu = zuluBtn.getSelection();

        enableGenerateButton(true);
    }

    private void enableGenerateButton(boolean isEnabled) {
        if (isEnabled && !getSelectedCategories().isEmpty()
                && !getChecked().isEmpty()) {
            getButton(IDialogConstants.OK_ID).setEnabled(true);
        } else {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
    }

    /**
     * Selection listener class handles selections.
     *
     * @author mlaryukhin
     *
     */
    private class ChkBtnSelectionListener extends SelectionAdapter {

        private Button b1;

        private Button b2;

        private Button b3;

        public ChkBtnSelectionListener(Button b1, Button b2, Button b3) {
            this.b1 = b1;
            this.b2 = b2;
            this.b3 = b3;
            b1.addSelectionListener(this);
            b2.addSelectionListener(this);
            b3.addSelectionListener(this);
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            super.widgetSelected(e);

            Button source = (Button) e.getSource();

            if (source == b1) {
                boolean select = b1.getSelection();
                b2.setSelection(select);
                b3.setSelection(select);
            } else {
                boolean select = b2.getSelection() && b3.getSelection();
                b1.setSelection(select);
            }

            updateLastUsed();
        }
    }

    public void setGfaFormatTool(PgenGfaFormatTool pgenGfaFormatTool) {
        this.pgenGfaFormatTool = pgenGfaFormatTool;
    }

}
