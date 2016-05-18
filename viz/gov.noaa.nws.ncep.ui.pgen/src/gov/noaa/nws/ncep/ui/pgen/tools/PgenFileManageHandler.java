/*
 * gov.noaa.nws.ncep.ui.pgen.controls.PgenFileManageHandler
 * 
 * 11 February 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.tools;

import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.controls.PgenFileManageDialog;
import gov.noaa.nws.ncep.ui.pgen.controls.PgenFileManageDialog1;
import gov.noaa.nws.ncep.ui.pgen.palette.PgenPaletteWindow;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.ui.tools.AbstractTool;

/**
 * Define a handler for PGEN file open/save controls.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	--------------------------
 * 02/09		#63			J. Wu   	Initial Creation.
 * 04/09		#103		B. Yin		Extends from AbstractPgenTool
 * 08/09		#335		J. Wu		Redefined "Save"/"Save As"/"Save All".
 * 01/16/2016   5054        randerso    Use proper parent shell
 * May 16, 2016 5640        bsteffen    Access button name through command parameter.
 *
 * </pre>
 * 
 * @author	J. Wu
 * @version	0.0.1
 */
public class PgenFileManageHandler extends AbstractTool {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);
        PgenSession session = PgenSession.getInstance();
        PgenResource resource = session.getPgenResource();
        PgenPaletteWindow palette = session.getPgenPalette();

        String btnName = event.getParameter("name");

        /*
         * Set "active" icon for the palette button corresponding to this tool
         */
        palette.setActiveIcon(btnName);

        String curFile = resource.getActiveProduct().getOutputFile();

        if (curFile != null && btnName.equalsIgnoreCase("Save")) {
            resource.saveCurrentProduct(curFile);
        } else if (curFile != null && btnName.equalsIgnoreCase("Save All")) {
            if (resource.getProducts().size() > 1) {
                resource.saveAllProducts();
            } else {
                resource.saveCurrentProduct(curFile);
            }
        } else { /* "Save As" */
            try {
                PgenFileManageDialog1 file_dlg = new PgenFileManageDialog1(
                        shell, btnName);
                file_dlg.setBlockOnOpen(true);
                file_dlg.open();
            } catch (VizException e) {
                e.printStackTrace();
            }
        }

        /*
         * Reset the original icon for the palette button corresponding to this
         * tool
         */
        palette.resetIcon(btnName);

        return null;
    }

}