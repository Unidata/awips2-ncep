/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
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
package gov.noaa.nws.ncep.ui.pgen.rsc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;

import gov.noaa.nws.ncep.ui.pgen.controls.PgenCommandManager;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;
import gov.noaa.nws.ncep.ui.pgen.elements.labeledlines.Label;
import gov.noaa.nws.ncep.ui.pgen.filter.CategoryFilter;
import gov.noaa.nws.ncep.ui.pgen.filter.ElementFilter;
import gov.noaa.nws.ncep.ui.pgen.productmanage.ProductManageDialog;

/**
 * Container for all PGEN Resources for a given session.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 22, 2021 95362      tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */

public class PgenResourceList {

    private List<PgenResource> resourceList = new ArrayList<>();

    public PgenResourceList() {
        // TODO Auto-generated constructor stub
    }

    public PgenResourceList(List<PgenResource> resourceList) {
        this.resourceList = resourceList;
    }

    /**
     * @param pgenResource
     */
    public PgenResourceList(PgenResource pgenResource) {
        List<PgenResource> newList = new ArrayList<>();
        newList.add(pgenResource);
        this.resourceList = newList;
    }

    /**
     * @param newResource
     */
    public void add(PgenResource newResource) {
        resourceList.add(newResource);
    }

    /**
     * @param de
     */
    public void addElement(AbstractDrawableComponent de) {
        for (PgenResource pgr : resourceList) {
            pgr.addElement(de);
        }
    }

    /**
     * @param elems
     */
    public void addElements(List<AbstractDrawableComponent> elems) {
        for (PgenResource pgr : resourceList) {
            pgr.addElements(elems);
        }
    }

    /**
     * @param elementFilter
     */
    public void addFilter(ElementFilter elementFilter) {
        for (PgenResource pgr : resourceList) {
            pgr.getFilters().addFilter(elementFilter);
        }
    }

    /**
     * @param ptIndex
     */
    public void addPtSelected(int ptIndex) {
        for (PgenResource pgr : resourceList) {
            pgr.addPtSelected(ptIndex);
        }
    }

    /**
     * @param adc
     */
    public void addSelected(AbstractDrawableComponent adc) {
        for (PgenResource pgr : resourceList) {
            pgr.addSelected(adc);
        }
    }

    /**
     * @param adcList
     */
    public void addSelected(List<? extends AbstractDrawableComponent> adcList) {
        for (PgenResource pgr : resourceList) {
            pgr.addSelected(adcList);
        }
    }

    /**
     * @param prod
     * @return
     */
    public String buildActivityLabel(Product prod) {
        return resourceList.get(0).buildActivityLabel(prod);
    }

    /**
     *
     */
    public void closeDialogs() {
        for (PgenResource pgr : resourceList) {
            pgr.closeDialogs();
        }
    }

    public boolean contains(PgenResource pgr) {
        return resourceList.contains(pgr);
    }

    /**
     *
     */
    public void deactivatePgenTools() {
        for (PgenResource pgr : resourceList) {
            pgr.deactivatePgenTools();
        }
    }

    /**
     * @param des
     * @param point1
     * @param point2
     */
    public void deleteElementPart(Line des, Coordinate point1,
            Coordinate point2) {
        for (PgenResource pgr : resourceList) {
            pgr.deleteElementPart(des, point1, point2);
        }

    }

    /**
     *
     */
    public void deleteSelectedElements() {
        for (PgenResource pgr : resourceList) {
            pgr.deleteSelectedElements();
        }
    }

    /**
     * @param loc
     * @param coordinate
     * @param coordinate2
     * @return
     */
    public double distanceFromLineSegment(Coordinate loc, Coordinate coordinate,
            Coordinate coordinate2) {
        return PgenResource.distanceFromLineSegment(loc, coordinate,
                coordinate2);
    }

    /**
     * @return
     */
    public Layer getActiveLayer() {
        return resourceList.get(0).getActiveLayer();
    }

    /**
     * @return
     */
    public Product getActiveProduct() {
        return resourceList.get(0).getActiveProduct();
    }

    /**
     * @return
     */
    public List<AbstractDrawableComponent> getAllSelected() {
        return resourceList.get(0).getAllSelected();
    }

    /**
     * @return
     */
    public PgenCommandManager getCommandMgr() {
        return resourceList.get(0).getCommandMgr();
    }

