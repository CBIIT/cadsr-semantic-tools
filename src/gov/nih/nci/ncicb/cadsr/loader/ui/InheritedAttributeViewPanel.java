package gov.nih.nci.ncicb.cadsr.loader.ui;

import gov.nih.nci.ncicb.cadsr.domain.*;
import gov.nih.nci.ncicb.cadsr.loader.event.*;

import gov.nih.nci.ncicb.cadsr.loader.ui.event.NavigationEvent;
import gov.nih.nci.ncicb.cadsr.loader.ui.event.NavigationListener;
import gov.nih.nci.ncicb.cadsr.loader.ui.tree.*;

import gov.nih.nci.ncicb.cadsr.loader.util.*;

import gov.nih.nci.ncicb.cadsr.loader.ElementsLists;

import gov.nih.nci.ncicb.cadsr.loader.ext.CadsrModule;
import gov.nih.nci.ncicb.cadsr.loader.ui.util.UIUtil;

import java.beans.PropertyChangeListener;

import java.util.*;
import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;


public class InheritedAttributeViewPanel extends JPanel
  implements NodeViewPanel, Editable {

  private DEPanel dePanel;
  private VDPanel vdPanel;
  private DescriptionPanel dsp;
  private ApplyButtonPanel applyButtonPanel;
  private NavigationButtonPanel navButtonPanel;

  private JLabel explainLabel;

  private UMLNode node;

  static final String DE_PANEL_KEY = "dePanel", 
    VD_PANEL_KEY = "vdPanel";

  private ElementsLists elements = ElementsLists.getInstance();

  private UserPreferences prefs = UserPreferences.getInstance();

  private InheritedAttributeList inheritedAttributes = InheritedAttributeList.getInstance();

  private JLabel conceptNamesLabel = new JLabel();

  public InheritedAttributeViewPanel(UMLNode node) 
  {
    this.node = node;
  
    dePanel = new DEPanel(node);
    vdPanel = new VDPanel(node);
    dsp = new DescriptionPanel(node);

    applyButtonPanel = new ApplyButtonPanel(this, (ReviewableUMLNode)node);
    navButtonPanel = new NavigationButtonPanel();    

    dePanel.addPropertyChangeListener(applyButtonPanel);
    vdPanel.addPropertyChangeListener(applyButtonPanel);
    dsp.addPropertyChangeListener(applyButtonPanel);

    initUI();
//     updateNode(node);
  }

  private void initUI() {

    setLayout(new BorderLayout());

    JPanel editPanel = new JPanel();
    editPanel.setLayout(new GridBagLayout());

    JPanel signaturePanel = new JPanel(new FlowLayout());
    JLabel conceptNamesTitle = new JLabel("UML Concepts: ");
    signaturePanel.add(conceptNamesTitle);
    signaturePanel.add(conceptNamesLabel);

//     add(signaturePanel, BorderLayout.NORTH);

    explainLabel = new JLabel("<html><u color=BLUE>Explain this</u></html>");
    ToolTipManager.sharedInstance().registerComponent(explainLabel);
    ToolTipManager.sharedInstance().setDismissDelay(3600000);
    
    String tooltipText = "<html>Description Area is disbled because this is Inherited Attribute <br>" +
        "and it should take its value from its parent. <br>" +
        "Modify the parent if you want to modify its value.</html>";

//    
//    whyLabel = new JLabel("<html><u color=BLUE>Why Disabled</u></html>");
//    ToolTipManager.sharedInstance().registerComponent(whyLabel);
//    ToolTipManager.sharedInstance().setDismissDelay(3600000);
//    whyLabel.setToolTipText("<html>Description Area is disbled because this is Inherited Attribute <br>" +
//        "and it should take its value from its parent. <br>" +
//        "Modify the parent if you want to modify its value.</html>");
        
    //UIUtil.insertInBag(editPanel, whyLabel, 0, 0);
    
    if(prefs.getUmlDescriptionOrder().equals("first"))
      UIUtil.insertInBag(editPanel, dsp.getDescriptionPanel(), 0, 1, 2, 1);
    else
      UIUtil.insertInBag(editPanel, dsp.getDescriptionPanel(), 0, 4, 2, 1); 

    dsp.disableInheritedAttDescArea();
    dsp.setAttDescToolTip(tooltipText);
    
    UIUtil.insertInBag(editPanel, dePanel, 0, 2);
    UIUtil.insertInBag(editPanel, vdPanel, 0, 3);

    UIUtil.insertInBag(editPanel, explainLabel, 1, 2);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); 
    JLabel space = new JLabel("      ");
    buttonPanel.add(space);
    buttonPanel.add(navButtonPanel);
    buttonPanel.add(applyButtonPanel);    

    JScrollPane scrollPane = new JScrollPane(editPanel);

    scrollPane.getVerticalScrollBar().setUnitIncrement(30);

    this.add(scrollPane, BorderLayout.CENTER);
    this.add(buttonPanel, BorderLayout.SOUTH);
  }


  public void addReviewListener(ReviewListener listener) {
    applyButtonPanel.addReviewListener(listener);
  }

  public void addNavigationListener(NavigationListener listener) 
  {
    applyButtonPanel.addNavigationListener(listener);
  }
  
  public void addElementChangeListener(ElementChangeListener listener) {
    dePanel.addElementChangeListener(listener);
    vdPanel.addElementChangeListener(listener);
    dsp.addElementChangeListener(listener);
  }

  public void addPropertyChangeListener(PropertyChangeListener l) {
    applyButtonPanel.addPropertyChangeListener(l);
    dePanel.addPropertyChangeListener(l);
    dsp.addPropertyChangeListener(l);
  }

  public void navigate(NavigationEvent evt) {  
    applyButtonPanel.navigate(evt);
  }

  public void applyPressed() throws ApplyException
  {
  
    dePanel.applyPressed();
    vdPanel.applyPressed();
    dsp.applyPressed();

    updateView();

  }


  public void updateNode(UMLNode node) {
    
    this.node = node;
      
    dePanel.updateNode(node);
    vdPanel.updateNode(node);

    applyButtonPanel.update();
    
    // need to figure out what goes in next line
    applyButtonPanel.init(true);


    updateView();
    
  }

  private void updateView() {
    // find the parent node so we can decide what's allowed here.

    // find rootNode
    UMLNode rootNode = null, tempNode = node.getParent();
    while(tempNode != null) {
      rootNode = tempNode;
      tempNode = tempNode.getParent();
    }

    DataElement de = (DataElement)node.getUserObject();
    DataElement parentDE = inheritedAttributes.getParent(de);
      
    // we only show dePanel 
    // if parent is mapped to DE or
    // if parent has no concept mapping or
    // if de is already mapped
    if(parentDE != null) {
      dePanel.setVisible(
        DEMappingUtil.isMappedToLVD(de) 
        &&
        ( !StringUtil.isEmpty(parentDE.getPublicId())
          || !StringUtil.isEmpty(de.getPublicId())
          || StringUtil.isEmpty(parentDE.getDataElementConcept().getProperty().getPreferredName()) 
          ));
    }
    
    if(StringUtil.isEmpty(de.getPublicId())) {
      vdPanel.setVisible(true);
    } else {
      vdPanel.setVisible(false);
    }
    
    if(dePanel.isVisible()) {
      explainLabel.setToolTipText("<html>From here, you may either map to a Value Domain<br>" + 
                                  "or a CDE. Please note that if you map to a CDE, <br>" + 
                                  "any Value Domain mapping for this attribute will be cleared.<br>" + 
                                  "Your CDE queries will be filtered to CDEs that match existing<br>" + 
                                  "Object Class or Property mappings</html>");
    } else if(!dePanel.isVisible() && DEMappingUtil.isMappedToLVD(de)){
      explainLabel.setToolTipText("<html> You may only map this inherited attribute to a Value Domain.<br>" + 
                                  "If you need to map this inherited attribute to an existing CDE,<br>" + 
                                  " you will need to remove the concept mapping of the parent attribute.</html>");
    } else {
      explainLabel.setToolTipText("<html>Because this inherited attribute is mapped to a caDSR <br>" + 
                                  "Local Value Domain, you may not map the attribute to a CDE or a Value Domain.<br>" + 
                                  "You can remove the mapping to the caDSR Local Value Domain in your UML modeling tool.</html>");
    }

  }


  /**
   * Find the given attribute in the given class's inheritance hierarchy. 
   * Returns the first non-inherited AttributeNode.
   * @return
   */
  private AttributeNode findSuper(String className, String attributeName, UMLNode rootNode) {

    // find the superclass by searching the OCR's for a generalization 
    
    List<ObjectClassRelationship> ocrs = 
      elements.getElements(DomainObjectFactory.newObjectClassRelationship());
    
    String superClassName = null;
    for(ObjectClassRelationship ocr : ocrs) {
      if (ocr.getType() == ObjectClassRelationship.TYPE_IS && 
          LookupUtil.lookupFullName(ocr.getSource()).equals(className)) {
        superClassName = LookupUtil.lookupFullName(ocr.getTarget());
        break;
      }
    }
    
    if (superClassName == null) {
      System.err.println("Superclass not found for "+className);
      return null;
    }
    
    // find the super class in the tree
    
    int div = superClassName.lastIndexOf(".");
    String sPackage = superClassName.substring(0, div);
    String sName = superClassName.substring(div+1);


    for(Object pchild : rootNode.getChildren()) {
      PackageNode pnode = (PackageNode)pchild;
      if (pnode.getFullPath().equals(sPackage)) {
        for(Object cchild : pnode.getChildren()) {
          ClassNode cnode = (ClassNode)cchild;
          if (cnode.getDisplay().equals(sName)) {
            PackageNode inherited = null;
            for(Object achild : cnode.getChildren()) {
              if ("Inherited Attributes".equals(
                    ((UMLNode)achild).getDisplay())) {
                // remember the inheritance subtree for later
                inherited = (PackageNode)achild;
              }
              else if (achild instanceof AttributeNode) {
                AttributeNode anode = (AttributeNode)achild;
                if (anode.getDisplay().equals(attributeName)) {
                  return anode;
                }
              }
            }
            // attribute wasn't found, check inheritance subtree
            if (inherited != null) {
              for(Object achild : inherited.getChildren()) {
                AttributeNode anode = (AttributeNode)achild;
                if (anode.getDisplay().equals(attributeName)) {
                  return findSuper(cnode.getFullPath(), attributeName, rootNode);
                }
              }
              
            }
          }
        } 
      }
    }
      
    return null;
  }

  public void setCadsrModule(CadsrModule cadsrModule) {
    dePanel.setCadsrModule(cadsrModule);
  }


}