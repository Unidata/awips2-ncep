package gov.noaa.nws.ncep.ui.nsharp.view;

/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.view.NsharpHandleArchiveFile
 * 
 * This java class performs the NSHARP loading archived files functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 12/23/2010	229			Chin Chen	Initial coding
 * 08/12/2014               Chin Chen   fixed issue that "load archive file with wrong time line displayed"
 * 12/17/2014   Task#5694   Chin Chen   added nsharpParseAndLoadTextFile() to be used by both NCP and D2D perspectives,
 *                                      also modified openArchiveFile() to use it.
 * 02/05/2015   Task#5694   Chin Chen   add code to support previous version archived file format 
 * 01/19/2016   5054        randerso    Changed to use MessageDialog to avoid creation of dummy shell 
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.NsharpStationInfo;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpDataHandling;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpNativeConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class NsharpHandleArchiveFile {
	/*
	 * Chin 12/17/2014, this function is to parse Nsharp saved text file and return saved 
	 * station information and sounding data information.
	 * 
	 * 	Input: 
	 *  String textFilePath: file path of text file to be parsed
	 *	List<NcSoundingLayer> sndLyList: a new created empty NcSoundingLayer List
	 *	NsharpStationInfo stninfo: a new created NsharpStationInfo instance
	 *	Return:
	 *  Boolean: true if parsing successful, false otherwise
	 *  Output:
	 *	The nsharpTextfileParser will parse input text file and store available station 
	 *  information using structure stninfo and sounding data using list sndLyList.
	 *  
	 *      
     * A typical saved file contents is as following....
     * SNDTYPE=OBS;  TITLE=KCHH 11.12(Thu) BUFRUA; STNID=KCHH; LAT=41.66667; LON=-69.96667;
     * PRESSURE HGHT TEMP DWPT WDIR WSPD OMEG 
     * 997.500000 129.000000 -3.250006 -3.381190 10.619656 1.627882 -9999.000
     * ........
     * 
     * NOTE: For backward compatible we also handle previous version text file 
     * A typical previous version of saved file contents is as following....
     */
	private static boolean nsharpTextfileParser(String textFilePath, List<NcSoundingLayer> sndLyList, 
			NsharpStationInfo stninfo) throws FileNotFoundException{
		StringBuilder strContent = new StringBuilder("");
		InputStream is = new FileInputStream(textFilePath);
		int byteread;
		try {
			while ((byteread = is.read()) != -1) {
				strContent.append((char) byteread);
				// System.out.println((char)byteread);
			}
			if (strContent.length() <= 100){
				is.close();
				return false;
			}
			//task#5694
			String pStr = "PRESSURE";
			int pIndex = strContent.indexOf(pStr);
			String headerString = strContent.substring(0, pIndex);
			
			String omegaStr = "OMEG";
			int omegaIndex = strContent.indexOf(omegaStr);
			int omegaLength = omegaStr.length();
			String dataString = strContent.substring(omegaIndex+omegaLength);
			dataString = dataString.trim();
			
			String lonStr="yy",latStr="yy";
			// System.out.println(strContent);
			int lat = headerString.indexOf("LAT=");
			if (lat > 0) {
				lat = lat + 4;
				int endIndex = headerString.substring(lat).indexOf(";");
				if (endIndex > 0) {
					latStr = headerString.substring(lat, lat
							+ endIndex);
					// stninfo.setLatitude(Float.parseFloat(latStr));
					stninfo.setLatitude(Double.parseDouble(latStr));
				}
				else{
					//backward compatible handling
					// get up to 4 digits for lon
					try {
						latStr = headerString.substring(lat, lat
								+ 4);
						stninfo.setLatitude(Double.parseDouble(latStr));
					}
					catch (IndexOutOfBoundsException e) {
						e.printStackTrace();
					}

				}
			}
			int lon = headerString.indexOf("LON=");
			if (lon > 0) {
				lon = lon + 4;
				int endIndex = headerString.substring(lon).indexOf(";");
				if (endIndex > 0) {
					lonStr = headerString.substring(lon, lon
							+ endIndex);
					stninfo.setLongitude(Double.parseDouble(lonStr));
				}
				else{
					//backward compatible handling
					// get up to 4 digits for lon
					try {
						lonStr = headerString.substring(lon, lon
								+ 4);
						stninfo.setLongitude(Double.parseDouble(lonStr));
					}
					catch (IndexOutOfBoundsException e) {
						e.printStackTrace();
					} 

				}
			}
			int snd = headerString.indexOf("SNDTYPE=");
			if (snd >= 0) {
				snd = snd + 8;
				int endIndex = headerString.substring(snd).indexOf(";");
				if (endIndex > 0) {
					String sndStr = headerString.substring(snd, snd
							+ endIndex);
					stninfo.setSndType(sndStr);
					if (NsharpLoadDialog.getAccess() != null) {
						if (sndStr.equals("PFC"))
							NsharpLoadDialog.getAccess()
							.setActiveLoadSoundingType(
									NsharpLoadDialog.PFC_SND);
						else if (sndStr.equals("MDL"))
							NsharpLoadDialog.getAccess()
							.setActiveLoadSoundingType(
									NsharpLoadDialog.MODEL_SND);
						else if (sndStr.equals("OBS"))
							NsharpLoadDialog.getAccess()
							.setActiveLoadSoundingType(
									NsharpLoadDialog.OBSER_SND);
					}
				}
			}
			
			int title = headerString.indexOf("TITLE=");
			if (title > 0) {
				title = title + 6;
				int endIndex = headerString.substring(title).indexOf(";");
				if (endIndex > 0) {
					String titleStr = headerString.substring(title, title
							+ endIndex);
					stninfo.setStnDisplayInfo(titleStr);
				}
				else{
					is.close();
					return false;
				}
			}
			else {
				//backward compatible handling
				//Two possible header formats supported here.
				//format 1,
				//BUFRUA  PTPN 06.12(Fri) BUFRUA  LAT=6.966670036315918 LON=158.2166748046875
				// title is "PTPN 06.12(Fri) BUFRUA"
				//format 2.
				//NAM NAM12  PointA 150125/03(Sun) NAM12  LAT=39.66764519797457 LON=-105.92044834352876
				// title is "PointA 150125/03(Sun) NAM12"
				StringTokenizer hdrst = new StringTokenizer(strContent.toString());
				int j =0;
				int titleIndex =0;
				
				while (hdrst.hasMoreTokens()) {
					j++;
					String tok = hdrst.nextToken();
					if(tok.contains("LAT")){
						titleIndex = j-3;
					}
				}
				String tStr="";
				boolean titleFound = false;
				j=0;
				hdrst = new StringTokenizer(strContent.toString());
				if(titleIndex >0){
					while (hdrst.hasMoreTokens()) {
						j++;
						String tok = hdrst.nextToken();

						if(j == titleIndex){
							tStr = tok;
							stninfo.setStnId(tok);
						}
						if(j > titleIndex && j < titleIndex+3){
							tStr =tStr + " "+ tok;
						}
						if (j == titleIndex+3 ){
							titleFound = true;
							stninfo.setStnDisplayInfo(tStr);
							break;
						}
					}
				}
				if(titleFound == false){
					stninfo.setStnDisplayInfo(latStr+"/"+lonStr+" ----NA N/A");
					stninfo.setStnId(latStr+"/"+lonStr);
				}
			}
			NcSoundingLayer sndLy = null;
			StringTokenizer st = new StringTokenizer(dataString);
			int i = 0;
			//seven parameters: PRESSURE  HGHT	   TEMP	  DWPT    WDIR     WSPD    OMEG
			int dataCycleLength = 7; 
			while (st.hasMoreTokens()) {				
				String tok = st.nextToken();
				if (i % dataCycleLength == 0) {
					sndLy = new NcSoundingLayer();
					sndLyList.add(sndLy);
					if (Float.isNaN(Float.parseFloat(tok)))
						sndLy.setPressure(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
					else
						sndLy.setPressure(Float.parseFloat(tok));

				} else if (i % dataCycleLength == 1) {
					if (Float.isNaN(Float.parseFloat(tok)))
						sndLy.setGeoHeight(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
					else
						sndLy.setGeoHeight(Float.parseFloat(tok));
				} else if (i % dataCycleLength == 2) {
					if (Float.isNaN(Float.parseFloat(tok)))
						sndLy.setTemperature(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
					else
						sndLy.setTemperature(Float.parseFloat(tok));
				} else if (i % dataCycleLength == 3) {
					if (Float.isNaN(Float.parseFloat(tok)))
						sndLy.setDewpoint(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
					else
						sndLy.setDewpoint(Float.parseFloat(tok));
				} else if (i % dataCycleLength == 4) {
					if (Float.isNaN(Float.parseFloat(tok)))
						sndLy.setWindDirection(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
					else
						sndLy.setWindDirection(Float
								.parseFloat(tok));
				} else if (i % dataCycleLength == 5) {
					if (Float.isNaN(Float.parseFloat(tok)))
						sndLy.setWindSpeed(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
					else
						sndLy.setWindSpeed(Float.parseFloat(tok));
				} else if (i % dataCycleLength == 6) {
					if (Float.isNaN(Float.parseFloat(tok)))
						sndLy.setOmega(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
					else
						sndLy.setOmega(Float.parseFloat(tok));

				}
				i++;
			}
			is.close();
			if (sndLyList.size() > 0) {
				// Remove sounding layers that not used by NSHARP, and
				// assume first layer is sfc layer from input data
				sndLyList = NsharpDataHandling
						.organizeSoundingDataForShow(sndLyList,
								sndLyList.get(0).getGeoHeight());
				return true;
			}
			else 
				return false;

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}
	}
	//Task#5694
    private static void displayMessage(Shell shell, String msg) {
        MessageDialog.openWarning(shell, "WARNING!", msg);
    }

    public static boolean nsharpParseAndLoadTextFile(String textFilePath) throws FileNotFoundException{
		List<NcSoundingLayer> sndLyList = new ArrayList<NcSoundingLayer>();
        NsharpStationInfo stninfo = new NsharpStationInfo();
        if(nsharpTextfileParser(textFilePath,sndLyList,stninfo) == false)
        {
            /*
             * TODO: It would be better if a shell were passed into this
             * method to be used as the parent of the message dialog
             */
            displayMessage(null, "Invalid sounding data retrieved from archive file " + textFilePath + "!!");
        	return false;        
        }
        else {
        	// minimum rtnSndList size will be 2 (50 & 75 mb layers),
            // but that is not enough
            // We need at least 2 regular layers for plotting
        	Map<String, List<NcSoundingLayer>> soundingLysLstMap = new HashMap<String, List<NcSoundingLayer>>();
            if (sndLyList != null && sndLyList.size() > 4)
            	soundingLysLstMap.put(stninfo.getStnDisplayInfo(), sndLyList);
            if (soundingLysLstMap.size() > 0) {
            	
            	// create/open  NsharpSkewTEditor
            	NsharpEditor editor = NsharpEditor.createOrOpenEditor();
            	NsharpResourceHandler rsc = editor.getRscHandler();
            	rsc.addArchiveRsc(soundingLysLstMap, stninfo);
            	rsc.setSoundingType(stninfo.getSndType());
            	NsharpEditor.bringEditorToTop();
            	
            } 
            else{
            	displayMessage(null, "Invalid sounding data retrieved from archive file "+ textFilePath+ "!!");
            	return false;
            }
        }
        return true;
	}
	//end Task#5694
    public static void openArchiveFile(Shell shell) {
         FileDialog fd = new FileDialog(shell, SWT.MULTI);
        fd.setText("Open");
        fd.setFilterPath("C:/");
        String[] filterExt = { "*.nsp", "*", "*.txt", "*.doc", ".rtf", "*.*" };
        fd.setFilterExtensions(filterExt);
        String selectedFilePath;
        String code = fd.open();
        if (code == null) {
            return;
        }
        String[] selecteds = fd.getFileNames();
        if (selecteds != null && selecteds.length > 0) {
           // Map<String, List<NcSoundingLayer>> soundingLysLstMap = new HashMap<String, List<NcSoundingLayer>>();
            try {
                for (int j = 0; j < selecteds.length; j++) {
                    selectedFilePath = "";
                    selectedFilePath = selectedFilePath + fd.getFilterPath();
                    if (selectedFilePath.charAt(selectedFilePath.length() - 1) != File.separatorChar) {
                        selectedFilePath = selectedFilePath + (File.separatorChar);
                    }
                    selectedFilePath = selectedFilePath + selecteds[j];
                    //Task#5694
                    nsharpParseAndLoadTextFile(selectedFilePath);
                 }
            } catch (FileNotFoundException e) {
            	displayMessage(shell, "File not found!!");
                e.printStackTrace();
            } catch (NumberFormatException e) {
                displayMessage(shell, "Parsing file and number format exception happened!!");
                e.printStackTrace();
            } 
        }

    }
}
