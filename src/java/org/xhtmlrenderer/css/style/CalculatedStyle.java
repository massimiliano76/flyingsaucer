/*
 * CalculatedStyle.java
 * Copyright (c) 2004 Patrick Wright, Torbj�rn Gannholm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package org.xhtmlrenderer.css.style;

import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValueList;
import org.xhtmlrenderer.css.Border;
import org.xhtmlrenderer.css.RuleNormalizer;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.ValueConstants;
import org.xhtmlrenderer.css.newmatch.CascadedStyle;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.value.BorderColor;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.XRRuntimeException;

import java.awt.*;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;


/**
 * Description of the Class
 *
 * @author empty
 */
public class CalculatedStyle {

    /**
     * The parent-style we inherit from
     */
    private CalculatedStyle _parent;

    /**
     * the matched properties at the base of this
     */
    private CascadedStyle _matched;

    /** The styles matched to our owner element. */
    //private List _matchedProps;

    /**
     * The main Map of XRProperties keyed by property name, after
     * cascade/inherit takes place. This is the map we look up properties with.
     * Do NOT call clear() (haha).
     */
    private Map _derivedPropertiesByName;

    /**
     * The derived border width for this RuleSet
     */
    private Border _drvBorderWidth;

    /**
     * The derived margin width for this RuleSet
     */
    private Border _drvMarginWidth;

    /**
     * The derived padding width for this RuleSet
     */
    private Border _drvPaddingWidth;

    /**
     * The derived background color value for this RuleSet
     */
    private Color _drvBackgroundColor;

    /**
     * The derived border color value for this RuleSet
     */
    private BorderColor _drvBorderColor;

    /**
     * The derived Color value for this RuleSet
     */
    private Color _drvColor;


    /**
     * Constructor for the CalculatedStyle object.
     * To get a derived style, use the Styler objects getDerivedStyle which will cache styles
     *
     * @param parent  PARAM
     * @param matched PARAM
     */
    CalculatedStyle(CalculatedStyle parent, CascadedStyle matched) {
        this();
        _parent = parent;
        _matched = matched;

        derive();
    }


    /**
     * Constructor for the CalculatedStyle object
     */
    protected CalculatedStyle() {
        _derivedPropertiesByName = new TreeMap();
    }


    /**
     * Returns true if property has been defined in this style.
     *
     * @param propName PARAM
     * @return Returns
     */
    public boolean hasProperty(String propName) {
        return _derivedPropertiesByName.get(propName) != null;
    }


    /**
     * Returns a XRProperty by name. Because we are a derived style, the
     * property will already be resolved at this point--the method is
     * synchronized in order to allow this resolution to happen safely. Thus, on
     * this XRProperty you can call actualValue() to get something meaningful.
     *
     * @param propName PARAM
     * @return Returns
     */
    public DerivedProperty propertyByName(String propName) {
        DerivedProperty prop = (DerivedProperty) _derivedPropertiesByName.get(propName);

        // but the property may not be defined for this Element
        if (prop == null) {
            DerivedValue val = null;

            // if it is inheritable (like color) and we are not root, ask our parent
            // for the value
            if (CSSName.propertyInherits(propName) && _parent != null && (prop = _parent.propertyByName(propName)) != null) {
                // get a copy, which is always a calculated value!
                prop = prop.copyForInherit();
            } else {
                // otherwise, use the initial value (defined by the CSS2 Spec)
                String initialValue = CSSName.initialValue(propName);
                if (initialValue == null) {
                    throw new XRRuntimeException("Property '" + propName + "' has no initial values assigned. Check CSSName declarations.");
                }
                initialValue = RuleNormalizer.convertIdent(propName, initialValue);
                org.xhtmlrenderer.css.impl.DefaultCSSPrimitiveValue cssval = new org.xhtmlrenderer.css.impl.DefaultCSSPrimitiveValue(initialValue);
                //a default value should always be absolute?
                DerivedValue xrVal = new DerivedValue(cssval, _parent);
                prop = new DerivedProperty(propName, xrVal);
            }
            _derivedPropertiesByName.put(propName, prop);
        }
        //prop.resolveValue( _parent );
        return prop;
    }

    /**
     * Converts to a String representation of the object.
     *
     * @return A string representation of the object.
     */
    public String toString() {
        return _derivedPropertiesByName.keySet().toString();
    }

    /**
     * Implemented for the DOMInspector of HTMLTest. Might be useful for other
     * things too
     *
     * @return The availablePropertyNames value
     */
    public java.util.Set getAvailablePropertyNames() {
        return _derivedPropertiesByName.keySet();
    }


