package gov.noaa.nws.ncep.viz.overlays.resources;

import java.io.File;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractNameGenerator;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

import gov.noaa.nws.ncep.viz.common.RGBColorAdapter;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.misc.IMiscResourceData;

/**
 *
 * Resource Data for displaying PGEN products as static overlays in CAVE
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ??/??/??                            Initial creation
 * Feb 28, 2019 7752       tjensen     Added dataURI
 *
 * </pre>
 *
 * @author tjensen
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "PgenOverlayResourceData")
public class PgenStaticOverlayResourceData extends AbstractNatlCntrsResourceData
        implements IMiscResourceData, INatlCntrsResourceData {

    @XmlElement
    private String pgenStaticProductLocation;

    @XmlElement
    private String pgenStaticProductName;

    @XmlElement
    private String pgenStaticProductDataURI;

    // override the original colors saved in the xml file
    @XmlElement
    protected boolean monoColorEnable;

    @XmlElement
    @XmlJavaTypeAdapter(RGBColorAdapter.class)
    private RGB color = new RGB(255, 255, 255);

    public PgenStaticOverlayResourceData() {
        super();
        this.nameGenerator = new AbstractNameGenerator() {

            @Override
            public String getName(AbstractVizResource<?, ?> resource) {
                return pgenStaticProductName;
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public PgenStaticOverlayResource constructResource(
            LoadProperties loadProperties, IDescriptor descriptor)
            throws VizException {
        return new PgenStaticOverlayResource(this, loadProperties);
    }

    public String getPgenStaticProductLocation() {
        return pgenStaticProductLocation;
    }

    public void setPgenStaticProductLocation(String pgenStaticProductLocation) {
        this.pgenStaticProductLocation = pgenStaticProductLocation;
    }

    public String getPgenStaticProductName() {
        return pgenStaticProductName;
    }

    public void setPgenStaticProductName(String pgenStaticProductName) {
        this.pgenStaticProductName = pgenStaticProductName;
    }

    public String getPgenStaticProductDataURI() {
        return pgenStaticProductDataURI;
    }

    public void setPgenStaticProductDataURI(String pgenStaticProductDataURI) {
        this.pgenStaticProductDataURI = pgenStaticProductDataURI;
    }

    public boolean getMonoColorEnable() {
        return monoColorEnable;
    }

    public void setMonoColorEnable(boolean monoColorEnable) {
        this.monoColorEnable = monoColorEnable;
    }

    public RGB getColor() {
        return color;
    }

    public void setColor(RGB color) {
        this.color = color;
        setLegendColor(color);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((color == null) ? 0 : color.hashCode());
        result = prime * result + (monoColorEnable ? 1231 : 1237);
        result = prime * result + ((pgenStaticProductDataURI == null) ? 0
                : pgenStaticProductDataURI.hashCode());
        result = prime * result + ((pgenStaticProductLocation == null) ? 0
                : pgenStaticProductLocation.hashCode());
        result = prime * result + ((pgenStaticProductName == null) ? 0
                : pgenStaticProductName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PgenStaticOverlayResourceData other = (PgenStaticOverlayResourceData) obj;
        if (color == null) {
            if (other.color != null) {
                return false;
            }
        } else if (!color.equals(other.color)) {
            return false;
        }
        if (monoColorEnable != other.monoColorEnable) {
            return false;
        }
        if (pgenStaticProductDataURI == null) {
            if (other.pgenStaticProductDataURI != null) {
                return false;
            }
        } else if (!pgenStaticProductDataURI
                .equals(other.pgenStaticProductDataURI)) {
            return false;
        }
        if (pgenStaticProductLocation == null) {
            if (other.pgenStaticProductLocation != null) {
                return false;
            }
        } else if (!pgenStaticProductLocation
                .equals(other.pgenStaticProductLocation)) {
            return false;
        }
        if (pgenStaticProductName == null) {
            if (other.pgenStaticProductName != null) {
                return false;
            }
        } else if (!pgenStaticProductName.equals(other.pgenStaticProductName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return pgenStaticProductLocation + File.separator
                + pgenStaticProductName;
    }

    @Override
    public MiscRscAttrs getMiscResourceAttrs() {
        MiscRscAttrs attrs = new MiscRscAttrs(1);

        attrs.addAttr(new MiscResourceAttr("monoColorEnable",
                "Override PGEN Element Colors", EditElement.CHECK_BOX, 1));
        attrs.addAttr(new MiscResourceAttr("color", "",
                EditElement.COLOR_PALLETE, 1));

        return attrs;
    }

}
