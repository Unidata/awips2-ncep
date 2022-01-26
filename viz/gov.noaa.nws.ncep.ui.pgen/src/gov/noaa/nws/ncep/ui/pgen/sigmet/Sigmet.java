/*
 * gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet
 *
 * September 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.sigmet;

import java.awt.Color;
import java.util.ArrayList;

import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.annotation.ElementOperations;
import gov.noaa.nws.ncep.ui.pgen.annotation.Operation;
import gov.noaa.nws.ncep.ui.pgen.display.FillPatternList.FillPattern;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;

/**
 * Element class for sigmet.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 09/09        160         Gang Zhang  Initial Creation.
 * 04/11        ?           B. Yin      Re-factor IAttribute
 * 03/12        #676        Q. Zhu      Added Issue Office field.
 * 04/28/20     77994       ksunil      new fields for TC.
 * May 22, 2020 78000       ksunil      New Tropical Cyclone UI components for Fcst
 * Feb 05, 2021 87538       smanoj      Added FCST Lat/Lon for Tropical Cyclone.
 * Apr 08, 2021 90325       smanoj      CARSAM Backup WMO headers update.
 * Jun 18, 2021 90732       mroos       Added variables for VolAsh altitude level info
 *
 * </pre>
 *
 * @author gzhang
 */

@ElementOperations({ Operation.COPY_MOVE, Operation.EXTRAPOLATE,
        Operation.DELETE_POINT, Operation.ADD_POINT, Operation.INTERPOLATE,
        Operation.MODIFY })
public class Sigmet extends AbstractSigmet {

    public final static String SIGMET_PGEN_CATEGORY = "Sigmet";

    public final static String SIGMET_PGEN_TYPE = "INTL_SIGMET";

    // public for DisplayElementFactory/others use
    public static final String AREA = "Area", LINE = "Line",
            ISOLATED = "Isolated";

    // new,amend...
    private String editableAttrStatus;

    // valid start time
    private String editableAttrStartTime;

    // valid end time
    private String editableAttrEndTime;

    private String editableAttrRemarks;

    private String editableAttrPhenom;

    private String editableAttrPhenom2;

    private String editableAttrPhenomName;

    private String editableAttrPhenomLat;

    private String editableAttrPhenomLon;

    private String editableAttrPhenomPressure;

    private String editableAttrPhenomMaxWind;

    private String editableAttrFreeText;

    private String editableAttrTrend;

    private String editableAttrMovement;

    private String editableAttrPhenomSpeed;

    private String editableAttrPhenomDirection;

    private String editableAttrLevel;

    private String editableAttrLevelInfo1;

    private String editableAttrLevelInfo2;

    private String editableAttrLevelText1;

    private String editableAttrLevelText2;

    private String editableAttrAltLevel;

    private String editableAttrAltLevelInfo1;

    private String editableAttrAltLevelInfo2;

    private String editableAttrAltLevelText1;

    private String editableAttrAltLevelText2;

    private String editableAttrFir;

    private String editableAttrCarSamBackupMode;

    private String editableAttrFcstAvail;

    private String editableAttrFcstTime;

    private String editableAttrFcstCntr;

    private String editableAttrFcstPhenomLat;

    private String editableAttrFcstPhenomLon;

    private String editableAttrFcstVADesc;

    private String editableAttrRALSelection;

    private String editableAttrAltLevelText;

    private String editableAttrAltitudeSelection;

    public Sigmet() {

    }

