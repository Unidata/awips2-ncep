/*
 * gov.noaa.nws.ncep.ui.pgen.tools.RetrieveHandler
 *
 * 11 February 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.tools;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.raytheon.viz.ui.tools.AbstractTool;

import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.controls.RetrieveActivityDialog;
import gov.noaa.nws.ncep.ui.pgen.palette.PgenPaletteWindow;

/**
 * Define a handler for PGEN Activity retrieve controls.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- ------------------------------------------
 * 03/13         977      S. Gilbert  Modified from PgenFileManageHandler
 * Jan 16, 2016  5054     randerso    Use proper parent shell
 * May 16, 2016  5640     bsteffen    Access button name through command
 *                                    parameter.
 * Mar 05, 2019  7752     tjensen     Removed btnName from dialog constructor
 *
 * </pre>
 *
 * @author J. Wu
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

        RetrieveActivityDialog retrieveDlg = new RetrieveActivityDialog(shell);
        retrieveDlg.setBlockOnOpen(true);
        retrieveDlg.open();

        /*
         * Reset the original icon for the palette button corresponding to this
         * tool
         */
        pallete.resetIcon(btnName);

        return null;
    }

}