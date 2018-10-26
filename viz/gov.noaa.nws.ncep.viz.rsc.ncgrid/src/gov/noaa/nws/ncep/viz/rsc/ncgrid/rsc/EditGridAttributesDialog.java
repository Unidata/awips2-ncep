package gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.AbstractEditResourceAttrsDialog;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet.RscAttrValue;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;

/**
 * The grid contour attribute editing dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * March 2010   164          M. Li  
 * July  2010   164          M. Li      Re-build the GUI using GEMPAK style
 * Sept  2010   164          M. Li      Add fint, fline and title
 * Nove  2010                M. Li      Add wind
 * Nov,22 2010  352          X. Guo     Add HILO, HLSYM and move all help functions
 *                                      into NcgridAttributesHelp.java
 * Feb 06, 2012  #538        Q. Zhou    Added skip and filter areas and implements. 
 * Sep 07, 2012  #743        Archana    Added CLRBAR
 * Sep 11, 2013  #1036       S. Gurung  Added TEXT attribute
 * 04/05/2016   R15715       dgilling   Refactored for new AbstractEditResourceAttrsDialog constructor.
 * Oct 17, 2016	 ----        M. James   Use single form text editor for attributes
 * 
 * @author mli
 * @version 1
 */

public class EditGridAttributesDialog extends AbstractEditResourceAttrsDialog {
	
    private RscAttrValue cint = null;

    private RscAttrValue glevel = null;

    private RscAttrValue gvcord = null;

    private RscAttrValue skip = null;

    private RscAttrValue filter = null;

    private RscAttrValue scale = null;

    private RscAttrValue gdpfun = null;

    private RscAttrValue type = null;

    private RscAttrValue lineAttr = null;

    private RscAttrValue fint = null;

    private RscAttrValue fline = null;

    private RscAttrValue hilo = null;

    private RscAttrValue hlsym = null;

    private RscAttrValue wind = null;

    private RscAttrValue title = null;

    private RscAttrValue colors = null;

    private RscAttrValue marker = null;

    private RscAttrValue grdlbl = null;

    private RscAttrValue clrbar = null;

    private RscAttrValue text = null;

    private StyledText gempakText;

    /**
     * Constructor
     * 
     * @param parentShell
     * @param dialogTitle
     */
    public EditGridAttributesDialog(Shell parentShell,
            INatlCntrsResourceData rd, Capabilities capabilities, Boolean apply) {
        super(parentShell, rd, capabilities, apply);
    }