    public Sigmet(Coordinate[] range, Color[] colors,
            gov.noaa.nws.ncep.ui.pgen.file.Sigmet fSig,
            ArrayList<Coordinate> sigmetPoints) {

        super(range, colors, fSig.getLineWidth(), fSig.getSizeScale(),
                fSig.isClosed(), fSig.isFilled(), sigmetPoints,
                fSig.getSmoothFactor(),
                FillPattern.valueOf(fSig.getFillPattern()),
                fSig.getPgenCategory(), fSig.getPgenType(), fSig.getType(),
                fSig.getWidth(), fSig.getEditableAttrArea(),
                fSig.getEditableAttrIssueOffice(), fSig.getEditableAttrStatus(),
                fSig.getEditableAttrId(), fSig.getEditableAttrSeqNum());

        this.editableAttrStatus = fSig.getEditableAttrStatus();
        this.editableAttrStartTime = fSig.getEditableAttrStartTime();
        this.editableAttrEndTime = fSig.getEditableAttrEndTime();
        this.editableAttrRemarks = fSig.getEditableAttrRemarks();
        this.editableAttrPhenom = fSig.getEditableAttrPhenom();
        this.editableAttrPhenom2 = fSig.getEditableAttrPhenom2();
        this.editableAttrPhenomName = fSig.getEditableAttrPhenomName();
        this.editableAttrPhenomLat = fSig.getEditableAttrPhenomLat();
        this.editableAttrPhenomLon = fSig.getEditableAttrPhenomLon();
        this.editableAttrPhenomPressure = fSig.getEditableAttrPhenomPressure();
        this.editableAttrPhenomMaxWind = fSig.getEditableAttrPhenomMaxWind();
        this.editableAttrFreeText = fSig.getEditableAttrFreeText();
        this.editableAttrTrend = fSig.getEditableAttrTrend();
        this.editableAttrMovement = fSig.getEditableAttrMovement();
        this.editableAttrPhenomSpeed = fSig.getEditableAttrPhenomSpeed();
        this.editableAttrPhenomDirection = fSig
                .getEditableAttrPhenomDirection();
        this.editableAttrLevel = fSig.getEditableAttrLevel();
        this.editableAttrLevelInfo1 = fSig.getEditableAttrLevelInfo1();
        this.editableAttrLevelInfo2 = fSig.getEditableAttrLevelInfo2();
        this.editableAttrLevelText1 = fSig.getEditableAttrLevelText1();
        this.editableAttrLevelText2 = fSig.getEditableAttrLevelText2();
        this.editableAttrAltLevel = fSig.getEditableAttrAltLevel();
        this.editableAttrAltLevelInfo1 = fSig.getEditableAttrAltLevelInfo1();
        this.editableAttrAltLevelInfo2 = fSig.getEditableAttrAltLevelInfo2();
        this.editableAttrAltLevelText1 = fSig.getEditableAttrAltLevelText1();
        this.editableAttrAltLevelText2 = fSig.getEditableAttrAltLevelText2();
        this.editableAttrFir = fSig.getEditableAttrFir();
        this.editableAttrCarSamBackupMode = fSig
                .getEditableAttrCarSamBackupMode();
        this.editableAttrFcstAvail = fSig.getEditableAttrFcstAvail();
        this.editableAttrFcstTime = fSig.getEditableAttrFcstTime();
        this.editableAttrFcstCntr = fSig.getEditableAttrFcstCntr();
        this.editableAttrFcstPhenomLat = fSig.getEditableAttrFcstPhenomLat();
        this.editableAttrFcstPhenomLon = fSig.getEditableAttrFcstPhenomLon();
        this.editableAttrFcstVADesc = fSig.getEditableAttrFcstVADesc();
        this.editableAttrRALSelection = fSig.getEditableAttrRALSelection();
        this.editableAttrAltLevelText = fSig.getEditableAttrAltLevelText();
        this.editableAttrAltitudeSelection = fSig
                .getEditableAttrAltitudeSelection();
    }

