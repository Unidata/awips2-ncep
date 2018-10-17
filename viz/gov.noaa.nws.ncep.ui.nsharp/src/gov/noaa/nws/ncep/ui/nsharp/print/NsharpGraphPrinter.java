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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import com.raytheon.uf.common.sounding.WxMath;
import com.raytheon.uf.common.sounding.util.UAPoint;
import com.raytheon.viz.core.graphing.LineStroke;
import com.raytheon.viz.core.graphing.WindBarbFactory;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibThermo;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LayerParameters;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpLineProperty;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWGraphics;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWxMath;
import gov.noaa.nws.ncep.ui.nsharp.background.NsharpSkewTPaneBackground;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;

/**
 * 
 * Print the top, graph portion, of an nsharp printout.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------------------
 * Oct 16, 2018  6845     bsteffen  Consolidated graph related printing here.
 *
 * </pre>
 *
 * @author bsteffen
 */
public class NsharpGraphPrinter {

    private final GC gc;

    private final NsharpResourceHandler handler;

    private final List<NcSoundingLayer> soundingLys;

    private final Rectangle skewtBounds;

    private final NsharpWGraphics skewtWorld;

    private final Rectangle hodoBounds;

    private final NsharpWGraphics hodoWorld;

    private final int windX;

    private final int heightX;

    public NsharpGraphPrinter(GC gc, Rectangle bounds,
            NsharpResourceHandler handler) {
        this.gc = gc;
        this.handler = handler;
        this.soundingLys = handler.getSoundingLys();

        int borderWidth = getPointWidth(2);
        int verticalMargin = borderWidth + gc.getFontMetrics().getHeight() * 2;
        int leftMargin = borderWidth + gc.stringExtent("1000").x;

        int x = bounds.x + leftMargin;
        int y = bounds.y + verticalMargin;
        int width = bounds.width * 7 / 8;
        int height = bounds.height - 2 * verticalMargin;

        skewtBounds = new Rectangle(x, y, width, height);
        skewtWorld = makeWorld(skewtBounds);
        skewtWorld.setWorldCoordinates(NsharpConstants.left,
                NsharpConstants.top, NsharpConstants.right,
                NsharpConstants.bottom);

        x += width * 11 / 16;
        height = width = width * 5 / 16;

        hodoBounds = new Rectangle(x, y, width, height);
        hodoWorld = makeWorld(hodoBounds);
        hodoWorld.setWorldCoordinates(-50, 90, 90, -50);

        int scale = gc.getFontMetrics().getAverageCharWidth() * 3;
        int windWidth = (int) (scale * NsharpResourceHandler.BARB_LENGTH);

        windX = skewtBounds.x + skewtBounds.width + windWidth;
        heightX = windX + windWidth;
    }

    private static NsharpWGraphics makeWorld(Rectangle pageArea) {
        return new NsharpWGraphics(pageArea.x, pageArea.y,
                pageArea.x + pageArea.width, pageArea.y + pageArea.height);
    }

    private int getPointWidth(int pixelWidth) {
        return pixelWidth * gc.getDevice().getDPI().x / 72;
    }

    public void print() {
        printSkewtBackground();

        if (soundingLys != null && !soundingLys.isEmpty()) {
            printSkewtData();
        }

        printHodoBackground();

        if (soundingLys != null && !soundingLys.isEmpty()) {
            printHodoWind();
        }
    }

    private void printSkewtBackground() {
        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
        gc.setLineStyle(SWT.LINE_SOLID);
        gc.setLineWidth(getPointWidth(2));
        gc.drawRectangle(skewtBounds);
        printPressureLabels();
        printTemperatureLabels();
        gc.setLineWidth(getPointWidth(1));

        gc.setClipping(skewtBounds);
        printPressureLines();

        Color dryAdiabatColor = new Color(gc.getDevice(),
                NsharpConstants.dryAdiabatColor);
        Color temperatureColor = new Color(gc.getDevice(),
                NsharpConstants.temperatureColor);

        gc.setForeground(dryAdiabatColor);
        gc.setLineStyle(SWT.LINE_DASH);
        printDryAdiabatBackgroundLines();

        gc.setForeground(temperatureColor);
        gc.setLineStyle(SWT.LINE_DOT);
        printTemperatureBackgroundLines();

        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
        gc.setLineStyle(SWT.LINE_SOLID);
        dryAdiabatColor.dispose();
        temperatureColor.dispose();
    }

