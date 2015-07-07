package gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.AbstractEditResourceAttrsDialog;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet.RscAttrValue;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * The grid contour attribute editing dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * March 2010	164		     M. Li		
 * July	 2010	164		     M. Li		Re-build the GUI using GEMPAK style
 * Sept  2010   164			 M. Li		Add fint, fline and title
 * Nove  2010	             M. Li		Add wind
 * Nov,22 2010  352			 X. Guo     Add HILO, HLSYM and move all help functions
 *                                      into NcgridAttributesHelp.java
 * Feb 06, 2012  #538        Q. Zhou    Added skip and filter areas and implements. 
 * Sep 07, 2012  #743        Archana      Added CLRBAR
 * Sep 11, 2013  #1036       S. Gurung  Added TEXT attribute
 * Jul 07, 2015	 ---         M. James   Modified compoisite to use single form text editor for attributes
 * 
 * @author mli
 * @version 1
 */

public class EditGridAttributesDialog extends AbstractEditResourceAttrsDialog {
    private RscAttrValue cintString = null;
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
    private RscAttrValue hilo =null;
    private RscAttrValue hlsym =null;
    private RscAttrValue wind = null;
    private RscAttrValue title = null;
    private RscAttrValue colors = null;
    private RscAttrValue marker = null;
    private RscAttrValue grdlbl = null;
    private RscAttrValue clrbar = null;
    private RscAttrValue text =null;
    
    private Text glevelText;
    private Text gvcordText;
    private Text skipText;
    private Text filterText;
    private Text scaleText;
    private Text gdpfunText;
    private Text typeText;
    private Text cintText;
    private Text lineAttrText;
    private Text fintAttrText;
    private Text flineAttrText;
    private Text hiloAttrText;
    private Text hlsymAttrText;
    private Text windAttrText;
    private Text titleAttrText;
    private Text colorsText;
    private Text markerText;
    private Text grdlblText;
    private Text clrbarText;
    private Text textAttrText;
    
    private StyledText gempakText;
    /**
     * Constructor
     * 
     * @param parentShell
     * @param dialogTitle
     */
    public EditGridAttributesDialog(Shell parentShell, INatlCntrsResourceData rd, Boolean apply ) {
        super(parentShell, rd, apply);
    }

    @Override
    public Composite createDialog(Composite composite) {
    	
    	lineAttr   = editedRscAttrSet.getRscAttr("lineAttributes");
    	cintString = editedRscAttrSet.getRscAttr("cint");
    	glevel     = editedRscAttrSet.getRscAttr("glevel");
    	gvcord     = editedRscAttrSet.getRscAttr("gvcord");
    	skip       = editedRscAttrSet.getRscAttr("skip");
    	filter     = editedRscAttrSet.getRscAttr("filter");
    	scale      = editedRscAttrSet.getRscAttr("scale");
    	gdpfun     = editedRscAttrSet.getRscAttr("gdpfun");
    	type       = editedRscAttrSet.getRscAttr("type");
    	fint       = editedRscAttrSet.getRscAttr("fint");
    	fline      = editedRscAttrSet.getRscAttr("fline");
    	hilo 	   = editedRscAttrSet.getRscAttr("hilo");
    	hlsym 	   = editedRscAttrSet.getRscAttr("hlsym");
    	wind       = editedRscAttrSet.getRscAttr("wind");
    	title      = editedRscAttrSet.getRscAttr("title");
    	colors     = editedRscAttrSet.getRscAttr("colors");
    	marker     = editedRscAttrSet.getRscAttr("marker");
    	grdlbl     = editedRscAttrSet.getRscAttr("grdlbl");
    	clrbar     = editedRscAttrSet.getRscAttr("clrbar");
    	text 	   = editedRscAttrSet.getRscAttr("text");
    	
    	// confirm the classes of the attributes..
    	if(clrbar.getAttrClass() != String.class ) {
    		System.out.println( "line is not of expected class? "+clrbar.getAttrClass().toString() );
    	}
    	if( lineAttr.getAttrClass() != String.class ) {
    		System.out.println( "line is not of expected class? "+ lineAttr.getAttrClass().toString() );
    	}
    	if (cintString.getAttrClass() != String.class ){
    		System.out.println( "cint is not of expected class? "+ cintString.getAttrClass().toString() );
    	}
    	if (cintString == null 
    			|| ((String)cintString.getAttrValue()).trim().length() <= 0)
    		cintString.setAttrValue((String)"");
    	if ( hilo != null  
    		    && ((String)hilo.getAttrValue()).trim().length() <= 0) {
    		hilo.setAttrValue((String)"");
    	}
    	if ( hlsym != null 
    			&& ((String)hlsym.getAttrValue()).trim().length() <= 0)
    		hlsym.setAttrValue((String)"");
    	if ( text != null 
    			&& ((String)text.getAttrValue()).trim().length() <= 0)
    		text.setAttrValue((String)"");
    	
        GridLayout contourIntervalsGridLayout = new GridLayout();
        contourIntervalsGridLayout.numColumns = 2;
        composite.setLayout(contourIntervalsGridLayout); 
        
        // Single form text editor. Must acccount for "lineAttribute" instead of "LINE"
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
        //Font mono = new Font(composite.getDisplay(), "Lucida Sans Typewriter", 7, SWT.NORMAL);
        //mono.getFontData()[0].setHeight(6);
        gempakText = new StyledText(composite,SWT.MULTI | SWT.H_SCROLL );
        gempakText.setLayoutData(new GridData(560, 460));
        gempakText.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        gempakText.setText(gempakEditable.substring(0, gempakEditable.length() - 2));
        gempakText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setGempakAttrValues();
			}
        });     
        return composite;
    }
    
	@Override
	public void initWidgets() {
		// TODO Auto-generated method stub
	}
	
	public void setGempakAttrValues() {
		String[] gempakVars = gempakText.getText().trim().split("\n", -1);
		// There's surely a better way to do this.
		for (String gemParm: gempakVars) {
			if (gemParm.toLowerCase().startsWith("glevel")) glevel.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("line")) lineAttr.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("cint")) cintString.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("glevel")) glevel.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("gvcord")) gvcord.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("skip")) skip.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("filter")) filter.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("scale")) scale.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("gdpfun")) gdpfun.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("type")) type.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("fint")) fint.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("fline")) fline.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("hilo")) hilo.setAttrValue((String)gemParm.split("=",2)[1].trim());
			if (gemParm.toLowerCase().startsWith("hlsym")) hlsym.setAttrValue((String)gemParm.split("=",2)[1].trim());
	    	if (gemParm.toLowerCase().startsWith("wind")) wind.setAttrValue((String)gemParm.split("=",2)[1].trim());
	    	if (gemParm.toLowerCase().startsWith("title")) title.setAttrValue((String)gemParm.split("=",2)[1].trim());
	    	if (gemParm.toLowerCase().startsWith("colors")) colors.setAttrValue((String)gemParm.split("=",2)[1].trim());
	    	if (gemParm.toLowerCase().startsWith("marker")) marker.setAttrValue((String)gemParm.split("=",2)[1].trim());
	    	if (gemParm.toLowerCase().startsWith("grdlbl")) grdlbl.setAttrValue((String)gemParm.split("=",2)[1].trim());
	    	if (gemParm.toLowerCase().startsWith("clrbar")) clrbar.setAttrValue((String)gemParm.split("=",2)[1].trim());
	    	if (gemParm.toLowerCase().startsWith("text")) text.setAttrValue((String)gemParm.split("=",2)[1].trim());
		}
	}
	
}

