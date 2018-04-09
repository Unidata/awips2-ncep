package gov.noaa.nws.ncep.viz.tools.imageProperties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Scale;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.viz.ui.editor.AbstractEditor;

import gov.noaa.nws.ncep.viz.common.display.IPowerLegend;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource2;
import gov.noaa.nws.ncep.viz.ui.display.AbstractNcEditor;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

/**
 * 
 * Contribution item added to the status bar which displays the image fading
 * information.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- -----------  --------------------------
 * 11/5/2009    183        qzhou        Initial created. 
 * 12/15/2009              G. Hull      display and pane listeners.
 * 10/04/2010   289        Archana      Added FadeHotKeyListener
 * 03/07/2011   R1G2-9     G. Hull      implement IVizEditorChangedListener, 
 *                                      editor no longer passed from Pane Changed Listener
 * 06/19/2012   #569       G. Hull      rm IVizEditorChangedListener. update() gets called
 *                                      from refreshGUIElements which is called from perspective's
 *                                      IVizEditorChangedListener
 * 06/21/2012   #632       G. Hull      Change behaviour for multiple images. Activate if all the 
 *                                      brightness's are the same.
 * 07/12/2012   ####       G. Hull      rm paneChangeListener. A pane change will now call refreshGUIElements
 * 02/11/13     #972        G. Hull     AbstractEditor instead of NCMapEditor
 * 09/22/2015   R7270      kbugenhagen  Added check that resource is an 
 *                                      AbstractNatlCntrsResource in updateFadeDisplay 
 *                                      to get rid of alert window for 
 *                                      non-AbstractNatlCntrsResource resources
 *                                      (like GeoTiffResource's).
 * 05/26/2016   R17960     bsteffen     Search within IPowerLegend resources to find image resources.
 * 10/20/2016   R20700     pmoyer       Implemented inclusion of AbstractNatlCntrsResource2 for
 *                                      control activation.
 * 04/09/2018   6889       njensen      Fix brightness compile error
 * </pre>
 * 
 * @author Q. Zhou
 * @version 1
 */
public class FadeDisplay extends ContributionItem {

    private FadeHotKeyListener fadeKeyListener = null;

    private Composite comp;

    private Scale scale;

    private Button btn0;

    private Button btn50;

    private Font font = new Font(Display.getCurrent(), "Monospace", 10,
            SWT.NORMAL);

    private ArrayList<AbstractVizResource<?, ?>> imageResources = null;

    private AbstractNcEditor activeDisp = null;

    /**
     * Constructor
     */
    public FadeDisplay() {
        super();
        imageResources = new ArrayList<>();
        if (fadeKeyListener == null) {
            fadeKeyListener = new FadeHotKeyListener();
        }
    }