    private void printPressureLabels() {
        int right = skewtBounds.x - gc.getLineWidth();
        for (int pressure : NsharpConstants.PRESSURE_NUMBERING_LEVELS) {
            String s = NsharpConstants.pressFormat.format(pressure);
            Coordinate coor = NsharpWxMath.getSkewTXY(pressure, 0);
            int y = (int) skewtWorld.mapY(coor.y);
            int width = gc.stringExtent(s).x;
            gc.drawString(s, right - width, y);
        }
    }

    private void printTemperatureLabels() {
        int y = skewtBounds.y - gc.getFontMetrics().getHeight()
                - gc.getLineWidth();
        for (int i = -60; i > -120; i -= 10) {
            String s = Integer.toString(i);
            Coordinate c = NsharpWxMath.getSkewTXY(100, i);
            int x = (int) skewtWorld.mapX(c.x);
            gc.drawString(s, x, y);
        }
        y = skewtBounds.y + skewtBounds.height + gc.getLineWidth();
        for (int i = 40; i > -50; i -= 10) {
            String s = Integer.toString(i);
            Coordinate c = NsharpWxMath.getSkewTXY(1050, i);
            int x = (int) skewtWorld.mapX(c.x);
            gc.drawString(s, x, y);
        }
    }

    private void printPressureLines() {
        int x1 = skewtBounds.x;
        int x2 = x1 + skewtBounds.width;
        for (Integer pressure : NsharpConstants.PRESSURE_MAIN_LEVELS) {
            Coordinate coor = NsharpWxMath.getSkewTXY(pressure, 0);
            int y = (int) skewtWorld.mapY(coor.y);
            gc.drawLine(x1, y, x2, y);

        }
        x2 = x1 + gc.getFontMetrics().getAverageCharWidth() * 3;
        for (int pressure : NsharpConstants.PRESSURE_MARK_LEVELS) {
            Coordinate coor = NsharpWxMath.getSkewTXY(pressure, 0);
            int y = (int) skewtWorld.mapY(coor.y);
            gc.drawLine(x1, y, x2 + 10, y);
        }
    }

    private void printDryAdiabatBackgroundLines() {
        List<List<UAPoint>> dryPoints = NsharpSkewTPaneBackground
                .getDryAdiabats(10, -40, 20);
        for (List<UAPoint> points : dryPoints) {
            List<Coordinate> path = new ArrayList<>();
            for (UAPoint p : points) {
                double temperature = NsharpConstants.kelvinToCelsius
                        .convert(p.temperature);
                path.add(new Coordinate(temperature, p.pressure));
            }
            drawSkewtLine(path);
        }
    }

    private void printTemperatureBackgroundLines() {
        for (int i = 70; i > -200; i -= 10) {
            Coordinate start = new Coordinate(i, 1050);
            Coordinate end = new Coordinate(i, 100);
            drawSkewtLine(Arrays.asList(start, end));
        }
    }

    private void printSkewtData() {
        gc.setLineWidth(getPointWidth(2));

        Color temperatureColor = getStyledLineColor(NsharpConstants.LINE_TEMP);
        Color dewpointColor = getStyledLineColor(NsharpConstants.LINE_DEWP);
        Color wetBulbColor = getStyledLineColor(NsharpConstants.LINE_WETBULB);
        Color parcelColor = getStyledLineColor(NsharpConstants.LINE_PARCEL);

        gc.setLineStyle(SWT.LINE_SOLID);
        gc.setForeground(temperatureColor);
        printTemperatureCurve(NsharpConstants.LINE_TEMP);

        gc.setForeground(dewpointColor);
        printTemperatureCurve(NsharpConstants.LINE_DEWP);

        gc.setLineStyle(SWT.LINE_DASH);
        gc.setForeground(wetBulbColor);
        printWetbulbTraceCurve();

        gc.setLineStyle(SWT.LINE_DASHDOTDOT);
        gc.setForeground(parcelColor);
        printParcelTraceCurve();

        gc.setLineWidth(getPointWidth(1));
        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
        gc.setLineStyle(SWT.LINE_SOLID);
        gc.setClipping((Rectangle) null);
        temperatureColor.dispose();
        dewpointColor.dispose();
        wetBulbColor.dispose();
        parcelColor.dispose();

        printVerticalWind();

        printHeightMarks();

    }

