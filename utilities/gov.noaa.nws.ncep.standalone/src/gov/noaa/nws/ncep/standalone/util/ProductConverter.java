/*
 * gov.noaa.nws.ncep.ui.pgen.file.ProductConverter
 * 
 * Date created: 17 February 2009
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.standalone.util;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

import gov.noaa.nws.ncep.common.staticdata.SPCCounty;
import gov.noaa.nws.ncep.edex.common.stationTables.IStationField.StationField;
import gov.noaa.nws.ncep.edex.common.stationTables.Station;
import gov.noaa.nws.ncep.edex.common.stationTables.StationTable;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourCircle;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourLine;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourMinmax;
import gov.noaa.nws.ncep.ui.pgen.contours.Contours;
import gov.noaa.nws.ncep.ui.pgen.display.ArrowHead.ArrowHeadType;
import gov.noaa.nws.ncep.ui.pgen.display.FillPatternList.FillPattern;
import gov.noaa.nws.ncep.ui.pgen.display.IAvnText.AviationTextType;
import gov.noaa.nws.ncep.ui.pgen.display.ISinglePoint;
import gov.noaa.nws.ncep.ui.pgen.display.IText.DisplayType;
import gov.noaa.nws.ncep.ui.pgen.display.IText.FontStyle;
import gov.noaa.nws.ncep.ui.pgen.display.IText.TextJustification;
import gov.noaa.nws.ncep.ui.pgen.display.IText.TextRotation;
import gov.noaa.nws.ncep.ui.pgen.display.IVector.VectorType;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.Arc;
import gov.noaa.nws.ncep.ui.pgen.elements.AvnText;
import gov.noaa.nws.ncep.ui.pgen.elements.ComboSymbol;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElementFactory;
import gov.noaa.nws.ncep.ui.pgen.elements.Jet;
import gov.noaa.nws.ncep.ui.pgen.elements.Jet.JetBarb;
import gov.noaa.nws.ncep.ui.pgen.elements.Jet.JetHash;
import gov.noaa.nws.ncep.ui.pgen.elements.Jet.JetLine;
import gov.noaa.nws.ncep.ui.pgen.elements.Jet.JetText;
import gov.noaa.nws.ncep.ui.pgen.elements.KinkLine;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.MidCloudText;
import gov.noaa.nws.ncep.ui.pgen.elements.Outlook;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;
import gov.noaa.nws.ncep.ui.pgen.elements.Text;
import gov.noaa.nws.ncep.ui.pgen.elements.Track;
import gov.noaa.nws.ncep.ui.pgen.elements.Vector;
import gov.noaa.nws.ncep.ui.pgen.elements.WatchBox;
import gov.noaa.nws.ncep.ui.pgen.elements.WatchBox.WatchStatus;
import gov.noaa.nws.ncep.ui.pgen.elements.labeledlines.Cloud;
import gov.noaa.nws.ncep.ui.pgen.elements.labeledlines.Label;
import gov.noaa.nws.ncep.ui.pgen.elements.labeledlines.LabeledLine;
import gov.noaa.nws.ncep.ui.pgen.elements.labeledlines.Turbulence;
import gov.noaa.nws.ncep.ui.pgen.elements.tcm.Tcm;
import gov.noaa.nws.ncep.ui.pgen.file.ColorType;
import gov.noaa.nws.ncep.ui.pgen.file.Point;
import gov.noaa.nws.ncep.ui.pgen.file.Products;
import gov.noaa.nws.ncep.ui.pgen.file.TrackConverter;
import gov.noaa.nws.ncep.ui.pgen.file.WatchBox.Hole;
import gov.noaa.nws.ncep.ui.pgen.file.WatchBox.Outline;
import gov.noaa.nws.ncep.ui.pgen.file.WatchBox.Status;
import gov.noaa.nws.ncep.ui.pgen.gfa.Gfa;
import gov.noaa.nws.ncep.ui.pgen.gfa.GfaRules;
import gov.noaa.nws.ncep.ui.pgen.gfa.GfaWording;
import gov.noaa.nws.ncep.ui.pgen.sigmet.CcfpInfo;
import gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet;
import gov.noaa.nws.ncep.ui.pgen.sigmet.VaaInfo;
import gov.noaa.nws.ncep.ui.pgen.sigmet.Volcano;
import gov.noaa.nws.ncep.ui.pgen.tca.TCAElement;
import gov.noaa.nws.ncep.viz.common.SnapUtil;

/**
 * Define a ProductConverter Class - some methods to convert the products
 * between XML format and the actual in-memory PGEN products.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	--------------------------
 * 02/17/09		#63			J. Wu   	Initial Creation.
 * 01/11        #137        Q.Zhou      Move it from pgen.file to here (made some export)
 * 										Made standalone not depend on edex and isconverter condition.
 *             							Modified convertXML2WatchBox(), convertXML2Volcano()  
 * 
 * 11/1/2011   137          Q.Zhou      Copied from pgen.file again due to pgen refactor (Removed pgen.original).
 * 										Redo above modifications. Modified on new Text definition.
 * 02/12       597          S. Gurung   Moved snap functionalities to SnapUtil from SigmetInfo. 
 * 03/12       #676         Q. Zhou     Added Issue Office to IntlSigmet.  
 * 03/12       #599         Q. Zhou     Modified watchbox for anchor & county columns
 * 11/13       1065-TTR850  J. Wu       Added kink lines.
 * 05/14       TTR995       J. Wu       Set text's "auto" flag to false.
 * 02/15       R6158        J. Wu       Preserve ithw/iwidth for Text/AvnText/MidCloudText.
 * 03/15       R6872        J. Wu       Add status/forecaster/center in vgf2xml conversion.
 * 02/25/2016  R13544       S. Russell  Refactored convert(file.DrawableElement elem)
 *                                      to reduce its size and increase readability.
 *                                      Updated convertDELine() ( formerly part
 *                                      of the function above) to use a new
 *                                      constructor for Line() to save the
 *                                      flipSide value.
 *                                      Refactored converDEs() into convertListOfDEs()
 *
 * 04/28/20     77994       ksunil      new fields in Sigmet for Tropical Cyclone.
 * May 22, 2020 78000       ksunil      New Tropical Cyclone UI components for Fcst
 * Apr 08, 2021 90325       smanoj      CARSAM Backup WMO headers update.
 * Jun 18, 2021 90732       mroos       Added variables for VolAsh altitude level info
 * 
 * </pre>
 *
 * @author J. Wu
 */
public class ProductConverter {

    /*
     * Convert a XML file Products object to a list of PGEN in-memory Product
     * objects
     */
    public static List<Product> convert(Products filePrds) {

        List<Product> prd = new ArrayList<Product>();

        for (gov.noaa.nws.ncep.ui.pgen.file.Product fPrd : filePrds
                .getProduct()) {
            Product p = new Product();

            p.setName(fPrd.getName());
            p.setForecaster(fPrd.getForecaster());

            if (fPrd.isOnOff() != null) {
                p.setOnOff(fPrd.isOnOff());
            }

            if (fPrd.getType() != null) {
                p.setType(fPrd.getType());
            }

            if (fPrd.getCenter() != null) {
                p.setCenter(fPrd.getCenter());
            }

            if (fPrd.getForecaster() != null) {
                p.setForecaster(fPrd.getForecaster());
            }

            if (fPrd.getStatus() != null) {
                p.setStatus(fPrd.getStatus());
            }
            if (fPrd.isSaveLayers() != null) {
                p.setSaveLayers(fPrd.isSaveLayers());
            } else {
                p.setSaveLayers(false);
            }

            p.setUseFile(false);
            p.setInputFile(null);

            String outFile = fPrd.getOutputFile();
            if (outFile != null) {
                p.setOutputFile(outFile);
            } else {
                p.setOutputFile(null);
            }

            p.setLayers(convertFileLayers(fPrd.getLayer()));

            prd.add(p);
        }

        return prd;
    }

    /*
     * Convert a list of XML file Layer objects to a list of PGEN in-memory
     * Layer objects
     */
    private static List<Layer> convertFileLayers(
            List<gov.noaa.nws.ncep.ui.pgen.file.Layer> flayers) {

        List<Layer> layers = new ArrayList<Layer>();

        for (gov.noaa.nws.ncep.ui.pgen.file.Layer fLayer : flayers) {

            Layer lyr = new Layer();
            lyr.setName(fLayer.getName());

            lyr.setColor(new Color(fLayer.getColor().getRed(),
                    fLayer.getColor().getGreen(), fLayer.getColor().getBlue(),
                    fLayer.getColor().getAlpha()));

            if (fLayer.isOnOff() != null) {
                lyr.setOnOff(fLayer.isOnOff());
                lyr.setMonoColor(fLayer.isMonoColor());
                lyr.setFilled(fLayer.isFilled());
            } else {
                lyr.setOnOff(true);
                lyr.setMonoColor(false);
                lyr.setFilled(false);
            }

            lyr.setInputFile(null);
            lyr.setOutputFile(null);

            lyr.setDrawables(convert(fLayer.getDrawableElement()));

            layers.add(lyr);
        }

        return layers;
    }

    /*
     * Convert an XML file DrawableElement object to a list of PGEN in-memory
     * DrawableElement objects
     */
    private static List<AbstractDrawableComponent> convert(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem) {

        List<AbstractDrawableComponent> des = new ArrayList<AbstractDrawableComponent>();

        // Its a Line
        if (!elem.getLine().isEmpty()) {
            convertDELine(elem, des);
        }
        // Its a Symbol
        else if (!elem.getSymbol().isEmpty()) {
            convertDESymbol(elem, des);
        }
        // Its Text
        else if (!elem.getText().isEmpty()) {
            convertDEText(elem, des);
        }
        // Its AVN Text
        else if (!elem.getAvnText().isEmpty()) {
            convertDEAVNText(elem, des);
        }
        // Its MidCloud Text
        else if (!elem.getMidCloudText().isEmpty()) {
            convertDECloudText(elem, des);
        }
        // Its an Arc
        else if (!elem.getArc().isEmpty()) {
            convertDEArc(elem, des);
        }

        // The types above can be used with a TrackCoverter for tracking storms
        // so add them now. Not necessary for the types following this
        // function call
        des.addAll(TrackConverter
                .getTrackElementListByTrackBeanList(elem.getTrack()));

        // Its a Vector
        if (!elem.getVector().isEmpty()) {
            convertDEVector(elem, des);
        }
        // Its a TCA
        else if (!elem.getTCA().isEmpty()) {
            convertDE_TCA(elem, des);
        }
        // Its a DECollection
        else if (!elem.getDECollection().isEmpty()) {
            convertDE_DECollection(elem, des);
        }
        // Its a Watch Box
        else if (!elem.getWatchBox().isEmpty()) {
            for (gov.noaa.nws.ncep.ui.pgen.file.WatchBox fwb : elem
                    .getWatchBox()) {
                des.add(convertXML2WatchBox(fwb));
            }
        }
        // Conturs
        else if (!elem.getContours().isEmpty()) {
            for (gov.noaa.nws.ncep.ui.pgen.file.Contours fdec : elem
                    .getContours()) {
                des.add(convertXML2Contours(fdec));
            }
        }
        // Its an Outlook
        else if (!elem.getOutlook().isEmpty()) {
            for (gov.noaa.nws.ncep.ui.pgen.file.Outlook fotlk : elem
                    .getOutlook()) {
                des.add(convertXML2Outlook(fotlk));
            }
        }
        // Its a Sigment
        else if (!elem.getSigmet().isEmpty()) {
            convertDESigment(elem, des);
        }
        // Its a GFA
        else if (!elem.getGfa().isEmpty()) {
            convertDE_GFA(elem, des);
        }
        // Its a Volcano
        else if (!elem.getVolcano().isEmpty()) {
            for (gov.noaa.nws.ncep.ui.pgen.file.Volcano fVol : elem
                    .getVolcano()) {
                des.add(convertXML2Volcano(fVol));
            }
        }
        // Its a TCM
        else if (!elem.getTcm().isEmpty()) {
            for (gov.noaa.nws.ncep.ui.pgen.file.TCM ftcm : elem.getTcm()) {
                des.add(convertXML2Tcm(ftcm));
            }
        }

        return des;
    }

    /*
     * Convert an XML file DrawableElement object to a GFA object
     */

    private static void convertDE_GFA(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem,
            List<AbstractDrawableComponent> des) {
        for (gov.noaa.nws.ncep.ui.pgen.file.Gfa fgfa : elem.getGfa()) {

            Color[] clr = new Color[fgfa.getColor().size()];
            int nn = 0;
            for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fgfa
                    .getColor()) {
                clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                        fColor.getBlue(), fColor.getAlpha());
            }

            ArrayList<Coordinate> linePoints = new ArrayList<Coordinate>();
            nn = 0;
            for (Point pt : fgfa.getPoint()) {
                linePoints.add(new Coordinate(pt.getLon(), pt.getLat()));
            }

            Coordinate gfaTextCoordinate = new Coordinate(fgfa.getLonText(),
                    fgfa.getLatText());

            Gfa gfa = new Gfa(null, clr, fgfa.getLineWidth(),
                    fgfa.getSizeScale(), fgfa.isClosed(), fgfa.isFilled(),
                    linePoints, gfaTextCoordinate, fgfa.getSmoothFactor(),
                    FillPattern.valueOf(fgfa.getFillPattern()),
                    fgfa.getPgenCategory(), fgfa.getPgenType(),
                    fgfa.getHazard(), fgfa.getFcstHr(), fgfa.getTag(),
                    fgfa.getDesk(), fgfa.getIssueType(), fgfa.getCycleDay(),
                    fgfa.getCycleHour(), fgfa.getType(), fgfa.getArea(),
                    fgfa.getBeginning(), fgfa.getEnding(), fgfa.getStates());

