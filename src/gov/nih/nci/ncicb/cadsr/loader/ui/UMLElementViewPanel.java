package gov.nih.nci.ncicb.cadsr.loader.ui;

import gov.nih.nci.ncicb.cadsr.domain.*;
import gov.nih.nci.ncicb.cadsr.loader.event.ReviewEvent;
import gov.nih.nci.ncicb.cadsr.loader.event.ReviewListener;

import gov.nih.nci.ncicb.cadsr.loader.ui.event.NavigationEvent;
import gov.nih.nci.ncicb.cadsr.loader.ui.event.NavigationListener;
import gov.nih.nci.ncicb.cadsr.loader.util.UserPreferencesEvent;
import gov.nih.nci.ncicb.cadsr.loader.util.UserPreferencesListener;
import gov.nih.nci.ncicb.cadsr.loader.ui.tree.*;

import gov.nih.nci.ncicb.cadsr.loader.util.*;
import gov.nih.nci.ncicb.cadsr.loader.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class UMLElementViewPanel extends JPanel
  implements ActionListener, KeyListener
             , ItemListener, CaretListener,
             UserPreferencesListener, NavigationListener {

  private Concept[] concepts;
  private ConceptUI[] conceptUIs;

//   private boolean unsavedChanges = false;

  private JPanel _this = this;

  private UMLNode node;
  private boolean remove = false;

  private static final String ADD = "ADD",
    DELETE = "DELETE",
    SAVE = "SAVE", 
    PREVIOUS = "PREVIOUS",
    NEXT = "NEXT";

  private JButton addButton, deleteButton, saveButton;
  private JButton previousButton, nextButton;
  private JCheckBox reviewButton;
  private List<ReviewListener> reviewListeners = new ArrayList();
  private List<NavigationListener> navigationListeners = new ArrayList();
  
  private JPanel gridPanel;
  private JScrollPane scrollPane;

  // initialize once the mode in which we're running
  private static boolean editable = false;
  static {
    UserSelections selections = UserSelections.getInstance();
    editable = selections.getProperty("MODE").equals(RunMode.Curator);
  }
  
  public UMLElementViewPanel(UMLNode node) 
  {
    this.node = node;
    initConcepts();
    initUI();
  }

  public void navigate(NavigationEvent evt) {
    if(saveButton.isEnabled()) {
      if(JOptionPane.showConfirmDialog(_this, "There are unsaved changes in this concept, would you like to apply the changes them?", "Unsaved Changed", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
        saveButton.doClick();
    }
  }

  public void updateNode(UMLNode node) 
  {
    this.node = node;
    initConcepts();
    updateConcepts(concepts);
  }

  private void initConcepts() 
  {
    concepts = NodeUtil.getConceptsFromNode(node);
  }

  private void updateConcepts(Concept[] concepts) {
    this.concepts = concepts;
    this.removeAll();
    initUI();
    this.updateUI();
  }

//   public Concept[] getConcepts() {
//     return null;
//   }

//   public boolean haveUnsavedChanges() {
//     return unsavedChanges;
//   }

  private void initUI() {
    this.setLayout(new BorderLayout());
    initViewPanel();
    initButtonPanel();
  }
  
  private void initViewPanel() {
 

    gridPanel = new JPanel(new GridLayout(-1, 1));
    scrollPane = new JScrollPane(gridPanel);

    conceptUIs = new ConceptUI[concepts.length];
    JPanel[] conceptPanels = new JPanel[concepts.length];

    JPanel summaryPanel = new JPanel();
    JLabel summaryTitle = new JLabel("UML Concept Code Summary: ");
    summaryPanel.add(summaryTitle);
    for(int i = 0; i < concepts.length; i++) {
      conceptUIs[i] = new ConceptUI(concepts[i]);
      JLabel label= new JLabel(concepts[i].getPreferredName());
      summaryPanel.add(label);
    }
    this.add(summaryPanel,BorderLayout.NORTH);

    UserPreferences prefs = UserPreferences.getInstance();
    if(prefs.getUmlDescriptionOrder().equals("first"))
      gridPanel.add(createDescriptionPanel());
      
    for(int i = 0; i<concepts.length; i++) {
      conceptUIs[i] = new ConceptUI(concepts[i]);

      String title = i == 0?"Primary Concept":"Qualifier Concept" +" #" + i;

      conceptPanels[i] = new JPanel();
      conceptPanels[i].setBorder
        (BorderFactory.createTitledBorder(title));

      conceptPanels[i].setLayout(new BorderLayout());

      JPanel mainPanel = new JPanel(new GridBagLayout());

      insertInBag(mainPanel, conceptUIs[i].labels[0], 0, 0);
      insertInBag(mainPanel, conceptUIs[i].labels[1], 0, 1);
      insertInBag(mainPanel, conceptUIs[i].labels[2], 0, 2);
      insertInBag(mainPanel, conceptUIs[i].labels[3], 0, 3);

      insertInBag(mainPanel, conceptUIs[i].code, 1, 0, 2, 1);
      insertInBag(mainPanel, conceptUIs[i].name, 1, 1, 2, 1);
      insertInBag(mainPanel, conceptUIs[i].defScrollPane, 1, 2, 2, 1);
      insertInBag(mainPanel, conceptUIs[i].defSource, 1, 3,1, 1);

      JButton evsButton = new JButton("Evs Link");
      insertInBag(mainPanel, evsButton, 2, 3);
      
      JButton upButton = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("up-arrow.gif")));
      JButton downButton = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("down-arrow.gif")));

      upButton.setPreferredSize(new Dimension(28, 35));
      downButton.setPreferredSize(new Dimension(28, 35));

      JPanel arrowPanel = new JPanel(new GridBagLayout());
      insertInBag(arrowPanel, upButton, 0, 0);
      insertInBag(arrowPanel, downButton, 0, 6);
//       arrowPanel.add(upButton, BorderLayout.NORTH);
//       arrowPanel.add(downButton, BorderLayout.SOUTH);
      
      conceptPanels[i].add(mainPanel, BorderLayout.CENTER);
      conceptPanels[i].add(arrowPanel, BorderLayout.EAST);
      gridPanel.add(conceptPanels[i]);

      conceptUIs[i].code.addKeyListener(this);
      conceptUIs[i].name.addKeyListener(this);
      conceptUIs[i].def.addKeyListener(this);
      conceptUIs[i].defSource.addKeyListener(this);

      conceptUIs[i].code.addCaretListener(this);
      conceptUIs[i].name.addCaretListener(this);
      conceptUIs[i].defSource.addCaretListener(this);
      conceptUIs[i].def.addCaretListener(this);
      
      final int index = i;
      if(index == 0)
        upButton.setVisible(false);
      if(index == concepts.length-1)
        downButton.setVisible(false);
      
      upButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          Concept temp = concepts[index-1];
          concepts[index-1] = concepts[index];
          concepts[index] = temp;
          updateConcepts(concepts);

          ((AdminComponent)node.getUserObject()).setPreferredName(ObjectUpdater.preferredNameFromConcepts(concepts));
          
          setButtonState(saveButton);
              
          }
        });
      
      downButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          Concept temp = concepts[index];
          concepts[index] = concepts[index+1];
          concepts[index+1] = temp;
          updateConcepts(concepts);

          ((AdminComponent)node.getUserObject()).setPreferredName(ObjectUpdater.preferredNameFromConcepts(concepts));
          
          setButtonState(saveButton);
              
        }
        });
      
      
      
    }
    
    if(prefs.getUmlDescriptionOrder().equals("last"))
      gridPanel.add(createDescriptionPanel());
    
    this.add(scrollPane, BorderLayout.CENTER);
    
  }

  private JPanel createDescriptionPanel() {
    JPanel umlPanel = new JPanel();
    umlPanel.setBorder
      (BorderFactory.createTitledBorder("UML Description"));   
    umlPanel.setLayout(new BorderLayout());
    
    JPanel descriptionPanel = new JPanel(new FlowLayout());
    JTextArea descriptionArea = new JTextArea();
    
    
    if(node instanceof ClassNode) {
      ObjectClass oc = (ObjectClass) node.getUserObject();
      descriptionArea.setText(oc.getPreferredDefinition());
    } else if(node instanceof AttributeNode) {
      DataElement de = (DataElement) node.getUserObject();

      for(Definition def : (List<Definition>) de.getDefinitions()) {
        descriptionArea.setText(def.getDefinition());
        break;
      }

    }

    descriptionArea.setLineWrap(true);
    descriptionArea.setWrapStyleWord(true);
    descriptionArea.setEditable(false);
    descriptionArea.setColumns(75);
    
    if(StringUtil.isEmpty(descriptionArea.getText())) 
    {
      descriptionArea.setVisible(false);
    }

    descriptionPanel.add(descriptionArea);
    umlPanel.add(descriptionPanel);    
    
    return umlPanel;
 
  }

  private void initButtonPanel()
  {
  	addButton = new JButton("Add");
  	deleteButton = new JButton("Remove");
  	saveButton = new JButton("Apply");
  	reviewButton = new JCheckBox("Reviewed");
  	previousButton = new JButton("Previous");
  	nextButton = new JButton("Next");
  	
  	reviewButton.setSelected(((ReviewableUMLNode)node).isReviewed());
  	addButton.setActionCommand(ADD);
  	deleteButton.setActionCommand(DELETE);
  	saveButton.setActionCommand(SAVE);
  	previousButton.setActionCommand(PREVIOUS);
  	nextButton.setActionCommand(NEXT);
  	addButton.addActionListener(this);
  	deleteButton.addActionListener(this);
  	saveButton.addActionListener(this);
  	reviewButton.addItemListener(this);
  	previousButton.addActionListener(this);
  	nextButton.addActionListener(this);
  	
  	if(concepts.length < 2)
  	  deleteButton.setEnabled(false);
  	saveButton.setEnabled(false);
  	setButtonState(reviewButton);
  	JPanel buttonPanel = new JPanel();
  	buttonPanel.add(addButton);
  	buttonPanel.add(deleteButton);
  	buttonPanel.add(saveButton);
  	buttonPanel.add(reviewButton);
  	buttonPanel.add(previousButton);
  	buttonPanel.add(nextButton);

  	this.add(buttonPanel, BorderLayout.SOUTH);

  }

  
  private void setButtonState(AbstractButton button)  {
    for(int i=0; i < conceptUIs.length; i++) {
      if(conceptUIs[i].code.getText().trim().equals("")
         | conceptUIs[i].name.getText().trim().equals("")
         | conceptUIs[i].defSource.getText().trim().equals("")
         | conceptUIs[i].def.getText().trim().equals("")) {
        button.setEnabled(false);
        return;
      }
      
      else
        button.setEnabled(true);
    }
  }

  public void caretUpdate(CaretEvent evt) {
    setButtonState(reviewButton);
    setButtonState(addButton);
  }
  
  public void keyTyped(KeyEvent evt) {}
  public void keyPressed(KeyEvent evt) {}
  public void keyReleased(KeyEvent evt) {
    setButtonState(saveButton);
  }


  public void preferenceChange(UserPreferencesEvent event) 
  {
    if(event.getTypeOfEvent() == UserPreferencesEvent.UML_DESCRIPTION) 
    {
      this.remove(gridPanel);
      initViewPanel();
      this.updateUI();
    }
  }

  
  public void actionPerformed(ActionEvent evt) {
    JButton button = (JButton)evt.getSource();
    if(button.getActionCommand().equals(SAVE)) {
      boolean update = remove;
      remove = false;
      Concept[] newConcepts = new Concept[concepts.length];
      
      for(int i = 0; i<concepts.length; i++) 
      {
        newConcepts[i] = concepts[i];
        // concept code has not changed
        if(conceptUIs[i].code.getText().equals(concepts[i].getPreferredName())) {
          concepts[i].setLongName(conceptUIs[i].name.getText());
          concepts[i].setPreferredDefinition(conceptUIs[i].def.getText());
          concepts[i].setDefinitionSource(conceptUIs[i].defSource.getText());
        } else { // concept code has changed
          Concept concept = DomainObjectFactory.newConcept();
          concept.setPreferredName(conceptUIs[i].code.getText());
          concept.setLongName(conceptUIs[i].name.getText());
          concept.setPreferredDefinition(conceptUIs[i].def.getText());
          concept.setDefinitionSource(conceptUIs[i].defSource.getText());
          newConcepts[i] = concept;
          update = true;
        }
      }

      if(update) {
        ObjectUpdater.update((AdminComponent)node.getUserObject(), concepts, newConcepts);
      }

      saveButton.setEnabled(false);
      reviewButton.setEnabled(true);


    } else if(button.getActionCommand().equals(ADD)) {
      Concept[] newConcepts = new Concept[concepts.length + 1];
      for(int i = 0; i<concepts.length; i++) {
        newConcepts[i] = concepts[i];
      }
      Concept concept = DomainObjectFactory.newConcept();
      concept.setPreferredName("");
      concept.setLongName("");
      concept.setDefinitionSource("");
      concept.setPreferredDefinition("");
      newConcepts[newConcepts.length - 1] = concept;
      concepts = newConcepts;

      this.remove(scrollPane);
      initViewPanel();
      this.updateUI();
      addButton.setEnabled(false);
      if(concepts.length > 1)
        deleteButton.setEnabled(true);
      setButtonState(addButton);
      setButtonState(saveButton);


    } else if(button.getActionCommand().equals(DELETE)) {
      Concept[] newConcepts = new Concept[concepts.length - 1];
      for(int i = 0; i<newConcepts.length; i++) {
        newConcepts[i] = concepts[i];
      }
      concepts = newConcepts;

      _this.remove(scrollPane);
      initViewPanel();

      setButtonState(saveButton);

      if(concepts.length < 2)
        deleteButton.setEnabled(false);
      this.updateUI();

      remove = true;

    } else if(button.getActionCommand().equals(PREVIOUS)) {
      NavigationEvent event = new NavigationEvent(NavigationEvent.NAVIGATE_PREVIOUS);
      fireNavigationEvent(event);
      remove = false;
    } else if(button.getActionCommand().equals(NEXT)) {
      NavigationEvent event = new NavigationEvent(NavigationEvent.NAVIGATE_NEXT);
      fireNavigationEvent(event);
      remove = false;
    }

  }
  
  public void fireNavigationEvent(NavigationEvent event) 
  {
    for(NavigationListener l : navigationListeners)
      l.navigate(event);
  }
  
  public void addNavigationListener(NavigationListener listener) 
  {
    navigationListeners.add(listener);
  }
  
  public void itemStateChanged(ItemEvent e) {
    if(e.getStateChange() == ItemEvent.SELECTED
       || e.getStateChange() == ItemEvent.DESELECTED
       ) {
      ReviewEvent event = new ReviewEvent();
      event.setUserObject(node);
      
      event.setReviewed(ItemEvent.SELECTED == e.getStateChange());

      if(event.isReviewed())
        saveButton.doClick();
      
      fireReviewEvent(event);
      
      //if item is reviewed go to next item in the tree
      if(e.getStateChange() == ItemEvent.SELECTED) 
      {
        NavigationEvent goToNext = new NavigationEvent(NavigationEvent.NAVIGATE_NEXT);
        fireNavigationEvent(goToNext);
      }
        
    }
  }
  
  public void fireReviewEvent(ReviewEvent event) {
    for(ReviewListener l : reviewListeners)
      l.reviewChanged(event);
  }
  

  public void addReviewListener(ReviewListener listener) {
    reviewListeners.add(listener);
  }
  
  private void insertInBag(JPanel bagComp, Component comp, int x, int y) {

    insertInBag(bagComp, comp, x, y, 1, 1);

  }

  private void insertInBag(JPanel bagComp, Component comp, int x, int y, int width, int height) {
    JPanel p = new JPanel();
    p.add(comp);

    bagComp.add(p, new GridBagConstraints(x, y, width, height, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
  }

}

class ConceptUI {
  // initialize once the mode in which we're running
  private static boolean editable = false;
  static {
    UserSelections selections = UserSelections.getInstance();
    editable = selections.getProperty("MODE").equals(RunMode.Curator);
  }

  JLabel[] labels = new JLabel[] {
    new JLabel("Concept Code"),
    new JLabel("Concept Preferred Name"),
    new JLabel("Concept Definition"),
    new JLabel("Concept Definition Source")
  };

  JTextField code = new JTextField(10);
  JTextField name = new JTextField(20);
  JTextArea def = new JTextArea();
  JTextField defSource = new JTextField(10);

  JScrollPane defScrollPane;

  public ConceptUI(Concept concept) {
    initUI(concept);
  }

  private void initUI(Concept concept) {
    def.setFont(new Font("Serif", Font.ITALIC, 16));
    def.setLineWrap(true);
    def.setWrapStyleWord(true);
    defScrollPane = new JScrollPane(def);
    defScrollPane
      .setVerticalScrollBarPolicy
      (JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    defScrollPane.setPreferredSize(new Dimension(400, 100));

    code.setText(concept.getPreferredName());
    name.setText(concept.getLongName());
    def.setText(concept.getPreferredDefinition());
    defSource.setText(concept.getDefinitionSource());

    if(!editable) {
      code.setEnabled(false);
      name.setEnabled(false);
      def.setEnabled(false);
      defSource.setEnabled(false);
    }
    
  }

  

}