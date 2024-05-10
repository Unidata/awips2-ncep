package gov.noaa.nws.ncep.viz.tools.predefinedArea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.geotools.api.geometry.Bounds;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.tools.AbstractTool;

import gov.noaa.nws.ncep.viz.common.area.AreaName.AreaSource;
import gov.noaa.nws.ncep.viz.common.area.PredefinedArea;
import gov.noaa.nws.ncep.viz.common.customprojection.GempakProjectionValuesUtil;
import gov.noaa.nws.ncep.viz.common.display.INatlCntrsRenderableDisplay;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.ui.display.NCMapRenderableDisplay;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayAreaProviderFactory;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

public class CreateCustomProjectionHandler extends AbstractTool implements IHandler {

	private NcCreateProjectionDialog dialog; 
	
    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.ui.tools.AbstractTool#execute(org.eclipse.core.commands.ExecutionEvent)
     */
    @Override
	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		super.execute(arg0);
		 
		Bounds envelope;
		
		ParameterValueGroup parameters;
				
		try {
			AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();
	
			if( editor == null ||
				NcEditorUtil.getNcDisplayType(editor) != NcDisplayType.NMAP_DISPLAY ) {
				throw new VizException("The active editor is not an NMAP display.");
			}
	
			// Note : it is possible that multiple non-geo-synced panes could be selected with 
			// different areas, but in this case we will just get the first one to initialize the 
			// dialog with.
			//
			IDisplayPane[] seldPanes = ( NcEditorUtil.arePanesGeoSynced( editor ) ?
					                     editor.getDisplayPanes() : NcEditorUtil.getSelectedPanes(editor) );
			
			if( seldPanes == null || seldPanes.length < 1 ) {
				throw new VizException("Unable to get a selected display pane?.");
			}
			
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	
			// This could/should be MapRenderableDisplay if it gave access to the
			// mapcenter
			if( seldPanes[0].getRenderableDisplay() instanceof NCMapRenderableDisplay &&
				seldPanes[0].getRenderableDisplay().getDescriptor() instanceof IMapDescriptor ) {
				
				NCMapRenderableDisplay disp = (NCMapRenderableDisplay)seldPanes[0].getRenderableDisplay();
				IMapDescriptor descript = (IMapDescriptor)disp.getDescriptor();
				CoordinateReferenceSystem crs = descript.getCRS();
	
				if( crs == null ) {
					throw new VizException("Unable to get Projection from the current display.");
				}
			
				String areaPrjName = null;
			
				areaPrjName = crs.getName().toString();

				parameters = CRS.getMapProjection(crs).getParameterValues();
				
				List<String> selectableProjs = NcProjectionList( selectProjectionMapFromGempak() );

				if( !selectableProjs.contains( areaPrjName ) ) {
					selectableProjs.add( areaPrjName );
				}
				
				if( dialog == null ) {
					dialog = new NcCreateProjectionDialog( shell );
				}
				
//				int mapHeight = computeHeight( descript.getGridGeometry(), disp.getZoomLevel() );
//				int mapWidth  = (int)(((double)descript.getMapWidth() ) * disp.getZoomLevel());

				dialog.initializeDialog( selectableProjs, areaPrjName, parameters, 
						disp.getMapCenter() );//, mapWidth, mapHeight );   
				
				dialog.setPanesToReproject( seldPanes );
				
				if( dialog.open() == Window.OK ) {
					for( IDisplayPane pane : seldPanes ) {						
						if( pane.getRenderableDisplay() instanceof INatlCntrsRenderableDisplay ) {
							INatlCntrsRenderableDisplay dispPane = (INatlCntrsRenderableDisplay)pane.getRenderableDisplay();
							PredefinedArea parea = NcDisplayAreaProviderFactory.createPredefinedAreaFromRenderableDisplay( 
									dispPane, AreaSource.DISPLAY_AREA );
							// Note : the zoomLevel was set to 1.0 in the VizDisplayPane but since the paint() sets
							// the renderable displays zoomLevel and it hasn't had a chance to execute yet, we will
							// go ahead and set the zoom level here.
							//
							parea.setZoomLevel( Double.toString( pane.getZoomLevel() ) );

							dispPane.setInitialArea( parea );
						}						
					}
				}				
			}
		}
		catch (VizException ve ) {
			MessageDialog errDlg = new MessageDialog( 
					NcDisplayMngr.getCaveShell(), "Error", null, 
					ve.getMessage(),
					MessageDialog.ERROR, new String[]{"OK"}, 0);
			errDlg.open();
			return null;
		}
		finally {
			dialog = null;
		}

		return null;
	}

    private Map<String, String> selectProjectionMapFromGempak() {
    	
    	Map<String, String> prjMap = GempakProjectionValuesUtil.initializeProjectionNameMap();
		Map<String, String> listMap = new HashMap<String, String>(10);
		for( String prjKey : prjMap.keySet() ) {
			String prjName = prjMap.get(prjKey);

			if( listMap.size() == 0 ) {
				listMap.put(prjKey, prjName);
			}
			else {			
				boolean b = listMap.values().contains(prjName);
				if( !b ) {
					listMap.put(prjKey, prjName);
					
				}
			}
		}
		
		return listMap;
    }
    
    public List<String> NcProjectionList(Map<String, String> prjMap) {
    	
    	String[] ncPrjLst = new String[prjMap.size()];
    	          
    	int i = 0;
    	for( String prjKey : prjMap.keySet() ) {
			String prjName = prjMap.get(prjKey);
			ncPrjLst[i] = prjName;
			i++;
		}
    	
		Arrays.sort(ncPrjLst);
	
    	return new ArrayList<String>( Arrays.asList(ncPrjLst) );
    }

    // NOTE : this will not work correctly; if an import projection/area
    // is implemented to initialize the mapCenter/width/height then will need
    // to compute this (and the width since the mapWidth is also incorrect in 
    // many cases) 
    // 
//	public int computeHeight( GeneralGridGeometry gridGeometry,
//			double zoomLevel ) throws VizException {
//		
//		CoordinateReferenceSystem crs = gridGeometry.getCoordinateReferenceSystem();
//		MathTransform mapToCoordinateTransform = gridGeometry
//										.getGridToCRS(PixelInCell.CELL_CENTER);
//
//		if (crs.getCoordinateSystem().getDimension() != 2) {
//			throw new VizException("CRS dimension != 2");
//		}
//
//		double centerX = (gridGeometry.getGridRange().getLow(0) + gridGeometry
//				.getGridRange().getHigh(0)) / 2;
//
//		double centerY = (gridGeometry.getGridRange().getLow(1) + gridGeometry
//				.getGridRange().getHigh(1)) / 2;
//
//		DirectPosition s1, d1, s2, d2;
//		s1 = new DirectPosition2D(centerX, centerY);
//		d1 = new DirectPosition2D(centerX, centerY + 1);
//		s2 = new DirectPosition2D(crs);
//		d2 = new DirectPosition2D(crs);
//
//		try {
//			mapToCoordinateTransform.transform(s1, s2);
//			mapToCoordinateTransform.transform(d1, d2);
//
//			GeodeticCalculator gc = new GeodeticCalculator(crs);
//			gc.setStartingPosition(s2);
//			gc.setDestinationPosition(d2);
//
//			double distance = gc.getOrthodromicDistance();
//
//			return (int) (distance * gridGeometry.getGridRange().getHigh(1) * zoomLevel);
//
//		} 
//        catch (MismatchedDimensionException e) {
//			throw new VizException( e );
//		} 
//        catch (TransformException e) {
//			throw new VizException( e );		
//		}			
//	}
}