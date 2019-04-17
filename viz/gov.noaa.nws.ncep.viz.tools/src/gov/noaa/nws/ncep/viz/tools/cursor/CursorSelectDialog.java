package gov.noaa.nws.ncep.viz.tools.cursor;

import java.util.Arrays;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;

import gov.noaa.nws.ncep.viz.tools.cursor.NCCursors.Color;
import gov.noaa.nws.ncep.viz.tools.cursor.NCCursors.CursorInfo;
import gov.noaa.nws.ncep.viz.tools.cursor.NCCursors.CursorRef;
import gov.noaa.nws.ncep.viz.tools.cursor.NCCursors.CursorType;

/**
 * This class provides cursor selection and editing in National Centers
 * perspective.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * June 2009     109        M. Li       Initial creation.
 * Feb 15, 2019  7562       tgurney     Rewrite
 *
 * </pre>
 *
 * @author mli
 *
 */

public class CursorSelectDialog extends CaveSWTDialog {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private Combo cursorRefCombo = null;

    private Combo cursorTypeCombo = null;

    private Combo cursorColorCombo = null;

    private Map<CursorRef, CursorInfo> cursorInfos;

    protected CursorSelectDialog(Shell parentShell) {
        super(parentShell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        setText("Cursor Select and Edit");
        cursorInfos = NCCursors.getInstance().getCursorInfos();
    }

    @Override
    protected void initializeComponents(Shell shell) {
        createCursorRefControls();
        addSeparator();
        createCursorTypeAndColorControls();
        addSeparator();
        createBottomButtons();
        refreshComboSelections();
    }

    private void createCursorRefControls() {
        Composite comp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        comp.setLayout(gl);

        new Label(comp, SWT.NONE).setText("CURSOR REF.       ");
        cursorRefCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        cursorRefCombo.setItems(getEnumNames(CursorRef.class));
        cursorRefCombo.select(0);

        cursorRefCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshComboSelections();
            }
        });
    }

    private void createCursorTypeAndColorControls() {
        Composite comp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        comp.setLayout(gl);

        new Label(comp, SWT.NONE).setText("CURSOR TYPE   ");
        cursorTypeCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        cursorTypeCombo.setItems(getEnumNames(CursorType.class));

        cursorTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getSelectedCursorInfo().type = CursorType
                        .valueOf(getComboSelection(cursorTypeCombo));
            }

        });

        Label label = new Label(comp, SWT.NONE);
        label.setText("CURSOR COLOR  ");

        cursorColorCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        cursorColorCombo.setItems(getEnumNames(Color.class));

        cursorColorCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getSelectedCursorInfo().color = Color
                        .valueOf(getComboSelection(cursorColorCombo));
            }
        });
    }

    private void createBottomButtons() {
        Composite centeredComp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(3, true);
        centeredComp.setLayout(gl);
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        centeredComp.setLayoutData(gd);

        Button ok_btn = new Button(centeredComp, SWT.NONE);
        ok_btn.setText("OK");
        ok_btn.setLayoutData(gd);
        ok_btn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Cursor oldCursor = getParent().getCursor();
                CursorRef oldCursorRef = NCCursors.getInstance()
                        .getCursorRef(oldCursor).orElse(CursorRef.DEFAULT);
                for (CursorInfo cursorInfo : cursorInfos.values()) {
                    NCCursors.getInstance().updateCursorInfo(cursorInfo);
                }
                NCCursors.getInstance().setCursor(getParent(), oldCursorRef);

                shell.dispose();
            }
        });

        Button default_btn = new Button(centeredComp, SWT.NONE);
        default_btn.setText("Defaults");
        default_btn.setLayoutData(gd);
        default_btn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    cursorInfos = NCCursors.getInstance()
                            .getDefaultCursorInfos();
                } catch (JAXBException e) {
                    statusHandler.warn("Failed to load cursor table", e);
                }
                cursorRefCombo.select(0);
                refreshComboSelections();
            }
        });

        Button closeBtn = new Button(centeredComp, SWT.NONE);
        closeBtn.setText("Close");
        closeBtn.setLayoutData(gd);
        closeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                shell.dispose();
            }
        });
    }

    private void refreshComboSelections() {
        CursorInfo cursorInfo = getSelectedCursorInfo();
        setComboSelection(cursorTypeCombo, cursorInfo.type.name());
        setComboSelection(cursorColorCombo, cursorInfo.color.name());
    }

    private void addSeparator() {
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        Label sepLbl = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        sepLbl.setLayoutData(gd);
    }

    private CursorInfo getSelectedCursorInfo() {
        return cursorInfos
                .get(CursorRef.valueOf(getComboSelection(cursorRefCombo)));
    }

    private static String getComboSelection(Combo c) {
        return c.getItems()[c.getSelectionIndex()];
    }

    private static void setComboSelection(Combo c, String s) {
        int index = Arrays.asList(c.getItems()).indexOf(s);
        c.select(index);
    }

    private static <T extends Enum<T>> String[] getEnumNames(Class<T> e) {
        return Arrays.stream(e.getEnumConstants()).map(Enum::name)
                .toArray(String[]::new);
    }

}
