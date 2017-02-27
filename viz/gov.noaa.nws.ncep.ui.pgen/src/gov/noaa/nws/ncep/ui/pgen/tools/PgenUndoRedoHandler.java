/*
 * PgenUndoRedoHandler
 * 
 * Date created: 14 APRIL 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.tools;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.palette.PgenPaletteWindow;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;

import com.raytheon.viz.ui.tools.AbstractTool;

/**
 * This Command Handler is used to request that the PgenCommandManager execute
 * an "undo" or a "redo" of the previous command.
 * 
 * <pre>
 * 
 *    SOFTWARE HISTORY
 *   
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 
 * 04/10        ?          S. Gilbert   Created.
 * 12/09   R5197/TTR1056   J. Wu        Reset to the last action after Undo/Redo.
 * May 16, 2016 5640        bsteffen    Access button name through command parameter.
 * Nov 18, 2016 25955      astrakovsky  Fixed null pointer exception by adding an empty string check.
 * 12/27/2016   R27572     B. Yin       Removed object handle-bars for Ctrl-Z.
 * 
 * </pre>
 * 
 * @author sgilbert
 * 
 */
public class PgenUndoRedoHandler extends AbstractTool {

    String lastActionIcon = "Select";

    /**
     * This method is sends either an "undo" or "redo" request to the current
     * PgenCommandManager
     * 
     * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        PgenSession session = PgenSession.getInstance();
        PgenPaletteWindow palette = session.getPgenPalette();

        String activeIcon = palette.getCurrentAction();
        if (activeIcon != null && activeIcon.length() > 0 && !activeIcon.equals("Undo")
                && !activeIcon.equals("Redo")) {
            lastActionIcon = activeIcon;
        }

        String commandID = palette.getItemMap().get(lastActionIcon)
                .getAttribute("commandId");

        PgenResource resource = session.getPgenResource();
        if (resource != null && resource.isEditable()) {
            String action = event.getParameter("name");
            if ("Undo".equals(action)) {
                session.getCommandManager().undo();
            } else if ("Redo".equals(action)) {
                session.getCommandManager().redo();
            } else {
                action = event.getParameter("action");
                if ("Undo".equals(action)) {
                    session.getCommandManager().undo();
                } else if ("Redo".equals(action)) {
                    session.getCommandManager().redo();
                }
            }
            
            /*
             * Remove handle bars.
             */
            resource.removeSelected();
            PgenUtil.refresh();

            // Reset to the previous command.
            PgenUtil.setCommandMode(commandID);

        }

        return null;
    }
}