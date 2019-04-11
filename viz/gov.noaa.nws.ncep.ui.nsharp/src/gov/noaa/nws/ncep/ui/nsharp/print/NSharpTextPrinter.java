/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.ui.nsharp.print;

import java.util.Map;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Helicity;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpWeatherDataStore;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpNativeConstants;

/**
 * 
 * Print the bottom text boxes on an nsharp printout.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------------------
 * Oct 16, 2018  6845     bsteffen  Consolidated text related printing here.
 *
 * </pre>
 *
 * @author bsteffen
 */
public class NSharpTextPrinter {

    private final GC gc;

    private final Rectangle pageBounds;

    private final NsharpResourceHandler handler;

    public NSharpTextPrinter(GC gc, Rectangle pageBounds,
            NsharpResourceHandler handler) {
        this.gc = gc;
        this.pageBounds = pageBounds;
        this.handler = handler;
    }

    public void print() {
        String thermo1 = printThermodynamicParametersBox1();
        String thermo2 = printThermodynamicParametersBox2();
        String thermo = thermo1 + "BOXLINE" + thermo2;
        String kinematic = printKinematicParametersBox();
        String storm = printStormStructureParametersBox();

        int gapWidth = gc.stringExtent("    ").x;
        int lineHeight = gc.getFontMetrics().getHeight();

        /* 2 points */
        gc.setLineWidth(2 * gc.getDevice().getDPI().x / 72);

        Rectangle thermoBounds = new Rectangle(pageBounds.x, pageBounds.y,
                (pageBounds.width - gapWidth) / 2, lineHeight * 23);
        printTextBox("THERMODYNAMIC PARAMETERS", thermo, thermoBounds);

        Rectangle kinematicBounds = new Rectangle(
                thermoBounds.x + thermoBounds.width + gapWidth, thermoBounds.y,
                thermoBounds.width, lineHeight * 13);
        printTextBox("KINEMATIC PARAMETERS", kinematic, kinematicBounds);

        Rectangle stormBounds = new Rectangle(kinematicBounds.x,
                kinematicBounds.y + kinematicBounds.height + lineHeight * 3,
                kinematicBounds.width, lineHeight * 9);
        printTextBox("STORM STRUCTURE PARAMETERS", storm, stormBounds);
    }

    private void printTextBox(String label, String content, Rectangle bounds) {
        int lineHeight = gc.getFontMetrics().getHeight();

        content = content.replace("\t", "    ");
        content = content.replace("\r", "");
        content = content.replace("BOXLINE", "BOXLINE\n");
        String[] lines = content.split("\n");

        int lineWidth = gc.getLineWidth();

        int height = lineWidth * 2;
        for (String line : lines) {
            if (line.equals("BOXLINE")) {
                height += lineWidth * 2;
            } else {
                height += lineHeight;
            }
        }

        bounds.height = height;

        gc.drawRectangle(bounds);
        int titleWidth = gc.stringExtent(label).x;
        gc.drawString(label, bounds.x + bounds.width / 2 - titleWidth / 2,
                bounds.y - lineHeight - lineWidth);

        int x = bounds.x + lineWidth;
        int y = bounds.y + lineWidth;

        for (String line : lines) {
            if (line.equals("BOXLINE")) {
                y += lineWidth;
                gc.drawLine(bounds.x, y, bounds.x + bounds.width, y);
                y += lineWidth;
            } else {
                gc.drawString(line, x, y);
                y += lineHeight;
            }
        }
    }

