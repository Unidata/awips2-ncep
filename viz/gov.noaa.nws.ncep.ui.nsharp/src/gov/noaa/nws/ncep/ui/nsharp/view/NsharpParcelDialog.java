package gov.noaa.nws.ncep.ui.nsharp.view;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;

/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.palette.NsharpParcelDialog
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer       Description
 * ------------- -------- -------------- -----------------------------------------
 * Mar 23, 2010  229      Chin Chen      Initial coding
 * Jul 05, 2016  15923    Chin Chen      NSHARP - Native Code replacement
 * 10/26/2018   DR20904   mgamazaychikov Changed parcel indices from 
 *                                          NsharpNativeConstants to NsharpLibSndglib
 * Dec 20, 2018  7575     bsteffen       Use Parcel numbers from NsharpLibSndglib
 * 
 * </pre>
 * 
 * @author Chin Chen
 */
public class NsharpParcelDialog extends Dialog {
    private static final String CUR_SFC = "Current Surface";

    private static final String FRCST_SFC = "Forecast Surface";

    private static final String MML = "Mean Mixing Layer";

    private static final String MUP = "Most Unstable Parcel";

    private static final String UDL = "User Defined Level";

    private static final String EFF = "Mean Effective Layer";

    private static final int BTN_WIDTH = 300;

    private static final int BTN_HEIGHT = 20;

    private static final int LABEL_GAP = 20;

    private static final int BTN_GAP_X = 5;

    private static final int BTN_GAP_Y = 5;

    private int userDefdParcelMb = 850;
    
    private int curParcelType;

    private Text userDefdMbtext;

    public NsharpParcelDialog(Shell parentShell) {
        super(parentShell);
    }