    /**
     * @return
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return resourceList.get(0).getCoordinateReferenceSystem();
    }

    /**
     * @return
     */
    public MapDescriptor getDescriptor() {
        return resourceList.get(0).getDescriptor();
    }

    /**
     * @param elSelected
     * @param firstDown
     * @return
     */
    public double getDistance(AbstractDrawableComponent adc, Coordinate loc) {
        return resourceList.get(0).getDistance(adc, loc);
    }

    /**
     * @return
     */
    public int getMaxDistToSelect() {
        return resourceList.get(0).getMaxDistToSelect();
    }

    /**
     * @param loc
     * @return
     */
    public AbstractDrawableComponent getNearestComponent(Coordinate loc) {
        return resourceList.get(0).getNearestComponent(loc);
    }

    public AbstractDrawableComponent getNearestComponent(Coordinate point,
            ElementFilter filter, boolean applyCatFilter) {
        return resourceList.get(0).getNearestComponent(point, filter,
                applyCatFilter);
    }

    /**
     * @param loc
     * @return
     */
    public DrawableElement getNearestElement(Coordinate loc) {
        return resourceList.get(0).getNearestElement(loc);
    }

    /**
     * @param point
     * @param dec
     * @return
     */
    public DrawableElement getNearestElement(Coordinate point,
            DECollection dec) {
        return resourceList.get(0).getNearestElement(point, dec);
    }

    public DrawableElement getNearestElement(Coordinate point, DECollection dec,
            DrawableElement nearestDe) {
        return resourceList.get(0).getNearestElement(point, dec, nearestDe);
    }

    /**
     * @param loc
     * @param acceptFilter
     * @return
     */
    public DrawableElement getNearestElement(Coordinate loc,
            ElementFilter filter) {
        return resourceList.get(0).getNearestElement(loc, filter);
    }

    /**
     * @return
     */
    public ProductManageDialog getProductManageDlg() {
        return resourceList.get(0).getProductManageDlg();
    }

    /**
     * @return
     */
    public List<Product> getProducts() {
        return resourceList.get(0).getProducts();
    }

    /**
     * @return
     */
    public ResourceProperties getProperties() {
        return resourceList.get(0).getProperties();
    }

    /**
     * @return
     */
    public PgenResourceData getResourceData() {
        return resourceList.get(0).getResourceData();

    }

    /**
     * @return the resourceList
     */
    public List<PgenResource> getResourceList() {
        return resourceList;
    }

    /**
     * @return
     */
    public AbstractDrawableComponent getSelectedComp() {
        return resourceList.get(0).getSelectedComp();
    }

    /**
     * @return
     */
    public DrawableElement getSelectedDE() {
        return resourceList.get(0).getSelectedDE();
    }

    /**
     * @return
     */
    public boolean isEditable() {
        return resourceList.get(0).isEditable();
    }

    /**
     * @return
     */
    public boolean isEmpty() {
        return resourceList.isEmpty();
    }

    public boolean matches(PgenResourceList other) {
        boolean match = true;

        if (resourceList.size() != other.resourceList.size()) {
            match = false;
        } else {
            for (PgenResource resource : resourceList) {
                if (!other.resourceList.contains(resource)) {
                    match = false;
                }
            }
        }

        return match;
    }

    /**
     * @param abstractDrawableComponent
     * @param verifySymbol
     */
    public void registerSelectedSymbol(AbstractDrawableComponent adc,
            Symbol sym) {
        for (PgenResource pgr : resourceList) {
            pgr.registerSelectedSymbol(adc, sym);
        }
    }

    /**
     *
     */
    public void removeAllActiveDEs() {
        for (PgenResource pgr : resourceList) {
            pgr.removeAllActiveDEs();
        }
    }

    /**
     * @param elem
     */
    public void removeElement(AbstractDrawableComponent elem) {
        for (PgenResource pgr : resourceList) {
            pgr.removeElement(elem);
        }
    }

    /**
     * @param elementFilter
     */
    public void removeFilter(ElementFilter elementFilter) {
        for (PgenResource pgr : resourceList) {
            pgr.getFilters().removeFilter(elementFilter);
        }
    }

    /**
     *
     */
    public void removeGhostLine() {
        for (PgenResource pgr : resourceList) {
            pgr.removeGhostLine();
        }
    }