    private String printKinematicParametersBox() {
        /*
         * this function is for printing and coded based on legacy nsharp
         * software show_meanwind() in xwvid3.c
         */
        NsharpWeatherDataStore weatherDataStore = handler.getWeatherDataStore();
        String textStr, finalTextStr = "\t";
        finalTextStr = finalTextStr + NsharpNativeConstants.MEAN_WIND_STR;

        // mean wind at 0-6 km
        WindComponent meanWind = weatherDataStore.getStormTypeToMeanWindMap()
                .get("SFC-6km");
        if (meanWind != null && NsharpLibBasics.qc(meanWind.getWdir())
                && NsharpLibBasics.qc(meanWind.getWspd())) {
            textStr = NsharpNativeConstants.MEANWIND_SFC6KM_LINE;
            textStr = String.format(textStr, meanWind.getWdir(),
                    meanWind.getWspd(),
                    NsharpLibBasics.kt_to_mps(meanWind.getWspd()));
        } else {
            textStr = NsharpNativeConstants.MEANWIND_SFC6KM_MISSING;
        }
        finalTextStr = finalTextStr + textStr;
        // mean wind at LFC-EL
        int currentParcel = handler.getCurrentParcel();
        NsharpWeatherDataStore.ParcelMiscParams parcelMisc = weatherDataStore
                .getParcelMiscParamsMap().get(currentParcel);
        meanWind = null;
        if (parcelMisc != null) {
            meanWind = parcelMisc.getMeanWindCompLfcToEl();
        }
        if (meanWind != null && NsharpLibBasics.qc(meanWind.getWdir())
                && NsharpLibBasics.qc(meanWind.getWspd())) {
            textStr = NsharpNativeConstants.MEANWIND_LFC_EL_LINE;
            textStr = String.format(textStr, meanWind.getWdir(),
                    meanWind.getWspd(),
                    NsharpLibBasics.kt_to_mps(meanWind.getWspd()));
        } else {
            textStr = NsharpNativeConstants.MEANWIND_LFC_EL_MISSING;
        }
        finalTextStr = finalTextStr + textStr;
        // mean wind at 850-200 mb
        meanWind = weatherDataStore.getMeanWindComp850To200mb();
        if (meanWind != null && NsharpLibBasics.qc(meanWind.getWdir())
                && NsharpLibBasics.qc(meanWind.getWspd())) {
            textStr = NsharpNativeConstants.MEANWIND_850_200MB_LINE;
            textStr = String.format(textStr, meanWind.getWdir(),
                    meanWind.getWspd(),
                    NsharpLibBasics.kt_to_mps(meanWind.getWspd()));
        } else {
            textStr = NsharpNativeConstants.MEANWIND_850_200MB_MISSING;
        }
        finalTextStr = finalTextStr + textStr + "\n";

        /*
         * the following function is for pritning and coded based on legacy
         * nsharp software show_shear() in xwvid3.c
         */
        finalTextStr = finalTextStr
                + NsharpNativeConstants.ENVIRONMENTAL_SHEAR_STR;

        finalTextStr = finalTextStr
                + NsharpNativeConstants.SHEAR_LAYER_DELTA_STR;

        // wind shear at Low - 3 km
        Map<String, Float> shearMap = weatherDataStore
                .getStormTypeToWindShearMap();
        float shearWind = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        if (shearMap != null && !shearMap.isEmpty()) {
            shearWind = shearMap.get("SFC-3km");
        }
        if (NsharpLibBasics.qc(shearWind)) {
            textStr = NsharpNativeConstants.SHEAR_LOW_3KM_LINE;
            textStr = String.format(textStr, shearWind,
                    NsharpLibBasics.kt_to_mps(shearWind),
                    NsharpLibBasics.kt_to_mps(shearWind) / .3F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_LOW_3KM_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // wind shear at Sfc - 2 km
        shearWind = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        if (shearMap != null && !shearMap.isEmpty()) {
            shearWind = shearMap.get("SFC-2km");
        }
        if (NsharpLibBasics.qc(shearWind)) {
            textStr = NsharpNativeConstants.SHEAR_SFC_2KM_LINE;
            textStr = String.format(textStr, shearWind,
                    NsharpLibBasics.kt_to_mps(shearWind),
                    NsharpLibBasics.kt_to_mps(shearWind) / .3F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_SFC_2KM_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // wind shear at Sfc - 6 km
        shearWind = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        if (shearMap != null && !shearMap.isEmpty()) {
            shearWind = shearMap.get("SFC-6km");
        }
        if (NsharpLibBasics.qc(shearWind)) {
            textStr = NsharpNativeConstants.SHEAR_SFC_6KM_LINE;
            textStr = String.format(textStr, shearWind,
                    NsharpLibBasics.kt_to_mps(shearWind),
                    NsharpLibBasics.kt_to_mps(shearWind) / .3F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_SFC_6KM_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // wind shear at Sfc - 12 km
        WindComponent comp = weatherDataStore.getShearWindCompSfcTo12km();
        shearWind = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        if (comp != null) {
            shearWind = comp.getWspd();
        }
        if (NsharpLibBasics.qc(shearWind)) {
            textStr = NsharpNativeConstants.SHEAR_SFC_12KM_LINE;
            textStr = String.format(textStr, shearWind,
                    NsharpLibBasics.kt_to_mps(shearWind),
                    NsharpLibBasics.kt_to_mps(shearWind) / .3F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_SFC_12KM_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // BRN Shear
        Parcel parcel = weatherDataStore.getParcelMap().get(currentParcel);
        if (parcel != null && NsharpLibBasics.qc(parcel.getBrnShear())) {
            textStr = NsharpNativeConstants.STORM_TYPE_BRNSHEAR_LINE;
            textStr = String.format(textStr, parcel.getBrnShear(),
                    NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else
            textStr = NsharpNativeConstants.STORM_TYPE_BRNSHEAR_MISSING;
        finalTextStr = finalTextStr + textStr;

        return finalTextStr;
    }

    private String printThermodynamicParametersBox1() {
        // this function is coded based on native nsharp
        // show_parcel() in xwvid3.c
        // This function is called to construct text string for printing
        NsharpWeatherDataStore weatherDataStore = handler.getWeatherDataStore();

        String finalTextStr = "";

        int currentParcel = handler.getCurrentParcel();
        Parcel pcl = weatherDataStore.getParcelMap().get(currentParcel);
        if (pcl == null) {
            return finalTextStr;
        }
        String hdrStr = NsharpNativeConstants.parcelToHdrStrMap
                .get(currentParcel);

        finalTextStr = finalTextStr + hdrStr;

        String textStr = NsharpNativeConstants.PARCEL_LPL_LINE;
        textStr = String.format(textStr, (int) pcl.getLplpres(),
                (int) pcl.getLpltemp(), (int) pcl.getLpldwpt(),
                (int) NsharpLibBasics.ctof(pcl.getLpltemp()),
                (int) NsharpLibBasics.ctof(pcl.getLpldwpt()));

        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getBplus())) {
            textStr = NsharpNativeConstants.PARCEL_CAPE_LINE;
            textStr = String.format(textStr, pcl.getBplus());
        } else {
            textStr = NsharpNativeConstants.PARCEL_CAPE_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getLi5())) {
            textStr = NsharpNativeConstants.PARCEL_LI_LINE;
            textStr = String.format(textStr, pcl.getLi5());
        } else {
            textStr = NsharpNativeConstants.PARCEL_LI_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getBfzl())) {
            textStr = NsharpNativeConstants.PARCEL_BFZL_LINE;
            textStr = String.format(textStr, pcl.getBfzl());
        } else {
            textStr = NsharpNativeConstants.PARCEL_BFZL_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getLimax())) {
            textStr = NsharpNativeConstants.PARCEL_LIMIN_LINE;
            textStr = String.format(textStr, pcl.getLimax(),
                    pcl.getLimaxpres());
        } else {
            textStr = NsharpNativeConstants.PARCEL_LIMIN_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getBminus())) {
            textStr = NsharpNativeConstants.PARCEL_CINH_LINE;
            textStr = String.format(textStr, pcl.getBminus());
        } else {
            textStr = NsharpNativeConstants.PARCEL_CINH_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getCap())) {
            textStr = NsharpNativeConstants.PARCEL_CAP_LINE;
            textStr = String.format(textStr, pcl.getCap(), pcl.getCappres());
        } else {
            textStr = NsharpNativeConstants.PARCEL_CAP_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        textStr = NsharpNativeConstants.PARCEL_LEVEL_LINE;
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getLclpres())
                && NsharpLibBasics.qc(pcl.getLclAgl())) {
            textStr = NsharpNativeConstants.PARCEL_LCL_LINE;
            textStr = String.format(textStr, pcl.getLclpres(), pcl.getLclAgl());
        } else {
            textStr = NsharpNativeConstants.PARCEL_LCL_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getLfcpres())
                && NsharpLibBasics.qc(pcl.getLfcAgl())
                && NsharpLibBasics.qc(pcl.getLfcTemp())) {
            textStr = NsharpNativeConstants.PARCEL_LFC_LINE;
            textStr = String.format(textStr, pcl.getLfcpres(), pcl.getLfcAgl(),
                    pcl.getLfcTemp());
        } else {
            textStr = NsharpNativeConstants.PARCEL_LFC_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getElpres())
                && NsharpLibBasics.qc(pcl.getElAgl())
                && NsharpLibBasics.qc(pcl.getElTemp())) {
            textStr = NsharpNativeConstants.PARCEL_EL_LINE;
            textStr = String.format(textStr, pcl.getElpres(), pcl.getElAgl(),
                    pcl.getElTemp());
        } else {
            textStr = NsharpNativeConstants.PARCEL_EL_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getMplpres())
                && NsharpLibBasics.qc(pcl.getMplAgl())) {
            textStr = NsharpNativeConstants.PARCEL_MPL_LINE;
            textStr = String.format(textStr, pcl.getMplpres(), pcl.getMplAgl());
        } else {
            textStr = NsharpNativeConstants.PARCEL_MPL_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        return finalTextStr;
    }