    /**
     * Populates the scale and buttons on the bottom bar
     */
    @Override
    public void fill(Composite parent) {

        comp = new Composite(parent, SWT.NONE);
        comp.setSize(200, 55);
        GridLayout gl = new GridLayout(3, false);
        gl.marginTop = 0;
        gl.verticalSpacing = 0;
        comp.setLayout(gl);

        StatusLineLayoutData slLayoutData = new StatusLineLayoutData();
        comp.setLayoutData(slLayoutData);

        /*
         * Add to the bottom bar
         */
        btn0 = new Button(comp, SWT.PUSH);
        btn50 = new Button(comp, SWT.PUSH);
        scale = new Scale(comp, SWT.NONE);

        btn0.setLayoutData(new GridData(25, 25));
        btn0.setText("0");
        btn0.setFont(font);

        btn0.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (AbstractVizResource<?, ?> rsc : imageResources) {
                    if ((rsc instanceof AbstractNatlCntrsResource<?, ?>)
                            || (rsc instanceof AbstractNatlCntrsResource2<?, ?>)) {
                        ImagingCapability imgCap = rsc
                                .getCapability(ImagingCapability.class);
                        imgCap.setBrightness(0.0f);
                    }
                }
                scale.setEnabled(true);
                scale.setSelection(0);
                activeDisp.refresh();
            }
        });

        btn50.setLayoutData(new GridData(25, 25));
        btn50.setText("N");
        btn50.setFont(font);
        btn50.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (AbstractVizResource<?, ?> rsc : imageResources) {
                    if ((rsc instanceof AbstractNatlCntrsResource<?, ?>)
                            || (rsc instanceof AbstractNatlCntrsResource2<?, ?>)) {
                        ImagingCapability imgCap = rsc
                                .getCapability(ImagingCapability.class);
                        imgCap.setBrightness(100 / 100.0f);
                    }
                }
                scale.setEnabled(true);
                scale.setSelection(100);
                activeDisp.refresh();
            }
        });

        scale.setLayoutData(new GridData(160, SWT.DEFAULT));
        scale.setMinimum(0);
        scale.setMaximum(200);
        scale.setIncrement(1);
        scale.setPageIncrement(5);
        scale.setSelection(50);

        scale.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                if (imageResources == null) {
                    return;
                }
                for (AbstractVizResource<?, ?> imgRsc : imageResources) {
                    if ((imgRsc instanceof AbstractNatlCntrsResource<?, ?>)
                            || (imgRsc instanceof AbstractNatlCntrsResource2<?, ?>)) {
                        ImagingCapability imgCap = imgRsc
                                .getCapability(ImagingCapability.class);
                        imgCap.setBrightness(scale.getSelection() / 100.0f);
                    }
                }
                activeDisp.refresh();
            }
        });
        scale.setEnabled(false);
        btn0.setEnabled(false);
        btn50.setEnabled(false);
        scale.setSelection(0);

        update();
    }

    /**
     * The display or selected pane has changed so get the new imageResources
     * and update the widgets with
     * 
     * @return
     */
    private void updateFadeDisplay() { //

        scale.setEnabled(false);
        btn0.setEnabled(false);
        btn50.setEnabled(false);

        AbstractEditor ed = NcDisplayMngr.getActiveNatlCntrsEditor();

        if (ed instanceof AbstractNcEditor) {
            activeDisp = (AbstractNcEditor) ed;
        }

        if (activeDisp == null) {
            return;
        }

        IDisplayPane[] seldPanes = NcEditorUtil.getSelectedPanes(activeDisp);

        imageResources.clear();

        for (IDisplayPane pane : seldPanes) {
            ResourceList rscList = pane.getDescriptor().getResourceList();

            imageResources.addAll(getImageResources(rscList));
        }

        int brightness = -1;

        // the buttons will work with multiple resources but
        // the scale will only work with more than one resource if all the
        // brightness values are the same.
        if (!imageResources.isEmpty()) {
            brightness = (int) (imageResources.get(0)
                    .getCapability(ImagingCapability.class).getBrightness() * 100f);

            // TODO : It is possible that a rsc has no image and so there may
            // not be a conflict. Or there may not be an image for the first
            // frame.
            for (AbstractVizResource<?, ?> imgRsc : imageResources) {
                if ((imgRsc instanceof AbstractNatlCntrsResource<?, ?>)
                        || (imgRsc instanceof AbstractNatlCntrsResource2<?, ?>)) {
                    ImagingCapability imgCap = imgRsc
                            .getCapability(ImagingCapability.class);
                    if (brightness != (int) (imgCap.getBrightness() * 100f)) {
                        brightness = -1;
                        scale.setToolTipText("Fade disabled due to multiple images with different brightnesses.");
                        break;
                    }
                }
            }
        } else {
            scale.setToolTipText("");
        }

        scale.setEnabled(brightness != -1);

        btn0.setEnabled((imageResources.size() >= 1));
        btn50.setEnabled((imageResources.size() >= 1));

        // load the widget with the current value from the image resource or 0
        // if disabled
        if (scale.isEnabled()) {
            scale.setSelection(brightness);
        } else {
            scale.setSelection(0);
        }
    }

    private static List<AbstractVizResource<?, ?>> getImageResources(
            ResourceList list) {
        List<AbstractVizResource<?, ?>> imageResources = new ArrayList<>();
        for (ResourcePair rp : list) {
            if (rp.getProperties().isSystemResource()) {
                continue;
            }
            AbstractVizResource<?, ?> resource = rp.getResource();
            if (resource instanceof AbstractNatlCntrsResource<?, ?>) {
                if (resource.hasCapability(ImagingCapability.class)) {
                    imageResources
                            .add(resource);
                }
            } else if (resource instanceof AbstractNatlCntrsResource2<?, ?>) {
                if (resource.hasCapability(ImagingCapability.class)) {
                    imageResources
                            .add(resource);
                }
            } else if (resource instanceof IPowerLegend) {
                imageResources
                        .addAll(getImageResources(((IPowerLegend) resource)
                                .getResourceList()));
            }
        }
        return imageResources;
    }

    @Override
    public void update() {

        updateFadeDisplay();
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    private class FadeHotKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.keyCode == 'I' || e.keyCode == 'i') {
                if (scale.getSelection() > 0) {
                    scale.setSelection(0);
                } else if (scale.getSelection() == 0) {
                    scale.setSelection(100);
                }

            } else if (e.keyCode == 'F' || e.keyCode == 'f') {
                scale.setSelection(100);
            }

            scale.notifyListeners(SWT.Selection, new Event());
        }

    }
}