    private void printTemperatureCurve(int lineNameIndex) {
        List<Coordinate> path = new ArrayList<>();
        for (NcSoundingLayer layer : soundingLys) {
            double t;
            if (lineNameIndex == NsharpConstants.LINE_TEMP) {
                t = layer.getTemperature();
            } else if (lineNameIndex == NsharpConstants.LINE_DEWP) {
                t = layer.getDewpoint();
            } else {
                break;
            }
            double pressure = layer.getPressure();
            if (t != NcSoundingLayer.MISSING) {
                path.add(new Coordinate(t, pressure));
            }
        }
        drawSkewtLine(path);
    }

    private void printWetbulbTraceCurve() {
        List<Coordinate> path = new ArrayList<>();
        for (NcSoundingLayer layer : this.soundingLys) {
            if (layer.getDewpoint() > -200) {
                float t = NsharpLibThermo.wetbulb(layer.getPressure(),
                        layer.getTemperature(), layer.getDewpoint());
                path.add(new Coordinate(t, layer.getPressure()));
            }
        }
        drawSkewtLine(path);
    }

    private void printParcelTraceCurve() {
        Parcel parcel = handler.getWeatherDataStore().getParcelMap()
                .get(handler.getCurrentParcel());
        if (parcel == null) {
            return;
        }
        List<Coordinate> path = new ArrayList<>();

        float vtemp = NsharpLibThermo.virtemp(parcel.getLplpres(),
                parcel.getLpltemp(), parcel.getLpldwpt());
        path.add(new Coordinate(vtemp, parcel.getLplpres()));
        LayerParameters dryLiftLayer = NsharpLibThermo.drylift(
                parcel.getLplpres(), parcel.getLpltemp(), parcel.getLpldwpt());
        vtemp = NsharpLibThermo.virtemp(dryLiftLayer.getPressure(),
                dryLiftLayer.getTemperature(), dryLiftLayer.getTemperature());
        path.add(new Coordinate(vtemp, dryLiftLayer.getPressure()));
        for (float i = dryLiftLayer.getPressure() - 50; i >= 100; i = i - 50) {
            float t3 = NsharpLibThermo.wetlift(dryLiftLayer.getPressure(),
                    dryLiftLayer.getTemperature(), i);
            vtemp = NsharpLibThermo.virtemp(i, t3, t3);
            path.add(new Coordinate(vtemp, i));
        }

        float t3 = NsharpLibThermo.wetlift(dryLiftLayer.getPressure(),
                dryLiftLayer.getTemperature(), 100);
        vtemp = NsharpLibThermo.virtemp(100, t3, t3);
        path.add(new Coordinate(vtemp, 100));
        drawSkewtLine(path);
    }

    /**
     * 
     * Print Wind barb for printing job This function followed algorithm in
     * plot_barbs (void) at xwvid1.c to choose wind bulb for drawing around
     * every 400m
     * 
     */
    private void printVerticalWind() {
        ArrayList<List<LineStroke>> windList = new ArrayList<>();

        int scale = gc.getFontMetrics().getAverageCharWidth() * 3;

        gc.drawLine(windX, skewtBounds.y, windX,
                skewtBounds.y + skewtBounds.height);

        float lastHeight = -999;
        double windY;
        for (NcSoundingLayer layer : soundingLys) {
            float pressure = layer.getPressure();
            float spd = layer.getWindSpeed();
            float dir = layer.getWindDirection();

            if (pressure < 100) {
                continue;
            }

            if ((layer.getGeoHeight() - lastHeight) < 400) {
                continue;
            }

            // Get the vertical ordinate.
            windY = skewtWorld.mapY(NsharpWxMath.getSkewTXY(pressure, 0).y);

            List<LineStroke> barb = WindBarbFactory
                    .getWindGraphics((double) (spd), (double) dir);
            if (barb != null) {
                for (LineStroke stroke : barb) {
                    stroke.scale(scale, -1 * scale);
                }
                WindBarbFactory.translateBarb(barb, windX, windY);
                windList.add(barb);
            }

            lastHeight = layer.getGeoHeight();
        }
        Coordinate pt1 = new Coordinate(0, 0), pt2;
        for (List<LineStroke> barb : windList) {
            for (LineStroke stroke : barb) {

                if (stroke.getType() == "M") {
                    pt1 = stroke.getPoint();
                } else if (stroke.getType() == "D") {
                    pt2 = stroke.getPoint();
                    gc.drawLine((int) pt1.x, (int) pt1.y, (int) pt2.x,
                            (int) pt2.y);
                }
            }
        }
    }