    /**
     *
     */
    public void removePtsSelected() {
        for (PgenResource pgr : resourceList) {
            pgr.removePtsSelected();
        }
    }

    /**
     *
     */
    public void removeSelected() {
        for (PgenResource pgr : resourceList) {
            pgr.removeSelected();
        }
    }

    /**
     * @param el
     */
    public boolean removeSelected(AbstractDrawableComponent el) {
        boolean found = false;
        for (PgenResource pgr : resourceList) {
            if (pgr.getAllSelected().contains(el)) {
                pgr.removeSelected(el);
                found = true;
            }
        }
        return found;
    }

    /**
     * @param ll
     * @param newll
     */
    public void replaceElement(AbstractDrawableComponent oldde,
            AbstractDrawableComponent newde) {
        for (PgenResource pgr : resourceList) {
            pgr.replaceElement(oldde, newde);
        }
    }

    /**
     * @param object
     * @param oldList
     * @param newList
     */
    public void replaceElements(DECollection parent,
            ArrayList<AbstractDrawableComponent> oldList,
            ArrayList<AbstractDrawableComponent> newList) {
        for (PgenResource pgr : resourceList) {
            pgr.replaceElements(parent, oldList, newList);
        }
    }

    /**
     * @param old
     * @param newLines
     */
    public void replaceElements(List<AbstractDrawableComponent> oldde,
            List<AbstractDrawableComponent> newde) {
        for (PgenResource pgr : resourceList) {
            pgr.replaceElements(oldde, newde);
        }
    }

    /**
     * @param newTxtList
     */
    public void replaceSelected(List<AbstractDrawableComponent> newTxtList) {
        for (PgenResource pgr : resourceList) {
            List<AbstractDrawableComponent> oldList = new ArrayList<>();
            oldList.addAll(pgr.getAllSelected());
            pgr.replaceElements(oldList, newTxtList);
        }
    }

    /**
     * @param parent
     */
    public void resetADC(AbstractDrawableComponent adc) {
        for (PgenResource pgr : resourceList) {
            pgr.resetADC(adc);
        }
    }

    /**
     *
     */
    public void resetAllElements() {
        for (PgenResource pgr : resourceList) {
            pgr.resetAllElements();
        }
    }

    /**
     * @param el
     */
    public void resetElement(DrawableElement el) {
        for (PgenResource pgr : resourceList) {
            pgr.resetElement(el);
        }
    }

    /**
     * @param pgenType
     * @return
     */
    public int selectObj(String pgenType) {
        int total = 0;
        for (PgenResource pgr : resourceList) {
            total += pgr.selectObj(pgenType);
        }
        return total;

    }

    /**
     * @param layer
     */
    public void setActiveLayer(Layer layer) {
        for (PgenResource pgr : resourceList) {
            pgr.setActiveLayer(layer);
        }
    }

    /**
     * @param categoryFilter
     */
    public void setCatFilter(CategoryFilter catFilter) {
        for (PgenResource pgr : resourceList) {
            pgr.setCatFilter(catFilter);
        }
    }

    /**
     *
     */
    public void setDefaultPtsSelectedColor() {
        for (PgenResource pgr : resourceList) {
            pgr.setDefaultPtsSelectedColor();
        }
    }

    /**
     * @param ghost
     */
    public void setGhostLine(AbstractDrawableComponent ghost) {
        for (PgenResource pgr : resourceList) {
            pgr.setGhostLine(ghost);
        }
    }

    /**
     * @param ghostLabel
     */
    public void setGhostLine(Label ghostLabel) {
        for (PgenResource pgr : resourceList) {
            pgr.setGhostLine(ghostLabel);
        }
    }

    /**
     * @param white
     */
    public void setPtsSelectedColor(Color clr) {
        for (PgenResource pgr : resourceList) {
            pgr.setPtsSelectedColor(clr);
        }
    }

    /**
     * @param resourceList
     *            the resourceList to set
     */
    public void setResourceList(List<PgenResource> resourceList) {
        this.resourceList = resourceList;
    }

    /**
     * @param nadc
     */
    public void setSelected(AbstractDrawableComponent nadc) {
        for (PgenResource pgr : resourceList) {
            pgr.setSelected(nadc);
        }
    }

    /**
     *
     */
    public void startProductManage() {
        resourceList.get(0).startProductManage();
    }

}
