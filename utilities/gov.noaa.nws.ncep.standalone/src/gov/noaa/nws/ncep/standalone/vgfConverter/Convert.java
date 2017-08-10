package gov.noaa.nws.ncep.standalone.vgfConverter;

import gov.noaa.nws.ncep.standalone.util.Util;
import gov.noaa.nws.ncep.standalone.vgfConverter.WrapperC.VgfXml;
import gov.noaa.nws.ncep.ui.pgen.file.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.file.Layer;
import gov.noaa.nws.ncep.ui.pgen.file.Product;
import gov.noaa.nws.ncep.ui.pgen.file.Products;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;

import com.sun.jna.Native;

/**
 * Convert
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 1/25/2009   203         Q. Zhou     Initial created
 * 9/09/2010   203         Q. Zhou     Added Main for cml. 
 * 1/06/2011   137         Q. Zhou     Added empty file checking
 * 1/25/2011   137         Q. Zhou     Add code to check the converted xml(if jaxb readable)
 * 11/2/2011   480         Q. Zhou     Added Activity and subActivity input fields to vgfConverter
 * 12/12/2011  548         Q. Zhou     Added -f -a options for contour table file and activities.
 * 6/13/2012   819         Q. Zhou     Added print for 0 length file
 * 08/28/2012  655         J. Wu       Allow specifying output file name - create output directory
 *                                     here instead in vgf2xml script.
 * 09/2013	   1042        J. Wu       Add converting of LPF files along with VG files.
 * 10/2014     ?           J. Wu       Set layer color mode to true only if "M"ono or "On" is detected.
 * 02/2015     R6192       J. Wu       Add converting of LPF files "manually" from existing XMLs.
 * 03/2015     R6872       J. Wu       Add status/forecaster/center in vgf2xml conversion.
 * </pre>
 * 
 * @author Q. Zhou
 * @version 1
 */

public class Convert {

    // Attributes' names defined in a legacy Layer Product File
    private static final String LPF_LAYER_NAME = "name";

    private static final String LPF_LAYER_INPUT_FILE = "file";

    private static final String LPF_LAYER_OUTPUT_FILE = "output_file";

    private static final String LPF_LAYER_COLOR_MODE = "color_mode";

    private static final String LPF_LAYER_COLOR_ID = "color_id";

    private static final String LPF_LAYER_FILL_MODE = "fill_mode";

    private static final String LPF_LAYER_GROUP_TYPE = "group_type";

    private static final String LPF_LAYER_DISPLAY_MODE = "display_mode";

    /*
     * The Color map used in legacy PGEN with 32 colors.
     */
    private static HashMap<Integer, Integer[]> nmapColors = null;

    /*
     * Convert.
     */
    public int convertMap(String fileIn, String fileOut, String activity,
            String subActivity, String site, String forecaster, String status,
            String contTbl, boolean manualVGF) throws IOException {

        /*
         * Get a list of VGF file in the given directory
         */
        List<File> vgfFiles = getVgfFiles(fileIn);
        boolean oneVgfIn = false;
        if (fileIn.endsWith(".vgf") && vgfFiles.size() == 1) { // one file
            oneVgfIn = true;
        }

        /*
         * Get a list of LPF file in the given directory
         */
        List<File> lpfFiles = getLpfFiles(fileIn);
        boolean oneLpfIn = false;
        if (fileIn.endsWith(".lpf") && lpfFiles.size() == 1) { // one file
            oneLpfIn = true;
        }

        /*
         * Add an VG file specified in the LPFs to the list of VG files, if they
         * are not already in and "manualVGF" is false; remove it from the list
         * if "manualVGF" is true.
         */
        for (File lpfile : lpfFiles) {
            HashMap<String, String> lpfMap = loadLpfParameters(lpfile.getName());
            ArrayList<String> layerPrefixes = getLayerPrefixes(lpfMap);

            for (String layerPre : layerPrefixes) {
                String layerFile = lpfMap.get(layerPre + "_"
                        + LPF_LAYER_INPUT_FILE);

                if (layerFile != null && layerFile.trim().length() > 0) {

                    boolean vgfExist = false;
                    File nvgfFile = new File(layerFile);

                    // check if this VGF is already included in the "vgfFiles"
                    // list.
                    if (nvgfFile.exists() && nvgfFile.canRead()) {
                        for (File vgfile : vgfFiles) {
                            if (vgfile.getCanonicalFile().equals(
                                    nvgfFile.getCanonicalFile())) {
                                vgfExist = true;
                                nvgfFile = vgfile;
                                break;
                            }
                        }
                    }

                    /*
                     * Remove from the "vgfFiles" list if it is in but manualVGF
                     * is true. Add into the "vgfFiles" list if it is not in but
                     * manualVGF is false.
                     */
                    if (vgfExist) {
                        if (manualVGF) {
                            vgfFiles.remove(nvgfFile);
                        }
                    } else {
                        if (!manualVGF) {
                            vgfFiles.add(nvgfFile);
                        }
                    }
                }
            }
        }

        /*
         * Now loop over all VG files to convert into XMLs first.
         */
        int counter = convertVgfFiles(vgfFiles, oneVgfIn, fileOut, activity,
                subActivity, site, forecaster, status, contTbl);
        if (counter > 0) {
            System.out.println(counter + " VG files are converted.  "
                    + "VGF to XML is finished.\n");
        }

        /*
         * Now convert LPFs into activities with multiple layers.
         */
        convertLpfFiles(lpfFiles, oneLpfIn, fileOut, activity, subActivity,
                site, forecaster, status);
        if (lpfFiles.size() > 0) {
            System.out.println(lpfFiles.size() + " LPF files are converted.  "
                    + "LPF to XML is finished.\n");
        }

        return counter;
    }

