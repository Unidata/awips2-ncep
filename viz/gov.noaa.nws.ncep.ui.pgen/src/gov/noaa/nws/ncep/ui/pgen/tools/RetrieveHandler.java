/*
 * gov.noaa.nws.ncep.ui.pgen.tools.RetrieveHandler
 * 
 * 11 February 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.tools;

import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.controls.RetrieveActivityDialog;
import gov.noaa.nws.ncep.ui.pgen.palette.PgenPaletteWindow;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.ui.tools.AbstractTool;

/**
 * Define a handler for PGEN Activity retrieve controls.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	--------------------------
 * 03/13		#977		S. Gilbert	Modified from PgenFileManageHandler
 * 01/16/2016   5054        randerso    Use proper parent shell
 * May 16, 2016 5640        bsteffen    Access button name through command parameter.
 * 
 * </pre>
 * 
 * @author J. Wu
 * @version 0.0.1
 */
public class RetrieveHandler extends AbstractTool {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        Shell shell = HandlerUtil.getActiveShell(event);

        PgenPaletteWindow pallete = PgenSession.getInstance().getPgenPalette();

        /*
         * Set "active" icon for the palette button corresponding to this tool
         */
        String btnName = event.getParameter("name");
        pallete.setActiveIcon(btnName);

        try {
            RetrieveActivityDialog retrieveDlg = new RetrieveActivityDialog(
                    shell, btnName);
            retrieveDlg.setBlockOnOpen(true);
            retrieveDlg.open();
        } catch (VizException e) {
            e.printStackTrace();
        }

        /*
         * Reset the original icon for the palette button corresponding to this
         * tool
         */
        pallete.resetIcon(btnName);

        return null;
    }

}