    private String printThermodynamicParametersBox2() {
        /*
         * this function is coded based on legacy native nsharp software
         * show_thermoparms(), show_moisture(),show_instability() in xwvid3.c
         * This function is called to construct text string for printing
         */
        NsharpWeatherDataStore weatherDataStore = handler.getWeatherDataStore();

        String finalTextStr = "";

        String textStr;
        float pw = weatherDataStore.getPw();
        if (pw >= 0) {
            textStr = NsharpNativeConstants.THERMO_PWATER_LINE;
            textStr = String.format(textStr, pw);
        } else {
            textStr = NsharpNativeConstants.THERMO_PWATER_MISSING;
        }
        finalTextStr = finalTextStr + textStr;
        if (NsharpLibBasics.qc(weatherDataStore.getMeanRh())) {
            textStr = NsharpNativeConstants.THERMO_MEANRH_LINE;
            textStr = String.format(textStr, weatherDataStore.getMeanRh(),
                    NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.THERMO_MEANRH_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getMeanMixRatio())) {
            textStr = NsharpNativeConstants.THERMO_MEANW_LINE;
            textStr = String.format(textStr,
                    weatherDataStore.getMeanMixRatio());
        } else {
            textStr = NsharpNativeConstants.THERMO_MEANW_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getLowRh())) {
            textStr = NsharpNativeConstants.THERMO_MEANLRH_LINE;
            textStr = String.format(textStr, weatherDataStore.getLowRh(),
                    NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.THERMO_MEANLRH_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getTopMoistLyrPress())) {
            textStr = NsharpNativeConstants.THERMO_TOP_LINE;
            textStr = String.format(textStr,
                    weatherDataStore.getTopMoistLyrPress(),
                    weatherDataStore.getTopMoistLyrHeight());
        } else {
            textStr = NsharpNativeConstants.THERMO_TOP_MISSING;
        }
        finalTextStr = finalTextStr + textStr + "BOXLINE";

