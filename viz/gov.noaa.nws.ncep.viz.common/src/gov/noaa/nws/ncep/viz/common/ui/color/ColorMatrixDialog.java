package gov.noaa.nws.ncep.viz.common.ui.color;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Provides a migration of the NMAP "Color Palette" dialog; presents a
 * ColorMatrixSelector in a standalone dialog.
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 07 May 2009  74B         bhebbard    Initial Creation.
 * 11 Dec 2009              bhebbard    Extend (jface) Dialog rather than RTS CaveJFACEDialog,
 *                                      to avoid background repaint (and grey buttons) in TO11D6.
 * 11/25/2016   R21762      P. Moyer    Implemented alpha transparency transmission
 * Aug 22, 2018  #7081      dgilling    Support refactored ColorMatrixSelector.
 * </pre>
 *
 * @author bhebbard
 */

public class ColorMatrixDialog extends Dialog {

    // Current attribute values.

    private RGB color = new RGB(155, 155, 155);

    private Integer alpha = new Integer(255);

    private String title = "Color Palette";

    private boolean alphaActive = false;

    /**
     * Constructor
     *
     * @param parentShell
     * @param dialogTitle
     */
    public ColorMatrixDialog(Shell parentShell, String dialogTitle,
            boolean alpha) {
        super(parentShell);
        this.title = dialogTitle;
        this.alphaActive = alpha;
    }


    @Override
    protected void buttonPressed(int buttonId) {
        super.buttonPressed(buttonId);
    }


    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (title != null) {
            shell.setText(title);
        }
    }


    @Override
    protected void createButtonsForButtonBar(Composite parent) {

        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);

    }


    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new FillLayout());

        final ColorMatrixSelector cms = new ColorMatrixSelector(composite,
                true, true, 0, 0, 18, 22, 0, 6, 4, alphaActive);
        cms.setColorValue(color);
        cms.setAlphaValue(alpha);
        cms.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                color = cms.getColorValue();
                alpha = cms.getAlphaValue();
                // dialog has no explicit "OK"; close immediately on color
                // change
                close();
            }
        });

        applyDialogFont(composite);
        return composite;
    }

    public RGB getColor() {
        return color;
    }

    public void setColor(RGB color) {
        this.color = color;
    }

    public Integer getAlpha() {
        return alpha;
    }

    public void setAlpha(Integer alpha) {
        this.alpha = alpha;
    }
}