    @Override
    public Composite createDialog(Composite composite) {

        lineAttr = editedRscAttrSet.getRscAttr("lineAttributes");
        
        cint = editedRscAttrSet.getRscAttr("cint");
        
        glevel = editedRscAttrSet.getRscAttr("glevel");
        
        gvcord = editedRscAttrSet.getRscAttr("gvcord");
        
        skip = editedRscAttrSet.getRscAttr("skip");
        
        filter = editedRscAttrSet.getRscAttr("filter");
        
        scale = editedRscAttrSet.getRscAttr("scale");
        
        gdpfun = editedRscAttrSet.getRscAttr("gdpfun");
        
        type = editedRscAttrSet.getRscAttr("type");
        
        fint = editedRscAttrSet.getRscAttr("fint");
        
        fline = editedRscAttrSet.getRscAttr("fline");
        
        hilo = editedRscAttrSet.getRscAttr("hilo");
        
        hlsym = editedRscAttrSet.getRscAttr("hlsym");
        
        wind = editedRscAttrSet.getRscAttr("wind");
        
        title = editedRscAttrSet.getRscAttr("title");
        
        colors = editedRscAttrSet.getRscAttr("colors");
        
        marker = editedRscAttrSet.getRscAttr("marker");
        
        grdlbl = editedRscAttrSet.getRscAttr("grdlbl");
        
        clrbar = editedRscAttrSet.getRscAttr("clrbar");
        
        text = editedRscAttrSet.getRscAttr("text");

        // confirm the classes of the attributes..
        if (clrbar.getAttrClass() != String.class) {
        	printWarning(clrbar);
        }

        if (lineAttr.getAttrClass() != String.class) {
        	printWarning(lineAttr);
        }

        if (cint.getAttrClass() != String.class) {
        	printWarning(cint);
        }

        if (cint == null || ((String) cint.getAttrValue()).trim().length() <= 0) {
            cint.setAttrValue("");
        }

        if (hilo != null && ((String) hilo.getAttrValue()).trim().length() <= 0) {
            hilo.setAttrValue("");
        }

        if (hlsym != null && ((String) hlsym.getAttrValue()).trim().length() <= 0) {
            hlsym.setAttrValue("");
        }

        if (text != null && ((String) text.getAttrValue()).trim().length() <= 0) {
            text.setAttrValue("");
        }

        GridLayout contourIntervalsGridLayout = new GridLayout();
        contourIntervalsGridLayout.numColumns = 2;
        
        /**/
        composite.setLayout(contourIntervalsGridLayout); 
        
        // Single form text editor. Must account for "lineAttribute" instead of "LINE"
        String gempakEditable = "";
        String gempakLabel = null;
        for (String gempakCommand : editedRscAttrSet.getAttrNames()) {
        	gempakLabel = editedRscAttrSet.getRscAttr(gempakCommand).attrName;
        	if (gempakLabel.equalsIgnoreCase("lineAttributes")){
        		gempakLabel = "line";
        	}
    		gempakEditable += 
   				String.format("%-7s", gempakLabel.toUpperCase()) + " = " +
    					editedRscAttrSet.getRscAttr(gempakCommand).getAttrValue().toString() 
    					+ "\n";
        }
        
        gempakText = new StyledText(composite,SWT.MULTI | SWT.H_SCROLL );
        gempakText.setLayoutData(new GridData(760, 400));
        gempakText.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        gempakText.setText(gempakEditable.substring(0, gempakEditable.length() - 2));
        
        gempakText.addModifyListener(new ModifyListener() {
        	public void modifyText(ModifyEvent e) {
	        	setGempakAttrValues();
			}
        });

        return composite;
    }

    public void printWarning(RscAttrValue val) {
        System.out.println("value is not of expected class? "
                + val.getAttrClass().toString());
    }
    @Override
    public void initWidgets() {
        // TODO Auto-generated method stub
    }
    
    public void setAttribute(String gemParm, String parm, RscAttrValue parmObject) {
    	if (gemParm.toLowerCase().startsWith(parm)){
    		parmObject.setAttrValue((String)gemParm.split("=",2)[1].trim());
    	}
    }
    
    public void setGempakAttrValues() {
 		String[] gempakVars = gempakText.getText().trim().split("\n", -1);
 		for (String gemParm: gempakVars) {
 			setAttribute(gemParm, "glevel", glevel);
 			setAttribute(gemParm, "line", lineAttr);
 			setAttribute(gemParm, "cint", cint);
 			setAttribute(gemParm, "gvcord", gvcord);
 			setAttribute(gemParm, "skip", skip);
 			setAttribute(gemParm, "filter", filter);
 			setAttribute(gemParm, "scale", scale);
 			setAttribute(gemParm, "gdpfun", gdpfun);
 			setAttribute(gemParm, "type", type);
 			setAttribute(gemParm, "fint", fint);
 			setAttribute(gemParm, "fline", fline);
 			setAttribute(gemParm, "hilo", hilo);
 			setAttribute(gemParm, "hlsym", hlsym);
 			setAttribute(gemParm, "wind", wind);
 			setAttribute(gemParm, "title", title);
 			setAttribute(gemParm, "colors", colors);
 			setAttribute(gemParm, "marker", marker);
 			setAttribute(gemParm, "grdlbl", grdlbl);
 			setAttribute(gemParm, "clrbar", clrbar);
 			setAttribute(gemParm, "text", text);
 		}
 	}
    
}