    /**
     * Convenience property accessor; returns a Border initialized with the
     * four-sided border width. Uses the actual value (computed actual value)
     * for this element.
     *
     * @return The borderWidth value
     */
    public Border getBorderWidth() {
        if (_drvBorderWidth == null) {
            Border border = new Border();
            // ASK: why is Josh forcing to an int in CSSAccessor? don't we want float/pixels?
            border.top = (int) propertyByName(CSSName.BORDER_WIDTH_TOP).computedValue().asFloat();
            border.bottom = (int) propertyByName(CSSName.BORDER_WIDTH_BOTTOM).computedValue().asFloat();
            border.left = (int) propertyByName(CSSName.BORDER_WIDTH_LEFT).computedValue().asFloat();
            border.right = (int) propertyByName(CSSName.BORDER_WIDTH_RIGHT).computedValue().asFloat();
            _drvBorderWidth = border;
        }
        return _drvBorderWidth;
    }


    /**
     * Convenience property accessor; returns a Border initialized with the
     * four-sided margin width. Uses the actual value (computed actual value)
     * for this element.
     *
     * @return The marginWidth value
     */
    public Border getMarginWidth() {
        if (_drvMarginWidth == null) {
            Border border = new Border();
            // ASK: why is Josh forcing to an int in CSSAccessor? don't we want float/pixels?
            border.top = (int) propertyByName(CSSName.MARGIN_TOP).computedValue().asFloat();
            border.bottom = (int) propertyByName(CSSName.MARGIN_BOTTOM).computedValue().asFloat();
            border.left = (int) propertyByName(CSSName.MARGIN_LEFT).computedValue().asFloat();
            border.right = (int) propertyByName(CSSName.MARGIN_RIGHT).computedValue().asFloat();
            _drvMarginWidth = border;
        }
        return _drvMarginWidth;
    }


    /**
     * Convenience property accessor; returns a Border initialized with the
     * four-sided padding width. Uses the actual value (computed actual value)
     * for this element.
     *
     * @return The paddingWidth value
     */
    public Border getPaddingWidth() {
        if (_drvPaddingWidth == null) {
            Border border = new Border();
            // ASK: why is Josh forcing to an int in CSSAccessor? don't we want float/pixels?
            border.top = (int) propertyByName(CSSName.PADDING_TOP).computedValue().asFloat();
            border.bottom = (int) propertyByName(CSSName.PADDING_BOTTOM).computedValue().asFloat();
            border.left = (int) propertyByName(CSSName.PADDING_LEFT).computedValue().asFloat();
            border.right = (int) propertyByName(CSSName.PADDING_RIGHT).computedValue().asFloat();
            _drvPaddingWidth = border;
        }
        return _drvPaddingWidth;
    }


    /**
     * Convenience property accessor; returns a Color initialized with the
     * background color value; Uses the actual value (computed actual value) for
     * this element.
     *
     * @return The backgroundColor value
     */
    public Color getBackgroundColor() {
        if (_drvBackgroundColor == null) {
            _drvBackgroundColor = propertyByName(CSSName.BACKGROUND_COLOR).computedValue().asColor();
            XRLog.cascade(Level.FINEST, "Background color: " + _drvBackgroundColor);
        }
        return _drvBackgroundColor;
    }


    /**
     * Convenience property accessor; returns a BorderColor initialized with the
     * four-sided border color. Uses the actual value (computed actual value)
     * for this element.
     *
     * @return The borderColor value
     */
    public BorderColor getBorderColor() {
        if (_drvBorderColor == null) {
            BorderColor bcolor = new BorderColor();
            bcolor.topColor = propertyByName(CSSName.BORDER_COLOR_TOP).computedValue().asColor();
            bcolor.rightColor = propertyByName(CSSName.BORDER_COLOR_RIGHT).computedValue().asColor();
            bcolor.bottomColor = propertyByName(CSSName.BORDER_COLOR_BOTTOM).computedValue().asColor();
            bcolor.leftColor = propertyByName(CSSName.BORDER_COLOR_LEFT).computedValue().asColor();
            _drvBorderColor = bcolor;
        }
        return _drvBorderColor;
    }


    /**
     * Convenience property accessor; returns a Color initialized with the
     * foreground color Uses the actual value (computed actual value) for this
     * element.
     *
     * @return The color value
     */
    public Color getColor() {
        if (_drvColor == null) {
            _drvColor = propertyByName(CSSName.COLOR).computedValue().asColor();
            XRLog.cascade(Level.FINEST, "Color: " + _drvColor);
        }
        return _drvColor;
    }