    /**
     * Main.
     */
    public static void main(String[] args) throws IOException {
        String fileName = "";
        String activity = "";
        String subActivity = "";
        String site = "OAX";
        String forecaster = "Default";
        String status = "UNKNOWN";

        if (args.length == 0 || args.length == 1) {
            System.out
                    .println("Please specify the source and the destination.\n");
            return;
        }

        if (!new File(args[0]).exists()) {
            System.out
                    .println("The Source directory or file does not exist.\n");
            return;
        }

        /*
         * Make the destination directory if it does not exist.
         */
        if (!(args[0].endsWith(".vgf") && args[1].endsWith(".xml"))) {
            File des = new File(args[1]);
            if (!des.exists()) {
                if (!des.mkdirs()) {
                    System.out
                            .println("The Destination directory does not exist.\n");
                    return;
                }
            }
        }

        /*
         * Parse activity, sub-activity, site, forecaster, status.
         */
        boolean manualVGF = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-m")) {
                manualVGF = true;
            }

            if (args[i].equalsIgnoreCase("-s")) {
                if ((i + 1) < args.length && args[i + 1] != null
                        && !args[i + 1].startsWith("-")) {
                    site = args[i + 1];
                }
            }

            if (args[i].equalsIgnoreCase("-f")) {
                if ((i + 1) < args.length && args[i + 1] != null
                        && !args[i + 1].startsWith("-")) {
                    forecaster = args[i + 1];
                }
            }

            if (args[i].equalsIgnoreCase("-u")) {
                if ((i + 1) < args.length && args[i + 1] != null
                        && !args[i + 1].startsWith("-")) {
                    status = args[i + 1];
                }
            }

            if (args[i].equalsIgnoreCase("-t")) {
                if ((i + 1) < args.length && args[i + 1] != null
                        && !args[i + 1].equalsIgnoreCase("-a")) {
                    fileName = args[i + 1];
                }
            }

