package gov.nih.nci.ncicb.cadsr.loader.ui;

import gov.nih.nci.ncicb.cadsr.domain.*;
import gov.nih.nci.ncicb.cadsr.loader.event.*;

import gov.nih.nci.ncicb.cadsr.loader.ext.CadsrModule;
import gov.nih.nci.ncicb.cadsr.loader.ui.event.NavigationEvent;
import gov.nih.nci.ncicb.cadsr.loader.ui.event.NavigationListener;
import gov.nih.nci.ncicb.cadsr.loader.ui.tree.*;
import gov.nih.nci.ncicb.cadsr.loader.ui.util.UIUtil;

import gov.nih.nci.ncicb.cadsr.loader.util.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.util.*;
import javax.swing.*;
import java.awt.*;

public class UMLElementViewPanel extends JPanel
  implements Editable, NodeViewPanel {

  private ConceptEditorPanel conceptEditorPanel;
  private DEPanel dePanel;
  private OCPanel ocPanel;
  private ButtonPanel buttonPanel;
  private GMEViewPanel gmePanel;
  private DescriptionPanel dsp;
  private PublicIdPanel publicIdPanel;//SIW-627 new VM ID Panel

  //TODO uncomment to enable concept inheritance feature
  //private ExcludeFromSemanticInheritancePanel excludeSemPanel;
  private UMLNode node;

  private InfoPanel cannotMapVMInfoPanel;

  private JPanel cardPanel;
  
  private JPanel displayedPanel;

  static final String DE_PANEL_KEY = "dePanel", 
    OC_PANEL_KEY = "ocPanel",
    CONCEPT_PANEL_KEY = "conceptPanel",
    CANNOT_MAP_VM_KEY = "cannotMapVm",
    VM_PUBLIC_ID_PANEL_KEY = "vmPublicIdpanel";//SIW_627
    
  private Map<String, JPanel> panelKeyMap = new HashMap<String, JPanel>();

  public UMLElementViewPanel(UMLNode node) 
  {
    this.node = node;
    conceptEditorPanel = new ConceptEditorPanel(node);
    dePanel = new DEPanel(node);
    ocPanel = new OCPanel(node);
    gmePanel= new GMEViewPanel(node);
    dsp = new DescriptionPanel(node);
    //SIW-627 VM public ID and Version panel
    publicIdPanel = new PublicIdPanel(node);

    // TODO uncomment to enable concept inheritance feature
    //excludeSemPanel = new ExcludeFromSemanticInheritancePanel(node);

    cannotMapVMInfoPanel = new InfoPanel();
    cannotMapVMInfoPanel.setOutputText("This Permissible Value cannot be modified because the Value Domain where it is included is mapped to a caDSR Value Domain");

    if(node instanceof AttributeNode){
      buttonPanel = new ButtonPanel(conceptEditorPanel, this, dePanel);}
    else if(node instanceof ValueMeaningNode)
    {
	buttonPanel = new ButtonPanel(conceptEditorPanel, this, publicIdPanel);//SIW-627 publicIdPanel was null before
    }
    else
    {
	buttonPanel = new ButtonPanel(conceptEditorPanel, this, ocPanel);
    }

    if (conceptEditorPanel != null) { conceptEditorPanel.addPropertyChangeListener(buttonPanel); }
    if (dePanel != null) { dePanel.addPropertyChangeListener(buttonPanel); }
    if (ocPanel != null) { ocPanel.addPropertyChangeListener(buttonPanel); }
    if (gmePanel != null) { gmePanel.addPropertyChangeListener(buttonPanel); }
    if (dsp != null) { dsp.addPropertyChangeListener(buttonPanel); }
    //SIW-627 publicIdPanel
    publicIdPanel.addPropertyChangeListener(buttonPanel);
    ValueMeaning vm = (ValueMeaning) node.getUserObject();
    if (vm != null) {
    	buttonPanel.setSwitchButtonEnabled(StringUtil.isEmpty(vm.getPublicId()));	
    }
    
    // TODO uncomment to enable concept inheritance feature
    //excludeSemPanel.addPropertyChangeListener(buttonPanel);

    
    cardPanel = new JPanel();
    initUI();
  }
  
  private void initUI() {

    conceptEditorPanel.initUI();
    
    cardPanel.setLayout(new CardLayout());
    cardPanel.add(conceptEditorPanel, CONCEPT_PANEL_KEY);
    cardPanel.add(dePanel, DE_PANEL_KEY);
    cardPanel.add(ocPanel, OC_PANEL_KEY);
    cardPanel.add(cannotMapVMInfoPanel, CANNOT_MAP_VM_KEY);
    cardPanel.add(publicIdPanel, VM_PUBLIC_ID_PANEL_KEY);//SIW-627
    
    panelKeyMap.put(CONCEPT_PANEL_KEY, conceptEditorPanel);
    panelKeyMap.put(DE_PANEL_KEY, dePanel);
    panelKeyMap.put(OC_PANEL_KEY, ocPanel);
    panelKeyMap.put(VM_PUBLIC_ID_PANEL_KEY, publicIdPanel);//SIW-627
    
    setLayout(new BorderLayout());
    
    JPanel newPanel = new JPanel();
    newPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); 
    JLabel space = new JLabel("      ");
    newPanel.add(space);
    newPanel.add(buttonPanel);
    
    JPanel editPanel = new JPanel();
    editPanel.setLayout(new GridBagLayout());

    UserPreferences prefs = UserPreferences.getInstance();
    if(prefs.getUmlDescriptionOrder().equals("first"))
      UIUtil.insertInBag(editPanel, dsp.getDescriptionPanel(), 0, 0);
    else 
      UIUtil.insertInBag(editPanel, dsp.getDescriptionPanel(), 0, 3);
    

    UIUtil.insertInBag(editPanel, cardPanel, 0, 1);
    UIUtil.insertInBag(editPanel, gmePanel, 0, 2);

    // TODO uncomment to enable concept inheritance feature
    //UIUtil.insertInBag(editPanel, excludeSemPanel, 0, 3);

    JScrollPane scrollPane = new JScrollPane(editPanel);
    scrollPane.getVerticalScrollBar().setUnitIncrement(30);

    this.add(scrollPane, BorderLayout.CENTER);
    this.add(newPanel, BorderLayout.SOUTH);
  }
  
  public void setEnabled(boolean enabled) {
    buttonPanel.setEnabled(enabled);
    conceptEditorPanel.setEnabled(enabled);
    dePanel.setEnabled(enabled);
  }
  
  public void switchCards(String key) 
  {
    CardLayout layout = (CardLayout)cardPanel.getLayout();

    if(displayedPanel instanceof ConceptEditorPanel) {
      conceptEditorPanel.setExpanded(false);
    }

    layout.show(cardPanel, key);
    displayedPanel = panelKeyMap.get(key);

    if(displayedPanel instanceof ConceptEditorPanel) {
      conceptEditorPanel.setExpanded(true);
    }

  }
  
  public void updateNode(UMLNode node) 
  {
  
    this.node = node;
    buttonPanel.setVisible(true);
    // is UMLNode a de?
    Object o = node.getUserObject();
    if(o instanceof DataElement) { //if it is, does it have pubID
      DataElement de = (DataElement)o;
      if(!StringUtil.isEmpty(de.getPublicId())) 
      {
        switchCards(DE_PANEL_KEY);
      } else {
        switchCards(CONCEPT_PANEL_KEY);
      }
    } else if(o instanceof ObjectClass) {
      ObjectClass oc = (ObjectClass)o;
      if(!StringUtil.isEmpty(oc.getPublicId())) 
      {
        switchCards(OC_PANEL_KEY);
      }
      else 
      {
        switchCards(CONCEPT_PANEL_KEY);
      }
    } else if (o instanceof ValueMeaning) { 
      ValueMeaningNode vmNode = (ValueMeaningNode)node;
      ValueMeaning vm = (ValueMeaning)node.getUserObject();//SIW-627
      AlternateName fullName = DomainObjectFactory.newAlternateName();
      fullName.setType(AlternateName.TYPE_FULL_NAME);
      fullName.setName(vmNode.getVdName());
      
      if (! StringUtil.isEmpty(vm.getPublicId())) {//SIW-627
    	  switchCards(VM_PUBLIC_ID_PANEL_KEY);
      }
      else if(StringUtil.isEmpty(LookupUtil.lookupValueDomain(fullName).getPublicId()))  {
        switchCards(CONCEPT_PANEL_KEY);
      } 
      else {
        switchCards(CANNOT_MAP_VM_KEY);
        buttonPanel.setVisible(false);
      }
    }
      
    conceptEditorPanel.updateNode(node);
    dePanel.updateNode(node);
    ocPanel.updateNode(node);
    gmePanel.updateNode(node);
    
 // TODO uncomment to enable concept inheritance feature
//    excludeSemPanel.updateNode(node);
    
    if(node instanceof AttributeNode)
      buttonPanel.setEditablePanel(dePanel);
    else if (node instanceof ValueMeaningNode)
      buttonPanel.setEditablePanel(publicIdPanel);//SIW-617 the parameter was null before
    else 
      buttonPanel.setEditablePanel(ocPanel);


    buttonPanel.propertyChange
      (new PropertyChangeEvent(this, ButtonPanel.SETUP, null, true));
    
    buttonPanel.update();
    
  }