    /**
     * @return The "background-position" property as a Point
     */
    public Point getBackgroundPosition() {
        DerivedProperty xrProp = propertyByName("background-position");

        if (xrProp.computedValue().isValueList()) {
            CSSValueList vl = (CSSValueList) xrProp.computedValue().cssValue();

            Point pt = new Point();

            pt.setLocation(((CSSPrimitiveValue) vl.item(0)).getFloatValue(CSSPrimitiveValue.CSS_PERCENTAGE),
                    ((CSSPrimitiveValue) vl.item(1)).getFloatValue(CSSPrimitiveValue.CSS_PERCENTAGE));

            return pt;
        } else {
            XRLog.layout("Property : " + xrProp + " is not a value list " + xrProp.computedValue().cssValue().getClass().getName());
        }
        return null;
    }

    public float getFloatPropertyRelative(String name, float parentValue) {
        DerivedProperty prop = propertyByName(name);
        DerivedValue value = prop.computedValue();
        return value.getFloatValueRelative(parentValue);
    }

    public float getFloatProperty(String name) {
        return propertyByName(name).computedValue().asFloat();
    }

    public String getStringProperty(String name) {
        return propertyByName(name).computedValue().asString();
    }

    /*public CSSValue getCSSValue(String name) {
        return propertyByName(name).computedValue().cssValue();
    } */

    public boolean isIdentifier(String name) {
        return propertyByName(name).computedValue().isIdentifier();
    }

    /**
     * <p/>
     * <p/>
     * Implements cascade/inherit/important logic. This should result in the
     * element for this style having a value for *each and every* (visual)
     * property in the CSS2 spec. The implementation is based on the notion that
     * the matched styles are given to us in a perfectly sorted order, such that
     * properties appearing later in the rule-set always override properties
     * appearing earlier. It also assumes that all properties in the CSS2 spec
     * are defined somewhere across all the matched styles; for example, that
     * the full-property set is given in the user-agent CSS that is always
     * loaded with styles. The current implementation makes no attempt to check
     * either of these assumptions. When this method exits, the derived property
     * list for this class will be populated with the properties defined for
     * this element, properly cascaded.</p>
     */
    private void derive() {
        if (_matched == null) {
            return;
        }//nothing to derive
        Iterator mProps = _matched.getMatchedPropertyDeclarations();
        while (mProps.hasNext()) {
            PropertyDeclaration pd = (PropertyDeclaration) mProps.next();
            DerivedProperty prop = deriveProperty(pd.getName(), pd.getValue());
            _derivedPropertiesByName.put(prop.propertyName(), prop);
            //System.err.println(pd.getName()+" "+pd.getValue());
        }
    }

    /**
     * Description of the Method
     *
     * @param name  PARAM
     * @param value PARAM
     * @return Returns
     */
    private DerivedProperty deriveProperty(String name, org.w3c.dom.css.CSSValue value) {
        DerivedValue specified = new DerivedValue(value, _parent);
        DerivedValue computed = specified;
        //isResolvable
        if (specified.isPrimitiveType() && !ValueConstants.isAbsoluteUnit(specified.cssValue())) {
            // inherit the value from parent element if value is set to inherit
            if (specified.forcedInherit()) {
                // if we are root, have no parent, use the initial value as
                // defined by the CSS2 spec
                if (_parent == null) {
                    throw new XRRuntimeException("XRPropertyImpl: trying to resolve an inherited property, but have no parent CalculatedStyle (root of document?)--property '" + name + "' may not be defined in CSS.");
                    // TODO
                } else {
                    computed = _parent.propertyByName(name).computedValue();
                }
            }

            // if value is relative value (e.g. percentage), resolve it
            if (computed.requiresComputation()) {
                computed.computeRelativeUnit(_parent, name);
            }
        }
        return new DerivedProperty(name, computed);
    }
} // end class

/*
 * $Id$
 *
 * $Log$
 * Revision 1.8  2004/12/05 18:11:36  tobega
 * Now uses style cache for pseudo-element styles. Also started preparing to replace inline node handling with inline content handling.
 *
 * Revision 1.7  2004/12/05 00:48:54  tobega
 * Cleaned up so that now all property-lookups use the CalculatedStyle. Also added support for relative values of top, left, width, etc.
 *
 * Revision 1.6  2004/11/15 12:42:23  pdoubleya
 * Across this checkin (all may not apply to this particular file)
 * Changed default/package-access members to private.
 * Changed to use XRRuntimeException where appropriate.
 * Began move from System.err.println to std logging.
 * Standard code reformat.
 * Removed some unnecessary SAC member variables that were only used in initialization.
 * CVS log section.
 *
 *
*/


