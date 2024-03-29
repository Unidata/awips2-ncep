/*
 * gov.noaa.nws.ncep.ui.pgen.tools.PgenOutlookSetCont
 * 
 * 19 April 2010
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.tools;

import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.OutlookAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.Outlook;

import com.raytheon.uf.viz.core.rsc.IInputHandler;
import org.locationtech.jts.geom.Coordinate;


/**
 * Implements a modal map tool to set continue lines for outlooks.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	--------------------------
 * 04/10			?		B. Yin   	Initial Creation.
 * May 16, 2016 5640        bsteffen    Access triggering component using PgenUtil.
 * 
 * </pre>
 * 
 * @author	B. Yin
 */
public class PgenOutlookSetCont extends AbstractPgenDrawingTool{

	//the Outlook working on
	private Outlook otlk;

	/**
	 * public constructor
	 */
	public PgenOutlookSetCont(){

		super();

	}
	
	@Override
	protected void activateTool( ) {
		super.activateTool();

        AbstractDrawableComponent triggerComponent = PgenUtil
                .getTriggerComponent(event);
        if (triggerComponent instanceof Outlook) {
            otlk = (Outlook) triggerComponent;
        }
	}


	@Override
	/**
	 * Return the current mouse handler
	 */
	public IInputHandler getMouseHandler() {

		if ( this.mouseHandler == null ) {

			this.mouseHandler = new PgenOutlookSetContHandler();

		}

		return this.mouseHandler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.raytheon.viz.ui.tools.AbstractModalTool#deactivateTool()
	 */
	@Override
	public void deactivateTool() {

		super.deactivateTool();

	}

	/**
	 * Implements input handler for mouse events.
	 * @author bingfan
	 *
	 */
	private class PgenOutlookSetContHandler extends InputHandlerDefaultImpl {

		DECollection dec;
		
		public PgenOutlookSetContHandler(){
			super();
			dec = null;
		}
		/*
		 * (non-Javadoc)
		 * 
		 * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseDown(int,
		 *      int, int)
		 */
		@Override	
		public boolean handleMouseDown(int anX, int aY, int button) {

			if (  !isResourceEditable() || otlk == null ) return false;
			
			//  Check if mouse is in geographic extent
			Coordinate loc = mapEditor.translateClick(anX, aY);
			if ( loc == null ) return false;

			if ( button == 1 ) {

				Line ln = otlk.getNearestLine( loc );
				
				//check if line is not grouped or in the same group
				if ( ln != null &&( ln.getParent().getParent() == dec ||  ln.getParent().getParent().equals(otlk))){
					
					//if line is in group, remove the line from the group 
					if ( ln.getParent().getParent() == dec ){
						drawingLayer.removeSelected(ln);
			
						otlk.rmLineFromGroup(ln, dec);
						if ( !otlk.contains(dec ) ) dec = null;
						
					}
					else {
						drawingLayer.addSelected(ln);
		
						if ( dec == null )	{
							dec = new DECollection( Outlook.OUTLOOK_LINE_GROUP );
							otlk.add(dec);
						}
						otlk.addLineToGroup(ln, dec);
					}
					
					drawingLayer.removeGhostLine();
					
				}
				else if ( ln != null && ln.getParent().getParent().getName().equalsIgnoreCase(Outlook.OUTLOOK_LINE_GROUP) && dec == null){
					//if the line is in a group when first click
					dec = (DECollection)ln.getParent().getParent();
					drawingLayer.setSelected(ln);
				}
				else if ( ln != null && ln.getParent().getParent().getName().equalsIgnoreCase( Outlook.OUTLOOK_LINE_GROUP) && dec != null){
					// if the ln is in a group that has only one line, the ln can be added to the current group.
					otlk.addLineToGroup(ln, dec);
					drawingLayer.setSelected(ln);
				}
				
				((OutlookAttrDlg)attrDlg).showContLines(otlk);
				mapEditor.refresh();
				return true;

			}
			else if ( button == 3 ) {

				return true;

			}
			else{

				return false;

			}

		}
		   
	    /*
	     * overrides the function in selecting tool
	     */
	    @Override
	    public boolean handleMouseUp(int x, int y, int button){
	        if ( !drawingLayer.isEditable() || shiftDown ) return false;

	        if (button == 3) {

                drawingLayer.removeSelected();
                PgenUtil.loadOutlookDrawingTool();
                dec = null;
                
	            return true;
	        }
	        else {
	            return false;
	        }
	    }
		@Override
		public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        	if ( !isResourceEditable() ) return false;
			return true;
		}
	}

}