    @Override
    public DrawableElement copy() {
        /*
         * create a new Line object and initially set its attributes to this
         * one's
         */
        Sigmet newSigmet = new Sigmet();
        newSigmet.update(this);

        /*
         * new Coordinates points are created and set, so we don't just set
         * references
         */
        ArrayList<Coordinate> ptsCopy = new ArrayList<>();
        for (int i = 0; i < this.getPoints().size(); i++) {
            ptsCopy.add(new Coordinate(this.getPoints().get(i)));
        }
        newSigmet.setPoints(ptsCopy);

        /*
         * new colors are created and set, so we don't just set references
         */
        Color[] colorCopy = new Color[this.getColors().length];
        for (int i = 0; i < this.getColors().length; i++) {
            colorCopy[i] = new Color(this.getColors()[i].getRed(),
                    this.getColors()[i].getGreen(),
                    this.getColors()[i].getBlue());
        }
        newSigmet.setColors(colorCopy);

        /*
         * new Strings are created for Type and LinePattern
         */
        newSigmet.setPgenCategory(new String(this.getPgenCategory()));
        newSigmet.setPgenType(new String(this.getPgenType()));
        newSigmet.setParent(this.getParent());
        newSigmet.setType(this.getType());
        newSigmet.setWidth(this.getWidth());

        newSigmet.setEditableAttrArea(this.getEditableAttrArea());
        newSigmet.setEditableAttrIssueOffice(this.getEditableAttrIssueOffice());
        newSigmet.setEditableAttrFromLine(this.getEditableAttrFromLine());
        newSigmet.setEditableAttrId(this.getEditableAttrId());
        newSigmet.setEditableAttrSeqNum(this.getEditableAttrSeqNum());

        // CCFP
        newSigmet.setEditableAttrFreeText(this.getEditableAttrFreeText());
        newSigmet.setEditableAttrFcstCntr(this.getEditableAttrFcstCntr());
        newSigmet.setEditableAttrFcstPhenomLat(
                this.getEditableAttrFcstPhenomLat());
        newSigmet.setEditableAttrFcstPhenomLon(
                this.getEditableAttrFcstPhenomLon());
        newSigmet.setEditableAttrFcstTime(this.getEditableAttrEndTime());
        newSigmet.setEditableAttrFcstAvail(this.getEditableAttrFcstAvail());

        newSigmet.setEditableAttrFromLine(this.getEditableAttrFromLine());
        newSigmet.setEditableAttrStartTime(this.getEditableAttrStartTime());
        newSigmet.setEditableAttrEndTime(this.getEditableAttrEndTime());
        newSigmet.setEditableAttrPhenom(this.getEditableAttrPhenom());
        newSigmet.setEditableAttrPhenom2(this.getEditableAttrPhenom2());
        newSigmet.setEditableAttrPhenomLat(this.getEditableAttrPhenomLat());
        newSigmet.setEditableAttrPhenomLon(this.getEditableAttrPhenomLon());
        newSigmet.setEditableAttrPhenomSpeed(this.getEditableAttrPhenomSpeed());
        newSigmet.setEditableAttrPhenomDirection(
                this.getEditableAttrPhenomDirection());

        newSigmet.setEditableAttrArea(this.getEditableAttrArea());
        newSigmet.setEditableAttrRemarks(this.getEditableAttrRemarks());
        newSigmet.setEditableAttrPhenomName(this.getEditableAttrPhenomName());
        newSigmet.setEditableAttrPhenomPressure(
                this.getEditableAttrPhenomPressure());
        newSigmet.setEditableAttrPhenomMaxWind(
                this.getEditableAttrPhenomMaxWind());
        newSigmet.setEditableAttrTrend(this.getEditableAttrTrend());
        newSigmet.setEditableAttrMovement(this.getEditableAttrMovement());
        newSigmet.setEditableAttrLevel(this.getEditableAttrLevel());
        newSigmet.setEditableAttrLevelInfo1(this.getEditableAttrLevelInfo1());
        newSigmet.setEditableAttrLevelInfo2(this.getEditableAttrLevelInfo2());
        newSigmet.setEditableAttrLevelText1(this.getEditableAttrLevelText1());
        newSigmet.setEditableAttrLevelText2(this.getEditableAttrLevelText2());
        newSigmet.setEditableAttrAltLevel(this.getEditableAttrAltLevel());
        newSigmet.setEditableAttrAltLevelInfo1(
                this.getEditableAttrAltLevelInfo1());
        newSigmet.setEditableAttrAltLevelInfo2(
                this.getEditableAttrAltLevelInfo2());
        newSigmet.setEditableAttrAltLevelText1(
                this.getEditableAttrAltLevelText1());
        newSigmet.setEditableAttrAltLevelText2(
                this.getEditableAttrAltLevelText2());
        newSigmet.setEditableAttrFir(this.getEditableAttrFir());
        newSigmet.setEditableAttrCarSamBackupMode(
                this.getEditableAttrCarSamBackupMode());
        newSigmet.setEditableAttrFcstVADesc(this.getEditableAttrFcstVADesc());
        newSigmet.setEditableAttrRALSelection(
                this.getEditableAttrRALSelection());
        newSigmet.setEditableAttrAltLevelText(
                this.getEditableAttrAltLevelText());
        newSigmet.setEditableAttrAltitudeSelection(
                this.getEditableAttrAltitudeSelection());
        return newSigmet;
    }

    public String getEditableAttrStatus() {
        return editableAttrStatus;
    }

    public void setEditableAttrStatus(String editableAttrStatus) {
        this.editableAttrStatus = editableAttrStatus;
    }

    public String getEditableAttrStartTime() {
        return editableAttrStartTime;
    }

    public void setEditableAttrStartTime(String editableAttrStartTime) {
        this.editableAttrStartTime = editableAttrStartTime;
    }