        if (NsharpLibBasics
                .qc(weatherDataStore.getSevenHundredTo500mbTempDelta())
                && NsharpLibBasics.qc(
                        weatherDataStore.getSevenHundredTo500mbLapseRate())) {
            textStr = NsharpNativeConstants.THERMO_700500mb_LINE;
            textStr = String.format(textStr,
                    weatherDataStore.getSevenHundredTo500mbTempDelta(),
                    weatherDataStore.getSevenHundredTo500mbLapseRate());
        } else {
            textStr = NsharpNativeConstants.THERMO_700500mb_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getEight50To500mbLapseRate())
                && NsharpLibBasics
                        .qc(weatherDataStore.getEight50To500mbTempDelta())) {
            textStr = NsharpNativeConstants.THERMO_850500mb_LINE;
            textStr = String.format(textStr,
                    weatherDataStore.getEight50To500mbTempDelta(),
                    weatherDataStore.getEight50To500mbLapseRate());
        } else {
            textStr = NsharpNativeConstants.THERMO_850500mb_MISSING;
        }
        finalTextStr = finalTextStr + textStr;
        finalTextStr = finalTextStr + "BOXLINE";

        // misc parameters data--------------//
        if (NsharpLibBasics.qc(weatherDataStore.getTotTots())) {
            textStr = NsharpNativeConstants.THERMO_TOTAL_LINE;
            textStr = String.format(textStr, weatherDataStore.getTotTots());
        } else {
            textStr = NsharpNativeConstants.THERMO_TOTAL_MISSING;
        }
        finalTextStr = finalTextStr + textStr;
        if (NsharpLibBasics.qc(weatherDataStore.getkIndex())) {
            textStr = NsharpNativeConstants.THERMO_KINDEX_LINE;
            textStr = String.format(textStr, weatherDataStore.getkIndex());
        } else {
            textStr = NsharpNativeConstants.THERMO_KINDEX_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getSweatIndex())) {
            textStr = NsharpNativeConstants.THERMO_SWEAT_LINE;
            textStr = String.format(textStr, weatherDataStore.getSweatIndex());
        } else {
            textStr = NsharpNativeConstants.THERMO_SWEAT_MISSING;
        }
        finalTextStr = finalTextStr + textStr;
        if (NsharpLibBasics.qc(weatherDataStore.getMaxTemp())) {
            textStr = NsharpNativeConstants.THERMO_MAXT_LINE;
            textStr = String.format(textStr, weatherDataStore.getMaxTemp());
        } else {
            textStr = NsharpNativeConstants.THERMO_MAXT_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getThetaDiff())) {
            textStr = NsharpNativeConstants.THERMO_THETAE_LINE;
            textStr = String.format(textStr, weatherDataStore.getThetaDiff());
        } else {
            textStr = NsharpNativeConstants.THERMO_THETAE_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getConvT())) {
            textStr = NsharpNativeConstants.THERMO_CONVT_LINE;
            textStr = String.format(textStr, weatherDataStore.getConvT());
        } else {
            textStr = NsharpNativeConstants.THERMO_CONVT_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getWbzft())) {
            textStr = NsharpNativeConstants.THERMO_WBZ_LINE;
            textStr = String.format(textStr, weatherDataStore.getWbzft());
        } else {
            textStr = NsharpNativeConstants.THERMO_WBZ_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getFgzft())) {
            textStr = NsharpNativeConstants.THERMO_FGZ_LINE;
            textStr = String.format(textStr, weatherDataStore.getFgzft());
        } else {
            textStr = NsharpNativeConstants.THERMO_FGZ_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        return finalTextStr;

    }

    private String printStormStructureParametersBox() {
        NsharpWeatherDataStore weatherDataStore = handler.getWeatherDataStore();
        String textStr, finalTextStr = "";
        // helicity for sfc-3 km
        Helicity helicity = weatherDataStore.getStormTypeToHelicityMap()
                .get("SFC-3km");
        if (helicity != null && NsharpLibBasics.qc(helicity.getPosHelicity())
                && NsharpLibBasics.qc(helicity.getNegHelicity())) {
            textStr = "Sfc - 3km SREH =\t\t%.0f m%c/s%c\r\n";
            textStr = String.format(textStr, helicity.getTotalHelicity(),
                    NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = "Sfc - 3km SREH =\t\tM";
        }
        finalTextStr = finalTextStr + textStr;
        // EFF. SREH
        helicity = weatherDataStore.getStormTypeToHelicityMap()
                .get("Eff Inflow");

        if (helicity != null && NsharpLibBasics.qc(helicity.getPosHelicity())
                && NsharpLibBasics.qc(helicity.getNegHelicity())) {
            textStr = NsharpNativeConstants.STORM_TYPE_EFF_LINE;
            textStr = String.format(textStr, helicity.getTotalHelicity(),
                    NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_EFF_MISSING;
        }

        finalTextStr = finalTextStr + textStr;
        // EHI
        int currentParcel = handler.getCurrentParcel();
        Parcel pcl = weatherDataStore.getParcelMap().get(currentParcel);
        if (pcl != null && NsharpLibBasics.qc(pcl.getBplus())) {
            float ehi = pcl.getBplus() * helicity.getTotalHelicity() / 160000;
            if (NsharpLibBasics.qc(ehi)) {
                textStr = NsharpNativeConstants.STORM_TYPE_EHI_LINE;
                textStr = String.format(textStr, ehi);
            } else {
                textStr = NsharpNativeConstants.STORM_TYPE_EHI_MISSING;
            }
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_EHI_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // BRN
        if (pcl != null && NsharpLibBasics.qc(pcl.getBrn())) {
            textStr = NsharpNativeConstants.STORM_TYPE_BRN_LINE;
            textStr = String.format(textStr, pcl.getBrn());
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_BRN_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // Strom wind
        finalTextStr = finalTextStr + NsharpNativeConstants.STORM_WIND_STR;
        finalTextStr = finalTextStr
                + NsharpNativeConstants.STORM_LAYER_VECTOR_STR;
        // pressure-weighted SR mean wind at sfc-2 km
        WindComponent srWind = weatherDataStore.getStormTypeToSrMeanWindMap()
                .get("SFC-2km");
        if (srWind != null && NsharpLibBasics.qc(srWind.getWdir())) {
            textStr = NsharpNativeConstants.STORM_SFC2KM_VECT_LINE;
            textStr = String.format(textStr, srWind.getWdir(), srWind.getWspd(),
                    NsharpLibBasics.kt_to_mps(srWind.getWspd()));
        } else {
            textStr = NsharpNativeConstants.STORM_SFC2KM_VECT_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // pressure-weighted SR mean wind at 4-6 km
        srWind = weatherDataStore.getSrMeanWindComp4To6km();
        if (srWind != null && NsharpLibBasics.qc(srWind.getWdir())) {
            textStr = NsharpNativeConstants.STORM_4_6KM_VECT_LINE;
            textStr = String.format(textStr, srWind.getWdir(), srWind.getWspd(),
                    NsharpLibBasics.kt_to_mps(srWind.getWspd()));
        } else {
            textStr = NsharpNativeConstants.STORM_4_6KM_VECT_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // pressure-weighted SR mean wind at 9-11 km
        srWind = weatherDataStore.getSrMeanWindComp9To11km();
        if (srWind != null && NsharpLibBasics.qc(srWind.getWdir())) {
            textStr = NsharpNativeConstants.STORM_9_11KM_VECT_LINE;
            textStr = String.format(textStr, srWind.getWdir(), srWind.getWspd(),
                    NsharpLibBasics.kt_to_mps(srWind.getWspd()));
        } else {
            textStr = NsharpNativeConstants.STORM_9_11KM_VECT_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        return finalTextStr;
    }
}
