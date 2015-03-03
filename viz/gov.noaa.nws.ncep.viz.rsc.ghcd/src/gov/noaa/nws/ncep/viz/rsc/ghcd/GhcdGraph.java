/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.viz.rsc.ghcd;

import gov.noaa.nws.ncep.viz.common.tsScaleMngr.XAxisScaleElement;
import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcher;
import gov.noaa.nws.ncep.viz.rsc.ghcd.rsc.GhcdResource;
import gov.noaa.nws.ncep.viz.rsc.ghcd.rsc.GhcdResourceData;
import gov.noaa.nws.ncep.viz.rsc.ghcd.rsc.GraphAttributes;
import gov.noaa.nws.ncep.viz.rsc.ghcd.util.GhcdUtil;
import gov.noaa.nws.ncep.viz.ui.display.NCTimeSeriesGraph;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.RGBColors;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource.ResourceStatus;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;
import com.raytheon.uf.viz.xy.graph.GraphLabelComparator;
import com.raytheon.uf.viz.xy.graph.XyGraphDescriptor;
import com.raytheon.uf.viz.xy.graph.axis.GraphAxis;
import com.raytheon.uf.viz.xy.graph.axis.IAxis;
import com.raytheon.uf.viz.xy.graph.axis.LinearAxisPlacer;
import com.raytheon.uf.viz.xy.graph.axis.LogarithmicAxisPlacer;
import com.raytheon.uf.viz.xy.graph.labeling.DataTimeLabel;
import com.raytheon.uf.viz.xy.graph.labeling.IGraphLabel;
import com.raytheon.uf.viz.xy.map.rsc.IGraphableResource;
import com.raytheon.viz.core.graphing.xy.XYImageData;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * The ghcd graph, needs to be extracted into AbstractGraph
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Sep 5, 2014   R4508       sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class GhcdGraph extends NCTimeSeriesGraph {

    protected static final NumberFormat nf = new DecimalFormat("0.0E00");

    protected SimpleDateFormat sdf;

    protected SimpleDateFormat topLabelSdf = new SimpleDateFormat(
            "HH:mm 'UTC' dd MMM yyyy");

    protected int duration;

    protected String xLabelFormat = "";

    protected Integer numIntervals = 4;

    protected Float xLabelInterval = 0.0f;

    protected Float xMajorTickInterval = 0.0f;

    protected Float xMinorTickInterval = 0.0f;

    protected RGB graphColor = null;

    protected GraphAttributes graphAttrs = null;

    protected boolean paintxAxisMajorMinorTicks = true;

    public GhcdGraph(XyGraphDescriptor descriptor) {
        super(descriptor);
        sdf = new SimpleDateFormat("HHmm");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        descriptor.getGraphResource().getCapability(ColorableCapability.class)
                .setColor(new RGB(255, 255, 255));
    }

    @Override
    protected void createAxes() {

        double minX = graphExtent.getMinX();
        double maxX = graphExtent.getMaxX();
        double minY = graphExtent.getMinY();
        double maxY = graphExtent.getMaxY();

        if (GhcdUtil.YSCALE_TYPE_LOG.equalsIgnoreCase(graphAttrs
                .getyScaleType())) {
            // get the list of y values based on log calculations
            graphAttrs.setyInterval(10.0f);
            float yVal = (float) ((LogarithmicAxisPlacer) xAxisPlacer)
                    .getMinDataValue();
            List<Float> yValList = new ArrayList<Float>();
            yValList.add(yVal);
            while (yVal < ((LogarithmicAxisPlacer) xAxisPlacer)
                    .getMaxDataValue()) {
                yVal = yVal * graphAttrs.getyInterval();
                yValList.add(yVal);
            }

            // Create the Axis if they do not exist
            // if (xAxes.length == 0) {
            xAxes = new IAxis[yValList.size()];
            for (int i = 0; i < xAxes.length; ++i) {
                xAxes[i] = new GraphAxis();
                xAxes[i].setLineStyle(LineStyle.DOTTED);
                xAxes[i].setDrawAxis(true);
                xAxes[i].setDiscreteValue(yValList.get(i));
            }
            // }
        } else if (GhcdUtil.YSCALE_TYPE_LINEAR.equalsIgnoreCase(graphAttrs
                .getyScaleType())) {
            // get the list of y values based on linear calculations
            float yVal = graphAttrs.getyScaleMin();
            List<Float> yValList = new ArrayList<Float>();
            yValList.add(yVal);

            while (yVal < ((LinearAxisPlacer) xAxisPlacer).getMaxDataValue()) {
                yVal = yVal + graphAttrs.getyInterval();
                yValList.add(yVal);
            }

            // Create the Axis if they do not exist
            // if (xAxes.length == 0) {
            xAxes = new IAxis[yValList.size()];
            for (int i = 0; i < xAxes.length; ++i) {
                xAxes[i] = new GraphAxis();
                xAxes[i].setLineStyle(LineStyle.DOTTED);
                xAxes[i].setDrawAxis(true);
                xAxes[i].setDiscreteValue(yValList.get(i));
            }
            // }
        }

        // Place them
        minX = graphExtent.getMinX();
        maxX = graphExtent.getMaxX();
        maxY = graphExtent.getMaxY();

        xAxisPlacer.setPixelWidth(graphExtent.getHeight());
        yAxisPlacer.setPixelWidth(graphExtent.getWidth());

        // Place the data axes
        double[] offsets = xAxisPlacer.placeAxes(xAxes);

        for (int i = 0; i < offsets.length; ++i) {
            double offset = offsets[i];
            xAxes[i].setStartLoc(new Coordinate(minX, maxY - offset, 0));
            xAxes[i].setEndLoc(new Coordinate(maxX, maxY - offset, 0));
        }

        // createVerticalAxes();
    }

    private void createVerticalAxes() {
        double start = 0;
        double end = 0;
        if (xLabels.size() > 0) {
            start = xLabels.get(0).getDiscreteValue();
            end = xLabels.get(xLabels.size() - 1).getDiscreteValue();
        }
        double diff = end - start;
        double numint = xLabels.size() - 1;

        yAxes = new IAxis[xLabels.size()];
        for (int i = 0; i < xLabels.size(); ++i) {
            yAxes[i] = new GraphAxis();
            yAxes[i].setLineStyle(LineStyle.SOLID);
            yAxes[i].setDrawAxis(true);
            yAxes[i].setDiscreteValue(start + (diff * i / numint));
        }

        double maxX = graphExtent.getMaxX();
        double maxY = graphExtent.getMaxY();

        double[] offsets = yAxisPlacer.placeAxes(yAxes);

        for (int i = 0; i < offsets.length; ++i) {
            double offset = offsets[i];
            yAxes[i].setStartLoc(new Coordinate(maxX - offset, maxY, 0));
            yAxes[i].setEndLoc(new Coordinate(maxX - offset, maxY, 0));
        }
    }

    @Override
    protected boolean canHandleResoruce(IGraphableResource<?, ?> rsc) {
        // Can only handle graphing of GhcdResources
        return (rsc instanceof GhcdResource);
    }

    @Override
    protected void paintUnits(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {

        RGB colorToUse = null;
        List<DrawableString> strings = new ArrayList<DrawableString>();
        for (IGraphableResource<?, ?> grsc : graphResource) {
            GhcdResource rsc = (GhcdResource) grsc;

            if (rsc == null) {
                continue;
            } else if (rsc.getData() == null) {
                continue;
            } else if (rsc.getData().getData() == null) {
                continue;
            } else if (rsc.getData().getData().size() < 1) {
                continue;
            }

            if (rsc.getProperties().isVisible()) {
                colorToUse = graphColor;

                if (rsc.getData() == null
                        || rsc.getData().getData().size() == 0
                        || !(rsc.getData().getData().get(0) instanceof XYImageData)) {
                    for (int i = 0; i < xAxes.length; i++) {
                        Coordinate[] coords = xAxes[i].getCoordinates();
                        if (coords[0].y < graphExtent.getMinY()) {
                            continue;
                        }

                        DrawableString parameters = new DrawableString("",
                                colorToUse);

                        parameters.font = unitsFont;
                        parameters.textStyle = TextStyle.DROP_SHADOW;
                        parameters.horizontalAlignment = HorizontalAlignment.RIGHT;
                        parameters.magnification = this.currentMagnification;

                        String value = df.format(xAxes[i].getDiscreteValue());

                        if (GhcdUtil.YSCALE_TYPE_LOG
                                .equalsIgnoreCase(graphAttrs.getyScaleType())) {
                            value = nf.format(xAxes[i].getDiscreteValue());
                        }
                        if (i == 0) {
                            parameters.verticallAlignment = VerticalAlignment.BOTTOM;
                        } else {
                            parameters.verticallAlignment = VerticalAlignment.MIDDLE;
                        }
                        parameters.setText(value, colorToUse);
                        parameters.setCoordinates(coords[0].x, coords[0].y,
                                coords[0].z);
                        strings.add(parameters);
                    }
                }
            }
        }
        target.drawStrings(strings);

        paintDataTimeUnits(target, paintProps, xLabels);
    }

    @Override
    public void constructVirtualExtent() {

        // make sure all resources are initialized
        for (IGraphableResource<?, ?> grsc : graphResource) {
            if (grsc instanceof GhcdResource) {
                GhcdResource rsc = (GhcdResource) grsc;
                if (rsc.getStatus() != ResourceStatus.INITIALIZED) {
                    return;
                }
                GhcdResourceData rscData = (GhcdResourceData) rsc
                        .getResourceData();
                graphAttrs = new GraphAttributes(rscData);
                rscData.setGraphAttr(graphAttrs);
            }
        }

        // setGraphParametersFromResource();

        double[] minMaxY = new double[2];
        xLabels.clear();

        getXaxisIntervals(xLabels);
        double minX = 0;
        double maxX = 0;
        minMaxY[0] = graphAttrs.getyScaleMin();
        minMaxY[1] = graphAttrs.getyScaleMax();

        if (xLabels.size() > 0) {
            minX = xLabels.get(0).getDiscreteValue();
            maxX = xLabels.get(xLabels.size() - 1).getDiscreteValue();
        }

        if (GhcdUtil.YSCALE_TYPE_LOG.equalsIgnoreCase(graphAttrs
                .getyScaleType())) {
            xAxisPlacer = new LogarithmicAxisPlacer(graphExtent.getHeight(),
                    minMaxY[0], minMaxY[1]);
        } else {
            xAxisPlacer = new LinearAxisPlacer(graphExtent.getHeight(),
                    minMaxY[0], minMaxY[1]);
        }

        yAxisPlacer = new LinearAxisPlacer(graphExtent.getWidth(), minX, maxX);

        updateVirtualExtent();

        sdf = new SimpleDateFormat(xLabelFormat);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        newResources = false;

    }

    private void getXaxisIntervals(List<IGraphLabel<DataTime>> xLabels) {
        for (IGraphableResource<?, ?> grsc : graphResource) {
            if (grsc instanceof GhcdResource) {
                GhcdResource rsc = (GhcdResource) grsc;

                DataTime[] range = rsc.getDataTimes();

                if (range == null || range.length == 0)
                    continue;

                DataTime start = range[0];
                xLabels.add(new DataTimeLabel(start));
                DataTime end = range[range.length - 1];
                xLabels.add(new DataTimeLabel(end));

                NCTimeMatcher tm = (NCTimeMatcher) rsc.getDescriptor()
                        .getTimeMatcher();

                if (tm != null) {
                    duration = tm.getGraphRange();
                    setGraphParameters();
                }

                long diff = end.getRefTime().getTime()
                        - start.getRefTime().getTime();

                for (int i = 1; i <= numIntervals - 1; i++) {
                    long startTime = start.getRefTime().getTime();
                    long tmp = (diff * i / numIntervals);

                    long newTime = startTime + tmp;
                    DataTime dtime = new DataTime(new Date(newTime));
                    xLabels.add(new DataTimeLabel(dtime));
                }
            }

            for (IGraphLabel<DataTime> label : xLabels) {
                label.setResource((AbstractVizResource<?, ?>) grsc);
            }

        }
        Collections.sort(xLabels, new GraphLabelComparator());
    }

    @Override
    protected void paintTitles(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        graphColor = descriptor.getGraphResource()
                .getCapability(ColorableCapability.class).getColor();

        RGB colorToUse = RGBColors.getRGBColor("white");

        // paint first x title and y title
        int i = 0, j = 0;
        for (IGraphableResource<?, ?> grsc : graphResource) {
            GhcdResource rsc = (GhcdResource) grsc;
            GhcdResourceData rscData = (GhcdResourceData) rsc.getResourceData();

            if (rsc.getProperties().isVisible()) {
                colorToUse = rscData.getDataColor();

                // paint x title
                paintXTitle(target, paintProps, graphAttrs.getxTitle(),
                        graphColor);

                // paint y title
                String rscTitle = rsc.getTitle().trim();
                String rscTitleLoc = rscData.getYTitlePosition().toUpperCase();
                String rscUnits = rscData.getYUnits().trim();
                String rscUnitsLoc = rscData.getYUnitsPosition().toUpperCase();

                String leftTitle = "";
                String rightTitle = "";

                if (GhcdUtil.TITLE_POSITION_LEFT.equalsIgnoreCase(rscTitleLoc)
                        && GhcdUtil.TITLE_POSITION_LEFT
                                .equalsIgnoreCase(rscUnitsLoc)) {
                    leftTitle = rscTitle + " " + rscUnits;
                    paintYTitle(target, paintProps, leftTitle, colorToUse, i);
                } else if (GhcdUtil.TITLE_POSITION_RIGHT
                        .equalsIgnoreCase(rscTitleLoc)
                        && GhcdUtil.TITLE_POSITION_RIGHT
                                .equalsIgnoreCase(rscUnitsLoc)) {
                    rightTitle = rscTitle + " " + rscUnits;
                    paintRhsYTitle(target, paintProps, rightTitle, colorToUse,
                            j);
                } else if (GhcdUtil.TITLE_POSITION_LEFT
                        .equalsIgnoreCase(rscTitleLoc)
                        && GhcdUtil.TITLE_POSITION_RIGHT
                                .equalsIgnoreCase(rscUnitsLoc)) {
                    leftTitle = rscTitle;
                    rightTitle = rscUnits;
                    paintYTitle(target, paintProps, leftTitle, colorToUse, i);
                    paintRhsYTitle(target, paintProps, rightTitle, colorToUse,
                            j);
                } else if (GhcdUtil.TITLE_POSITION_RIGHT
                        .equalsIgnoreCase(rscTitleLoc)
                        && GhcdUtil.TITLE_POSITION_LEFT
                                .equalsIgnoreCase(rscUnitsLoc)) {
                    leftTitle = rscUnits;
                    rightTitle = rscTitle;
                    paintYTitle(target, paintProps, leftTitle, colorToUse, i);
                    paintRhsYTitle(target, paintProps, rightTitle, colorToUse,
                            j);
                }

                if (!"".equals(leftTitle))
                    i++;
                if (!"".equals(rightTitle))
                    j++;
            }
        }

        paintTopLabels(target, paintProps);
        paintBorderRect(target, paintProps, xLabels);

    }

    protected void paintTopLabels(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        // Paint the top labels
        Date lastDataDate = null;
        boolean lastDataDateDrawn = false;
        for (IGraphableResource<?, ?> grsc : graphResource) {
            GhcdResource rsc = (GhcdResource) grsc;
            if (rsc == null) {
                continue;
            }

            GhcdResourceData rscData = (GhcdResourceData) rsc.getResourceData();

            if (rscData == null) {
                continue;
            } else if (rsc.getData().getData() == null) {
                continue;
            } else if (rsc.getData().getData().size() < 1) {
                continue;
            }

            if (rsc.getProperties().isVisible()) {
                if (graphAttrs.getDisplayLastDataDate()) {
                    if (lastDataDate == null
                            || rsc.getLastDataTime().getRefTime()
                                    .after(lastDataDate))
                        lastDataDate = rsc.getLastDataTime().getRefTime();
                }

                if (graphAttrs.getGraphTopTitle() != null
                        || !graphAttrs.getGraphTopTitle().isEmpty()) {
                    DrawableString topTitleStr = new DrawableString(
                            graphAttrs.getGraphTopTitle(),// rscData.getGraphTopTitle(),
                            graphColor);
                    topTitleStr.font = titleFont;
                    topTitleStr.textStyle = TextStyle.DROP_SHADOW;
                    topTitleStr.horizontalAlignment = HorizontalAlignment.LEFT;
                    topTitleStr.verticallAlignment = VerticalAlignment.BOTTOM;
                    topTitleStr.magnification = this.currentMagnification;
                    double x = graphExtent.getMinX();
                    double y = this.getExtent().getMinY() - 7;
                    topTitleStr.setCoordinates(x, y);

                    target.drawStrings(topTitleStr);
                }

                if (lastDataDate != null && !lastDataDateDrawn) {
                    DrawableString lastDataString = new DrawableString(
                            "Last Data " + topLabelSdf.format(lastDataDate),
                            graphColor);
                    lastDataString.font = titleFont;
                    lastDataString.textStyle = TextStyle.DROP_SHADOW;
                    lastDataString.horizontalAlignment = HorizontalAlignment.CENTER;
                    lastDataString.verticallAlignment = VerticalAlignment.BOTTOM;
                    lastDataString.magnification = this.currentMagnification;
                    double x = graphExtent.getMaxX() - 130
                            * paintProps.getZoomLevel()
                            - titleFont.getFontSize();
                    double y = this.getExtent().getMinY() - 3;
                    lastDataString.setCoordinates(x, y);

                    target.drawStrings(lastDataString);
                    lastDataDateDrawn = true;
                }
            }
        }
    }

    protected void paintXTitle(IGraphicsTarget target,
            PaintProperties paintProps, String title, RGB titleColor)
            throws VizException {

        if (title != null && !title.isEmpty()) {
            if (xLabelFormat.startsWith("mm")) {
                title = title + " (minutes)";
            } else if (xLabelFormat.startsWith("HH")) {
                title = title + " (hours)";
            } else if (xLabelFormat.startsWith("MMM")) {
                title = title + " (days)";
            }
        }

        DrawableString titleString = new DrawableString(title, titleColor);
        titleString.font = titleFont;
        titleString.textStyle = TextStyle.DROP_SHADOW;
        titleString.horizontalAlignment = HorizontalAlignment.CENTER;
        titleString.verticallAlignment = VerticalAlignment.TOP;
        titleString.magnification = this.currentMagnification;

        double x = graphExtent.getMinX() + graphExtent.getWidth() / 2;
        double y = graphExtent.getMaxY() + 25;
        titleString.setCoordinates(x, y);

        target.drawStrings(titleString);
    }

    protected void paintRhsYTitle(IGraphicsTarget target,
            PaintProperties paintProps, String title, RGB titleColor, int index)
            throws VizException {

        // Paint the titles
        double ratio = paintProps.getCanvasBounds().height
                / paintProps.getView().getExtent().getHeight();
        DrawableString titleString = new DrawableString(title, titleColor);
        titleString.font = titleFont;
        titleString.textStyle = TextStyle.DROP_SHADOW;
        titleString.horizontalAlignment = HorizontalAlignment.LEFT;
        titleString.verticallAlignment = VerticalAlignment.BOTTOM;
        titleString.rotation = 90;
        titleString.magnification = this.currentMagnification;
        int width = target.getStringsBounds(titleString).getBounds().width;
        int height = target.getStringsBounds(titleString, "H").getBounds().height * 2;
        double x = graphExtent.getMaxX() + 40 + height * (index);
        double y = graphExtent.getMaxY()
                - ((graphExtent.getHeight() - (width / ratio)) / 2);
        titleString.setCoordinates(x, y);

        target.drawStrings(titleString);
    }

    @Override
    protected void paintYTitle(IGraphicsTarget target,
            PaintProperties paintProps, String title, RGB titleColor, int index)
            throws VizException {
        // Paint the titles
        double ratio = paintProps.getCanvasBounds().height
                / paintProps.getView().getExtent().getHeight();
        DrawableString titleString = new DrawableString(title, titleColor);
        titleString.font = titleFont;
        titleString.textStyle = TextStyle.DROP_SHADOW;
        titleString.horizontalAlignment = HorizontalAlignment.LEFT;
        titleString.verticallAlignment = VerticalAlignment.BOTTOM;
        titleString.rotation = 90;
        titleString.magnification = this.currentMagnification;
        int width = target.getStringsBounds(titleString).getBounds().width;
        int height = target.getStringsBounds(titleString, "H").getBounds().height * 2;
        double x = graphExtent.getMinX() - 75 - height * (index);
        double y = graphExtent.getMaxY()
                - ((graphExtent.getHeight() - (width / ratio)) / 2);
        titleString.setCoordinates(x, y);

        target.drawStrings(titleString);
    }

    @Override
    protected void paintDataTimeUnits(IGraphicsTarget target,
            PaintProperties paintProps, List<IGraphLabel<DataTime>> xLabels)
            throws VizException {

        if (graphAttrs.getDisplayXLabels()) {
            List<DrawableString> strings = new ArrayList<DrawableString>(
                    xLabels.size());

            for (IGraphLabel<DataTime> xLabel : xLabels) {
                double val = xLabel.getDiscreteValue();
                Date date = xLabel.getUnderlyingObject().getRefTime();

                RGB labelColor = graphColor;
                DrawableString parameters = new DrawableString(
                        sdf.format(date), labelColor);
                parameters.font = unitsFont;
                parameters.horizontalAlignment = HorizontalAlignment.CENTER;
                parameters.verticallAlignment = VerticalAlignment.TOP;
                parameters.magnification = this.currentMagnification;

                double offset = yAxisPlacer.getPixelLoc(val);

                Coordinate loc = new Coordinate(graphExtent.getMinX() + offset,
                        graphExtent.getMaxY(), 0);

                parameters.setCoordinates(loc.x, loc.y, loc.z);

                strings.add(parameters);
            }
            target.drawStrings(strings);
        }
        paintTicks(target, paintProps, xLabels);

    }

    private void paintBorderRect(IGraphicsTarget target,
            PaintProperties paintProps, List<IGraphLabel<DataTime>> xLabels)
            throws VizException {

        target.drawRect(graphExtent, graphColor, 1, 1);
    }

    private void paintTicks(IGraphicsTarget target, PaintProperties paintProps,
            List<IGraphLabel<DataTime>> xLabels) throws VizException {

        // paint X Axes ticks
        if (xLabels != null && xLabels.size() > 0) {
            Date start = xLabels.get(0).getUnderlyingObject().getRefTime();
            Date end = xLabels.get(xLabels.size() - 1).getUnderlyingObject()
                    .getRefTime();

            for (int i = 0; i < xLabels.size(); i++) {

                double val = xLabels.get(i).getDiscreteValue();
                double offset = yAxisPlacer.getPixelLoc(val);

                // paint label ticks on x-axis on bottom of graph
                target.drawLine(graphExtent.getMinX() + offset,
                        graphExtent.getMaxY() - 12, 0, graphExtent.getMinX()
                                + offset, graphExtent.getMaxY(), 0, graphColor,
                        1, LineStyle.SOLID);

                // paint label ticks on x-axis on top of graph
                target.drawLine(graphExtent.getMinX() + offset,
                        graphExtent.getMinY() + 12, 0, graphExtent.getMinX()
                                + offset, graphExtent.getMinY(), 0, graphColor,
                        1, LineStyle.SOLID);

                if (paintxAxisMajorMinorTicks) {
                    // paint major tick marks
                    if (i < (xLabels.size() - 1)) {
                        start = xLabels.get(i).getUnderlyingObject()
                                .getRefTime();

                        Date newEnd = xLabels.get(i + 1).getUnderlyingObject()
                                .getRefTime();

                        if (start.getTime() < newEnd.getTime()) {

                            long startTime = start.getTime();
                            long newTime = 0;

                            for (int j = 0; j < xLabelInterval
                                    / xMajorTickInterval
                                    && newTime < newEnd.getTime(); j++) {
                                long tmp = (long) (xMajorTickInterval * 60000);
                                newTime = startTime + tmp;

                                if (new Date(newTime).before(end)) {
                                    double offsetMjTick = yAxisPlacer
                                            .getPixelLoc(newTime);
                                    target.drawLine(graphExtent.getMinX()
                                            + offsetMjTick,
                                            graphExtent.getMaxY() - 12, 0,
                                            graphExtent.getMinX()
                                                    + offsetMjTick,
                                            graphExtent.getMaxY(), 0,
                                            graphColor, 1, LineStyle.SOLID);

                                    target.drawLine(graphExtent.getMinX()
                                            + offsetMjTick,
                                            graphExtent.getMinY() + 12, 0,
                                            graphExtent.getMinX()
                                                    + offsetMjTick,
                                            graphExtent.getMinY(), 0,
                                            graphColor, 1, LineStyle.SOLID);
                                }
                                startTime = newTime;
                            }

                            // paint minor tick marks
                            startTime = start.getTime();
                            newTime = 0;
                            for (int j = 0; j < xLabelInterval
                                    / xMinorTickInterval
                                    && newTime < newEnd.getTime(); j++) {
                                long tmp = (long) (xMinorTickInterval * 60000);
                                newTime = startTime + tmp;

                                if (new Date(newTime).before(end)) {
                                    double offsetMnTick = yAxisPlacer
                                            .getPixelLoc(newTime);
                                    target.drawLine(graphExtent.getMinX()
                                            + offsetMnTick,
                                            graphExtent.getMaxY() - 6, 0,
                                            graphExtent.getMinX()
                                                    + offsetMnTick,
                                            graphExtent.getMaxY(), 0,
                                            graphColor, 1, LineStyle.SOLID);

                                    target.drawLine(graphExtent.getMinX()
                                            + offsetMnTick,
                                            graphExtent.getMinY() + 6, 0,
                                            graphExtent.getMinX()
                                                    + offsetMnTick,
                                            graphExtent.getMinY(), 0,
                                            graphColor, 1, LineStyle.SOLID);
                                }
                                startTime = newTime;
                            }
                        }
                    }
                }
            }
        }

        // paint y axes major ticks
        double[] offsets = xAxisPlacer.placeAxes(xAxes);

        if (offsets.length > 0) {
            double lastYLabelOffset = offsets[offsets.length - 1];

            for (int i = 0; i < offsets.length; ++i) {
                double offset = offsets[i];

                Coordinate[] coords = xAxes[i].getCoordinates();
                if (coords[0].y < graphExtent.getMinY()) {
                    continue;
                }

                try {
                    target.drawLine(graphExtent.getMinX(),
                            graphExtent.getMinY() + offset, 0,
                            graphExtent.getMinX() + 15, graphExtent.getMinY()
                                    + offset, 0, graphColor, 1, LineStyle.SOLID);
                    target.drawLine(graphExtent.getMaxX(),
                            graphExtent.getMinY() + offset, 0,
                            graphExtent.getMaxX() - 15, graphExtent.getMinY()
                                    + offset, 0, graphColor, 1, LineStyle.SOLID);
                } catch (VizException e) {
                }

            }

            // paint y axes minor ticks
            if (GhcdUtil.YSCALE_TYPE_LINEAR.equalsIgnoreCase(graphAttrs
                    .getyScaleType())) {
                // paint y axes minor ticks for linear scale type
                int numTicks = graphAttrs.getyNumTicks();
                for (int i = 0; i < numTicks; ++i) {

                    double offset = (graphExtent.getMaxY() - graphExtent
                            .getMinY()) / (numTicks - 1);

                    target.drawLine(graphExtent.getMinX(),
                            graphExtent.getMinY() + offset * i, 0,
                            graphExtent.getMinX() + 10, graphExtent.getMinY()
                                    + offset * i, 0, graphColor, 1,
                            LineStyle.SOLID);

                    target.drawLine(graphExtent.getMaxX(),
                            graphExtent.getMinY() + offset * i, 0,
                            graphExtent.getMaxX() - 10, graphExtent.getMinY()
                                    + offset * i, 0, graphColor, 1,
                            LineStyle.SOLID);

                }
            } else {

                // paint y axes minor ticks for log scale type
                for (int i = 0; i < xAxes.length; i++) {
                    IAxis axis = xAxes[i];

                    double yValue = axis.getDiscreteValue();

                    double delta = findNiceDelta(yValue, 10);

                    if ((i + 1) < xAxes.length) {
                        IAxis nextAxis = xAxes[i + 1];

                        while (yValue < nextAxis.getDiscreteValue()) {
                            yValue = yValue + (delta * 10);
                            double tickoffset = xAxisPlacer.getPixelLoc(yValue);

                            double y = graphExtent.getMinY() + lastYLabelOffset
                                    - tickoffset;
                            if (y > graphExtent.getMaxY()) {
                                continue;
                            }

                            try {
                                target.drawLine(
                                        graphExtent.getMinX(),
                                        graphExtent.getMinY()
                                                + lastYLabelOffset - tickoffset,
                                        0,
                                        graphExtent.getMinX() + 10,
                                        graphExtent.getMinY()
                                                + lastYLabelOffset - tickoffset,
                                        0, graphColor, 1, LineStyle.SOLID);
                                target.drawLine(
                                        graphExtent.getMaxX(),
                                        graphExtent.getMinY()
                                                + lastYLabelOffset - tickoffset,
                                        0,
                                        graphExtent.getMaxX() - 10,
                                        graphExtent.getMinY()
                                                + lastYLabelOffset - tickoffset,
                                        0, graphColor, 1, LineStyle.SOLID);
                            } catch (VizException e) {

                            }
                        }
                    }
                }
            }
        }
    }

    double findNiceDelta(double yVal, int count) {
        double step = yVal / count, order = Math.pow(10,
                Math.floor(Math.log10(step))), delta = (int) (step / order + 0.5);

        double ndex[] = { 1, 1.5f, 2, 2.5f, 5, 10 };
        int ndexLenght = ndex.length;
        for (int i = ndexLenght - 2; i > 0; --i)
            if (delta > ndex[i])
                return ndex[i + 1] * order;
        return delta * order;
    }

    public void setGraphParameters() {

        // for (IGraphableResource<?, ?> grsc : graphResource) {
        // if (grsc instanceof GhcdResource) {
        // GhcdResource rsc = (GhcdResource) grsc;
        // GhcdResourceData rscData = (GhcdResourceData) rsc
        // .getResourceData();
        // graphAttrs = new GraphAttributes(rscData);
        // rscData.setGraphAttr(graphAttrs);
        // }
        // }

        // set title font
        String titleFontName = graphAttrs.getTitleFont();
        String titleFontStyle = graphAttrs.getTitleStyle();
        float titleFontSize = (float) Integer.parseInt(graphAttrs
                .getTitleFontSize());

        IFont derivedFont = target.initializeFont(titleFontName, titleFontSize,
                new IFont.Style[] {});

        if (titleFontStyle.equalsIgnoreCase("Bold")) {
            derivedFont = target.initializeFont(titleFontName, titleFontSize,
                    new IFont.Style[] { IFont.Style.BOLD });
        }

        if (derivedFont != null) {
            titleFont = derivedFont;
            titleFont.setSmoothing(false);
            titleFont.setScaleFont(false);
        }

        // set units font
        String unitsFontName = graphAttrs.getUnitsFont();
        String unitsFontStyle = graphAttrs.getUnitsStyle();
        float unitsFontSize = (float) Integer.parseInt(graphAttrs
                .getUnitsFontSize());

        derivedFont = target.initializeFont(unitsFontName, unitsFontSize,
                new IFont.Style[] {});

        if (unitsFontStyle.equalsIgnoreCase("Bold")) {
            derivedFont = target.initializeFont(unitsFontName, unitsFontSize,
                    new IFont.Style[] { IFont.Style.BOLD });
        }

        if (derivedFont != null) {
            unitsFont = derivedFont;
            unitsFont.setSmoothing(false);
            unitsFont.setScaleFont(false);
        }

        // set scaling parameters
        HashMap<Integer, XAxisScaleElement> xAxisScaleMap = graphAttrs
                .getxAxisScale().getxAxisScaleMap();

        if (!xAxisScaleMap.containsKey(duration)) {
            paintxAxisMajorMinorTicks = false;
        }

        int tmpDuration = duration;
        if (duration <= 2) {
            tmpDuration = 2;
        } else if (duration <= 6) {
            tmpDuration = 6;
        } else if (duration <= 12) {
            tmpDuration = 12;
        } else if (duration <= 24) {
            tmpDuration = 24;
        } else if (duration <= 72) {
            tmpDuration = 72;
        } else if (duration <= 168) {
            tmpDuration = 168;
        } else if (duration > 168) {
            tmpDuration = 720;
        }

        XAxisScaleElement xascElmt = xAxisScaleMap.get(tmpDuration);

        if (xascElmt != null) {

            xLabelFormat = xascElmt.getLabelFormat();
            xLabelInterval = 1.0f * xascElmt.getLabelInterval();
            xMajorTickInterval = 1.0f * xascElmt.getMajorTickInterval();
            xMinorTickInterval = 1.0f * xascElmt.getMinorTickInterval();

            if (duration != 2 && duration != 6 && duration != 12
                    && duration != 24 && duration != 72 && duration != 168
                    && duration != 720) {
                xMajorTickInterval = 1.0f * xLabelInterval;
                xMinorTickInterval = 1.0f * xLabelInterval;
            }

            numIntervals = (int) (duration * 60 / xLabelInterval);
        } else {
            numIntervals = 4;
        }

    }

}