    public String getEditableAttrEndTime() {
        return editableAttrEndTime;
    }

    public void setEditableAttrEndTime(String editableAttrEndTime) {
        this.editableAttrEndTime = editableAttrEndTime;
    }

    public String getEditableAttrRemarks() {
        return editableAttrRemarks;
    }

    public void setEditableAttrRemarks(String editableAttrRemarks) {
        this.editableAttrRemarks = editableAttrRemarks;
    }

    public String getEditableAttrPhenom() {
        return editableAttrPhenom;
    }

    public void setEditableAttrPhenom(String editableAttrPhenom) {
        this.editableAttrPhenom = editableAttrPhenom;
    }

    public String getEditableAttrPhenom2() {
        return editableAttrPhenom2;
    }

    public void setEditableAttrPhenom2(String editableAttrPhenom2) {
        this.editableAttrPhenom2 = editableAttrPhenom2;
    }

    public String getEditableAttrPhenomName() {
        return editableAttrPhenomName;
    }

    public void setEditableAttrPhenomName(String editableAttrPhenomName) {
        this.editableAttrPhenomName = editableAttrPhenomName;
    }

    public String getEditableAttrPhenomLat() {
        return editableAttrPhenomLat;
    }

    public void setEditableAttrPhenomLat(String editableAttrPhenomLat) {
        this.editableAttrPhenomLat = editableAttrPhenomLat;
    }

    public String getEditableAttrPhenomLon() {
        return editableAttrPhenomLon;
    }

    public void setEditableAttrPhenomLon(String editableAttrPhenomLon) {
        this.editableAttrPhenomLon = editableAttrPhenomLon;
    }

    public String getEditableAttrPhenomPressure() {
        return editableAttrPhenomPressure;
    }

    public void setEditableAttrPhenomPressure(
            String editableAttrPhenomPressure) {
        this.editableAttrPhenomPressure = editableAttrPhenomPressure;
    }

    public String getEditableAttrPhenomMaxWind() {
        return editableAttrPhenomMaxWind;
    }

    public void setEditableAttrPhenomMaxWind(String editableAttrPhenomMaxWind) {
        this.editableAttrPhenomMaxWind = editableAttrPhenomMaxWind;
    }

    public String getEditableAttrFreeText() {
        return editableAttrFreeText;
    }

    public void setEditableAttrFreeText(String editableAttrFreeText) {
        this.editableAttrFreeText = editableAttrFreeText;
    }

    public String getEditableAttrTrend() {
        return editableAttrTrend;
    }

    public void setEditableAttrTrend(String editableAttrTrend) {
        this.editableAttrTrend = editableAttrTrend;
    }

    public String getEditableAttrMovement() {
        return editableAttrMovement;
    }

    public void setEditableAttrMovement(String editableAttrMovement) {
        this.editableAttrMovement = editableAttrMovement;
    }

    public String getEditableAttrPhenomSpeed() {
        return editableAttrPhenomSpeed;
    }

    public void setEditableAttrPhenomSpeed(String editableAttrPhenomSpeed) {
        this.editableAttrPhenomSpeed = editableAttrPhenomSpeed;
    }

    public String getEditableAttrPhenomDirection() {
        return editableAttrPhenomDirection;
    }

    public void setEditableAttrPhenomDirection(
            String editableAttrPhenomDirection) {
        this.editableAttrPhenomDirection = editableAttrPhenomDirection;
    }

    public String getEditableAttrLevel() {
        return editableAttrLevel;
    }

    public void setEditableAttrLevel(String editableAttrLevel) {
        this.editableAttrLevel = editableAttrLevel;
    }

    public String getEditableAttrLevelInfo1() {
        return editableAttrLevelInfo1;
    }

    public void setEditableAttrLevelInfo1(String editableAttrLevelInfo1) {
        this.editableAttrLevelInfo1 = editableAttrLevelInfo1;
    }

    public String getEditableAttrLevelInfo2() {
        return editableAttrLevelInfo2;
    }

    public void setEditableAttrLevelInfo2(String editableAttrLevelInfo2) {
        this.editableAttrLevelInfo2 = editableAttrLevelInfo2;
    }

    public String getEditableAttrLevelText1() {
        return editableAttrLevelText1;
    }

    public void setEditableAttrLevelText1(String editableAttrLevelText1) {
        this.editableAttrLevelText1 = editableAttrLevelText1;
    }

    public String getEditableAttrLevelText2() {
        return editableAttrLevelText2;
    }