    private void printHeightMarks() {
        int textHeight = gc.getFontMetrics().getHeight();
        int tickWidth = textHeight;
        int lineWidth = gc.getLineWidth();
        int labelOffset = tickWidth + lineWidth;

        /* Start with center line and top labels. */
        int y = skewtBounds.y;
        gc.drawLine(heightX, y, heightX, y + skewtBounds.height);
        gc.drawLine(heightX - tickWidth * 2, y, heightX + tickWidth * 2, y);

        String s = "Kft  Km";
        int x = heightX - gc.stringExtent(s).x / 2;
        y -= textHeight + lineWidth;
        gc.drawString(s, x, y);

        s = "MSL";
        x = heightX - gc.stringExtent(s).x / 2;
        y -= textHeight;
        gc.drawString(s, x, y);

        /* top level mark at 100 mbar */
        y = skewtBounds.y + lineWidth;
        float hgt = NsharpLibBasics.i_hght(soundingLys, 100);
        gc.drawString(Float.toString(hgt / 1000F), heightX + lineWidth, y);
        /* feet marks */
        for (int feet : NsharpConstants.HEIGHT_LEVEL_FEET) {
            s = Integer.toString(feet / 1000);
            float meters = (float) NsharpConstants.feetToMeters.convert(feet);
            double pressure = NsharpLibBasics.i_pres(soundingLys, meters);
            y = (int) skewtWorld.mapY(NsharpWxMath.getSkewTXY(pressure, -50).y);
            int width = gc.stringExtent(s).x;
            gc.drawString(s, heightX - labelOffset - width, y - textHeight / 2);
            gc.drawLine(heightX, y, heightX - tickWidth, y);
        }
        /* meter marks */
        for (int meters : NsharpConstants.HEIGHT_LEVEL_METERS) {
            s = Integer.toString(meters / 1000);
            double pressure = NsharpLibBasics.i_pres(soundingLys, meters);
            y = (int) skewtWorld.mapY(NsharpWxMath.getSkewTXY(pressure, -50).y);

            gc.drawString(s, heightX + labelOffset, y - textHeight / 2);
            gc.drawLine(heightX, y, heightX + tickWidth, y);
        }
        /* Surface mark */
        String sfcLabel = "SFC("
                + Integer.toString((int) soundingLys.get(0).getGeoHeight())
                + "m)";
        y = (int) skewtWorld.mapY(NsharpWxMath
                .getSkewTXY(soundingLys.get(0).getPressure(), -50).y);
        gc.drawString(sfcLabel, heightX + lineWidth, y + gc.getLineWidth());
        gc.drawLine(heightX, y, heightX + tickWidth, y);
    }

    private Color getStyledLineColor(int lineNameIndex) {
        NsharpLineProperty lineProps = handler.getLinePropertyMap()
                .get(NsharpConstants.lineNameArray[lineNameIndex]);
        RGB rgb = lineProps.getLineColor();
        Color back = gc.getBackground();
        /*
         * On the display the lines are usually drawn on a black background but
         * on paper it is usually a white background so invert colors too close
         * to white so they don't disappear.
         */
        if (back.getRed() == 255 && back.getGreen() == 255
                && back.getBlue() == 255) {
            if (rgb.red > 240 && rgb.green > 240 && rgb.blue > 240) {
                rgb = new RGB(255 - rgb.red, 255 - rgb.green, 255 - rgb.blue);
            }
        }
        return new Color(gc.getDevice(), rgb);
    }

    private void drawSkewtLine(List<Coordinate> tempAndPresList) {
        Path path = new Path(gc.getDevice());
        boolean first = true;
        for (Coordinate tempAndPres : tempAndPresList) {
            if (tempAndPres == null) {
                first = true;
                continue;
            }
            double t = tempAndPres.x;
            double p = tempAndPres.y;

            Coordinate c = NsharpWxMath.getSkewTXY(p, t);
            c = skewtWorld.map(c);
            float x = (float) c.x;
            float y = (float) c.y;
            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }
        gc.drawPath(path);
    }