            if (args[i].equalsIgnoreCase("-a")) {
                if ((i + 1) < args.length && args[i + 1] != null
                        && !args[i + 1].equalsIgnoreCase("-t")) {
                    activity = args[i + 1];
                    if ((i + 2) < args.length && args[i + 2] != null
                            && !args[i + 2].equalsIgnoreCase("-t"))
                        subActivity = args[i + 2];
                }
            }
        }

        // Do conversion
        new Convert().convertMap(args[0], args[1], activity, subActivity, site,
                forecaster, status, fileName, manualVGF);

    }

    /*
     * Convert a list of VGF files to XML.
     */
    private int convertVgfFiles(List<File> vgfFiles, boolean oneVgfIn,
            String fileOut, String activity, String subActivity, String site,
            String forecaster, String status, String contTbl) {

        int counter = 0;
        String outFile = null;
        if (vgfFiles != null) {
            for (int ii = 0; ii < vgfFiles.size(); ii++) {
                File theFile = vgfFiles.get(ii);
                if (theFile.length() != 0) {

                    String inFile = theFile.getAbsolutePath();

                    if (oneVgfIn && fileOut.endsWith(".xml")) {
                        if (fileOut.contains("/")) {
                            outFile = new String(fileOut);
                        } else {
                            outFile = new String("./" + fileOut);
                        }
                    } else {
                        String s = theFile.getName();
                        String s1 = s.substring(0, s.lastIndexOf("."));
                        s = s1 + ".xml";

                        if (fileOut.endsWith("//")) {
                            outFile = fileOut.substring(0,
                                    fileOut.lastIndexOf("/"))
                                    + s;
                        } else if (fileOut.endsWith("/")) {
                            outFile = fileOut + s;
                        } else {
                            outFile = fileOut + "/" + s;
                        }
                    }

                    // Convert vgf to xml
                    VgfXml wrap = (VgfXml) Native.loadLibrary("VgfXml",
                            VgfXml.class);
                    wrap.vgfToXml(inFile, outFile, activity, subActivity, site,
                            forecaster, status, contTbl);

                    // Check for the converted xml
                    Products convertedRight = null;
                    try {
                        convertedRight = Util.read(outFile);
                    } catch (FileNotFoundException ee) {
                        ee.printStackTrace();
                    }

                    if (convertedRight != null) {
                        counter++;
                        System.out.println("The file " + inFile
                                + " is converted to " + outFile);
                    }
                }
            }
        }

        return counter;
    }

    /*
     * Convert a list of VGF files to XML.
     */
    private void convertLpfFiles(List<File> lpfFiles, boolean oneLpfIn,
            String fileOut, String activity, String subActivity, String site,
            String forecaster, String status) throws IOException {
        for (File lpfile : lpfFiles) {
            Products xmlPrds = loadLpfFile(lpfile.getName(), activity,
                    subActivity, site, forecaster, status);
            if (xmlPrds != null) {

                String outFile = null;

                if (oneLpfIn && fileOut.endsWith(".xml")) {
                    if (fileOut.contains("/")) {
                        outFile = new String(fileOut);
                    } else {
                        outFile = new String("./" + fileOut);
                    }
                } else {
                    String s0 = lpfile.getName();
                    String s1 = s0.substring(0, s0.lastIndexOf("."));
                    s0 = s1 + ".xml";

                    if (fileOut.endsWith("//")) {
                        outFile = fileOut
                                .substring(0, fileOut.lastIndexOf("/")) + s0;
                    } else if (fileOut.endsWith("/")) {
                        outFile = fileOut + s0;
                    } else {
                        outFile = fileOut + "/" + s0;
                    }
                }

                // Write out the XML.
                Util.write(outFile, xmlPrds, Products.class);
                System.out.println("The file " + lpfile.getName()
                        + " is converted to  " + outFile);

                // Need to update "Products" header with name space info.
                updateXmlHeader(outFile);
            }
        }

    }

    /*
     * Get a list VGF files in the given directory.
     */
    private List<File> getVgfFiles(String infile) {

        List<File> vgfFiles = new ArrayList<File>();

        if (infile.endsWith("*") || infile.endsWith("*.vgf")) {
            infile = infile.substring(0, infile.lastIndexOf("/"));
        }

        File vgfDir = new File(infile);
        if (vgfDir.isFile() && infile.endsWith(".vgf")) {
            vgfFiles.add(vgfDir); // infile is a file
        } else { // infile is a directory
            File[] files;
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".vgf");
                }
            };

            files = vgfDir.listFiles(filter);

            if (files != null && files.length > 0) {
                for (int ii = 0; ii < files.length; ii++) {
                    vgfFiles.add(files[ii]);
                }
            }
        }

        return vgfFiles;
    }

    /*
     * Get a list LPF files in the given directory.
     */
    private List<File> getLpfFiles(String infile) {

        List<File> lpFiles = new ArrayList<File>();

        if (infile.endsWith("*") || infile.endsWith("*.lpf")) {
            infile = infile.substring(0, infile.lastIndexOf("/"));
        }

        File lpfDir = new File(infile);
        if (lpfDir.isFile() && infile.endsWith(".lpf")) {
            lpFiles.add(lpfDir); // infile is a file
        } else { // infile is a directory
            File[] files;
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".lpf");
                }
            };

            files = lpfDir.listFiles(filter);

            if (files != null && files.length > 0) {
                for (int ii = 0; ii < files.length; ii++) {
                    lpFiles.add(files[ii]);
                }
            }
        }

        return lpFiles;
    }

    /*
     * Load the LPF file contents into a HashMap.
     * 
     * Note: valid entry should be in format of "<tag>value"
     */
    private LinkedHashMap<String, String> loadLpfParameters(String fname) {

        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();

        // Check if the given file exists and readable.
        File thisFile = null;
        if (fname != null) {
            thisFile = new File(fname);
            if (!thisFile.exists() || !thisFile.canRead()) {
                thisFile = null;
            }
        }

        if (thisFile == null) {
            return params;
        }

        try {

            Scanner fileScanner = new Scanner(thisFile);

            try {
                // first use a Scanner to get each line
                while (fileScanner.hasNextLine()) {
                    String nextLine = fileScanner.nextLine().trim();

                    // process each line
                    if (!(nextLine.startsWith("!"))) {

                        int start = nextLine.indexOf("<");
                        int end = nextLine.indexOf(">");

                        if (start >= 0 && end > 0 && end > start
                                && nextLine.length() > end) {

                            String name = nextLine.substring(start + 1, end)
                                    .trim();
                            if (name.length() > 0) {
                                String value = nextLine.substring(end + 1)
                                        .trim();

                                if (value.length() > 0) {
                                    params.put(name, value);
                                }
                            }
                        }
                    }
                }
            } finally {
                fileScanner.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return params;
    }

    /*
     * Find all layer prefixes in a loaded LPF file (e.g., layer1, layer3, ...)
     * 
     * @param lpfMap HashMap<String, String>
     * 
     * @return ArrayList<String>
     */
    private ArrayList<String> getLayerPrefixes(HashMap<String, String> lpfMap) {

        ArrayList<String> layerKeys = new ArrayList<String>();
        if (lpfMap != null && lpfMap.size() > 0) {
            for (String key : lpfMap.keySet()) {
                if (key != null && key.contains(LPF_LAYER_NAME)) {
                    layerKeys
                            .add(new String(key.substring(0, key.indexOf("_"))));
                }
            }
        }

        return layerKeys;

    }

    /*
     * Load a full-path LPF file into a PGEN product
     * 
     * @param prods
     */
    private Products loadLpfFile(String fname, String activity,
            String subactivity, String site, String forecaster, String status) {

        Products pgenFilePrds = null;

        HashMap<String, String> lpfMap = loadLpfParameters(fname);

        ArrayList<String> layerPrefixes = getLayerPrefixes(lpfMap);

        // Load layers.
        if (layerPrefixes.size() > 0) {
            pgenFilePrds = new Products();

            for (String layerPre : layerPrefixes) {

                Products layerFp = null;

                String layerName = lpfMap.get(layerPre + "_" + LPF_LAYER_NAME);

                if (layerName == null || layerName.trim().length() == 0) {
                    continue;
                }

                String layerOutputFile = lpfMap.get(layerPre + "_"
                        + LPF_LAYER_OUTPUT_FILE);
                String layerColorMode = lpfMap.get(layerPre + "_"
                        + LPF_LAYER_COLOR_MODE);
                String layerColorID = lpfMap.get(layerPre + "_"
                        + LPF_LAYER_COLOR_ID);
                String layerFillMode = lpfMap.get(layerPre + "_"
                        + LPF_LAYER_FILL_MODE);
                String layerGroupType = lpfMap.get(layerPre + "_"
                        + LPF_LAYER_GROUP_TYPE);
                String layerDisplayMode = lpfMap.get(layerPre + "_"
                        + LPF_LAYER_DISPLAY_MODE);

                /*
                 * Look for a full-path input file for the layer.
                 */
                String layerFile = lpfMap.get(layerPre + "_"
                        + LPF_LAYER_INPUT_FILE);
                String layerInputFile = layerFile.replace(".vgf", ".xml");

                // Read in the contents in the specified file.
                if (layerInputFile != null) {
                    File ft = new File(layerInputFile);
                    if (ft.exists() && ft.canRead()) {
                        try {
                            layerFp = Util.read(layerInputFile);
                        } catch (FileNotFoundException ee) {
                            ee.printStackTrace();
                        }
                    }
                }

                // Create a default product if there is no file specified for
                // the layer.
                if (layerFp == null) {
                    Product dfltp = new Product();
                    dfltp.setName("Default");
                    dfltp.setCenter("Default");
                    dfltp.setForecaster("Default");
                    dfltp.setStatus("UNKNOWN");
                    dfltp.setOnOff(true);
                    dfltp.setType("Default");
                    dfltp.setSaveLayers(false);
                    dfltp.setUseFile(false);
                    dfltp.setOutputFile(fname.replace(".lpf", ".xml"));

                    Layer dfltly = new Layer();
                    dfltp.getLayer().add(dfltly);

                    layerFp = new Products();
                    layerFp.getProduct().add(dfltp);
                }

                // Add to the product list
                if (pgenFilePrds.getProduct().size() == 0) {
                    pgenFilePrds.getProduct().addAll(layerFp.getProduct());
                    pgenFilePrds.getProduct().get(0)
                            .setOutputFile(fname.replace(".lpf", ".xml"));
                } else {
                    pgenFilePrds.getProduct().get(0).getLayer()
                            .addAll(layerFp.getProduct().get(0).getLayer());
                }

                /*
                 * Update layer attributes with those in the LPF.
                 */
                int nly = pgenFilePrds.getProduct().get(0).getLayer().size();
                gov.noaa.nws.ncep.ui.pgen.file.Layer clayer = pgenFilePrds
                        .getProduct().get(0).getLayer().get(nly - 1);
                clayer.setName(layerName);

                // Color mode
                clayer.setMonoColor(false);
                if (layerColorMode != null
                        && layerColorMode.trim().length() > 0) {
                    String mp = layerColorMode.trim().toUpperCase();
                    if (mp.startsWith("M") || mp.equals("ON")) {
                        clayer.setMonoColor(true);
                    }
                }

                // Fill mode
                if (layerFillMode != null && layerFillMode.trim().length() > 0
                        && layerFillMode.trim().equalsIgnoreCase("On")) {
                    clayer.setFilled(true);
                } else {
                    clayer.setFilled(false);
                }

                // Display mode
                if (layerDisplayMode != null
                        && layerDisplayMode.trim().length() > 0
                        && layerDisplayMode.trim().equalsIgnoreCase("On")) {
                    clayer.setOnOff(true);
                } else {
                    clayer.setOnOff(false);
                }

                // layer color
                if (layerColorID != null && layerColorID.trim().length() > 0) {
                    int colorNum = Integer.parseInt(layerColorID);
                    if (colorNum > 0 && colorNum <= 32) {
                        Integer[] nmapColor = getNmapColors().get(colorNum);
                        if (nmapColor != null) {
                            if (clayer.getColor() == null) {
                                clayer.setColor(new gov.noaa.nws.ncep.ui.pgen.file.Color());
                                clayer.getColor().setAlpha(255);
                            }

                            clayer.getColor().setRed(nmapColor[0]);
                            clayer.getColor().setGreen(nmapColor[1]);
                            clayer.getColor().setBlue(nmapColor[2]);
                        }
                    }
                }

                if (clayer.getDrawableElement() == null) {
                    clayer.setDrawableElement(new DrawableElement());
                }
            }
        }

        /*
         * Set activity type string.
         */
        if (pgenFilePrds != null) {
            String type = "Default";
            if (activity != null && activity.trim().length() > 0) {
                type = "" + activity;
                if (subactivity != null && subactivity.trim().length() > 0) {
                    type += "(" + subactivity + ")";
                }
            }

            pgenFilePrds.getProduct().get(0).setType(type);

            if (site != null && site.trim().length() > 0) {
                pgenFilePrds.getProduct().get(0).setCenter(site);
            }

            if (forecaster != null && forecaster.trim().length() > 0) {
                pgenFilePrds.getProduct().get(0).setForecaster(forecaster);
            }

            if (status != null && status.trim().length() > 0) {
                pgenFilePrds.getProduct().get(0).setStatus(status);
            }
        }

        return pgenFilePrds;

    }

    /*
     * The Color map used in legacy PGEN with 32 colors.
     * 
     * @return HashMap<Integer, Integer[]>
     */
    private static HashMap<Integer, Integer[]> getNmapColors() {
        if (nmapColors == null) {
            nmapColors = new HashMap<Integer, Integer[]>();
            nmapColors.put(0, new Integer[] { 0, 0, 0 });
            nmapColors.put(1, new Integer[] { 255, 228, 220 });
            nmapColors.put(2, new Integer[] { 255, 0, 0 });
            nmapColors.put(3, new Integer[] { 0, 255, 0 });
            nmapColors.put(4, new Integer[] { 0, 0, 255 });
            nmapColors.put(5, new Integer[] { 255, 255, 0 });
            nmapColors.put(6, new Integer[] { 0, 255, 255 });
            nmapColors.put(7, new Integer[] { 255, 0, 255 });
            nmapColors.put(8, new Integer[] { 139, 71, 38 });
            nmapColors.put(9, new Integer[] { 255, 130, 71 });
            nmapColors.put(10, new Integer[] { 255, 165, 79 });
            nmapColors.put(11, new Integer[] { 255, 174, 185 });
            nmapColors.put(12, new Integer[] { 255, 106, 106 });
            nmapColors.put(13, new Integer[] { 238, 44, 44 });
            nmapColors.put(14, new Integer[] { 139, 0, 0 });
            nmapColors.put(15, new Integer[] { 205, 0, 0 });
            nmapColors.put(16, new Integer[] { 238, 64, 0 });
            nmapColors.put(17, new Integer[] { 255, 127, 0 });
            nmapColors.put(18, new Integer[] { 205, 133, 0 });
            nmapColors.put(19, new Integer[] { 255, 215, 0 });
            nmapColors.put(20, new Integer[] { 238, 238, 0 });
            nmapColors.put(21, new Integer[] { 127, 255, 0 });
            nmapColors.put(22, new Integer[] { 0, 205, 0 });
            nmapColors.put(23, new Integer[] { 0, 139, 0 });
            nmapColors.put(24, new Integer[] { 16, 78, 139 });
            nmapColors.put(25, new Integer[] { 30, 144, 255 });
            nmapColors.put(26, new Integer[] { 0, 178, 238 });
            nmapColors.put(27, new Integer[] { 0, 238, 238 });
            nmapColors.put(28, new Integer[] { 137, 104, 205 });
            nmapColors.put(29, new Integer[] { 145, 44, 238 });
            nmapColors.put(30, new Integer[] { 139, 0, 139 });
            nmapColors.put(31, new Integer[] { 255, 255, 255 });
            nmapColors.put(32, new Integer[] { 0, 0, 0 });
        }

        return nmapColors;
    }

    /*
     * Updates an activity XML converted from LPF with some name space info so
     * it could be recognized by CAVE PGEN.
     */
    private void updateXmlHeader(String fname) throws IOException {

        // Check if the given file exists and readable.
        File thisFile = null;
        if (fname != null) {
            thisFile = new File(fname);
            if (!thisFile.exists() || !thisFile.canRead()) {
                thisFile = null;
            }
        }

        if (thisFile != null) {

            // Read in
            StringBuilder sbd = new StringBuilder();
            try {
                Scanner fileScanner = new Scanner(thisFile);
                try {
                    while (fileScanner.hasNextLine()) {
                        String nextLine = fileScanner.nextLine();
                        if (nextLine.contains("<Products>")) {
                            nextLine = "<Products "
                                    + "xmlns:ns2=\"com.raytheon.uf.common.datadelivery.registry\" "
                                    + "xmlns:ns3=\"http://www.example.org/productType\">";
                        }
                        sbd.append(nextLine + "\n");
                    }
                } finally {
                    fileScanner.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Write out
            if (sbd.length() > 0) {
                FileWriter xfile = new FileWriter(fname);
                xfile.write(sbd.toString());
                xfile.close();
            }
        }
    }

}