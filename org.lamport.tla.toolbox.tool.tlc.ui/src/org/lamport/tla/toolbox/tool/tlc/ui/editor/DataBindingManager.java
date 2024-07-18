package org.lamport.tla.toolbox.tool.tlc.ui.editor;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Takes care of section on pages, and attributes on sections
 * @author Simon Zambrovski
 * @version $Id$
 */
/**
 * @author Simon Zambrovski
 */
public class DataBindingManager implements ISectionConstants
{
    private static final String[] EMPTY = new String[0];

    // section parts containing the sections
    private Hashtable<String, SectionPart> sectionParts = new Hashtable<String, SectionPart>(13);
    // storage to retrieve the page for a section
    private Hashtable<String, String> pageForSection = new Hashtable<String, String>(13);
    // storage to retrieve sections on a given page
    private Hashtable<String, Vector<String>> sectionsForPage = new Hashtable<String, Vector<String>>(13);
    // storage to retrieve the section for a given attribute
    private Hashtable<String, String> sectionForAttribute = new Hashtable<String, String>(37);
    // storage to retrieve the viewer for a given attribute
    private Hashtable<String, Object> viewerForAttribute = new Hashtable<String, Object>(37);

    /** 
     * expands a section by given section id
     */
    public void expandSection(String id)
    {
        SectionPart part = sectionParts.get(id);
        if (part == null)
        {
            throw new IllegalArgumentException("No section for id");
        }
        if (!part.getSection().isExpanded())
        {
            part.getSection().setExpanded(true);
        }
    }

    /**
     * Enables or disables all section on the current page. More precisely, this
     * means setting the enablement state of any child of a
     * section that is a {@link Composite} but not a {@link Section}
     * to enabled.
     * 
     * @param enabled 
     */
    public void setAllSectionsEnabled(String pageId, boolean enabled)
    {
        String[] sectionIds = getSectionsForPage(pageId);
        for (int i = 0; i < sectionIds.length; i++)
        {
            enableSection(sectionIds[i], enabled);
        }
    }

    /**
     * enables a section by given id. More precisely, this
     * means setting the enablement state of any child of the
     * section that is a {@link Composite} but not a {@link Section}
     * to enabled.
     */
    private void enableSection(String id, boolean enabled)
    {
        SectionPart part = sectionParts.get(id);
        if (part == null)
        {
            throw new IllegalArgumentException("No section for id");
        }
        Section section = part.getSection();
        Control[] children = section.getChildren();
        for (int i = 0; i < children.length; i++)
        {

            if (children[i] instanceof Composite)
            {
                enableSectionComposite((Composite) children[i], enabled);
            }
        }
    }

    /**
     * Sets the enablement state of a section's composite. More precisely, this
     * means setting the enablement state of any child of the
     * composite that is  not a {@link Section}
     * to enabled.
     * 
     * @param composite
     */
    private void enableSectionComposite(Composite composite, boolean enable)
    {
        Control[] children = composite.getChildren();
        for (int i = 0; i < children.length; i++)
        {
            if (!(children[i] instanceof Section))
            {
                children[i].setEnabled(enable);
            }
        }
    }

    /**
     * retrieves the id of the page the section is on
     */
    public String getSectionPage(String id)
    {
        String pageId;
        if ((pageId = pageForSection.get(id)) != null)
        {
            return pageId;
        } else
        {
            throw new IllegalArgumentException("No page for id");
        }
    }

    /**
     * Adds a section to the manager
     * @param section
     * @param id
     * @param pageId
     */
    public void bindSection(SectionPart sectionPart, String id, String pageId)
    {
        // store the section
        sectionParts.put(id, sectionPart);

        // store the page id
        pageForSection.put(id, pageId);

        Vector<String> sectionIds = sectionsForPage.get(pageId);
        if (sectionIds == null)
        {
            sectionIds = new Vector<String>();
            sectionsForPage.put(pageId, sectionIds);
        }

        sectionIds.add(id);
    }

    /**
     * Retrieves the section of the current page
     * @param pageId page id 
     * @return an array with sections or empty array
     */
    private String[] getSectionsForPage(String pageId)
    {
        Vector<String> sectionIds = sectionsForPage.get(pageId);
        if (sectionIds == null)
        {
            return EMPTY;
        } else
        {
            return (String[]) sectionIds.toArray(new String[sectionIds.size()]);
        }
    }

    /**
     * Retrieves a section id if the attribute is found  
     * @param attributeName the id of the attribute
     * @return the id of the section, or <code>null</code> if not found
     */
    public String getSectionForAttribute(String attributeName)
    {
        return sectionForAttribute.get(attributeName);
    }

    /**
     * Retrieves the section by id
     * @param sectionId
     * @return the section part, or <code>null</code> if not found
     */
    public SectionPart getSection(String sectionId)
    {
        return (SectionPart) sectionParts.get(sectionId);
    }

    /**
     * Bind an attribute name <code>attributeName</code> to the viewer <code>attributeViewer</code> location in the section part <code>sectionPart</code>
     * This method should be called after the section is bound to the section id and page using {@link DataBindingManager#bindSection(SectionPart, String, String)} method
     * @param attributeName
     * @param attributeViewer
     * @param sectionPart
     */
    public void bindAttribute(String attributeName, Object attributeViewer, SectionPart sectionPart)
    {
        // bind the viewer
        viewerForAttribute.put(attributeName, attributeViewer);
        // bind the section id
        Enumeration<String> enumeration = sectionParts.keys();
        while (enumeration.hasMoreElements())
        {
            String sectionId = enumeration.nextElement();
            SectionPart registeredPart = sectionParts.get(sectionId);
            if (registeredPart.equals(sectionPart))
            {
                sectionForAttribute.put(attributeName, sectionId);
                break;
            }
        }
    }

    /**
     * Retrieves the viewer for given attribute
     * @param attributeName
     * @return the Viewer
     */
    public Object getAttributeControl(String attributeName)
    {
        return viewerForAttribute.get(attributeName);
    }

}