    private void printHodoBackground() {
        gc.fillRectangle(hodoBounds);
        gc.setLineWidth(getPointWidth(2));
        gc.drawRectangle(hodoBounds);
        gc.setClipping(hodoBounds);
        gc.setLineWidth(getPointWidth(1));

        Coordinate center = new Coordinate(hodoWorld.mapX(0),
                hodoWorld.mapY(0));

        gc.setLineStyle(SWT.LINE_SOLID);
        // draw the spokes.
        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 2) {
            double x = 200 * Math.cos(angle);
            double y = 200 * Math.sin(angle);
            gc.drawLine((int) center.x, (int) center.y, (int) hodoWorld.mapX(x),
                    (int) hodoWorld.mapY(y));
        }

        // draw circles
        gc.setLineStyle(SWT.LINE_DOT);
        for (int spd = 10; spd <= 100; spd += 10) {
            int dist = (int) (hodoWorld.mapX(spd) - center.x);
            gc.drawOval((int) center.x - dist, (int) center.y - dist, 2 * dist,
                    2 * dist);

            if (spd % 30 == 0) {
                Coordinate uv = WxMath.uvComp(spd, 240);

                gc.drawString("" + spd, (int) hodoWorld.mapX(uv.x),
                        (int) hodoWorld.mapY(uv.y), false);
            }
        }
        gc.setLineStyle(SWT.LINE_SOLID);

        int textHeight = gc.getFontMetrics().getHeight();
        int lineWidth = getPointWidth(2);

        int x = (int) center.x - gc.stringExtent("1800").x / 2;
        int y = hodoBounds.y + lineWidth;

        // label the spokes
        gc.drawString("180" + NsharpConstants.DEGREE_SYMBOL, x, y);
        y = hodoBounds.y + hodoBounds.height - textHeight - lineWidth;
        gc.drawString("360" + NsharpConstants.DEGREE_SYMBOL, x, y);

        y = (int) center.y - lineWidth / 2;
        x = hodoBounds.x + hodoBounds.width - gc.stringExtent("2700").x
                - lineWidth;
        gc.drawString("270" + NsharpConstants.DEGREE_SYMBOL, x, y);

        x = hodoBounds.x + lineWidth;
        gc.drawString("90" + NsharpConstants.DEGREE_SYMBOL, x, y);
        gc.setClipping((Rectangle) null);

    }

    private void printHodoWind() {
        gc.setClipping(hodoBounds);
        gc.setLineStyle(SWT.LINE_SOLID);
        gc.setLineWidth(getPointWidth(1));

        Color red = gc.getDevice().getSystemColor(SWT.COLOR_RED);
        Color green = gc.getDevice().getSystemColor(SWT.COLOR_GREEN);
        Color yellow = gc.getDevice().getSystemColor(SWT.COLOR_YELLOW);
        Color cyan = gc.getDevice().getSystemColor(SWT.COLOR_CYAN);
        Color violet = new Color(gc.getDevice(), NsharpConstants.color_violet);

        float surfaceLevel = soundingLys.get(0).getGeoHeight();

        Coordinate c0 = null;
        Coordinate c1;
        for (NcSoundingLayer layer : soundingLys) {
            if (layer.getPressure() < 100 || layer.getWindSpeed() < 0)
                continue;
            if (layer.getGeoHeight() < (3000 + surfaceLevel)) {
                gc.setForeground(red);
            } else if (layer.getGeoHeight() < (6000 + surfaceLevel)) {
                gc.setForeground(green);
            } else if (layer.getGeoHeight() < (9000 + surfaceLevel)) {
                gc.setForeground(yellow);
            } else if (layer.getGeoHeight() < (12000 + surfaceLevel)) {
                gc.setForeground(cyan);
            } else {
                gc.setForeground(violet);
            }
            float wspd = layer.getWindSpeed();
            float wdir = layer.getWindDirection();
            c1 = WxMath.uvComp(wspd, wdir);
            if (c0 != null) {
                gc.drawLine((int) hodoWorld.mapX(c0.x),
                        (int) hodoWorld.mapY(c0.y), (int) hodoWorld.mapX(c1.x),
                        (int) hodoWorld.mapY(c1.y));
            }
            c0 = c1;
        }
        gc.setClipping((Rectangle) null);
        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
        violet.dispose();
    }

}