    public void setEditableAttrLevelText2(String editableAttrLevelText2) {
        this.editableAttrLevelText2 = editableAttrLevelText2;
    }

    public String getEditableAttrAltLevel() {
        return editableAttrAltLevel;
    }

    public void setEditableAttrAltLevel(String editableAttrLevel) {
        this.editableAttrAltLevel = editableAttrLevel;
    }

    public String getEditableAttrAltLevelInfo1() {
        return editableAttrAltLevelInfo1;
    }

    public void setEditableAttrAltLevelInfo1(String editableAttrLevelInfo1) {
        this.editableAttrAltLevelInfo1 = editableAttrLevelInfo1;
    }

    public String getEditableAttrAltLevelInfo2() {
        return editableAttrAltLevelInfo2;
    }

    public void setEditableAttrAltLevelInfo2(String editableAttrLevelInfo2) {
        this.editableAttrAltLevelInfo2 = editableAttrLevelInfo2;
    }

    public String getEditableAttrAltLevelText1() {
        return editableAttrAltLevelText1;
    }

    public void setEditableAttrAltLevelText1(String editableAttrLevelText1) {
        this.editableAttrAltLevelText1 = editableAttrLevelText1;
    }

    public String getEditableAttrAltLevelText2() {
        return editableAttrAltLevelText2;
    }

    public void setEditableAttrAltLevelText2(String editableAttrLevelText2) {
        this.editableAttrAltLevelText2 = editableAttrLevelText2;
    }

    public String isEditableAttrFcstAvail() {
        return editableAttrFcstAvail;
    }

    public void setEditableAttrFcstAvail(String editableAttrFcstAvail) {
        this.editableAttrFcstAvail = editableAttrFcstAvail;
    }

    public String getEditableAttrFcstAvail() {
        return editableAttrFcstAvail;
    }

    public String getEditableAttrFcstTime() {
        return editableAttrFcstTime;
    }

    public void setEditableAttrFcstTime(String editableAttrFcstTime) {
        this.editableAttrFcstTime = editableAttrFcstTime;
    }

    public String getEditableAttrFcstCntr() {
        return editableAttrFcstCntr;
    }

    public void setEditableAttrFcstCntr(String editableAttrFcstCntr) {
        this.editableAttrFcstCntr = editableAttrFcstCntr;
    }

    public String getEditableAttrFcstPhenomLat() {
        return editableAttrFcstPhenomLat;
    }

    public void setEditableAttrFcstPhenomLat(String editableAttrFcstPhenomLat) {
        this.editableAttrFcstPhenomLat = editableAttrFcstPhenomLat;
    }

    public String getEditableAttrFcstPhenomLon() {
        return editableAttrFcstPhenomLon;
    }

    public void setEditableAttrFcstPhenomLon(String editableAttrFcstPhenomLon) {
        this.editableAttrFcstPhenomLon = editableAttrFcstPhenomLon;
    }

    public String getEditableAttrFcstVADesc() {
        return editableAttrFcstVADesc;
    }

    public void setEditableAttrFcstVADesc(String editableAttrFcstVADesc) {
        this.editableAttrFcstVADesc = editableAttrFcstVADesc;
    }

    public String getEditableAttrFir() {
        return editableAttrFir;
    }

    public void setEditableAttrFir(String editableAttrFir) {
        this.editableAttrFir = editableAttrFir;
    }

    public String getEditableAttrCarSamBackupMode() {
        return editableAttrCarSamBackupMode;
    }

    public void setEditableAttrCarSamBackupMode(
            String editableAttrCarSamBackupMode) {
        this.editableAttrCarSamBackupMode = editableAttrCarSamBackupMode;
    }

    public String getEditableAttrRALSelection() {
        return editableAttrRALSelection;
    }

    public void setEditableAttrRALSelection(String editableRALSelection) {
        this.editableAttrRALSelection = editableRALSelection;
    }

    public String getEditableAttrAltLevelText() {
        return editableAttrAltLevelText;
    }

    public void setEditableAttrAltLevelText(String editableAltLevelText1) {
        this.editableAttrAltLevelText = editableAltLevelText1;
    }

    public String getEditableAttrAltitudeSelection() {
        return editableAttrAltitudeSelection;
    }

    public void setEditableAttrAltitudeSelection(
            String editableAttrAltitudeSelection) {
        this.editableAttrAltitudeSelection = editableAttrAltitudeSelection;
    }
}
