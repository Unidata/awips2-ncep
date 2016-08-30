/*
 * gov.noaa.nws.ncep.ui.pgen.layering.PegnHotKayHandler.java
 *
 * 26 March 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.layering;

import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * Hot key handler for switching between activity layers.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#      Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 05/09        R?           archana     Created
 * 08/15/2016   R21066       J. Wu       Re-implemented.
 * 
 * </pre>
 * 
 * @author archana
 * 
 */
public class PgenLayeringHotKeyHandler extends AbstractHandler {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.
     * ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
         String layerIndexStr = event.getParameter("layerIndex");
         if(layerIndexStr == null || layerIndexStr.isEmpty()){
            return null;
         }

        int layerIndex = Integer.parseInt(layerIndexStr);
        PgenResource pgenResource = PgenUtil.findPgenResource(PgenUtil
                .getActiveEditor());
        if (pgenResource != null && layerIndex > 0) {
            Product activeProduct = pgenResource.getActiveProduct();
            int layerListSize = activeProduct.getLayers().size();

            if (layerListSize >= layerIndex) {
                Layer layerToActivate = activeProduct.getLayer(layerIndex - 1);
                // Switch to the new layer and update on GUI.
                PgenSession.getInstance().getPgenResource().getResourceData()
                        .switchLayer(layerToActivate.getName());
            }

        }

        return null;
    }

}
