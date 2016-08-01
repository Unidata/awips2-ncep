package gov.noaa.nws.ncep.standalone.xmlConverter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * XmlLoading
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 03/2009	137			Q. Zhou		Initial created
 * 09/2010	137			Q. Zhou		Added Main for cml.
 * 01/2011	137         Q. Zhou		Added empty file checking
 * 06/2012	819         Q. Zhou		Added print for 0 length file
 * 09/2013	1042		J. Wu		Add options to split activity with
 * 										multiple layers to LPF with multiple
 * 										VG files. 			
 * </pre>
 * 
 * @author Q. Zhou
 * @version 1
 */

public class XmlLoading {

	public int loading(String in, String out, boolean split ) throws IOException {
		String outFile = null;		
		List<File> vgfFiles = new  ArrayList<File>();	
		int counter = 0; //file counter
		
		File vgfDir = new File(in);		
		
		if (in.endsWith("*") || in.endsWith("*.xml")) {
			in = in.substring(0, in.lastIndexOf("/"));
		}	
		
		if (in.endsWith(".xml") ) {  //one file
			vgfFiles.add( vgfDir ) ;
		}
		else {									//directory
			File[] files;			
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			};
			
			files = vgfDir.listFiles(filter);
			if (files == null || files.length == 0) {
				System.out.println("The xml file does not exist.");
				return 0;
			}
				
			for (int x = 0 ; x < files.length ; x++)
				vgfFiles.add(files[x]);
			
		}
		
		/*
		 *  Loop over all VG files
		 */
		if ( vgfFiles != null ) {
			for ( int ii = 0; ii < vgfFiles.size(); ii++ ) {
				File theFile = vgfFiles.get(ii);
				if ( theFile.length() != 0 ) {
					String fin = theFile.getAbsolutePath();
					String s = theFile.getName();
					String s1 = s.substring(0, s.lastIndexOf("."));
					String sout =  s1+ ".tag";

					if (out.endsWith("//")) {
						outFile = out.substring(0, out.lastIndexOf("/")) + sout;
					}
					else if (out.endsWith("/")) {
						outFile = out + sout;
					}
					else {
						outFile = out + "/" + sout;
					}
					
					//do convert
					int converted = new XmlConvert().convertXml(fin, outFile, split );
					if ( converted != 0 ) {
						counter++;
					}
				}				
				else {
					System.out.println("***The xml file " + theFile + " is 0 length and is not converted.");
				}
			}
		}
		
		if ( counter >= 1 ) {
	    	System.out.println( "\n" + counter +" files are converted.  " + "The Conversion to tag is finished.");
		}
		
		return counter;
	}
	
	
	/**
	 * Main.
	 */	
	public static void main(String[] args) throws IOException { 
		
		File inf = new File( args[0] );
		if ( ! inf.exists() ) {
			System.out.println("The Source directory or file does not exist.\n");
			return ;
		}
		
		/*
		 * Check if the user specified a file name for output (not directory).
		 */
		File outf = new File( args[1] );				
        if ( !outf.exists() ) {
		     System.out.println("The Destination directory does not exist.\n");
			 return ;
		}
				
		/*
		 * Check if the user wants to split an activity with multiple layers into
		 * multiple VG files (one layer per VGF).  Default is "no".
		 */		
		boolean  split = false;
		if ( args.length > 2 && args[2].equalsIgnoreCase("-S") ) { 
			split = true;
		}
				
		new XmlLoading().loading(args[0], args[1], split );
	}
}
