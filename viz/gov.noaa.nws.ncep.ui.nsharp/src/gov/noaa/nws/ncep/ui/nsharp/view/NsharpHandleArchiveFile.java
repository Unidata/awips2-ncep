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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
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
     */
	public static boolean nsharpTextfileParser(String textFilePath, List<NcSoundingLayer> sndLyList, 
			NsharpStationInfo stninfo) throws FileNotFoundException {
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
			// System.out.println(strContent);
			int lat = strContent.indexOf("LAT=");
			if (lat > 0) {
				lat = lat + 4;
				int endIndex = strContent.substring(lat).indexOf(";");
				if (endIndex > 0) {
					String latStr = strContent.substring(lat, lat
							+ endIndex);
					// stninfo.setLatitude(Float.parseFloat(latStr));
					stninfo.setLatitude(Double.parseDouble(latStr));
				}
			}
			int lon = strContent.indexOf("LON=");
			if (lon > 0) {
				lon = lon + 4;
				int endIndex = strContent.substring(lon).indexOf(";");
				if (endIndex > 0) {
					String lonStr = strContent.substring(lon, lon
							+ endIndex);
					stninfo.setLongitude(Double.parseDouble(lonStr));
				}
			}
			int snd = strContent.indexOf("SNDTYPE=");
			if (snd >= 0) {
				snd = snd + 8;
				int endIndex = strContent.substring(snd).indexOf(";");
				if (endIndex > 0) {
					String sndStr = strContent.substring(snd, snd
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
			int title = strContent.indexOf("TITLE=");
			if (title > 0) {
				title = title + 6;
				int endIndex = strContent.substring(title).indexOf(";");
				if (endIndex > 0) {
					String titleStr = strContent.substring(title, title
							+ endIndex);
					stninfo.setStnDisplayInfo(titleStr);
				}
				else{
					is.close();
					return false;
				}
			}
			else {
				is.close();
				return false;
			}
			NcSoundingLayer sndLy = null;
			StringTokenizer st = new StringTokenizer(
					strContent.toString());
			int i = 0;
			int dataStartIndex = 15;
			int dataCycleLength = 7;
			while (st.hasMoreTokens()) {
				i++;
				String tok = st.nextToken();
				if (tok.equals("OMEG")) {
					dataStartIndex = i + 1;
				}
				if (i >= dataStartIndex) {

					if ((i - dataStartIndex) % dataCycleLength == 0) {
						sndLy = new NcSoundingLayer();
						sndLyList.add(sndLy);
						if (Float.isNaN(Float.parseFloat(tok)))
							sndLy.setPressure(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
						else
							sndLy.setPressure(Float.parseFloat(tok));

					} else if ((i - dataStartIndex) % dataCycleLength == 1) {
						if (Float.isNaN(Float.parseFloat(tok)))
							sndLy.setGeoHeight(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
						else
							sndLy.setGeoHeight(Float.parseFloat(tok));
					} else if ((i - dataStartIndex) % dataCycleLength == 2) {
						if (Float.isNaN(Float.parseFloat(tok)))
							sndLy.setTemperature(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
						else
							sndLy.setTemperature(Float.parseFloat(tok));
					} else if ((i - dataStartIndex) % dataCycleLength == 3) {
						if (Float.isNaN(Float.parseFloat(tok)))
							sndLy.setDewpoint(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
						else
							sndLy.setDewpoint(Float.parseFloat(tok));
					} else if ((i - dataStartIndex) % dataCycleLength == 4) {
						if (Float.isNaN(Float.parseFloat(tok)))
							sndLy.setWindDirection(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
						else
							sndLy.setWindDirection(Float
									.parseFloat(tok));
					} else if ((i - dataStartIndex) % dataCycleLength == 5) {
						if (Float.isNaN(Float.parseFloat(tok)))
							sndLy.setWindSpeed(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
						else
							sndLy.setWindSpeed(Float.parseFloat(tok));
					} else if ((i - dataStartIndex) % dataCycleLength == 6) {
						if (Float.isNaN(Float.parseFloat(tok)))
							sndLy.setOmega(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
						else
							sndLy.setOmega(Float.parseFloat(tok));

					}

				}

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
	public static boolean nsharpParseAndLoadTextFile(String textFilePath) throws FileNotFoundException{
		List<NcSoundingLayer> sndLyList = new ArrayList<NcSoundingLayer>();
        NsharpStationInfo stninfo = new NsharpStationInfo();
        if(nsharpTextfileParser(textFilePath,sndLyList,stninfo) == false)
        {
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
            else 
            	return false;
        }
        return true;
	}

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
                    if( nsharpParseAndLoadTextFile(selectedFilePath) == false) {
                    	MessageBox mb = new MessageBox(shell, SWT.ICON_WARNING
                    			| SWT.OK);
                    	mb.setMessage("Invalid sounding data retrieved from archive file "+ selectedFilePath+ "!!");
                    	mb.open();
                    	//continue;
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {
                System.out.println("number format exception happened");
                e.printStackTrace();
            } 
        }

    }
}
