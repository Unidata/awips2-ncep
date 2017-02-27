package gov.noaa.nws.ncep.standalone.xmlConverter;

import gov.noaa.nws.ncep.standalone.util.ProductConverter;
import gov.noaa.nws.ncep.standalone.util.Util;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.file.Products;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * XmlConvert
 * 
 * This program could either convert a CAVE PGEN activity XML file into one
 * single NMAP2 VG file with all elements in it OR convert each layer in the
 * activity XML into its own VG file and generate an LPF for this activity. For
 * the latter, "-s" should be specified at command line and the activity XML
 * should have more than one layer in it.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 3/25/2010   137         Q. Zhou     Initial created
 * 4/21/2010   137         Q. Zhou     Add watch.(now read anchor from table, otherwise uncommand the first few lines in Main.
 * 8/12/2010               Q. Zhou     modified contours group number -- count
 * 9/09/2010   137         Q. Zhou     Removed Localizaton.  Use environment vars.
 * 1/06/2011   137         Q. Zhou     Created local copy of ProductConverter.
 *  								   Removed edex running and isconverter condition.
 * 1/25/2011   137         Q. Zhou     Refact code, Separate DE and Collection.
 * 6/13/2012   819         Q. Zhou     Added print for files that don't have Pgen content.
 * 09/2013	   1042		   J. Wu	   Add options to split activity with
 * 										multiple layers to LPF with multiple
 * 										VG files.
 * </pre>
 * 
 * @author Q. Zhou
 * @version 1
 */

public class XmlConvert {
    public int convertXml(String in, String out, boolean split)
            throws IOException {

        /*
         * Read in the PGEN activity XML. Note that there would be only ONE
         * "Product" in each XML file.
         */
        Products products = Util.read(in);

        if (products == null) {
            return 0;
        }

        List<Product> prod = ProductConverter.convert(products);

        /*
         * Check if the user wants to split each layer into a separate VG file -
         * do this only when there is more than one layer in this activity.
         */
        if (split && prod.get(0).getLayers().size() > 1) {
            System.out.println("Converting " + in + " to:\n");
            return splitXml(prod.get(0), out);
        } else {
            System.out.println("Converting " + in + " to " + out + "\n");
            return convertXml(prod.get(0), out);
        }
    }

    /*
     * Simply convert all elements and save into a single VG file.
     */
    private int convertXml(Product prod, String outfile) throws IOException {

        // Open file and write a VG header into it.
        FileWriter fw = new FileWriter(outfile);
        if (!prod.isEmpty()) {
            fw.write(VgfTags.vgfHead);
        }

        String vgf = "";
        DrawableElement de = null;
        int collectionCnt = 0;
        int contourTot = 0; // for grpNums of contour and symLabel

        // Write all elements.
        List<Layer> lay = prod.getLayers();
        for (int jj = 0; jj < lay.size(); jj++) {
            List<AbstractDrawableComponent> adc = lay.get(jj).getDrawables();
            contourTot = 0;

            if (adc.size() > 0) {
                for (int kk = 0; kk < adc.size(); kk++) {

                    if (adc.get(kk) instanceof DrawableElement) {
                        de = adc.get(kk).getPrimaryDE();
                        vgf = ToTag.tagVGF(vgf, de);
                        fw.write(vgf);
                    }

                    else if (adc.get(kk) instanceof DECollection) {
                        vgf = ToTagCollection.tagCollection(vgf, adc.get(kk),
                                fw, collectionCnt, contourTot);
                        collectionCnt++;
                        // count total CONTOURS de number
                        if (adc.get(kk).getName().equalsIgnoreCase("Contours")) {
                            Iterator<DrawableElement> it = adc.get(kk)
                                    .createDEIterator();
                            while ((de = it.next()) != null) {
                                if (de.getPgenCategory().equalsIgnoreCase(
                                        "Lines"))
                                    contourTot++;
                            }
                        }
                    }
                }
            }
        }

        fw.close();

        return 1;
    }

    /*
     * Split the layers in the activity into separate VG files and generate an
     * LPF file.
     */
    private int splitXml(Product prod, String outfile) throws IOException {

        String lpfFile = outfile.replace(".tag", ".lpf");
        System.out.println("\t\t" + lpfFile);

        FileWriter lpfw = new FileWriter(lpfFile);
        lpfw.write(generateLpfHeader(lpfFile));

        String vgfTagStr = "";
        DrawableElement de = null;
        int collectionCnt = 0;
        int contourTot = 0; // for grpNums of contour and symLabel

        List<Layer> lay = prod.getLayers();

        for (int jj = 0; jj < lay.size(); jj++) {

            String layerTagf = outfile.replace(".tag", "_"
                    + lay.get(jj).getName() + ".tag");
            String layerVgf = layerTagf.replace(".tag", ".vgf");
            System.out.println("\t\t" + layerTagf);

            // Write LPF attributes for this layer.
            lpfw.write(generateLpfAttrs(layerVgf, lay.get(jj), jj + 1));

            // Write VG header information.
            FileWriter layerTagfw = new FileWriter(layerTagf);
            layerTagfw.write(VgfTags.vgfHead);

            // Write VG elements information.
            List<AbstractDrawableComponent> adc = lay.get(jj).getDrawables();
            contourTot = 0;

            if (adc.size() > 0) {
                for (int kk = 0; kk < adc.size(); kk++) {

                    if (adc.get(kk) instanceof DrawableElement) {
                        de = adc.get(kk).getPrimaryDE();
                        vgfTagStr = ToTag.tagVGF(vgfTagStr, de);
                        layerTagfw.write(vgfTagStr);
                    } else if (adc.get(kk) instanceof DECollection) {
                        vgfTagStr = ToTagCollection.tagCollection(vgfTagStr,
                                adc.get(kk), layerTagfw, collectionCnt,
                                contourTot);
                        collectionCnt++;
                        // count total CONTOURS de number
                        if (adc.get(kk).getName().equalsIgnoreCase("Contours")) {
                            Iterator<DrawableElement> it = adc.get(kk)
                                    .createDEIterator();
                            while ((de = it.next()) != null) {
                                if (de.getPgenCategory().equalsIgnoreCase(
                                        "Lines"))
                                    contourTot++;
                            }
                        }
                    }
                }
            }

            layerTagfw.close();
        }

        lpfw.close();

        return 1;
    }

    /*
     * Create an LPF header string with file name and date in it.
     */
    private String generateLpfHeader(String lpffile) {
        String lpfHeader = ""
                + "!--------------------------------------------------------------------------------\n"
                + "!--------------------------------------------------------------------------------\n"
                + "!\n"
                + "!  "
                + lpffile
                + "\n"
                + "!\n"
                + "!  This is a layer procedure file for nmap2.\n"
                + "!\n"
                + "!  Creation date:	"
                + Calendar.getInstance().getTime()
                + "\n"
                + "!\n"
                + "!  Created by:		"
                + "xml2vgf converter.\n"
                + "!\n"
                + "!--------------------------------------------------------------------------------\n"
                + "!--------------------------------------------------------------------------------\n"
                + "!\n";

        return lpfHeader;
    }

    /*
     * Create attributes for an LPF layer.
     */
    private String generateLpfAttrs(String vgfile, Layer layerin, int layernum) {
        String lpfAttr = "";
        String pref = "layer" + layernum;
        String colorMode = layerin.isMonoColor() ? "All" : "Mono";
        String display = layerin.isOnOff() ? "On" : "Off";
        String fill = layerin.isFilled() ? "On" : "Off";
        int color = XmlUtil.getColorTag(getColorStr(layerin.getColor()));
        lpfAttr += "<" + pref + "_name>\t\t" + layerin.getName() + "\n" + "<"
                + pref + "_file>\t\t" + vgfile + "\n" + "<" + pref
                + "_color_mode>\t" + colorMode + "\n" + "<" + pref
                + "_color_id>\t" + color + "\n" + "<" + pref + "_fill_mode>\t"
                + fill + "\n" + "<" + pref + "_group_type>\t" + "None" + "\n"
                + "<" + pref + "_display_mode>\t" + display + "\n\n";

        return lpfAttr;
    }

    /*
     * Get a color string to be converted into a NMAP color ID.
     */
    private String getColorStr(Color clr) {

        String colStr = "";

        if (clr != null) {
            String tem = clr.toString(); // java.awt.Color[r=0,g=255,b=255]
            colStr = tem.substring(tem.indexOf("[") + 1, tem.indexOf("]"));
        }

        return colStr;
    }

}