    private void createDialogContents(Composite parent) {

        final Group btnGp = new Group(parent, SWT.SHADOW_ETCHED_IN);

        Listener radioGpLsner = new Listener() {
            @Override
            public void handleEvent(Event event) {
                Control[] children = btnGp.getChildren();
                for (int j = 0; j < children.length; j++) {
                    Control child = children[j];
                    if (child instanceof Button) {
                        Button button = (Button) child;
                        if (button.getSelection()) {
                            curParcelType = Short.parseShort(button.getData()
                                    .toString());

                            NsharpEditor editor = NsharpEditor
                                    .getActiveNsharpEditor();
                            if (editor != null) {
                                NsharpResourceHandler skewtRsc = NsharpEditor
                                        .getActiveNsharpEditor()
                                        .getRscHandler();
                                if (skewtRsc != null) {
                                    skewtRsc.setCurrentParcel(curParcelType);
                                    editor.refresh();
                                }
                            }
                            // only one button should be selected, get out
                            // of here
                            break;
                        }
                    }
                }
            }
        };
        Button curSfcBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
        curSfcBtn.setText(CUR_SFC);
        curSfcBtn.setEnabled(true);
        curSfcBtn.setBounds(btnGp.getBounds().x + BTN_GAP_X, btnGp.getBounds().y
                + LABEL_GAP, BTN_WIDTH, BTN_HEIGHT);
        curSfcBtn.setData(NsharpLibSndglib.PARCELTYPE_OBS_SFC);
        curSfcBtn.addListener(SWT.MouseUp, radioGpLsner);

        Button frcstBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
        frcstBtn.setText(FRCST_SFC);
        frcstBtn.setEnabled(true);
        frcstBtn.setBounds(btnGp.getBounds().x + BTN_GAP_X,
                curSfcBtn.getBounds().y + curSfcBtn.getBounds().height
                        + BTN_GAP_Y, BTN_WIDTH, BTN_HEIGHT);
        frcstBtn.setData(NsharpLibSndglib.PARCELTYPE_FCST_SFC);
        frcstBtn.addListener(SWT.MouseUp, radioGpLsner);

        Button mmlBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
        mmlBtn.setText(MML);
        mmlBtn.setEnabled(true);
        mmlBtn.setBounds(btnGp.getBounds().x + BTN_GAP_X, frcstBtn.getBounds().y
                + frcstBtn.getBounds().height + BTN_GAP_Y, BTN_WIDTH, BTN_HEIGHT);
        mmlBtn.setData(NsharpLibSndglib.PARCELTYPE_MEAN_MIXING);
        mmlBtn.addListener(SWT.MouseUp, radioGpLsner);
        Button mupBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
        mupBtn.setText(MUP);
        mupBtn.setEnabled(true);
        mupBtn.setBounds(btnGp.getBounds().x + BTN_GAP_X, mmlBtn.getBounds().y
                + mmlBtn.getBounds().height + BTN_GAP_Y, BTN_WIDTH, BTN_HEIGHT);
        mupBtn.setData(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE);
        mupBtn.addListener(SWT.MouseUp, radioGpLsner);
        Button effBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
        effBtn.setText(EFF);
        effBtn.setEnabled(true);
        effBtn.setBounds(btnGp.getBounds().x + BTN_GAP_X, mupBtn.getBounds().y
                + mupBtn.getBounds().height + BTN_GAP_Y, BTN_WIDTH, BTN_HEIGHT);
        effBtn.setData(NsharpLibSndglib.PARCELTYPE_EFF);
        effBtn.addListener(SWT.MouseUp, radioGpLsner);

        Button udlBtn = new Button(btnGp, SWT.RADIO | SWT.BORDER);
        udlBtn.setText(UDL);
        udlBtn.setEnabled(true);
        udlBtn.setBounds(btnGp.getBounds().x + BTN_GAP_X, effBtn.getBounds().y
                + effBtn.getBounds().height + BTN_GAP_Y, BTN_WIDTH, BTN_HEIGHT);
        udlBtn.setData(NsharpLibSndglib.PARCELTYPE_USER_DEFINED);
        udlBtn.addListener(SWT.MouseUp, radioGpLsner);

        udlBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // when CR is entered, this fcn is called.
                // do nothing here.
                // system will call okPressed() next.
            }
        });

        userDefdMbtext = new Text(btnGp, SWT.BORDER | SWT.SINGLE);
        userDefdMbtext.setBounds(btnGp.getBounds().x + BTN_GAP_X,
                udlBtn.getBounds().y + udlBtn.getBounds().height + BTN_GAP_Y,
                BTN_WIDTH / 4, BTN_HEIGHT);
        userDefdMbtext.setEnabled(true);
        userDefdMbtext.setVisible(true);

        // to make sure user enter digits only
        userDefdMbtext.addListener(SWT.Verify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                String string = e.text;
                char[] chars = new char[string.length()];
                string.getChars(0, chars.length, chars, 0);

                for (int i = 0; i < chars.length; i++) {
                    if (!('0' <= chars[i] && chars[i] <= '9')) {
                        e.doit = false;
                        return;
                    }
                }

            }
        });
        NsharpResourceHandler skewtRsc = NsharpEditor.getActiveNsharpEditor()
                .getRscHandler();
        if (skewtRsc != null) {
            userDefdParcelMb = skewtRsc.getWeatherDataStore().getUserDefdParcelMb();
            curParcelType = skewtRsc.getCurrentParcel();
            switch (curParcelType) {
            case NsharpLibSndglib.PARCELTYPE_OBS_SFC:
                curSfcBtn.setSelection(true);
                break;
            case NsharpLibSndglib.PARCELTYPE_EFF:
                effBtn.setSelection(true);
                break;
            case NsharpLibSndglib.PARCELTYPE_FCST_SFC:
                frcstBtn.setSelection(true);
                break;
            case NsharpLibSndglib.PARCELTYPE_MEAN_MIXING:
                mmlBtn.setSelection(true);
                break;
            case NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE:
                mupBtn.setSelection(true);
                break;
            case NsharpLibSndglib.PARCELTYPE_USER_DEFINED:
                udlBtn.setSelection(true);
                break;
            default:
                break;
            }
        }
        userDefdMbtext.setText(Integer.toString(userDefdParcelMb));
    }

    @Override
    public void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        Button okBtn = createButton(parent, IDialogConstants.OK_ID,
                IDialogConstants.OK_LABEL, true);
        okBtn.addListener(SWT.MouseUp, new Listener() {
            @Override
            public void handleEvent(Event event) {
                String textStr = userDefdMbtext.getText();
                if ((textStr != null) && !(textStr.isEmpty())) {
                    userDefdParcelMb = Integer.decode(textStr);
                }
                NsharpResourceHandler skewtRsc = NsharpEditor
                        .getActiveNsharpEditor().getRscHandler();
                skewtRsc.getWeatherDataStore().setUserDefdParcelMb(userDefdParcelMb);
                skewtRsc.setCurrentParcel(curParcelType);
                close();
            }
        });

        Button canBtn = createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CLOSE_LABEL, false);
        canBtn.addListener(SWT.MouseUp, new Listener() {
            @Override
            public void handleEvent(Event event) {
                close();
            }
        });
    }

    // This function name is miss leading....
    // This function is called when CR is preseed, but NOT "ok" button.
    // Override this and move close() from here to OK button Listener
    // So, we only close when "OK" is pressed, not "CR".
    @Override
    public void okPressed() {
        setReturnCode(OK);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Parcels Display Configuration");
    }

    @Override
    public Control createDialogArea(Composite parent) {
        Composite top;
        top = (Composite) super.createDialogArea(parent);

        // Create the main layout for the shell.
        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        top.setLayout(mainLayout);

        // Initialize all of the menus, controls, and layouts
        createDialogContents(top);

        return top;
    }

    @Override
    public int open() {
        if (this.getShell() == null) {
            this.create();
        }
        this.getShell().setLocation(
                this.getShell().getParent().getLocation().x + 1100,
                this.getShell().getParent().getLocation().y + 200);
        return super.open();
    }

}