            gfa.setGfaValue(Gfa.GR, fgfa.getGr());
            gfa.setGfaValue(Gfa.FREQUENCY, fgfa.getFrequency());
            gfa.setGfaValue(Gfa.CATEGORY, fgfa.getTsCategory());
            gfa.setGfaValue(Gfa.FZL_RANGE, fgfa.getFzlRange());
            gfa.setGfaValue(Gfa.LEVEL, fgfa.getLevel());
            gfa.setGfaValue(Gfa.INTENSITY, fgfa.getIntensity());
            gfa.setGfaValue(Gfa.SPEED, fgfa.getSpeed());
            gfa.setGfaValue(Gfa.DUE_TO, fgfa.getDueTo());
            gfa.setGfaValue(Gfa.LYR, fgfa.getLyr());
            gfa.setGfaValue(Gfa.COVERAGE, fgfa.getCoverage());
            gfa.setGfaValue(Gfa.BOTTOM, fgfa.getBottom());
            gfa.setGfaValue(Gfa.TOP, fgfa.getTop());
            if (fgfa.getTop() != null && fgfa.getBottom() != null) {
                gfa.setGfaValue(Gfa.TOP_BOTTOM,
                        fgfa.getTop() + "/" + fgfa.getBottom());
            }
            gfa.setGfaValue(Gfa.FZL_TOP_BOTTOM, fgfa.getFzlTopBottom());
            gfa.setGfaValue(Gfa.CONTOUR, fgfa.getContour());
            if ("ICE".equals(gfa.getGfaHazard())) {
                gfa.setGfaType("");
                gfa.setGfaValue("Type", fgfa.getType());
            }
            des.add(gfa);
        }
    }

    /*
     * Convert an XML file DrawableElement object to a sigment
     */

    private static void convertDESigment(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem,
            List<AbstractDrawableComponent> des) {
        for (gov.noaa.nws.ncep.ui.pgen.file.Sigmet fSig : elem.getSigmet()) {

            Color[] clr = new Color[fSig.getColor().size()];
            int nn = 0;
            for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fSig
                    .getColor()) {
                clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                        fColor.getBlue(), fColor.getAlpha());
            }

            ArrayList<Coordinate> sigmetPoints = new ArrayList<Coordinate>();
            nn = 0;
            for (Point pt : fSig.getPoint()) {
                sigmetPoints.add(new Coordinate(pt.getLon(), pt.getLat()));
            }

            Sigmet sigmet = new Sigmet(null, clr, fSig, sigmetPoints);

            des.add(sigmet);
        }
    }

    /*
     * Convert an XML file DrawableElement object to a DEColleciton obj
     */

    private static void convertDE_DECollection(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem,
            List<AbstractDrawableComponent> des) {
        for (gov.noaa.nws.ncep.ui.pgen.file.DECollection fdec : elem
                .getDECollection()) {
            String cname = fdec.getCollectionName();
            if (cname.equalsIgnoreCase("jet")) {
                Jet jet = convertXML2Jet(fdec);
                if (jet != null) {
                    des.add(jet);
                }
            } else if (cname.equalsIgnoreCase("Cloud")) {
                des.add(convertXML2Cloud(fdec));
            } else if (cname.equalsIgnoreCase("Turbulence")) {
                des.add(convertXML2Turb(fdec));
            } else if (cname.contains("CCFP_SIGMET")) {
                des.add(convertXML2Ccfp(fdec));
            } else {
                DECollection dec = new DECollection(cname);
                dec.setPgenCategory(fdec.getPgenCategory());
                dec.setPgenType(fdec.getPgenType());
                dec.add(convert(fdec.getDrawableElement()));
                des.add(dec);
            }
        }
    }

    /*
     * Convert an XML file DrawableElement object to a TCAElement
     */

    private static void convertDE_TCA(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem,
            List<AbstractDrawableComponent> des) {
        for (gov.noaa.nws.ncep.ui.pgen.file.TCA ftca : elem.getTCA()) {

            TCAElement tca = new TCAElement();
            tca.setPgenType(ftca.getPgenType());
            tca.setPgenCategory(ftca.getPgenCategory());

            tca.setStormNumber(ftca.getStormNumber());
            tca.setStormName(ftca.getStormName());
            tca.setBasin(ftca.getBasin());
            tca.setIssueStatus(ftca.getIssueStatus());
            tca.setStormType(ftca.getStormType());
            tca.setAdvisoryNumber(ftca.getAdvisoryNumber());
            tca.setTimeZone(ftca.getTimeZone());
            tca.setTextLocation(ftca.getTextLocation());

            Calendar advTime = Calendar
                    .getInstance(TimeZone.getTimeZone("GMT"));
            XMLGregorianCalendar xmlCal = ftca.getAdvisoryTime();
            advTime.set(xmlCal.getYear(), xmlCal.getMonth() - 1,
                    xmlCal.getDay(), xmlCal.getHour(), xmlCal.getMinute(),
                    xmlCal.getSecond());
            tca.setAdvisoryTime(advTime);

            tca.setAdvisories(ftca.getAdvisories());

            des.add(tca);
        }
    }

    /*
     * Convert an XML file DrawableElement object to a PGen vector
     */

    private static void convertDEVector(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem,
            List<AbstractDrawableComponent> des) {
        for (gov.noaa.nws.ncep.ui.pgen.file.Vector fVector : elem.getVector()) {

            Color[] clr = new Color[fVector.getColor().size()];
            int nn = 0;
            for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fVector
                    .getColor()) {
                clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                        fColor.getBlue(), fColor.getAlpha());
            }

            Point loc = fVector.getPoint();

            VectorType vtype = null;
            String pgenType = fVector.getPgenType();
            if (pgenType.equalsIgnoreCase("Hash")) {
                vtype = VectorType.HASH_MARK;
            } else if (pgenType.equalsIgnoreCase("Barb")) {
                vtype = VectorType.WIND_BARB;
            } else {
                vtype = VectorType.ARROW;
            }

            Vector vector = new Vector(null, clr, fVector.getLineWidth(),
                    fVector.getSizeScale(), fVector.isClear(),
                    (new Coordinate(loc.getLon(), loc.getLat())), vtype,
                    fVector.getSpeed(), fVector.getDirection(),
                    fVector.getArrowHeadSize(), fVector.isDirectionOnly(),
                    fVector.getPgenCategory(), fVector.getPgenType());

            des.add(vector);
        }
    }

    /*
     * Convert an XML file DrawableElement object to a DrawableElement Arc
     */

    private static void convertDEArc(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem,
            List<AbstractDrawableComponent> des) {
        for (gov.noaa.nws.ncep.ui.pgen.file.Arc fArc : elem.getArc()) {

            Color[] clr = new Color[fArc.getColor().size()];
            int nn = 0;
            for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fArc
                    .getColor()) {
                clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                        fColor.getBlue(), fColor.getAlpha());
            }

            ArrayList<Coordinate> linePoints = new ArrayList<Coordinate>();
            for (Point pt : fArc.getPoint()) {
                linePoints.add(new Coordinate(pt.getLon(), pt.getLat()));
            }

            Arc arc = new Arc(null, clr[0], fArc.getLineWidth(),
                    fArc.getSizeScale(), fArc.isClosed(), fArc.isFilled(),
                    fArc.getSmoothFactor(),
                    FillPattern.valueOf(fArc.getFillPattern()),
                    fArc.getPgenType(), linePoints.get(0), linePoints.get(1),
                    fArc.getPgenCategory(), fArc.getAxisRatio(),
                    fArc.getStartAngle(), fArc.getEndAngle());

            des.add(arc);

        }
    }

    /*
     * Convert an XML file DrawableElement object to a DEC cloud text
     */

    private static void convertDECloudText(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem,
            List<AbstractDrawableComponent> des) {
        for (gov.noaa.nws.ncep.ui.pgen.file.MidCloudText mText : elem
                .getMidCloudText()) {

            Color[] clr = new Color[mText.getColor().size()];
            int nn = 0;
            for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : mText
                    .getColor()) {
                clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                        fColor.getBlue(), fColor.getAlpha());
            }

            Point loc = mText.getPoint();

            MidCloudText text = new MidCloudText((Coordinate[]) null,
                    mText.getFontName(), mText.getFontSize(),
                    TextJustification.valueOf(mText.getJustification()),
                    (new Coordinate(loc.getLon(), loc.getLat())),
                    mText.getCloudTypes(), mText.getCloudAmounts(),
                    mText.getTurbulenceType(), mText.getTurbulenceLevels(),
                    mText.getIcingType(), mText.getIcingLevels(),
                    mText.getTstormTypes(), mText.getTstormLevels(),
                    FontStyle.valueOf(mText.getStyle()), clr[0],
                    mText.getPgenCategory(), mText.getPgenType());

            // R6158 - preserve ithw/iwidth
            if (mText.getIthw() != null) {
                text.setIthw(mText.getIthw());
            }

            if (mText.getIwidth() != null) {
                text.setIwidth(mText.getIwidth());
            }

            des.add(text);
        }
    }

    /*
     * Convert an XML file DrawableElement object to AVN text
     */

    private static void convertDEAVNText(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem,
            List<AbstractDrawableComponent> des) {
        for (gov.noaa.nws.ncep.ui.pgen.file.AvnText aText : elem.getAvnText()) {

            Color[] clr = new Color[aText.getColor().size()];
            int nn = 0;
            for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : aText
                    .getColor()) {
                clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                        fColor.getBlue(), fColor.getAlpha());
            }

            Point loc = aText.getPoint();

            AvnText text = new AvnText((Coordinate[]) null, aText.getFontName(),
                    aText.getFontSize(),
                    TextJustification.valueOf(aText.getJustification()),
                    (new Coordinate(loc.getLon(), loc.getLat())),
                    AviationTextType.valueOf(aText.getAvnTextType()),
                    aText.getTopValue(), aText.getBottomValue(),
                    FontStyle.valueOf(aText.getStyle()), clr[0],
                    aText.getSymbolPatternName(), aText.getPgenCategory(),
                    aText.getPgenType());

            // R6158 - preserve ithw/iwidth
            if (aText.getIthw() != null) {
                text.setIthw(aText.getIthw());
            }

            if (aText.getIwidth() != null) {
                text.setIwidth(aText.getIwidth());
            }

            des.add(text);
        }
    }

    /*
     * Convert an XML file DrawableElement object to DrawableElement text
     */
    private static void convertDEText(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem,
            List<AbstractDrawableComponent> des) {
        for (gov.noaa.nws.ncep.ui.pgen.file.Text fText : elem.getText()) {

            Color[] clr = new Color[fText.getColor().size()];
            int nn = 0;
            for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fText
                    .getColor()) {
                clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                        fColor.getBlue(), fColor.getAlpha());
            }

            Point loc = fText.getPoint();

            String[] st = new String[fText.getTextLine().size()];

            int nline = 0;
            for (String str : fText.getTextLine()) {
                st[nline++] = str;
            }

            Text text = new Text((Coordinate[]) null, fText.getFontName(),
                    fText.getFontSize(),
                    TextJustification.valueOf(fText.getJustification()),
                    (new Coordinate(loc.getLon(), loc.getLat())),
                    fText.getRotation(),
                    TextRotation.valueOf(fText.getRotationRelativity()), st,
                    FontStyle.valueOf(fText.getStyle()), clr[0], 0, 0,
                    fText.isMask(), DisplayType.valueOf(fText.getDisplayType()),
                    fText.getPgenCategory(), fText.getPgenType());

            if (fText.getXOffset() != null) {
                text.setXOffset(fText.getXOffset());
            }

            if (fText.getYOffset() != null) {
                text.setYOffset(fText.getYOffset());
            }

            if (fText.isHide() != null) {
                text.setHide(fText.isHide());
            }

            // R6158 - preserve ithw/iwidth
            if (fText.getIthw() != null) {
                text.setIthw(fText.getIthw());
            }

            if (fText.getIwidth() != null) {
                text.setIwidth(fText.getIwidth());
            }

            /*
             * if (fText.isAuto() != null) { text.setAuto(fText.isAuto()); }
             */
            text.setAuto(false);
            des.add(text);
        }
    }

    /*
     * Convert an XML file DrawableElement object to a Symbol object
     */

    private static void convertDESymbol(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem,
            List<AbstractDrawableComponent> des) {
        for (gov.noaa.nws.ncep.ui.pgen.file.Symbol fSymbol : elem.getSymbol()) {

            Color[] clr = new Color[fSymbol.getColor().size()];
            int nn = 0;
            for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fSymbol
                    .getColor()) {
                clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                        fColor.getBlue(), fColor.getAlpha());
            }

            Point loc = fSymbol.getPoint();

            if (fSymbol.getPgenCategory().equals("Combo")) {
                ComboSymbol symbol = new ComboSymbol(null, clr,
                        fSymbol.getLineWidth(), fSymbol.getSizeScale(),
                        fSymbol.isClear(),
                        (new Coordinate(loc.getLon(), loc.getLat())),
                        fSymbol.getPgenCategory(), fSymbol.getPgenType());
                des.add(symbol);
            } else {
                Symbol symbol = new Symbol(null, clr, fSymbol.getLineWidth(),
                        fSymbol.getSizeScale(), fSymbol.isClear(),
                        (new Coordinate(loc.getLon(), loc.getLat())),
                        fSymbol.getPgenCategory(), fSymbol.getPgenType());
                des.add(symbol);
            }

        }
    }

    /*
     * Convert an XML file DrawableElement object to a line
     */

    private static void convertDELine(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem,
            List<AbstractDrawableComponent> des) {
        for (gov.noaa.nws.ncep.ui.pgen.file.Line fLine : elem.getLine()) {

            Color[] clr = new Color[fLine.getColor().size()];
            int nn = 0;
            for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fLine
                    .getColor()) {
                clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                        fColor.getBlue(), fColor.getAlpha());
            }

            ArrayList<Coordinate> linePoints = new ArrayList<Coordinate>();
            nn = 0;
            for (Point pt : fLine.getPoint()) {
                linePoints.add(new Coordinate(pt.getLon(), pt.getLat()));
            }

            Line line;

            if (fLine.getArrowHeadType() != null
                    && fLine.getKinkPosition() != null) {
                line = new KinkLine(null, clr, fLine.getLineWidth(),
                        fLine.getSizeScale(), fLine.isClosed(),
                        fLine.isFilled(), linePoints, fLine.getSmoothFactor(),
                        FillPattern.valueOf(fLine.getFillPattern()),
                        fLine.getPgenCategory(), fLine.getPgenType(),
                        fLine.getKinkPosition(),
                        ArrowHeadType.valueOf(fLine.getArrowHeadType()));
            } else {
                line = new Line(null, clr, fLine.getLineWidth(),
                        fLine.getSizeScale(), fLine.isClosed(),
                        fLine.isFilled(), linePoints, fLine.getSmoothFactor(),
                        FillPattern.valueOf(fLine.getFillPattern()),
                        fLine.getPgenCategory(), fLine.getPgenType(),
                        fLine.isFlipSide());
            }

            des.add(line);
        }
    }

    /*
     * Convert a list of in-memory PGEN Products into a XML file Products object
     */
    public static Products convert(List<Product> prds) {

        Products fprds = new Products();

        for (Product prd : prds) {

            gov.noaa.nws.ncep.ui.pgen.file.Product p = new gov.noaa.nws.ncep.ui.pgen.file.Product();

            p.setName(prd.getName());
            p.setType(prd.getType());
            p.setForecaster(prd.getForecaster());
            p.setCenter(prd.getCenter());

            if (prd.getForecaster() != null) {
                p.setForecaster(prd.getForecaster());
            }

            if (prd.getStatus() != null) {
                p.setStatus(prd.getStatus());
            }

            String outFile = prd.getOutputFile();
            if (outFile != null) {
                p.setOutputFile(outFile);
            } else {
                p.setOutputFile(null);
            }

            p.setInputFile(null);
            p.setUseFile(false);
            p.setOnOff(prd.isOnOff());
            p.setSaveLayers(prd.isSaveLayers());

            p.getLayer().addAll(convertLayers(prd.getLayers()));

            fprds.getProduct().add(p);

        }

        return fprds;
    }

    /*
     * Convert a list of PGEN in-memory Layer objects into a list of XML file
     * Layer objects
     */
    private static List<gov.noaa.nws.ncep.ui.pgen.file.Layer> convertLayers(
            List<Layer> layers) {

        List<gov.noaa.nws.ncep.ui.pgen.file.Layer> flyrs = new ArrayList<gov.noaa.nws.ncep.ui.pgen.file.Layer>();

        for (Layer lyr : layers) {
            gov.noaa.nws.ncep.ui.pgen.file.Layer l = new gov.noaa.nws.ncep.ui.pgen.file.Layer();

            if (lyr.getName() != null) {
                l.setName(lyr.getName());
            }

            gov.noaa.nws.ncep.ui.pgen.file.Color clr = new gov.noaa.nws.ncep.ui.pgen.file.Color();

            if (lyr.getColor() != null) {

                clr.setRed(lyr.getColor().getRed());
                clr.setGreen(lyr.getColor().getGreen());
                clr.setBlue(lyr.getColor().getBlue());
                clr.setAlpha(lyr.getColor().getAlpha());

            } else {

                clr.setRed(0);
                clr.setGreen(255);
                clr.setBlue(255);
                clr.setAlpha(255);
            }

            l.setColor(clr);

            l.setOnOff(lyr.isOnOff());
            l.setMonoColor(lyr.isMonoColor());
            l.setFilled(lyr.isFilled());
            l.setInputFile(null);
            l.setOutputFile(null);
            l.setDrawableElement(convertListOfDEs(lyr.getDrawables()));

            flyrs.add(l);
        }

        return flyrs;
    }

    /*
     * Convert a list of in-memory PGEN DrawableElement objects into a list of
     * XML file DrawableElement objects
     */
    private static gov.noaa.nws.ncep.ui.pgen.file.DrawableElement convertListOfDEs(
            List<AbstractDrawableComponent> des) {

        gov.noaa.nws.ncep.ui.pgen.file.DrawableElement fde = new gov.noaa.nws.ncep.ui.pgen.file.DrawableElement();

        // For each ADC obj in the input List
        for (AbstractDrawableComponent adc : des) {

            // If a single DE
            if (adc instanceof DrawableElement) {
                DrawableElement de = (DrawableElement) adc;

                // Its a Line
                if (de instanceof Line) {

                    if (de instanceof Arc) {
                        convertLODLineArc(fde, de);
                    } else if (de instanceof Gfa) {
                        convertLODLineGFA(fde, de);
                    } else if (de instanceof Track) {
                        fde.getTrack().add(TrackConverter
                                .getTrackBeanByTrackElement((Track) de));
                    } else if (de instanceof Sigmet) {
                        convertLODLineSigmet(fde, de);
                    } else {
                        convertLODLine(fde, de);
                    }

                }
                // Its a Symbol
                else if (de instanceof Symbol || de instanceof ComboSymbol) {
                    convertLODSymbol(fde, de);
                }
                // Its AvnText
                else if (de instanceof AvnText) {
                    convertLODAvnText(fde, de);
                }
                // Its MidCloudText
                else if (de instanceof MidCloudText) {
                    convertLODMidCloutText(fde, de);
                }
                // Its Text
                else if (de instanceof Text) {
                    convertLODText(fde, de);
                }
                // Its a Vector
                else if (de instanceof Vector) {
                    converLODVector(fde, de);
                }
                // Its a Watch Box
                else if (de instanceof WatchBox) {
                    fde.getWatchBox().add(convertWatchBox2XML((WatchBox) de));
                }
                // Its a TCM
                else if (de instanceof Tcm) {
                    fde.getTcm().add(convertTcm2XML((Tcm) de));
                }
                // Its a TCAElement
                else if (de instanceof TCAElement) {
                    convertLODTCAElement(fde, de);
                }
                // Its a Voclcano
                else if (de instanceof Volcano) {
                    fde.getVolcano().add(convertVolcano2XML((Volcano) de));
                }
            }
            // Not a Single DrawableElement, but a DE Collection
            else if (adc instanceof DECollection) {

                if (adc.getName().equalsIgnoreCase("Contours")) {
                    fde.getContours().add(convertContours2XML((Contours) adc));
                } else if (adc.getName().equalsIgnoreCase("Outlook")) {
                    fde.getOutlook().add(convertOutlook2XML((Outlook) adc));
                } else {
                    fde.getDECollection()
                            .add(convertDECollection2XML((DECollection) adc));
                }
            }

        } // end For Loop: For each ADC obj in the input List

        return fde;
    }

    /*
     * Convert in-memory PGEN TCA DrawableElement object into a list held in an
     * XML file DrawableElement fde
     */

    private static void convertLODTCAElement(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement fde,
            DrawableElement de) {
        TCAElement tcaEl = (TCAElement) de;

        gov.noaa.nws.ncep.ui.pgen.file.TCA tca = new gov.noaa.nws.ncep.ui.pgen.file.TCA();

        tca.setPgenType(tcaEl.getPgenType());
        tca.setPgenCategory(tcaEl.getPgenCategory());

        tca.setStormNumber(tcaEl.getStormNumber());
        tca.setStormName(tcaEl.getStormName());
        tca.setBasin(tcaEl.getBasin());
        tca.setIssueStatus(tcaEl.getIssueStatus());
        tca.setStormType(tcaEl.getStormType());
        tca.setAdvisoryNumber(tcaEl.getAdvisoryNumber());
        tca.setTimeZone(tcaEl.getTimeZone());
        tca.setTextLocation(tcaEl.getTextLocation());

        Calendar advTime = tcaEl.getAdvisoryTime();
        XMLGregorianCalendar xmlCal = null;
        try {
            xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    advTime.get(Calendar.YEAR), advTime.get(Calendar.MONTH) + 1,
                    advTime.get(Calendar.DAY_OF_MONTH),
                    advTime.get(Calendar.HOUR_OF_DAY),
                    advTime.get(Calendar.MINUTE), advTime.get(Calendar.SECOND),
                    advTime.get(Calendar.MILLISECOND), 0);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        tca.setAdvisoryTime(xmlCal);

        tca.setAdvisories(tcaEl.getAdvisories());

        fde.getTCA().add(tca);
    }

    /*
     * Convert in-memory PGEN Vector DrawableElement object into a list held in
     * an XML file DrawableElement fde
     */
    private static void converLODVector(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement fde,
            DrawableElement de) {
        gov.noaa.nws.ncep.ui.pgen.file.Vector vector = new gov.noaa.nws.ncep.ui.pgen.file.Vector();

        for (Color clr : de.getColors()) {

            gov.noaa.nws.ncep.ui.pgen.file.Color fclr = new gov.noaa.nws.ncep.ui.pgen.file.Color();

            fclr.setRed(clr.getRed());
            fclr.setGreen(clr.getGreen());
            fclr.setBlue(clr.getBlue());
            fclr.setAlpha(clr.getAlpha());

            vector.getColor().add(fclr);
        }

        Point fpt = new Point();
        fpt.setLat(((ISinglePoint) de).getLocation().y);
        fpt.setLon(((ISinglePoint) de).getLocation().x);
        vector.setPoint(fpt);

        vector.setPgenType(de.getPgenType());
        vector.setPgenCategory(de.getPgenCategory());

        vector.setLineWidth(de.getLineWidth());
        vector.setSizeScale(de.getSizeScale());
        vector.setClear(((Vector) de).isClear());

        vector.setDirection(((Vector) de).getDirection());
        vector.setSpeed(((Vector) de).getSpeed());
        vector.setArrowHeadSize(((Vector) de).getArrowHeadSize());
        vector.setDirectionOnly(((Vector) de).hasDirectionOnly());

        fde.getVector().add(vector);
    }
                        sigmet.setEditableAttrAltLevel(
                                ((Sigmet) de).getEditableAttrAltLevel());
                        sigmet.setEditableAttrAltLevelInfo1(
                                ((Sigmet) de).getEditableAttrAltLevelInfo1());
                        sigmet.setEditableAttrAltLevelInfo2(
                                ((Sigmet) de).getEditableAttrAltLevelInfo2());
                        sigmet.setEditableAttrAltLevelText1(
                                ((Sigmet) de).getEditableAttrAltLevelText1());
                        sigmet.setEditableAttrAltLevelText2(
                                ((Sigmet) de).getEditableAttrAltLevelText2());

    /*
     * Convert in-memory PGEN Text DrawableElement object into a list held in an
     * XML file DrawableElement fde
     */

    private static void convertLODText(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement fde,
            DrawableElement de) {
        gov.noaa.nws.ncep.ui.pgen.file.Text text = new gov.noaa.nws.ncep.ui.pgen.file.Text();

        for (Color clr : de.getColors()) {

            gov.noaa.nws.ncep.ui.pgen.file.Color fclr = new gov.noaa.nws.ncep.ui.pgen.file.Color();

            fclr.setRed(clr.getRed());
            fclr.setGreen(clr.getGreen());
            fclr.setBlue(clr.getBlue());
            fclr.setAlpha(clr.getAlpha());
            text.getColor().add(fclr);
        }

        Point fpt = new Point();
        fpt.setLat(((ISinglePoint) de).getLocation().y);
        fpt.setLon(((ISinglePoint) de).getLocation().x);
        text.setPoint(fpt);

        for (String st : ((Text) de).getString()) {
            text.getTextLine().add(new String(st));
        }

        text.setXOffset(((Text) de).getXOffset());
        text.setYOffset(((Text) de).getYOffset());
        text.setDisplayType(((Text) de).getDisplayType().name());
        text.setMask(((Text) de).maskText());
        text.setRotationRelativity(((Text) de).getRotationRelativity().name());
        text.setRotation(((Text) de).getRotation());
        text.setJustification(((Text) de).getJustification().name());
        text.setStyle(((Text) de).getStyle().name());
        text.setFontName(((Text) de).getFontName());
        text.setFontSize(((Text) de).getFontSize());
        text.setPgenType(((Text) de).getPgenType());
        text.setPgenCategory(de.getPgenCategory());

        text.setHide(((Text) de).getHide());
        text.setAuto(((Text) de).getAuto());

        text.setIthw(((Text) de).getIthw());
        text.setIwidth(((Text) de).getIwidth());

        fde.getText().add(text);
    }

    /*
     * Convert in-memory PGEN MidCloudText DrawableElement object into a list
     * held in an XML file DrawableElement fde
     */
    private static void convertLODMidCloutText(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement fde,
            DrawableElement de) {
        MidCloudText mcde = (MidCloudText) de;

        gov.noaa.nws.ncep.ui.pgen.file.MidCloudText mtext = new gov.noaa.nws.ncep.ui.pgen.file.MidCloudText();

        for (Color clr : de.getColors()) {

            gov.noaa.nws.ncep.ui.pgen.file.Color fclr = new gov.noaa.nws.ncep.ui.pgen.file.Color();

            fclr.setRed(clr.getRed());
            fclr.setGreen(clr.getGreen());
            fclr.setBlue(clr.getBlue());
            fclr.setAlpha(clr.getAlpha());
            mtext.getColor().add(fclr);
        }

        Point fpt = new Point();
        fpt.setLat(((ISinglePoint) de).getLocation().y);
        fpt.setLon(((ISinglePoint) de).getLocation().x);
        mtext.setPoint(fpt);

        mtext.setCloudTypes(mcde.getCloudTypes());
        mtext.setCloudAmounts(mcde.getCloudAmounts());
        mtext.setTurbulenceType(mcde.getTurbulencePattern());
        mtext.setTurbulenceLevels(mcde.getTurbulenceLevels());
        mtext.setIcingType(mcde.getIcingPattern());
        mtext.setIcingLevels(mcde.getIcingLevels());
        mtext.setTstormTypes(mcde.getTstormTypes());
        mtext.setTstormLevels(mcde.getTstormLevels());

        mtext.setIthw(mcde.getIthw());
        mtext.setIwidth(mcde.getIwidth());

        mtext.setJustification(mcde.getJustification().name());
        mtext.setStyle(mcde.getStyle().name());
        mtext.setFontName(mcde.getFontName());
        mtext.setFontSize(mcde.getFontSize());
        mtext.setPgenType(mcde.getPgenType());
        mtext.setPgenCategory(mcde.getPgenCategory());

        fde.getMidCloudText().add(mtext);
    }

    /*
     * Convert in-memory PGEN AvnText DrawableElement object into a list held in
     * an XML file DrawableElement fde
     */
    private static void convertLODAvnText(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement fde,
            DrawableElement de) {
        gov.noaa.nws.ncep.ui.pgen.file.AvnText atext = new gov.noaa.nws.ncep.ui.pgen.file.AvnText();

        for (Color clr : de.getColors()) {

            gov.noaa.nws.ncep.ui.pgen.file.Color fclr = new gov.noaa.nws.ncep.ui.pgen.file.Color();

            fclr.setRed(clr.getRed());
            fclr.setGreen(clr.getGreen());
            fclr.setBlue(clr.getBlue());
            fclr.setAlpha(clr.getAlpha());
            atext.getColor().add(fclr);
        }

        Point fpt = new Point();
        fpt.setLat(((ISinglePoint) de).getLocation().y);
        fpt.setLon(((ISinglePoint) de).getLocation().x);
        atext.setPoint(fpt);

        atext.setAvnTextType(((AvnText) de).getAvnTextType().name());
        atext.setTopValue(((AvnText) de).getTopValue());
        atext.setBottomValue(((AvnText) de).getBottomValue());

        atext.setIthw(((AvnText) de).getIthw());
        atext.setIwidth(((AvnText) de).getIwidth());

        atext.setJustification(((AvnText) de).getJustification().name());
        atext.setStyle(((AvnText) de).getStyle().name());
        atext.setFontName(((AvnText) de).getFontName());
        atext.setFontSize(((AvnText) de).getFontSize());
        atext.setSymbolPatternName(((AvnText) de).getSymbolPatternName());
        atext.setPgenType(de.getPgenType());
        atext.setPgenCategory(de.getPgenCategory());

        fde.getAvnText().add(atext);
    }

    /*
     * Convert in-memory PGEN Symbol DrawableElement object into a list held in
     * an XML file DrawableElement fde
     */
    private static void convertLODSymbol(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement fde,
            DrawableElement de) {
        gov.noaa.nws.ncep.ui.pgen.file.Symbol symbol = new gov.noaa.nws.ncep.ui.pgen.file.Symbol();

        for (Color clr : de.getColors()) {

            gov.noaa.nws.ncep.ui.pgen.file.Color fclr = new gov.noaa.nws.ncep.ui.pgen.file.Color();

            fclr.setRed(clr.getRed());
            fclr.setGreen(clr.getGreen());
            fclr.setBlue(clr.getBlue());
            fclr.setAlpha(clr.getAlpha());

            symbol.getColor().add(fclr);
        }

        Point fpt = new Point();
        fpt.setLat(((ISinglePoint) de).getLocation().y);
        fpt.setLon(((ISinglePoint) de).getLocation().x);
        symbol.setPoint(fpt);

        symbol.setPgenType(de.getPgenType());
        symbol.setPgenCategory(de.getPgenCategory());
        symbol.setLineWidth(de.getLineWidth());
        symbol.setSizeScale(de.getSizeScale());
        symbol.setClear(((ISinglePoint) de).isClear());

        fde.getSymbol().add(symbol);
    }

    /*
     * Convert in-memory PGEN Line DrawableElement object into a list held in an
     * XML file DrawableElement fde
     */
    private static void convertLODLine(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement fde,
            DrawableElement de) {
        gov.noaa.nws.ncep.ui.pgen.file.Line line = new gov.noaa.nws.ncep.ui.pgen.file.Line();

        for (Color clr : de.getColors()) {

            gov.noaa.nws.ncep.ui.pgen.file.Color fclr = new gov.noaa.nws.ncep.ui.pgen.file.Color();

            fclr.setRed(clr.getRed());
            fclr.setGreen(clr.getGreen());
            fclr.setBlue(clr.getBlue());
            fclr.setAlpha(clr.getAlpha());

            line.getColor().add(fclr);
        }

        for (Coordinate crd : ((Line) de).getLinePoints()) {

            Point fpt = new Point();
            fpt.setLat(crd.y);
            fpt.setLon(crd.x);

            line.getPoint().add(fpt);
        }

        line.setPgenCategory(de.getPgenCategory());
        line.setLineWidth(de.getLineWidth());
        line.setSizeScale(de.getSizeScale());
        line.setSmoothFactor(((Line) de).getSmoothFactor());
        line.setClosed(((Line) de).isClosedLine());
        line.setFilled(((Line) de).isFilled());
        line.setPgenType(de.getPgenType());

        line.setFillPattern(((Line) de).getFillPattern().name());
        line.setFlipSide(((Line) de).isFlipSide());

        // specific attributes for KinkLine
        if (de instanceof KinkLine) {
            line.setArrowHeadType(((KinkLine) de).getArrowHeadType().name());
            line.setKinkPosition(((KinkLine) de).getKinkPosition());
        }

        fde.getLine().add(line);
    }

    /*
     * Convert in-memory PGEN Sigmet DrawableElement object into a list held in
     * an XML file DrawableElement fde
     */
    private static void convertLODLineSigmet(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement fde,
            DrawableElement de) {
        gov.noaa.nws.ncep.ui.pgen.file.Sigmet sigmet = new gov.noaa.nws.ncep.ui.pgen.file.Sigmet();

        for (Color clr : de.getColors()) {

            gov.noaa.nws.ncep.ui.pgen.file.Color fclr = new gov.noaa.nws.ncep.ui.pgen.file.Color();

            fclr.setRed(clr.getRed());
            fclr.setGreen(clr.getGreen());
            fclr.setBlue(clr.getBlue());
            fclr.setAlpha(clr.getAlpha());

            sigmet.getColor().add(fclr);
        }

        for (Coordinate crd : ((Sigmet) de).getLinePoints()) {

            Point fpt = new Point();
            fpt.setLat(crd.y);
            fpt.setLon(crd.x);

            sigmet.getPoint().add(fpt);
        }

        sigmet.setPgenCategory(de.getPgenCategory());
        sigmet.setLineWidth(de.getLineWidth());
        sigmet.setSizeScale(de.getSizeScale());
        sigmet.setSmoothFactor(((Sigmet) de).getSmoothFactor());
        sigmet.setClosed(((Sigmet) de).isClosedLine());
        sigmet.setFilled(((Sigmet) de).isFilled());
        sigmet.setPgenType(de.getPgenType());
        sigmet.setFillPattern(((Sigmet) de).getFillPattern().name());

        sigmet.setType(((Sigmet) de).getType());
        sigmet.setWidth(((Sigmet) de).getWidth());

        sigmet.setEditableAttrArea(((Sigmet) de).getEditableAttrArea());
        sigmet.setEditableAttrIssueOffice(
                ((Sigmet) de).getEditableAttrIssueOffice());
        sigmet.setEditableAttrStatus(((Sigmet) de).getEditableAttrStatus());
        sigmet.setEditableAttrId(((Sigmet) de).getEditableAttrId());
        sigmet.setEditableAttrSeqNum(((Sigmet) de).getEditableAttrSeqNum());
        sigmet.setEditableAttrStartTime(
                ((Sigmet) de).getEditableAttrStartTime());
        sigmet.setEditableAttrEndTime(((Sigmet) de).getEditableAttrEndTime());
        sigmet.setEditableAttrRemarks(((Sigmet) de).getEditableAttrRemarks());
        sigmet.setEditableAttrPhenom(((Sigmet) de).getEditableAttrPhenom());
        sigmet.setEditableAttrPhenom2(((Sigmet) de).getEditableAttrPhenom2());
        sigmet.setEditableAttrPhenomName(
                ((Sigmet) de).getEditableAttrPhenomName());
        sigmet.setEditableAttrPhenomLat(
                ((Sigmet) de).getEditableAttrPhenomLat());
        sigmet.setEditableAttrPhenomLon(
                ((Sigmet) de).getEditableAttrPhenomLon());
        sigmet.setEditableAttrPhenomPressure(
                ((Sigmet) de).getEditableAttrPhenomPressure());
        sigmet.setEditableAttrPhenomMaxWind(
                ((Sigmet) de).getEditableAttrPhenomMaxWind());
        sigmet.setEditableAttrFreeText(((Sigmet) de).getEditableAttrFreeText());
        sigmet.setEditableAttrTrend(((Sigmet) de).getEditableAttrTrend());
        sigmet.setEditableAttrMovement(((Sigmet) de).getEditableAttrMovement());
        sigmet.setEditableAttrPhenomSpeed(
                ((Sigmet) de).getEditableAttrPhenomSpeed());
        sigmet.setEditableAttrPhenomDirection(
                ((Sigmet) de).getEditableAttrPhenomDirection());
        sigmet.setEditableAttrLevel(((Sigmet) de).getEditableAttrLevel());
        sigmet.setEditableAttrLevelInfo1(
                ((Sigmet) de).getEditableAttrLevelInfo1());
        sigmet.setEditableAttrLevelInfo2(
                ((Sigmet) de).getEditableAttrLevelInfo2());
        sigmet.setEditableAttrLevelText1(
                ((Sigmet) de).getEditableAttrLevelText1());
        sigmet.setEditableAttrLevelText2(
                ((Sigmet) de).getEditableAttrLevelText2());
        sigmet.setEditableAttrFromLine(((Sigmet) de).getEditableAttrFromLine());
        sigmet.setEditableAttrFir(((Sigmet) de).getEditableAttrFir());
        sigmet.setEditableAttrCarSamBackupMode(((Sigmet) de).getEditableAttrCarSamBackupMode());

        fde.getSigmet().add(sigmet);
    }

    /*
     * Convert in-memory PGEN GFA DrawableElement object into a list held in an
     * XML file DrawableElement fde
     */
    private static void convertLODLineGFA(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement fde,
            DrawableElement de) {
        gov.noaa.nws.ncep.ui.pgen.file.Gfa fgfa = new gov.noaa.nws.ncep.ui.pgen.file.Gfa();

        for (Color clr : de.getColors()) {

            gov.noaa.nws.ncep.ui.pgen.file.Color fclr = new gov.noaa.nws.ncep.ui.pgen.file.Color();

            fclr.setRed(clr.getRed());
            fclr.setGreen(clr.getGreen());
            fclr.setBlue(clr.getBlue());
            fclr.setAlpha(clr.getAlpha());

            fgfa.getColor().add(fclr);
        }

        for (Coordinate crd : ((Gfa) de).getLinePoints()) {

            Point fpt = new Point();
            fpt.setLat(crd.y);
            fpt.setLon(crd.x);

            fgfa.getPoint().add(fpt);
        }

        fgfa.setPgenCategory(de.getPgenCategory());
        fgfa.setLineWidth(de.getLineWidth());
        fgfa.setSizeScale(de.getSizeScale());
        fgfa.setSmoothFactor(((Gfa) de).getSmoothFactor());
        fgfa.setClosed(((Gfa) de).isClosedLine());
        fgfa.setFilled(((Gfa) de).isFilled());
        fgfa.setPgenType(de.getPgenType());
        Gfa e = (Gfa) de;
        if (e.getGfaTextCoordinate() != null) {
            fgfa.setLatText(e.getGfaTextCoordinate().y);
            fgfa.setLonText(e.getGfaTextCoordinate().x);
        }
        fgfa.setHazard(nvl(e.getGfaHazard()));
        fgfa.setFcstHr(nvl(e.getGfaFcstHr()));
        fgfa.setTag(nvl(e.getGfaTag()));
        fgfa.setDesk(nvl(e.getGfaDesk()));
        fgfa.setIssueType(nvl(e.getGfaIssueType()));
        fgfa.setCycleDay(e.getGfaCycleDay());
        fgfa.setCycleHour(e.getGfaCycleHour());
        fgfa.setType(nvl(e.getGfaType()));
        fgfa.setArea(nvl(e.getGfaArea()));
        fgfa.setBeginning(nvl(e.getGfaBeginning()));
        fgfa.setEnding(nvl(e.getGfaEnding()));
        fgfa.setStates(nvl(e.getGfaStates()));
        fgfa.setGr(nvl(e.getGfaValue(Gfa.GR)));
        fgfa.setFrequency(nvl((e.getGfaValue(Gfa.FREQUENCY))));
        fgfa.setTsCategory(nvl(e.getGfaValue(Gfa.CATEGORY)));
        fgfa.setFzlRange(nvl(e.getGfaValue(Gfa.FZL_RANGE)));
        fgfa.setLevel(nvl(e.getGfaValue(Gfa.LEVEL)));
        fgfa.setIntensity(nvl(e.getGfaValue(Gfa.INTENSITY)));
        fgfa.setSpeed(nvl(e.getGfaValue(Gfa.SPEED)));
        fgfa.setDueTo(nvl(e.getGfaValue(Gfa.DUE_TO)));
        fgfa.setLyr(nvl(e.getGfaValue(Gfa.LYR)));
        fgfa.setCoverage(nvl(e.getGfaValue(Gfa.COVERAGE)));
        fgfa.setBottom(nvl(e.getGfaBottom()));
        fgfa.setTop(nvl(e.getGfaTop()));
        fgfa.setFzlTopBottom(nvl(e.getGfaValue(Gfa.FZL_TOP_BOTTOM)));
        fgfa.setContour(nvl(e.getGfaValue(Gfa.CONTOUR)));
        fgfa.setIsOutlook(e.isOutlook());
        if ("ICE".equals(e.getGfaHazard())) {
            fgfa.setType(nvl(e.getGfaValue("Type")));
        }
        Calendar cal = e.getAttribute(Gfa.ISSUE_TIME, Calendar.class);
        SimpleDateFormat sdf = new SimpleDateFormat("ddHHmm");
        if (cal != null) {
            fgfa.setIssueTime(sdf.format(cal.getTime()));
        }
        cal = e.getAttribute(Gfa.UNTIL_TIME, Calendar.class);
        if (cal != null) {
            fgfa.setUntilTime(sdf.format(cal.getTime()));
        }
        if (e.getAttribute(GfaRules.WORDING) != null) {
            GfaWording w = e.getAttribute(GfaRules.WORDING, GfaWording.class);
            fgfa.setFromCondsDvlpg(GfaRules.replacePlusWithCycle(
                    w.getFromCondsDvlpg(), e.getGfaCycleHour()));
            fgfa.setFromCondsEndg(GfaRules.replacePlusWithCycle(
                    w.getFromCondsEndg(), e.getGfaCycleHour()));
            fgfa.setCondsContg(GfaRules.replacePlusWithCycle(w.getCondsContg(),
                    e.getGfaCycleHour()));
            fgfa.setOtlkCondsDvlpg(GfaRules.replacePlusWithCycle(
                    w.getOtlkCondsDvlpg(), e.getGfaCycleHour()));
            fgfa.setOtlkCondsEndg(GfaRules.replacePlusWithCycle(
                    w.getOtlkCondsEndg(), e.getGfaCycleHour()));
        }
        // textVOR
        ArrayList<Coordinate> pts = e.getPoints();
        pts = SnapUtil.getSnapWithStation(pts, SnapUtil.VOR_STATION_LIST, 10,
                16, false);
        Coordinate[] a = new Coordinate[pts.size()];
        a = pts.toArray(a);
        String s = "";
        if (fgfa.getHazard().equalsIgnoreCase("FZLVL")) {
            if (fgfa.isClosed()) {
                s = SnapUtil.getVORText(a, "-", "Area", -1, true, false, true);
            } else {
                s = SnapUtil.getVORText(a, "-", "Line", -1, true, false, true);
            }
        } else if (fgfa.getHazard().equalsIgnoreCase("LLWS")) {
            s = SnapUtil.getVORText(a, "-", "Area", -1, true, false, true);
        } else {
            s = SnapUtil.getVORText(a, " TO ", "Area", -1, true, false, true);
        }
        fgfa.setTextVor(s);

        fgfa.setFillPattern(nvl(((Gfa) de).getFillPattern().name()));

        fde.getGfa().add(fgfa);
    }

    /*
     * Convert in-memory PGEN Arc DrawableElement object into a list held in an
     * XML file DrawableElement fde
     */
    private static void convertLODLineArc(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement fde,
            DrawableElement de) {
        gov.noaa.nws.ncep.ui.pgen.file.Arc arc = new gov.noaa.nws.ncep.ui.pgen.file.Arc();

        for (Color clr : de.getColors()) {

            gov.noaa.nws.ncep.ui.pgen.file.Color fclr = new gov.noaa.nws.ncep.ui.pgen.file.Color();

            fclr.setRed(clr.getRed());
            fclr.setGreen(clr.getGreen());
            fclr.setBlue(clr.getBlue());
            fclr.setAlpha(clr.getAlpha());

            arc.getColor().add(fclr);
        }

        for (Coordinate crd : ((Arc) de).getLinePoints()) {

            Point fpt = new Point();
            fpt.setLat(crd.y);
            fpt.setLon(crd.x);

            arc.getPoint().add(fpt);
        }

        arc.setPgenCategory(de.getPgenCategory());
        arc.setLineWidth(de.getLineWidth());
        arc.setSizeScale(de.getSizeScale());
        arc.setSmoothFactor(((Arc) de).getSmoothFactor());
        arc.setClosed(((Arc) de).isClosedLine());
        arc.setFilled(((Arc) de).isFilled());
        arc.setPgenType(de.getPgenType());

        arc.setFillPattern(((Arc) de).getFillPattern().name());
        arc.setAxisRatio(((Arc) de).getAxisRatio());
        arc.setStartAngle(((Arc) de).getStartAngle());
        arc.setEndAngle(((Arc) de).getEndAngle());

        fde.getArc().add(arc);
    }

    /**
     * Convert a DECollection to a format that can be used by jaxb and saved to
     * XML files
     * 
     * @param dec
     * @return
     */
    private static gov.noaa.nws.ncep.ui.pgen.file.DECollection convertDECollection2XML(
            DECollection dec) {

        gov.noaa.nws.ncep.ui.pgen.file.DECollection fdec = new gov.noaa.nws.ncep.ui.pgen.file.DECollection();

        String cName = dec.getCollectionName();
        if (cName != null) {
            fdec.setCollectionName(cName);
        }

        fdec.setPgenType(dec.getPgenType());
        fdec.setPgenCategory(dec.getPgenCategory());

        List<AbstractDrawableComponent> componentList = new ArrayList<AbstractDrawableComponent>();
        Iterator<AbstractDrawableComponent> it = dec.getComponentIterator();

        while (it.hasNext()) {
            componentList.add(it.next());
        }

        fdec.setDrawableElement(convertListOfDEs(componentList));

        return fdec;

    }

    /**
     * Convert a DECollection in an XML file to a jet element
     * 
     * @param dec
     * @return
     */
    private static Jet convertXML2Jet(
            gov.noaa.nws.ncep.ui.pgen.file.DECollection dec) {
        Jet jet = new Jet();
        jet.setPgenCategory(dec.getPgenCategory());
        jet.setPgenType(dec.getPgenType());

        jet.remove(jet.getJetLine());
        gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem = dec
                .getDrawableElement();

        if (elem.getLine() != null) {
            jet.add(convertJetLine(elem.getLine().get(0), jet));
        } else
            return null;

        if (elem.getVector() != null) {
            for (gov.noaa.nws.ncep.ui.pgen.file.Vector fVector : elem
                    .getVector()) {
                jet.add(convertJetHash(fVector, jet));
            }
        }

        if (elem.getDECollection() != null) {
            for (gov.noaa.nws.ncep.ui.pgen.file.DECollection fdec : elem
                    .getDECollection()) {
                if (fdec.getCollectionName().equalsIgnoreCase("WindInfo")) {
                    DECollection wind = new DECollection("WindInfo");

                    gov.noaa.nws.ncep.ui.pgen.file.DrawableElement de = fdec
                            .getDrawableElement();
                    if (de.getVector() != null) {
                        wind.add(convertJetBarb(de.getVector().get(0), jet));
                    }

                    if (de.getText() != null) {
                        wind.add(convertJetFL(de.getText().get(0), jet));
                    }

                    jet.add(wind);

                }
            }
        }

        return jet;
    }

    /**
     * Convert a XML Line to a jet line
     * 
     * @return
     */
    private static JetLine convertJetLine(
            gov.noaa.nws.ncep.ui.pgen.file.Line jetLine, Jet jet) {

        Color[] clr = new Color[jetLine.getColor().size()];
        int nn = 0;
        for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : jetLine.getColor()) {
            clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                    fColor.getBlue(), fColor.getAlpha());
        }

        ArrayList<Coordinate> linePoints = new ArrayList<Coordinate>();
        nn = 0;
        for (Point pt : jetLine.getPoint()) {
            linePoints.add(new Coordinate(pt.getLon(), pt.getLat()));
        }

        JetLine newLine = jet.new JetLine(null, clr, jetLine.getLineWidth(),
                jetLine.getSizeScale(), jetLine.isClosed(), jetLine.isFilled(),
                linePoints, jetLine.getSmoothFactor(),
                FillPattern.valueOf(jetLine.getFillPattern()),
                jetLine.getPgenCategory(), jetLine.getPgenType());

        newLine.setParent(jet);

        return newLine;
    }

    /**
     * Convert a vector from XML to jet hash
     * 
     * @param fVector
     * @param jet
     * @return
     */
    private static JetHash convertJetHash(
            gov.noaa.nws.ncep.ui.pgen.file.Vector fVector, Jet jet) {
        Color[] clr = new Color[fVector.getColor().size()];
        int nn = 0;
        for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fVector.getColor()) {
            clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                    fColor.getBlue(), fColor.getAlpha());
        }

        Point loc = fVector.getPoint();

        VectorType vtype = null;
        String pgenType = fVector.getPgenType();

        if (pgenType.equalsIgnoreCase("Hash")) {
            vtype = VectorType.HASH_MARK;
        }
        JetHash hash = jet.new JetHash(null, clr, fVector.getLineWidth(),
                fVector.getSizeScale(), fVector.isClear(),
                (new Coordinate(loc.getLon(), loc.getLat())), vtype,
                fVector.getSpeed(), fVector.getDirection(),
                fVector.getArrowHeadSize(), fVector.isDirectionOnly(),
                fVector.getPgenCategory(), fVector.getPgenType());
        hash.setParent(jet);

        return hash;
    }

    /**
     * Convert a Vector from XML to jet barb
     * 
     * @param fVector
     * @param jet
     * @return
     */
    private static JetBarb convertJetBarb(
            gov.noaa.nws.ncep.ui.pgen.file.Vector fVector, Jet jet) {
        Color[] clr = new Color[fVector.getColor().size()];
        int nn = 0;
        for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fVector.getColor()) {
            clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                    fColor.getBlue(), fColor.getAlpha());
        }

        Point loc = fVector.getPoint();

        VectorType vtype = null;
        String pgenType = fVector.getPgenType();

        if (pgenType.equalsIgnoreCase("Barb")) {
            vtype = VectorType.WIND_BARB;
        }
        JetBarb barb = jet.new JetBarb(null, clr, fVector.getLineWidth(),
                fVector.getSizeScale(), fVector.isClear(),
                (new Coordinate(loc.getLon(), loc.getLat())), vtype,
                fVector.getSpeed(), fVector.getDirection(),
                fVector.getArrowHeadSize(), fVector.isDirectionOnly(),
                fVector.getPgenCategory(), fVector.getPgenType());
        barb.setParent(jet);
        return barb;
    }

    /**
     * Convert an XML text to a jet flight level text
     * 
     * @param fText
     * @return
     */
    private static JetText convertJetFL(
            gov.noaa.nws.ncep.ui.pgen.file.Text fText, Jet aJet) {
        Color[] clr = new Color[fText.getColor().size()];
        int nn = 0;
        for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fText.getColor()) {
            clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                    fColor.getBlue(), fColor.getAlpha());
        }

        Point loc = fText.getPoint();

        String[] st = new String[fText.getTextLine().size()];

        int nline = 0;
        for (String str : fText.getTextLine()) {
            st[nline++] = str;
        }

        JetText text = aJet.new JetText((Coordinate[]) null,
                fText.getFontName(), fText.getFontSize(),
                TextJustification.valueOf(fText.getJustification()),
                (new Coordinate(loc.getLon(), loc.getLat())),
                fText.getRotation(),
                TextRotation.valueOf(fText.getRotationRelativity()), st,
                FontStyle.valueOf(fText.getStyle()), clr[0], 0, 0,
                fText.isMask(), DisplayType.valueOf(fText.getDisplayType()),
                fText.getPgenCategory(), fText.getPgenType());

        text.setLatLonFlag(true);

        return text;
    }

    /**
     * Convert a Contours object to an XML Contours object.
     * 
     * @param cnt
     * @return
     */
    private static gov.noaa.nws.ncep.ui.pgen.file.Contours convertContours2XML(
            Contours cnt) {

        gov.noaa.nws.ncep.ui.pgen.file.Contours contours = new gov.noaa.nws.ncep.ui.pgen.file.Contours();

        contours.setCollectionName("Contours");
        contours.setPgenType("Contours");
        contours.setPgenCategory("MET");

        contours.setParm(cnt.getParm());
        contours.setLevel(cnt.getLevel());
        contours.setCint(cnt.getCint());
        if (cnt.getForecastHour() != null) {
            contours.setForecastHour(cnt.getForecastHour());
        }

        Calendar cntTime = cnt.getTime1();
        XMLGregorianCalendar xmlCal = null;
        try {
            xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    cntTime.get(Calendar.YEAR), cntTime.get(Calendar.MONTH) + 1,
                    cntTime.get(Calendar.DAY_OF_MONTH),
                    cntTime.get(Calendar.HOUR_OF_DAY),
                    cntTime.get(Calendar.MINUTE), cntTime.get(Calendar.SECOND),
                    cntTime.get(Calendar.MILLISECOND), 0);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        contours.setTime1(xmlCal);

        Calendar cntTime2 = cnt.getTime2();
        XMLGregorianCalendar xmlCal2 = null;
        try {
            xmlCal2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    cntTime2.get(Calendar.YEAR),
                    cntTime2.get(Calendar.MONTH) + 1,
                    cntTime2.get(Calendar.DAY_OF_MONTH),
                    cntTime2.get(Calendar.HOUR_OF_DAY),
                    cntTime2.get(Calendar.MINUTE),
                    cntTime2.get(Calendar.SECOND),
                    cntTime2.get(Calendar.MILLISECOND), 0);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        contours.setTime2(xmlCal2);

        Iterator<AbstractDrawableComponent> it = cnt.getComponentIterator();
        while (it.hasNext()) {

            AbstractDrawableComponent next = it.next();

            if (next instanceof DECollection) {
                contours.getDECollection()
                        .add(convertDECollection2XML((DECollection) next));
            }

        }

        return contours;

    }

    /**
     * Convert a JAXB XML Contours object to a PGEN Contours object.
     * 
     * @param cnt
     * @return
     */
    private static Contours convertXML2Contours(
            gov.noaa.nws.ncep.ui.pgen.file.Contours cnt) {

        Contours contours = new Contours("Contours");

        contours.setPgenType(cnt.getPgenType());
        contours.setPgenCategory(cnt.getPgenCategory());

        contours.setParm(cnt.getParm());
        contours.setLevel(cnt.getLevel());
        contours.setCint(cnt.getCint());

        if (cnt.getForecastHour() != null) {
            contours.setForecastHour(cnt.getForecastHour());
        }

        Calendar cntTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        XMLGregorianCalendar xmlCal = cnt.getTime1();
        if (xmlCal != null)
            cntTime.set(xmlCal.getYear(), xmlCal.getMonth() - 1,
                    xmlCal.getDay(), xmlCal.getHour(), xmlCal.getMinute(),
                    xmlCal.getSecond());
        contours.setTime1(cntTime);

        Calendar cntTime2 = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        XMLGregorianCalendar xmlCal2 = cnt.getTime2();
        if (xmlCal2 != null)
            cntTime2.set(xmlCal2.getYear(), xmlCal2.getMonth() - 1,
                    xmlCal2.getDay(), xmlCal2.getHour(), xmlCal2.getMinute(),
                    xmlCal2.getSecond());
        contours.setTime2(cntTime2);

        for (gov.noaa.nws.ncep.ui.pgen.file.DECollection fdec : cnt
                .getDECollection()) {

            if (fdec.getCollectionName().equals("ContourLine")) {
                ContourLine contourLine = new ContourLine();

                List<AbstractDrawableComponent> delist = convert(
                        fdec.getDrawableElement());
                String[] labelString = null;
                int numOfLabels = 0;

                for (AbstractDrawableComponent de : delist) {

                    de.setParent(contourLine);
                    contourLine.add(de);

                    if (de instanceof Text) {
                        numOfLabels++;

                        if (labelString == null) {
                            labelString = ((Text) de).getText();
                        }
                    }

                }

                // Set the number of labels and label strings
                contourLine.setNumOfLabels(numOfLabels);
                contourLine.setLabelString(labelString);

                contourLine.setParent(contours);

                contours.add(contourLine);
            } else if (fdec.getCollectionName().equals("ContourMinmax")) {

                ContourMinmax contourMinmax = new ContourMinmax();

                List<AbstractDrawableComponent> delist = convert(
                        fdec.getDrawableElement());
                for (AbstractDrawableComponent de : delist) {

                    de.setParent(contourMinmax);
                    contourMinmax.add(de);

                }

                contourMinmax.setParent(contours);

                contours.add(contourMinmax);
            } else if (fdec.getCollectionName().equals("ContourCircle")) {

                ContourCircle contourCircle = new ContourCircle();

                List<AbstractDrawableComponent> delist = convert(
                        fdec.getDrawableElement());
                for (AbstractDrawableComponent de : delist) {

                    de.setParent(contourCircle);
                    contourCircle.add(de);

                }

                contourCircle.setParent(contours);

                contours.add(contourCircle);
            }
        }

        return contours;

    }

    /**
     * Convert a WatchBox element to file
     * 
     * @param wb
     * @return
     */
    private static gov.noaa.nws.ncep.ui.pgen.file.WatchBox convertWatchBox2XML(
            WatchBox wb) {
        gov.noaa.nws.ncep.ui.pgen.file.WatchBox fwb = new gov.noaa.nws.ncep.ui.pgen.file.WatchBox();

        fwb.setPgenCategory(wb.getPgenCategory());
        fwb.setPgenType(wb.getPgenType());
        fwb.setBoxShape(wb.getBoxShape());
        fwb.setFillFlag(wb.getFillFlag());
        fwb.setSymbolSize(wb.getWatchSymbolSize());
        fwb.setSymbolWidth(wb.getWatchSymbolWidth());
        fwb.setSymbolType(wb.getWatchSymbolType());

        // issue info
        fwb.setIssueStatus(wb.getIssueStatus());

        Calendar issueTime = wb.getIssueTime();
        if (issueTime != null) {
            XMLGregorianCalendar xmlIssueCal = null;
            try {
                xmlIssueCal = DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(issueTime.get(Calendar.YEAR),
                                issueTime.get(Calendar.MONTH) + 1,
                                issueTime.get(Calendar.DAY_OF_MONTH),
                                issueTime.get(Calendar.HOUR_OF_DAY),
                                issueTime.get(Calendar.MINUTE),
                                issueTime.get(Calendar.SECOND),
                                issueTime.get(Calendar.MILLISECOND), 0);
            } catch (DatatypeConfigurationException e) {
                e.printStackTrace();
            }
            fwb.setIssueTime(xmlIssueCal);
        }

        Calendar expTime = wb.getExpTime();
        if (expTime != null) {
            XMLGregorianCalendar xmlExpCal = null;
            try {
                xmlExpCal = DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(expTime.get(Calendar.YEAR),
                                expTime.get(Calendar.MONTH) + 1,
                                expTime.get(Calendar.DAY_OF_MONTH),
                                expTime.get(Calendar.HOUR_OF_DAY),
                                expTime.get(Calendar.MINUTE),
                                expTime.get(Calendar.SECOND),
                                expTime.get(Calendar.MILLISECOND), 0);
            } catch (DatatypeConfigurationException e) {
                e.printStackTrace();
            }
            fwb.setExpTime(xmlExpCal);
        }

        fwb.setSeverity(wb.getSeverity());
        fwb.setTimeZone(wb.getTimeZone());
        fwb.setHailSize(wb.getHailSize());
        fwb.setGust(wb.getGust());
        fwb.setTop(wb.getTop());
        fwb.setMoveDir(wb.getMoveDir());
        fwb.setMoveSpeed(wb.getMoveSpeed());
        fwb.setAdjAreas(wb.getAdjAreas());
        fwb.setReplWatch(wb.getReplWatch());
        fwb.setContWatch(wb.getContWatch());
        fwb.setIssueFlag(wb.getIssueFlag());
        fwb.setWatchType(wb.getWatchType());
        fwb.setForecaster(wb.getForecaster());
        fwb.setWatchNumber(wb.getWatchNumber());
        fwb.setEndPointAnc(wb.getEndPointAnc());
        fwb.setEndPointVor(wb.getEndPointVor());
        fwb.setHalfWidthNm(wb.getHalfWidthNm());
        fwb.setHalfWidthSm(wb.getHalfWidthSm());
        fwb.setWatchAreaNm(wb.getWathcAreaNm());

        // set color
        for (Color clr : wb.getColors()) {

            gov.noaa.nws.ncep.ui.pgen.file.Color fclr = new gov.noaa.nws.ncep.ui.pgen.file.Color();

            fclr.setRed(clr.getRed());
            fclr.setGreen(clr.getGreen());
            fclr.setBlue(clr.getBlue());
            fclr.setAlpha(clr.getAlpha());
            fwb.getColor().add(fclr);
        }

        // set fill color
        Color fColor = wb.getFillColor();
        if (fColor != null) {
            gov.noaa.nws.ncep.ui.pgen.file.Color fill = new gov.noaa.nws.ncep.ui.pgen.file.Color();
            fill.setRed(fColor.getRed());
            fill.setGreen(fColor.getGreen());
            fill.setBlue(fColor.getBlue());
            fill.setAlpha(fColor.getAlpha());
            ColorType ct = new ColorType();
            ct.setColor(fill);
            fwb.setFillColor(ct);
        }

        // set points
        for (Coordinate crd : wb.getLinePoints()) {

            Point fpt = new Point();
            fpt.setLat(crd.y);
            fpt.setLon(crd.x);

            fwb.getPoint().add(fpt);
        }

        // set anchor points
        for (Station stn : wb.getAnchors()) {
            fwb.getAnchorPoints().add(stn.getStid() + " " + stn.getState() + " "
                    + stn.getStnname());
        }

        // set county list
        for (SPCCounty cnty : wb.getCountyList()) {

            String cntyName = "";
            if (cnty.getName() != null) {
                cntyName = cnty.getName().replaceAll("City of ", "")
                        .replaceAll(" City", "");
            }

            fwb.getCounties().add(String.format(
                    "%1$-7s%2$-5s%3$-12s%4$6.2f%5$8.2f%6$7s%7$5s %8$s",
                    cnty.getUgcId(), cnty.getState(), cntyName,
                    cnty.getCentriod().y, cnty.getCentriod().x, cnty.getFips(),
                    cnty.getWfo(), cnty.getZoneName().toUpperCase()));
        }

        // set states
        for (String str : wb.getStates()) {
            fwb.getStates().add(str);
        }

        // set wfos
        String wfoStr = "";
        for (String str : wb.getWFOs()) {
            wfoStr += str + "...";
        }
        fwb.setWfos(wfoStr);

        // status info
        if (wb.getStatusHistory() != null) {
            for (WatchStatus ws : wb.getStatusHistory()) {

                Status fws = new Status();

                fws.setFromLine(ws.getFromLine());
                fws.setStatusForecaster(ws.getStatusForecaster());
                fws.setMesoDiscussionNumber(ws.getDiscussion());

                Calendar statusValidTime = ws.getStatusValidTime();
                if (statusValidTime != null) {
                    XMLGregorianCalendar xmlCal = null;
                    try {
                        xmlCal = DatatypeFactory.newInstance()
                                .newXMLGregorianCalendar(
                                        statusValidTime.get(Calendar.YEAR),
                                        statusValidTime.get(Calendar.MONTH) + 1,
                                        statusValidTime
                                                .get(Calendar.DAY_OF_MONTH),
                                        statusValidTime
                                                .get(Calendar.HOUR_OF_DAY),
                                        statusValidTime.get(Calendar.MINUTE),
                                        statusValidTime.get(Calendar.SECOND),
                                        statusValidTime
                                                .get(Calendar.MILLISECOND),
                                        0);
                    } catch (DatatypeConfigurationException e) {
                        e.printStackTrace();
                    }
                    fws.setStatusValidTime(xmlCal);
                }

                Calendar statusExpTime = ws.getStatusValidTime();
                if (statusExpTime != null) {
                    XMLGregorianCalendar xmlCal = null;
                    try {
                        xmlCal = DatatypeFactory.newInstance()
                                .newXMLGregorianCalendar(
                                        statusExpTime.get(Calendar.YEAR),
                                        statusExpTime.get(Calendar.MONTH) + 1,
                                        statusExpTime
                                                .get(Calendar.DAY_OF_MONTH),
                                        statusExpTime.get(Calendar.HOUR_OF_DAY),
                                        statusExpTime.get(Calendar.MINUTE),
                                        statusExpTime.get(Calendar.SECOND),
                                        statusExpTime.get(Calendar.MILLISECOND),
                                        0);
                    } catch (DatatypeConfigurationException e) {
                        e.printStackTrace();
                    }
                    fws.setStatusExpTime(xmlCal);
                }

                fwb.getStatus().add(fws);

            }
        }

        // set outlines and holes
        Geometry union = wb.getCountyUnion();

        if (union != null) {
            // loop through all polygons in the union
            for (int ii = 0; ii < union.getNumGeometries(); ii++) {

                // set outlines
                Polygon poly = (Polygon) union.getGeometryN(ii);

                LineString outside = poly.getExteriorRing();

                Outline ol = new Outline();

                for (Coordinate pt : outside.getCoordinates()) {
                    Point fpt = new Point();
                    fpt.setLat(pt.y);
                    fpt.setLon(pt.x);
                    ol.getPoint().add(fpt);
                }

                fwb.getOutline().add(ol);

                // loop through all holes of the polygon
                for (int jj = 0; jj < poly.getNumInteriorRing(); jj++) {

                    LineString ls = poly.getInteriorRingN(jj);
                    Hole hole = new Hole();

                    for (Coordinate pt : ls.getCoordinates()) {
                        Point fpt = new Point();
                        fpt.setLat(pt.y);
                        fpt.setLon(pt.x);
                        hole.getPoint().add(fpt);
                    }

                    fwb.getHole().add(hole);

                }
            }
        }

        return fwb;
    }

    /**
     * Convert XML to WatchBox element
     * 
     * @param fwb
     * @return
     */
    private static WatchBox convertXML2WatchBox(
            gov.noaa.nws.ncep.ui.pgen.file.WatchBox fwb) {
        WatchBox wb = new WatchBox();

        wb.setPgenCategory(fwb.getPgenCategory());
        wb.setPgenType(fwb.getPgenType());
        wb.setWatchBoxShape(fwb.getBoxShape());
        wb.setFillFlag(fwb.isFillFlag());
        wb.setWatchSymbolSize(fwb.getSymbolSize());
        wb.setWatchSymbolWidth(fwb.getSymbolWidth());
        wb.setWatchSymbolType(fwb.getSymbolType());
        wb.setWatchType(fwb.getWatchType());

        // set issue info
        wb.setIssueStatus(fwb.getIssueStatus());

        Calendar issueTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        XMLGregorianCalendar xmlIssueCal = fwb.getIssueTime();
        if (xmlIssueCal != null) {
            issueTime.set(xmlIssueCal.getYear(), xmlIssueCal.getMonth() - 1,
                    xmlIssueCal.getDay(), xmlIssueCal.getHour(),
                    xmlIssueCal.getMinute(), xmlIssueCal.getSecond());
            wb.setIssueTime(issueTime);
        }

        Calendar expTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        XMLGregorianCalendar xmlExpCal = fwb.getExpTime();
        if (xmlExpCal != null) {
            expTime.set(xmlExpCal.getYear(), xmlExpCal.getMonth() - 1,
                    xmlExpCal.getDay(), xmlExpCal.getHour(),
                    xmlExpCal.getMinute(), xmlExpCal.getSecond());
            wb.setExpTime(expTime);
        }

        wb.setSeverity(fwb.getSeverity());
        wb.setTimeZone(fwb.getTimeZone());

        if (fwb.getHailSize() != null)
            wb.setHailSize(fwb.getHailSize());

        if (fwb.getGust() != null)
            wb.setGust(fwb.getGust());

        if (fwb.getTop() != null)
            wb.setTop(fwb.getTop());

        if (fwb.getMoveDir() != null)
            wb.setMoveDir(fwb.getMoveDir());

        if (fwb.getMoveSpeed() != null)
            wb.setMoveSpeed(fwb.getMoveSpeed());

        wb.setAdjAreas(fwb.getAdjAreas());

        if (fwb.getReplWatch() != null)
            wb.setReplWatch(fwb.getReplWatch());

        if (fwb.getContWatch() != null)
            wb.setContWatch(fwb.getContWatch());

        if (fwb.getIssueFlag() != null)
            wb.setIssueFlag(fwb.getIssueFlag());

        wb.setForecaster(fwb.getForecaster());

        if (fwb.getWatchNumber() != null)
            wb.setWatchNumber(fwb.getWatchNumber());

        wb.setEndPointAnc(fwb.getEndPointAnc());
        wb.setEndPointVor(fwb.getEndPointVor());

        if (fwb.getWatchAreaNm() != null)
            wb.setWathcAreaNm(fwb.getWatchAreaNm());

        if (fwb.getHalfWidthNm() != null)
            wb.setHalfWidthNm(fwb.getHalfWidthNm());

        if (fwb.getHalfWidthSm() != null)
            wb.setHalfWidthSm(fwb.getHalfWidthSm());

        // set color

        Color[] clr = new Color[fwb.getColor().size()];
        int nn = 0;
        for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fwb.getColor()) {
            clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                    fColor.getBlue(), fColor.getAlpha());
        }
        wb.setColors(clr);

        // set points
        ArrayList<Coordinate> linePoints = new ArrayList<Coordinate>();
        nn = 0;
        for (Point pt : fwb.getPoint()) {
            linePoints.add(new Coordinate(pt.getLon(), pt.getLat()));
        }
        wb.setPointsOnly(linePoints);

        // set fill color
        if (fwb.getFillColor() != null) {
            gov.noaa.nws.ncep.ui.pgen.file.Color fColor = fwb.getFillColor()
                    .getColor();
            if (fColor != null) {
                Color fill = new Color(fColor.getRed(), fColor.getGreen(),
                        fColor.getBlue(), fColor.getAlpha());
                wb.setFillColor(fill);
            }
        }

        /*
         * modified for standalone
         */
        String fileDir = ProductConverter.class.getProtectionDomain()
                .getCodeSource().getLocation().toString();
        // file:/usr1/qzhou/to11dr3/workspace/gov.noaa.nws.ncep.pgen/bin/
        // file:/usr1/qzhou/R1G1-6/workspace/gov.noaa.nws.ncep.standalone/
        // file:/usr1/qzhou/R1G1-6/workspace/gov.noaa.nws.ncep.standalone/distXC/xmlConverter.jar
        if (fileDir.endsWith(".jar"))
            fileDir = fileDir.substring(5, (fileDir.lastIndexOf("/") + 1));
        else
            fileDir = fileDir.substring(5);

        // set anchor points
        Station anchors[] = new Station[fwb.getAnchorPoints().size()];
        nn = 0;
        for (String str : fwb.getAnchorPoints()) {
            if (!str.equalsIgnoreCase(""))
                anchors[nn++] = (new StationTable(
                        fileDir + "table/spcwatch.xml")).getStation(
                                StationField.STID, str.substring(0, 3));
        }
        wb.setAnchors(anchors[0], anchors[1]);

        // set county list
        Station county[] = new Station[fwb.getCounties().size()];
        int n = 0;

        for (String str : fwb.getCounties()) {
            // str = MIC147 MI St. Clair 42.94 -82.68 26147 DTX
            String[] substr = str.split("\\s+");
            if (substr.length > 1)
                str = substr[5];

            county[n] = (new StationTable(fileDir + "table/mzcntys.xml"))
                    .getStation(StationField.STNM, str);
            if (county[n] != null) {
                // convert Staton to County
                SPCCounty wbCounty = new SPCCounty(county[n].getStnnum(), // getFips(),
                        county[n].getStnname(), county[n].getWfo(), "", // county[n].getStid(),
                                                                        // //ugcId,
                        county[n].getState(), county[n].getCountry(), "", // zoneName(),,
                        new Coordinate(county[n].getLongitude(),
                                county[n].getLatitude()), // centroid
                        null, // shape
                        false // marineZone
                );

                wb.addCounty(wbCounty);
            }
            n++;
        }

        // status info
        for (Status fws : fwb.getStatus()) {

            Calendar statusValidTime = Calendar
                    .getInstance(TimeZone.getTimeZone("GMT"));
            XMLGregorianCalendar xmlStatusValidCal = fws.getStatusValidTime();
            if (xmlStatusValidCal != null) {
                statusValidTime.set(xmlStatusValidCal.getYear(),
                        xmlStatusValidCal.getMonth() - 1,
                        xmlStatusValidCal.getDay(), xmlStatusValidCal.getHour(),
                        xmlStatusValidCal.getMinute(),
                        xmlStatusValidCal.getSecond());
            }

            Calendar statusExpTime = Calendar
                    .getInstance(TimeZone.getTimeZone("GMT"));
            XMLGregorianCalendar xmlStatusExpCal = fws.getStatusExpTime();
            if (xmlStatusExpCal != null) {
                statusExpTime.set(xmlStatusExpCal.getYear(),
                        xmlStatusExpCal.getMonth() - 1,
                        xmlStatusExpCal.getDay(), xmlStatusExpCal.getHour(),
                        xmlStatusExpCal.getMinute(),
                        xmlStatusExpCal.getSecond());
            }

            wb.addStatus(fws.getFromLine(), fws.getMesoDiscussionNumber(),
                    statusValidTime, statusExpTime, fws.getStatusForecaster());

        }

        return wb;
    }

    /**
     * Convert an Outlook object to an XML Outlook object.
     * 
     * @param cnt
     * @return
     */
    private static gov.noaa.nws.ncep.ui.pgen.file.Outlook convertOutlook2XML(
            Outlook otlk) {

        gov.noaa.nws.ncep.ui.pgen.file.Outlook fotlk = new gov.noaa.nws.ncep.ui.pgen.file.Outlook();

        fotlk.setDays(otlk.getDays());
        fotlk.setForecaster(otlk.getForecaster());
        fotlk.setName(otlk.getName());
        fotlk.setPgenType(otlk.getPgenType());
        fotlk.setPgenCategory(otlk.getPgenCategory());
        fotlk.setOutlookType(otlk.getOutlookType());
        fotlk.setLineInfo(otlk.getLineInfo());

        XMLGregorianCalendar xmlCal = null;

        if (otlk.getParm() != null) {
            fotlk.setParm(otlk.getParm());
            fotlk.setLevel(otlk.getLevel());
            fotlk.setCint(otlk.getCint());

            Calendar cntTime = otlk.getTime1();

            try {
                xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(
                        cntTime.get(Calendar.YEAR),
                        cntTime.get(Calendar.MONTH) + 1,
                        cntTime.get(Calendar.DAY_OF_MONTH),
                        cntTime.get(Calendar.HOUR_OF_DAY),
                        cntTime.get(Calendar.MINUTE),
                        cntTime.get(Calendar.SECOND),
                        cntTime.get(Calendar.MILLISECOND), 0);
            } catch (DatatypeConfigurationException e) {
                e.printStackTrace();
            }

            fotlk.setTime(xmlCal);

        }

        Calendar issueTime = otlk.getIssueTime();
        if (issueTime != null) {
            xmlCal = null;
            try {
                xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(
                        issueTime.get(Calendar.YEAR),
                        issueTime.get(Calendar.MONTH) + 1,
                        issueTime.get(Calendar.DAY_OF_MONTH),
                        issueTime.get(Calendar.HOUR_OF_DAY),
                        issueTime.get(Calendar.MINUTE),
                        issueTime.get(Calendar.SECOND), 0, 0);
            } catch (DatatypeConfigurationException e) {
                e.printStackTrace();
            }
            fotlk.setIssueTime(xmlCal);
        }

        Calendar expTime = otlk.getExpirationTime();
        if (expTime != null) {
            xmlCal = null;
            try {
                xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(
                        expTime.get(Calendar.YEAR),
                        expTime.get(Calendar.MONTH) + 1,
                        expTime.get(Calendar.DAY_OF_MONTH),
                        expTime.get(Calendar.HOUR_OF_DAY),
                        expTime.get(Calendar.MINUTE),
                        expTime.get(Calendar.SECOND), 0, 0);
            } catch (DatatypeConfigurationException e) {
                e.printStackTrace();
            }
            fotlk.setExpTime(xmlCal);
        }

        Iterator<AbstractDrawableComponent> it = otlk.getComponentIterator();
        while (it.hasNext()) {

            AbstractDrawableComponent next = it.next();

            if (next instanceof DECollection) {
                fotlk.getDECollection()
                        .add(convertDECollection2XML((DECollection) next));
            }

        }

        return fotlk;

    }

    /**
     * Convert a JAXB XML Outlook object to a PGEN Outlook object.
     * 
     * @param cnt
     * @return
     */
    private static Outlook convertXML2Outlook(
            gov.noaa.nws.ncep.ui.pgen.file.Outlook fotlk) {

        DrawableElementFactory def = new DrawableElementFactory();
        Outlook otlk = def.createOutlook(null, null, null, null);

        otlk.setPgenType(fotlk.getPgenType());
        otlk.setPgenCategory(fotlk.getPgenCategory());
        otlk.setDays(fotlk.getDays());
        otlk.setForecaster(fotlk.getForecaster());
        otlk.setOutlookType(fotlk.getOutlookType());
        otlk.setLineInfo(fotlk.getLineInfo());

        if (fotlk.getParm() != null) {
            otlk.setParm(fotlk.getParm());
            otlk.setLevel(fotlk.getLevel());
            otlk.setCint(fotlk.getCint());

            Calendar cntTime = Calendar
                    .getInstance(TimeZone.getTimeZone("GMT"));
            XMLGregorianCalendar xmlCal = fotlk.getTime();
            cntTime.set(xmlCal.getYear(), xmlCal.getMonth() - 1,
                    xmlCal.getDay(), xmlCal.getHour(), xmlCal.getMinute(),
                    xmlCal.getSecond());
            otlk.setTime1(cntTime);
        }

        Calendar issueTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        issueTime.set(Calendar.MILLISECOND, 0);
        XMLGregorianCalendar xmlIssueCal = fotlk.getIssueTime();
        if (xmlIssueCal != null) {
            issueTime.set(xmlIssueCal.getYear(), xmlIssueCal.getMonth() - 1,
                    xmlIssueCal.getDay(), xmlIssueCal.getHour(),
                    xmlIssueCal.getMinute(), xmlIssueCal.getSecond());
            otlk.setIssueTime(issueTime);
        }

        Calendar expTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        issueTime.set(Calendar.MILLISECOND, 0);

        XMLGregorianCalendar xmlExpCal = fotlk.getExpTime();
        if (xmlExpCal != null) {
            expTime.set(xmlExpCal.getYear(), xmlExpCal.getMonth() - 1,
                    xmlExpCal.getDay(), xmlExpCal.getHour(),
                    xmlExpCal.getMinute(), xmlExpCal.getSecond());
            otlk.setExpirationTime(expTime);
        }

        for (gov.noaa.nws.ncep.ui.pgen.file.DECollection fdec : fotlk
                .getDECollection()) {
            // for non-grouped labeled lines
            if (fdec.getCollectionName()
                    .equalsIgnoreCase(Outlook.OUTLOOK_LABELED_LINE)) {
                DECollection dec = new DECollection(
                        Outlook.OUTLOOK_LABELED_LINE);
                List<AbstractDrawableComponent> delist = convert(
                        fdec.getDrawableElement());
                for (AbstractDrawableComponent de : delist) {
                    de.setParent(dec);
                    dec.add(de);
                }
                dec.setParent(otlk);
                otlk.add(dec);

            }
            // for grouped labeled lines
            else if (fdec.getCollectionName()
                    .equalsIgnoreCase(Outlook.OUTLOOK_LINE_GROUP)) {
                DECollection grp = new DECollection(Outlook.OUTLOOK_LINE_GROUP);
                // add all labeled lines
                for (gov.noaa.nws.ncep.ui.pgen.file.DECollection labeledLine : fdec
                        .getDrawableElement().getDECollection()) {
                    DECollection lblLine = new DECollection(
                            Outlook.OUTLOOK_LABELED_LINE);
                    List<AbstractDrawableComponent> des = convert(
                            labeledLine.getDrawableElement());
                    for (AbstractDrawableComponent de : des) {
                        de.setParent(lblLine);
                        lblLine.add(de);
                    }
                    lblLine.setParent(grp);
                    grp.add(lblLine);
                }
                grp.setParent(otlk);
                otlk.add(grp);
            }

        }

        return otlk;

    }

    // temp modification for standalone
    public static Properties load(File propsFile) {
        Properties props = new Properties();
        try {
            FileInputStream fis = new FileInputStream(propsFile);
            props.load(fis);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return props;
    }

    /**
     * Convert XML to Volcano element
     * 
     * public for saving elements with SaveMsgDlg and TEST/RESUME types
     * 
     * @param fVol
     * @return Volcano element
     */

    public static Volcano convertXML2Volcano(
            gov.noaa.nws.ncep.ui.pgen.file.Volcano fVol) {
        Volcano vol = new Volcano();

        vol.setPgenCategory(fVol.getPgenCategory());
        vol.setPgenType(fVol.getPgenType());
        vol.setSizeScale(fVol.getSizeScale());
        vol.setLineWidth(fVol.getLineWidth());
        vol.setClear(fVol.isClear());

        Color[] clr = new Color[fVol.getColor().size()];
        int nn = 0;
        for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fVol.getColor()) {
            clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                    fColor.getBlue(), fColor.getAlpha());
        }
        vol.setColors(clr);

        /*
         * Modified for standalone
         */
        boolean isNoneDrawable = false;

        ArrayList<Coordinate> volPoints = new ArrayList<Coordinate>();
        nn = 0;
        for (Point pt : fVol.getPoint()) {
            volPoints.add(new Coordinate(pt.getLon(), pt.getLat()));
        }
        if (!isNoneDrawable)
            vol.setPoints(volPoints);

        vol.setName(fVol.getName());
        vol.setNumber(fVol.getNumber());
        vol.setTxtLoc(fVol.getTxtLoc());
        vol.setArea(fVol.getArea());
        vol.setElev(fVol.getElev());

        vol.setOrigStnVAAC(fVol.getOrigStnVAAC());
        vol.setWmoId(fVol.getWmoId());
        vol.setHdrNum(fVol.getHdrNum());
        vol.setProduct(fVol.getProduct());
        vol.setYear(fVol.getYear());
        vol.setAdvNum(fVol.getAdvNum());
        vol.setCorr(fVol.getCorr());

        vol.setInfoSource(fVol.getInfoSource());
        vol.setAddInfoSource(fVol.getAddInfoSource());
        vol.setAviColorCode(fVol.getAviColorCode());
        vol.setErupDetails(fVol.getErupDetails());

        vol.setObsAshDate(fVol.getObsAshDate());
        vol.setObsAshTime(fVol.getObsAshTime());
        vol.setNil(fVol.getNil());

        vol.setObsFcstAshCloudInfo(fVol.getObsFcstAshCloudInfo());
        vol.setObsFcstAshCloudInfo6(fVol.getObsFcstAshCloudInfo6());
        vol.setObsFcstAshCloudInfo12(fVol.getObsFcstAshCloudInfo12());
        vol.setObsFcstAshCloudInfo18(fVol.getObsFcstAshCloudInfo18());

        vol.setRemarks(fVol.getRemarks());
        vol.setNextAdv(fVol.getNextAdv());
        vol.setForecasters(fVol.getForecasters());

        return vol;
    }

    /**
     * Convert Volcano element to XML file
     * 
     * public for saving elements with SaveMsgDlg and TEST/RESUME types
     * 
     * @param vol
     * @return XML file
     */

    public static gov.noaa.nws.ncep.ui.pgen.file.Volcano convertVolcano2XML(
            Volcano vol) {
        gov.noaa.nws.ncep.ui.pgen.file.Volcano fVol = new gov.noaa.nws.ncep.ui.pgen.file.Volcano();
        String vp = vol.getProduct() == null ? null : vol.getProduct().trim();
        boolean isNoneDrawable = Arrays
                .asList(VaaInfo.ProductInfo.getProduct(VaaInfo.LOCS[1]))
                .contains(vp);

        // set color
        for (Color clr : vol.getColors()) {

            gov.noaa.nws.ncep.ui.pgen.file.Color fclr = new gov.noaa.nws.ncep.ui.pgen.file.Color();

            fclr.setRed(clr.getRed());
            fclr.setGreen(clr.getGreen());
            fclr.setBlue(clr.getBlue());
            fclr.setAlpha(clr.getAlpha());
            fVol.getColor().add(fclr);
        }

        for (Coordinate crd : vol.getLinePoints()) {
            if (isNoneDrawable)
                break;
            Point fpt = new Point();
            fpt.setLat(crd.y);
            fpt.setLon(crd.x);

            fVol.getPoint().add(fpt);
        }

        fVol.setPgenCategory(vol.getPgenCategory());
        fVol.setPgenType(vol.getPgenType());
        fVol.setSizeScale(vol.getSizeScale());
        fVol.setLineWidth(vol.getLineWidth());
        fVol.setClear(vol.isClear());

        fVol.setName(vol.getName());
        fVol.setNumber(vol.getNumber());
        fVol.setTxtLoc(vol.getTxtLoc());
        fVol.setArea(vol.getArea());
        fVol.setElev(vol.getElev());

        fVol.setOrigStnVAAC(vol.getOrigStnVAAC());
        fVol.setWmoId(vol.getWmoId());
        fVol.setHdrNum(vol.getHdrNum());
        fVol.setProduct(vol.getProduct());
        fVol.setYear(vol.getYear());
        fVol.setAdvNum(vol.getAdvNum());
        fVol.setCorr(vol.getCorr());

        fVol.setInfoSource(vol.getInfoSource());
        fVol.setAddInfoSource(vol.getAddInfoSource());
        fVol.setAviColorCode(vol.getAviColorCode());
        fVol.setErupDetails(vol.getErupDetails());

        fVol.setObsAshDate(vol.getObsAshDate());
        fVol.setObsAshTime(vol.getObsAshTime());
        fVol.setNil(vol.getNil());

        fVol.setObsFcstAshCloudInfo(vol.getObsFcstAshCloudInfo());
        fVol.setObsFcstAshCloudInfo6(vol.getObsFcstAshCloudInfo6());
        fVol.setObsFcstAshCloudInfo12(vol.getObsFcstAshCloudInfo12());
        fVol.setObsFcstAshCloudInfo18(vol.getObsFcstAshCloudInfo18());

        fVol.setRemarks(vol.getRemarks());
        fVol.setNextAdv(vol.getNextAdv());
        fVol.setForecasters(vol.getForecasters());

        return fVol;
    }

    private static String nvl(String str) {
        return "".equals(str) ? null : str;
    }

    /**
     * Convert a DECollection in an XML file to a labeled line element
     * 
     * @param dec
     * @return
     */
    private static LabeledLine convertXML2LabeledLine(
            gov.noaa.nws.ncep.ui.pgen.file.DECollection dec, LabeledLine ll) {
        if (ll != null) {
            ll.setPgenCategory(dec.getPgenCategory());
            ll.setPgenType(dec.getPgenType());

            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement elem = dec
                    .getDrawableElement();

            if (elem.getDECollection() != null) {
                // convert labels
                for (gov.noaa.nws.ncep.ui.pgen.file.DECollection lblDec : elem
                        .getDECollection()) {
                    if (lblDec.getCollectionName().equalsIgnoreCase("Label")) {
                        Label lbl = new Label();
                        lbl.setParent(ll);

                        gov.noaa.nws.ncep.ui.pgen.file.DrawableElement lblDe = lblDec
                                .getDrawableElement();

                        if (lblDe.getLine() != null) {
                            // convert arrow lines
                            for (gov.noaa.nws.ncep.ui.pgen.file.Line arrowLine : lblDe
                                    .getLine()) {

                                // get colors
                                Color[] clr = new Color[arrowLine.getColor()
                                        .size()];
                                int nn = 0;
                                for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : arrowLine
                                        .getColor()) {
                                    clr[nn++] = new Color(fColor.getRed(),
                                            fColor.getGreen(), fColor.getBlue(),
                                            fColor.getAlpha());
                                }

                                ArrayList<Coordinate> linePoints = new ArrayList<Coordinate>();
                                nn = 0;
                                for (Point pt : arrowLine.getPoint()) {
                                    linePoints.add(new Coordinate(pt.getLon(),
                                            pt.getLat()));
                                }

                                Line line = new Line(null, clr,
                                        arrowLine.getLineWidth(),
                                        arrowLine.getSizeScale(),
                                        arrowLine.isClosed(),
                                        arrowLine.isFilled(), linePoints,
                                        arrowLine.getSmoothFactor(),
                                        FillPattern.valueOf(
                                                arrowLine.getFillPattern()),
                                        arrowLine.getPgenCategory(),
                                        arrowLine.getPgenType());

                                line.setParent(lbl);
                                lbl.addArrow(line);
                            }
                        }

                        if (lblDe.getMidCloudText() != null) {
                            // convert label texts.
                            // mid-level text is for clouds.
                            for (gov.noaa.nws.ncep.ui.pgen.file.MidCloudText aText : lblDe
                                    .getMidCloudText()) {

                                Color[] clr = new Color[aText.getColor()
                                        .size()];
                                int nn = 0;
                                for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : aText
                                        .getColor()) {
                                    clr[nn++] = new Color(fColor.getRed(),
                                            fColor.getGreen(), fColor.getBlue(),
                                            fColor.getAlpha());
                                }

                                Point loc = aText.getPoint();

                                MidCloudText text = new MidCloudText(
                                        (Coordinate[]) null,
                                        aText.getFontName(),
                                        aText.getFontSize(),
                                        TextJustification.valueOf(
                                                aText.getJustification()),
                                        new Coordinate(loc.getLon(),
                                                loc.getLat()),
                                        aText.getCloudTypes(),
                                        aText.getCloudAmounts(),
                                        aText.getTurbulenceType(),
                                        aText.getTurbulenceLevels(),
                                        aText.getIcingType(),
                                        aText.getIcingLevels(),
                                        aText.getTstormTypes(),
                                        aText.getTstormLevels(),
                                        FontStyle.valueOf(aText.getStyle()),
                                        clr[0], aText.getPgenCategory(),
                                        aText.getPgenType());

                                // text.setTwoColumns(false);
                                if (aText.getIthw() != null) {
                                    text.setIthw(aText.getIthw());
                                }

                                if (aText.getIwidth() != null) {
                                    text.setIwidth(aText.getIwidth());
                                }

                                text.setParent(lbl);
                                lbl.setSpe(text);
                            }
                        }

                        if (lblDe.getAvnText() != null) {
                            // convert label texts.
                            // avn text is for turbulence.
                            for (gov.noaa.nws.ncep.ui.pgen.file.AvnText aText : lblDe
                                    .getAvnText()) {

                                Color[] clr = new Color[aText.getColor()
                                        .size()];
                                int nn = 0;
                                for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : aText
                                        .getColor()) {
                                    clr[nn++] = new Color(fColor.getRed(),
                                            fColor.getGreen(), fColor.getBlue(),
                                            fColor.getAlpha());
                                }

                                Point loc = aText.getPoint();

                                AvnText text = new AvnText((Coordinate[]) null,
                                        aText.getFontName(),
                                        aText.getFontSize(),
                                        TextJustification.valueOf(
                                                aText.getJustification()),
                                        new Coordinate(loc.getLon(),
                                                loc.getLat()),
                                        AviationTextType.valueOf(
                                                aText.getAvnTextType()),
                                        aText.getTopValue(),
                                        aText.getBottomValue(),
                                        FontStyle.valueOf(aText.getStyle()),
                                        clr[0], aText.getSymbolPatternName(),
                                        aText.getPgenCategory(),
                                        aText.getPgenType());

                                // text.setTwoColumns(false);
                                if (aText.getIthw() != null) {
                                    text.setIthw(aText.getIthw());
                                }

                                if (aText.getIwidth() != null) {
                                    text.setIwidth(aText.getIwidth());
                                }

                                text.setParent(lbl);
                                lbl.setSpe(text);
                            }
                        }
                        if (lblDe.getText() != null) {
                            handleCcfpText(lblDe, lbl);
                        }
                        ll.addLabel(lbl);
                    }

                }

            }

            if (elem.getLine() != null) {
                // convert lines
                for (gov.noaa.nws.ncep.ui.pgen.file.Line fLine : elem
                        .getLine()) {

                    // get colors
                    Color[] clr = new Color[fLine.getColor().size()];
                    int nn = 0;
                    for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : fLine
                            .getColor()) {
                        clr[nn++] = new Color(fColor.getRed(),
                                fColor.getGreen(), fColor.getBlue(),
                                fColor.getAlpha());
                    }

                    ArrayList<Coordinate> linePoints = new ArrayList<Coordinate>();
                    nn = 0;
                    for (Point pt : fLine.getPoint()) {
                        linePoints
                                .add(new Coordinate(pt.getLon(), pt.getLat()));
                    }

                    Line line = new Line(null, clr, fLine.getLineWidth(),
                            fLine.getSizeScale(), fLine.isClosed(),
                            fLine.isFilled(), linePoints,
                            fLine.getSmoothFactor(),
                            FillPattern.valueOf(fLine.getFillPattern()),
                            fLine.getPgenCategory(), fLine.getPgenType());

                    line.setParent(ll);
                    ll.addLine(line);
                }

            }
        }

        return ll;

    }

    /**
     * Convert a DECollection in an XML file to a cloud element
     * 
     * @param dec
     * @return
     */
    private static Cloud convertXML2Cloud(
            gov.noaa.nws.ncep.ui.pgen.file.DECollection dec) {
        Cloud cloud = new Cloud("Cloud");
        return (Cloud) convertXML2LabeledLine(dec, cloud);

    }

    /**
     * Convert a DECollection in an XML file to a turbulence element
     * 
     * @param dec
     * @return
     */
    private static Turbulence convertXML2Turb(
            gov.noaa.nws.ncep.ui.pgen.file.DECollection dec) {
        Turbulence turb = new Turbulence("Turbulence");
        return (Turbulence) convertXML2LabeledLine(dec, turb);

    }

    private static gov.noaa.nws.ncep.ui.pgen.sigmet.Ccfp convertXML2Ccfp(
            gov.noaa.nws.ncep.ui.pgen.file.DECollection dec) {
        // use getCollectionName() for speed and time
        String[] attr = dec.getCollectionName().split(CcfpInfo.TEXT_SEPERATOR);
        Sigmet sig = new Sigmet();
        sig.setEditableAttrPhenomSpeed(attr[1]);
        sig.setEditableAttrPhenomDirection(attr[2]);
        sig.setEditableAttrStartTime(attr[3]);
        sig.setEditableAttrEndTime(attr[4]);
        sig.setEditableAttrPhenom(attr[5]);
        sig.setEditableAttrPhenom2(attr[6]);
        sig.setEditableAttrPhenomLat(attr[7]);
        sig.setEditableAttrPhenomLon(attr[8]);
        sig.setType(attr[attr.length - 1]);

        gov.noaa.nws.ncep.ui.pgen.sigmet.Ccfp ccfp = new gov.noaa.nws.ncep.ui.pgen.sigmet.Ccfp(
                dec.getCollectionName());
        ccfp.setSigmet(sig);
        return (gov.noaa.nws.ncep.ui.pgen.sigmet.Ccfp) convertXML2LabeledLine(
                dec, ccfp);
    }

    private static void handleCcfpText(
            gov.noaa.nws.ncep.ui.pgen.file.DrawableElement lblDe, Label lbl) {

        for (gov.noaa.nws.ncep.ui.pgen.file.Text aText : lblDe.getText()) {

            Color[] clr = new Color[aText.getColor().size()];
            int nn = 0;
            for (gov.noaa.nws.ncep.ui.pgen.file.Color fColor : aText
                    .getColor()) {
                clr[nn++] = new Color(fColor.getRed(), fColor.getGreen(),
                        fColor.getBlue(), fColor.getAlpha());
            }

            Point loc = aText.getPoint();

            Text text = new Text(null, aText.getFontName(), aText.getFontSize(),
                    TextJustification.valueOf(aText.getJustification()),
                    new Coordinate(loc.getLon(), loc.getLat()), 0.0,
                    TextRotation.SCREEN_RELATIVE,
                    aText.getTextLine().toArray(new String[] {}),
                    FontStyle.valueOf(aText.getStyle()), clr[0], 0, 0, true,
                    DisplayType.BOX, aText.getPgenCategory(),
                    aText.getPgenType());

            text.setParent(lbl);
            lbl.setSpe(text);
        }
    }

    /**
     * Convert a JAXB XML TCM object to a PGEN Tcm object.
     * 
     * @param cnt
     * @return
     */
    private static Tcm convertXML2Tcm(
            gov.noaa.nws.ncep.ui.pgen.file.TCM fileTcm) {

        Tcm pgenTcm = new Tcm();

        pgenTcm.setPgenType(fileTcm.getPgenType());
        pgenTcm.setPgenCategory(fileTcm.getPgenCategory());

        pgenTcm.setAdvisoryNumber(fileTcm.getAdvisoryNumber());
        pgenTcm.setBasin(fileTcm.getBasin());
        pgenTcm.setCentralPressure(fileTcm.getCentralPressure());
        pgenTcm.setCorrection(fileTcm.getCorrection());
        pgenTcm.setEyeSize(fileTcm.getEyeSize());
        pgenTcm.setStormName(fileTcm.getStormName());
        pgenTcm.setStormNumber(fileTcm.getStormNumber());
        pgenTcm.setStormType(fileTcm.getStormType());

        Calendar tcmTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        XMLGregorianCalendar xmlCal = fileTcm.getAdvisoryTime();
        tcmTime.set(xmlCal.getYear(), xmlCal.getMonth() - 1, xmlCal.getDay(),
                xmlCal.getHour(), xmlCal.getMinute(), xmlCal.getSecond());
        pgenTcm.setTime(tcmTime);

        pgenTcm.setWaveQuatro(fileTcm.getTcmWaves());
        pgenTcm.setTcmFcst(fileTcm.getTcmFcst());

        return pgenTcm;

    }

    /**
     * Convert a Tcm object to an XML TCM object.
     * 
     * @param cnt
     * @return
     */
    private static gov.noaa.nws.ncep.ui.pgen.file.TCM convertTcm2XML(
            Tcm pgenTcm) {

        gov.noaa.nws.ncep.ui.pgen.file.TCM fileTcm = new gov.noaa.nws.ncep.ui.pgen.file.TCM();

        // fileTcm.setAdvisories(advisories);
        fileTcm.setAdvisoryNumber(pgenTcm.getAdvisoryNumber());

        XMLGregorianCalendar xmlCal = null;
        Calendar tcmTime = pgenTcm.getAdvisoryTime();

        try {
            xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    tcmTime.get(Calendar.YEAR), tcmTime.get(Calendar.MONTH) + 1,
                    tcmTime.get(Calendar.DAY_OF_MONTH),
                    tcmTime.get(Calendar.HOUR_OF_DAY),
                    tcmTime.get(Calendar.MINUTE), tcmTime.get(Calendar.SECOND),
                    tcmTime.get(Calendar.MILLISECOND), 0);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        fileTcm.setAdvisoryTime(xmlCal);

        fileTcm.setCentralPressure(pgenTcm.getCentralPressure());
        fileTcm.setCorrection(pgenTcm.isCorrection());
        fileTcm.setEyeSize(pgenTcm.getEyeSize());
        fileTcm.setPositionAccuracy(pgenTcm.getPositionAccuracy());

        fileTcm.setBasin(pgenTcm.getBasin());
        fileTcm.setPgenCategory(pgenTcm.getPgenCategory());
        fileTcm.setPgenType(pgenTcm.getPgenType());
        fileTcm.setStormName(pgenTcm.getStormName());
        fileTcm.setStormNumber(pgenTcm.getStormNumber());
        fileTcm.setStormType(pgenTcm.getStormType());

        fileTcm.setTcmWaves(pgenTcm.getWaveQuarters());
        fileTcm.setTcmFcst(pgenTcm.getTcmFcst());

        return fileTcm;

    }

}