//  public boolean isReviewed() 
//  {
//    return ((ReviewableUMLNode)node).isReviewed();
//  }

  public void apply(boolean value) throws ApplyException
  {
    conceptEditorPanel.apply(value);
  }

  public void navigate(NavigationEvent evt) {  
    buttonPanel.navigate(evt);
  }

  public void addReviewListener(ReviewListener listener) {
    buttonPanel.addReviewListener(listener);
  }

  public void addNavigationListener(NavigationListener listener) 
  {
    buttonPanel.addNavigationListener(listener);
  }
  
  public void addElementChangeListener(ElementChangeListener listener) {
    conceptEditorPanel.addElementChangeListener(listener);
    dePanel.addElementChangeListener(listener);
    dsp.addElementChangeListener(listener);
    publicIdPanel.addElementChangeListener(listener);//SIW-627
 // TODO uncomment to enable concept inheritance feature
//    excludeSemPanel.addElementChangeListener(listener);
  }

  public void addPropertyChangeListener(PropertyChangeListener l) {
	  
	// TODO uncomment to enable concept inheritance feature	  
//    excludeSemPanel.addPropertyChangeListener(l);
    if (dsp != null) { dsp.addPropertyChangeListener(l); }
    if (conceptEditorPanel != null) { conceptEditorPanel.addPropertyChangeListener(l); }
    if (buttonPanel != null) { buttonPanel.addPropertyChangeListener(l); }
    if (dePanel != null) { dePanel.addPropertyChangeListener(l); }
    if (ocPanel != null) { ocPanel.addPropertyChangeListener(l); }
  }
  
  public void applyPressed() throws ApplyException
  {
    if(displayedPanel instanceof Editable) 
    {
     ((Editable)displayedPanel).applyPressed();
    }
    dsp.applyPressed();
    
 // TODO uncomment to enable concept inheritance feature
//    excludeSemPanel.applyPressed();
  }
    
  public ConceptEditorPanel getConceptEditorPanel() {
    return conceptEditorPanel;
  }
  
  public void setCadsrModule(CadsrModule cadsrModule) {
    dePanel.setCadsrModule(cadsrModule);
    ocPanel.setCadsrModule(cadsrModule);
  }

}

      