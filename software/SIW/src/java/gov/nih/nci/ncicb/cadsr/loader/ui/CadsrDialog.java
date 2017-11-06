package gov.nih.nci.ncicb.cadsr.loader.ui;

import gov.nih.nci.cadsr.freestylesearch.util.SearchResults;
import gov.nih.nci.ncicb.cadsr.domain.*;
import gov.nih.nci.ncicb.cadsr.loader.ext.CadsrModule;
import gov.nih.nci.ncicb.cadsr.loader.ext.CadsrModuleListener;
import gov.nih.nci.ncicb.cadsr.loader.ext.CadsrPublicApiModule;
import gov.nih.nci.ncicb.cadsr.loader.ext.FreestyleModule;
import gov.nih.nci.ncicb.cadsr.loader.ui.tree.AttributeNode;
import gov.nih.nci.ncicb.cadsr.loader.ui.tree.ClassNode;
import gov.nih.nci.ncicb.cadsr.loader.ui.tree.UMLNode;
import gov.nih.nci.ncicb.cadsr.loader.util.*;
import gov.nih.nci.ncicb.cadsr.loader.UserSelections;

// bad bad import. Fix me!
import gov.nih.nci.ncicb.cadsr.dao.EagerConstants;


import gov.nih.nci.ncicb.cadsr.loader.ui.util.UIUtil;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Color;
import java.awt.FlowLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.log4j.Logger;

public class CadsrDialog extends JDialog implements ActionListener, KeyListener, CadsrModuleListener
{
  private CadsrDialog _this = this;

  private JLabel searchLabel = new JLabel("Search:");
  private JTextField searchField = new JTextField(10);
  private JLabel whereToSearchLabel = new JLabel("Search By");
  private JLabel numberOfResultsLabel = new JLabel("Results Per Page");
  private JComboBox searchSourceCombo;
  private JComboBox numberOfResultsCombo;
  private JCheckBox includeRetiredCB = new JCheckBox("<html>Include<br> Retired?</html>");
  private JPanel searchPanel = null;
  private JButton searchButton = new JButton("Search");
  
  private AbstractTableModel tableModel = null;
  private JTable resultTable = null;
  
  private static final String PUBLIC_ID = "Public ID";
  private static final String LONG_NAME = "Long Name";
  
  private JButton previousButton = new JButton("Previous"),
    nextButton = new JButton("Next"), 
    suggestButton = new JButton("Suggest"), 
    closeButton = new JButton("Close");
  
  private JLabel indexLabel = new JLabel("");
  private JLabel searchPrefLabel = new JLabel("Search Preferences");

  private CadsrModule cadsrModule;
  private FreestyleModule freestyleModule;

  private Collection<Representation> preferredRepTerms;

  private boolean showRepTermWarning = false;

  private static String SEARCH = "SEARCH",
    PREVIOUS = "PREVIOUS",
    NEXT = "NEXT",
    SUGGEST = "SUGGEST",
    CLOSE = "CLOSE";
    
  private java.util.List<SearchResultWrapper> resultSet = new ArrayList<SearchResultWrapper>();

  private String[] columnNames = {
    "LongName", "Workflow Status", "Public Id", "Version", 
    "Preferred Definition", "Context Name", "Registration Status"
  };

  private int colWidth[] = {15, 15, 15, 15, 30, 15, 15};

  private UserPreferences prefs = UserPreferences.getInstance();

  private int pageSize = prefs.getCadsrResultsPerPage();

  private int pageIndex = 0;
  
  private AdminComponent choiceAdminComponent = null;

  public static final int MODE_OC = 1;
  public static final int MODE_PROP = 2;
  public static final int MODE_VD = 3;
  public static final int MODE_DE = 4;
  public static final int MODE_CD = 5;
  public static final int MODE_CS = 7;
  public static final int MODE_REP = 9;
  //SIW-627
  public static final int MODE_VM = 10;
  private int mode;

  private static Logger logger = Logger.getLogger(CadsrDialog.class.getName());
  
  private UMLNode node;

  private InheritedAttributeList inheritedList = InheritedAttributeList.getInstance();
  
  public CadsrDialog(int runMode)
  {
    super((JFrame)null, true);
    
    this.mode = runMode;

    switch (mode) {
    case MODE_OC:
      this.setTitle("Search for Object Class");
      break;
    case MODE_PROP:
      this.setTitle("Search for Property");
      break;
    case MODE_CS:
      this.setTitle("Search for Classification Schemes");
      break;
    case MODE_VD:
      this.setTitle("Search for Value Domain");
      break;
    case MODE_DE:
      this.setTitle("Search for Data Element");
      break;
    case MODE_CD:
      this.setTitle("Search for Conceptual Domain");
      break;
    case MODE_REP:
      this.setTitle("Search for Rep Term");
      break;
    case MODE_VM://SIW-627
      this.setTitle("Search for Value Meaning");
      break;
    }

    this.getContentPane().setLayout(new BorderLayout());

    String values[] = {LONG_NAME, PUBLIC_ID};
    searchSourceCombo = new JComboBox(values);
    searchPanel = new JPanel(new GridBagLayout());
    
   
    tableModel = new AbstractTableModel() {
        public String getColumnName(int col) {
          return columnNames[col].toString();
        }
        public int getRowCount() { 
          return (int)Math.min(resultSet.size(), pageSize); 
        }
        public int getColumnCount() { 
          return columnNames.length; 
        }
        public Object getValueAt(int row, int col) {
          row = row + pageSize * pageIndex;

          if(row >= resultSet.size())
            return "";

          SearchResultWrapper res = resultSet.get(row);
          
          String s = "";
          switch (col) {
          case 0:
            s = res.getLongName();
            break;
          case 1:
            s = res.getWorkflowStatus();
            break;
          case 2:
            s = res.getPublicId();
            break;
          case 3:
            s = res.getVersion();
            break;
          case 4:
            s = res.getPreferredDefinition();
            break;
          case 5:
            s = res.getContextName();
            break;
          case 6:
            s = res.getRegistrationStatus();
          default:
            break;
          }
          return s;
        }
        public boolean isCellEditable(int row, int col)
        { return false; }
  
   };
    
   resultTable = new JTable(tableModel) {
        public String getToolTipText(java.awt.event.MouseEvent e) {
          java.awt.Point p = e.getPoint();
          int rowIndex = rowAtPoint(p);
          int colIndex = columnAtPoint(p);
          
          return (String)getModel().getValueAt(rowIndex, colIndex);
        }
      };
      
    DefaultTableCellRenderer  tcrColumn  =  new DefaultTableCellRenderer();
    tcrColumn.setVerticalAlignment(JTextField.TOP);
    resultTable.getColumnModel().getColumn(3).setCellRenderer(tcrColumn);
    resultTable.getColumnModel().getColumn(4).setCellRenderer(tcrColumn);
    
    if((mode == MODE_VD) || (mode == MODE_VM)) {
      resultTable.getColumnModel().getColumn(6).setMaxWidth(0);
      resultTable.getColumnModel().getColumn(6).setResizable(false);
    }

    int c = 0;
    for(int width : colWidth) {
      TableColumn col = resultTable.getColumnModel().getColumn(c++);
      col.setPreferredWidth(width);
    }
    
    resultTable.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent evt) {
          if(evt.getClickCount() == 2) {
            int row = resultTable.getSelectedRow();
            if(row > -1) {
              SearchResultWrapper choiceSearchResultWrapper = (resultSet.get(pageIndex * pageSize + row));
              if(mode == MODE_DE) {
                _this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                Map<String, Object> queryFields = new HashMap<String, Object>();
                queryFields.put(CadsrModule.PUBLIC_ID, new String(choiceSearchResultWrapper.getPublicId()));
                queryFields.put(CadsrModule.VERSION, new Float(choiceSearchResultWrapper.getVersion()));
                
                
                try {
                  Collection<DataElement> result = cadsrModule.findDataElement(queryFields);
                  if(result.size() == 0)
                    JOptionPane.showMessageDialog
                      (null, "No Results Found","Empty Result", JOptionPane.ERROR_MESSAGE);
                
                  DataElement de = result.iterator().next();
                  choiceAdminComponent = de;
                  //DataElement de = (DataElement)choiceSearchResultWrapper;
                  // is this DE valid?
                  // i.e does it have an Object Class and Property?
                  // if not, throw error message
                  if(de.getDataElementConcept().getObjectClass() == null || de.getDataElementConcept().getProperty() == null) {
                    JOptionPane.showMessageDialog
                      (null, PropertyAccessor.getProperty("de.invalid"), "Invalid Selection", JOptionPane.ERROR_MESSAGE);
                    choiceSearchResultWrapper = null;
                    return;
                  }
                } catch (Exception e) {
                  logger.error("Error querying Cadsr " + e);
                } finally {
                  _this.setCursor(Cursor.getDefaultCursor());
                }
              } 
              else 
              {
                choiceAdminComponent = choiceSearchResultWrapper.getAdminComponent();
              }
              _this.setVisible(false);
            }
          }
        }
      });
    
    JScrollPane scrollPane = new JScrollPane(resultTable);
    
    Integer[] number = {new Integer(5),
                        new Integer(10),
                        new Integer(25),
                        new Integer(50),
                        new Integer(100)};
    
    numberOfResultsCombo = new JComboBox(number);
    numberOfResultsCombo.setSelectedItem(prefs.getCadsrResultsPerPage());
    
    if(mode == MODE_DE) {
      searchButton.setText("Freestyle Search");
      searchField.setColumns(20);
      UIUtil.insertInBag(searchPanel, searchLabel, 0, 0);
      UIUtil.insertInBag(searchPanel, searchField, 1, 0);
      UIUtil.insertInBag(searchPanel, searchButton, 4, 0);
      UIUtil.insertInBag(searchPanel, includeRetiredCB, 5, 0);
      UIUtil.insertInBag(searchPanel, suggestButton, 6, 0);
    }
    
    else {
      UIUtil.insertInBag(searchPanel, searchLabel, 0, 0);
      UIUtil.insertInBag(searchPanel, searchField, 1, 0);
      UIUtil.insertInBag(searchPanel, whereToSearchLabel, 2, 0);
      UIUtil.insertInBag(searchPanel, searchSourceCombo, 3, 0);
      UIUtil.insertInBag(searchPanel, includeRetiredCB, 4, 0);
      UIUtil.insertInBag(searchPanel, searchButton, 5, 0);
    }
    
    searchField.addKeyListener(this);

    searchButton.addActionListener(this);
    searchButton.addKeyListener(this);
    searchButton.setActionCommand(SEARCH);
    
    suggestButton.addActionListener(this);
    suggestButton.setActionCommand(SUGGEST);
    
    JPanel browsePanel = new JPanel();
    browsePanel.add(previousButton);
    browsePanel.add(indexLabel);
    browsePanel.add(nextButton);
    browsePanel.add(closeButton);
    browsePanel.add(numberOfResultsLabel);
    browsePanel.add(numberOfResultsCombo);
    browsePanel.add(searchPrefLabel);
        
    searchPrefLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
              SearchPreferencesDialog spd = new SearchPreferencesDialog();
              spd.setVisible(true);
            }
            public void mouseEntered(MouseEvent evt) {
               searchPrefLabel.setForeground(Color.BLUE);
            }
            public void mouseExited(MouseEvent evt) {
                searchPrefLabel.setForeground(Color.BLACK);
            }            
        });
    
    numberOfResultsCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        JComboBox cb = (JComboBox)event.getSource();
        Integer selection = (Integer)cb.getSelectedItem();
        pageSize = selection.intValue();
        updateTable();
        updateIndexLabel();
        prefs.setCadsrResultsPerPage(selection.intValue());
      }
    });
    
    previousButton.setActionCommand(PREVIOUS);
    nextButton.setActionCommand(NEXT);
    closeButton.setActionCommand(CLOSE);
    previousButton.setEnabled(false);
    nextButton.setEnabled(false);
    previousButton.addActionListener(this);
    nextButton.addActionListener(this);
    closeButton.addActionListener(this);
    
//     if(mode == MODE_REP){
//         JLabel explainLabel = new JLabel("<html><u color=BLUE>Explain this</u></html>");
//         ToolTipManager.sharedInstance().registerComponent(explainLabel);
//         ToolTipManager.sharedInstance().setDismissDelay(3600000);
//         explainLabel.setToolTipText(PropertyAccessor.getProperty("pref.rep.term.label"));
//         JPanel explainThisPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
//         explainThisPanel.add(explainLabel);
//         this.getContentPane().add(explainThisPanel, BorderLayout.NORTH);
//     }
//     else{
    this.getContentPane().add(searchPanel, BorderLayout.NORTH);
//     }
    this.getContentPane().add(scrollPane, BorderLayout.CENTER);
    this.getContentPane().add(browsePanel, BorderLayout.SOUTH);
    this.setSize(630,525);

    suggestButton.setVisible(true);
    
  }
  
  public void startSearch(String searchString) {
    searchField.setText(searchString);
    searchButton.doClick();
  }
  
  public AdminComponent getAdminComponent() {
    try {
      return choiceAdminComponent;
    } finally {
      choiceAdminComponent = null;
    } 
  }

  public void startSearchPreferredRepTerms() {
    
    // disable this feature for now!!
    if (true)
      return;
    

    if(preferredRepTerms == null) {
      try {
        preferredRepTerms = cadsrModule.findPreferredRepTerms();
      } catch (Exception e) {
        logger.error("Error querying Cadsr " + e);
      } finally {
        _this.setCursor(Cursor.getDefaultCursor());
      }
    }
    
    if(preferredRepTerms != null) {
      resultSet = new ArrayList<SearchResultWrapper>();
      for(Representation rep : preferredRepTerms)
        resultSet.add(new SearchResultWrapper(rep));
      
      Collections.sort(resultSet, new Comparator<SearchResultWrapper>() {
        public int compare(SearchResultWrapper s1, SearchResultWrapper s2) {
            return s1.getLongName().compareTo(s2.getLongName());
        }
      });
    }
  }

  public void startSearchCDEByOCConcept(Concept concept) {
    resultSet = new ArrayList<SearchResultWrapper>();
    
    for(DataElement de : cadsrModule.findDEByOCConcept(concept))
      resultSet.add(new SearchResultWrapper(de));
    
    pageIndex = 0;
    updateTable();
  }
  
  public void actionPerformed(ActionEvent event) 
  {
    JButton button = (JButton)event.getSource();
    boolean cbStatus = includeRetiredCB.isSelected();
    if(button.getActionCommand().equals(SEARCH)) {

      // !! Disable this for now


//       if(mode == MODE_REP) {
//         if(!showRepTermWarning) {
//           int result = JOptionPane.showConfirmDialog(_this, PropertyAccessor.getProperty("search.nonPreferred.repTerms"), "Warning", JOptionPane.YES_NO_OPTION);
//           if (result == JOptionPane.NO_OPTION)
//             return;
//           showRepTermWarning = true;
//         }
//       }
      
      String selection = (String) searchSourceCombo.getSelectedItem();
      String text = searchField.getText() == null ? "" : searchField.getText().trim();

      resultSet = new ArrayList<SearchResultWrapper>();
      
      _this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      
      try {
        Map<String, Object> queryFields = new HashMap<String, Object>();
        if(selection.equals(LONG_NAME)) {
          queryFields.put(CadsrModule.LONG_NAME, text);
        } else if(selection.equals(PUBLIC_ID)) {
          queryFields.put(CadsrModule.PUBLIC_ID, new Long(text));
        }
        switch (mode) {
        case MODE_OC:
          for(ObjectClass oc : cadsrModule.findObjectClass(queryFields))
            resultSet.add(new SearchResultWrapper(oc));
          break;
        case MODE_PROP:
          for(Property p : cadsrModule.findProperty(queryFields))
            resultSet.add(new SearchResultWrapper(p));
          break;
        case MODE_DE:
          UserSelections selections = UserSelections.getInstance();
          Boolean er = (Boolean)selections.getProperty("exclude.retired.workflow.status");
          boolean excludeRetired = false;
          if(er != null)
            excludeRetired = er;

          DataElement searchDE = (DataElement)node.getUserObject();
          Property prop = null;
          if(inheritedList.isInherited(searchDE)) {
            DataElement parentDE = inheritedList.getParent(searchDE);
            prop = parentDE.getDataElementConcept().getProperty();
          }

          for(SearchResults sr : freestyleModule.findSearchResults(text, excludeRetired)) {
            if(prop != null && prop.getVersion() != null && (!prop.getPublicId().equals(new Integer(sr.getPropertyID()).toString()) || !prop.getVersion().equals(new Float(sr.getPropertyVersion())))) // prop doesn't match, skip this one.
              continue;
            ObjectClass searchOC = searchDE.getDataElementConcept().getObjectClass();
            if(searchOC.getPublicId() != null
               && (!searchOC.getPublicId().equals(new Integer(sr.getObjectClassID()).toString()) 
                   || !searchOC.getVersion().equals(new Float(sr.getObjectClassVersion()))))
              continue;

            Object[] list = 
              (Object[]) selections.getProperty("exclude.registration.statuses");
            
            Set<String> setOfExcluded = new HashSet<String>();
            String[] defaultStatuses = PropertyAccessor.getProperty("default.excluded.registration.statuses").split(",");
            if(list != null) {
              for(Object s : list)
                setOfExcluded.add(s.toString());
            }
            else {
              for(String s : defaultStatuses)
                setOfExcluded.add(s);
            }
            
            if(sr != null)
              if(!setOfExcluded.contains(sr.getRegistrationStatus()))
                  if(cbStatus)
                      resultSet.add(new SearchResultWrapper(sr));
                  else
                    if(sr.getWorkflowStatus() != null 
                      && (sr.getWorkflowStatus().toUpperCase().indexOf("RETIRED") == -1))
                        resultSet.add(new SearchResultWrapper(sr));
          }
          break;
        case MODE_CS:
          List<String> eager = new ArrayList<String>();
          eager.add(EagerConstants.CS_CSI);
          eager.add(EagerConstants.CS_CSI + ".csi");

          for(ClassificationScheme cs : cadsrModule.findClassificationScheme(queryFields, eager))
            resultSet.add(new SearchResultWrapper(cs));       
          break;
        case MODE_CD:
          for(ConceptualDomain cd : cadsrModule.findConceptualDomain(queryFields))
            if(cbStatus)
              resultSet.add(new SearchResultWrapper(cd));       
            else
              if(cd.getWorkflowStatus() != null 
                 && (cd.getWorkflowStatus().toUpperCase().indexOf("RETIRED") == -1))
                resultSet.add(new SearchResultWrapper(cd));
          break;
        case MODE_REP:
          queryFields.put(CadsrModule.WORKFLOW_STATUS, Representation.WF_STATUS_RELEASED);
          for(Representation rep : cadsrModule.findRepresentation(queryFields))
            if(cbStatus)
              resultSet.add(new SearchResultWrapper(rep));       
            else
              if(rep.getWorkflowStatus() != null 
                 && (rep.getWorkflowStatus().toUpperCase().indexOf("RETIRED") == -1))
                resultSet.add(new SearchResultWrapper(rep));
          break;
        case MODE_VD:
          for(ValueDomain vd : cadsrModule.findValueDomain(queryFields)){
            if(cbStatus)
                resultSet.add(new SearchResultWrapper(vd));
            else
                if(vd.getWorkflowStatus() != null 
                    && (vd.getWorkflowStatus().toUpperCase().indexOf("RETIRED") == -1))
                        resultSet.add(new SearchResultWrapper(vd));
          }
          break;
        case MODE_VM:
            for(ValueMeaning vm : cadsrModule.findValueMeaning(queryFields)){
              if(cbStatus)
                  resultSet.add(new SearchResultWrapper(vm));
              else
                  if(vm.getWorkflowStatus() != null 
                      && (vm.getWorkflowStatus().toUpperCase().indexOf("RETIRED") == -1))
                          resultSet.add(new SearchResultWrapper(vm));
            }
            break;
        }
      } catch (Exception e){
        logger.error("Error querying Cadsr " + e);
      } // end of try-catch
      
      _this.setCursor(Cursor.getDefaultCursor());

      // remove excluded contexts
      {
        List<String> excludeContext = Arrays.asList(PropertyAccessor.getProperty("vd.exclude.contexts").split(","));
        for(ListIterator<SearchResultWrapper> it = resultSet.listIterator(); it.hasNext();) {
          SearchResultWrapper sr = it.next();
          if(excludeContext.contains(sr.getContextName())) {
            it.remove();
          }
        }
      }

      pageIndex = 0;
      updateTable();
      
    } else if(button.getActionCommand().equals(SUGGEST)) {
      
        searchField.setText("");
        resultSet = new ArrayList();

        AttributeNode attrNode = (AttributeNode)node;
        DataElement _de = (DataElement)node.getUserObject();
        ClassNode classNode = null;
        if(inheritedList.isInherited(_de)) {
          classNode = (ClassNode)node.getParent().getParent();
        } else {
         classNode = (ClassNode)node.getParent();
        }
        String className = classNode.getDisplay();
        String attrName = attrNode.getDisplay();

        _this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        try {
            for(DataElement de : cadsrModule.suggestDataElement(className, attrName))
                resultSet.add(new SearchResultWrapper(de));
        }
        catch (Exception e) {
            logger.error("Error querying Cadsr " + e);
        }

        _this.setCursor(Cursor.getDefaultCursor());

        pageIndex = 0;
        updateTable();
        
    }
    else if(button.getActionCommand().equals(PREVIOUS)) {
      pageIndex--;
      updateTable();
    } else if(button.getActionCommand().equals(NEXT)) {
      pageIndex++;
      updateTable();
    } else if(button.getActionCommand().equals(CLOSE)) {
      this.setVisible(false);
    }
    updateIndexLabel();
  }
  
  private void updateIndexLabel() {
    if(resultSet.size() == 0) {
      indexLabel.setText("");
    } else {
      StringBuilder sb = new StringBuilder();
      int start = pageSize * pageIndex;
      int end = (int)Math.min(resultSet.size(), start + pageSize); 
      sb.append(start);
      sb.append("-");
      sb.append(end);
      sb.append(" of " + resultSet.size());
      indexLabel.setText(sb.toString());
    }
    
  }

  private void updateTable() {
    tableModel.fireTableDataChanged();

    previousButton.setEnabled(pageIndex > 0);

    nextButton.setEnabled(resultSet.size() > (pageIndex * pageSize + pageSize));

  }
  
  public void keyPressed(KeyEvent evt) {
    if(evt.getKeyCode() == KeyEvent.VK_ENTER)
      searchButton.doClick();
  }

  public void keyTyped(KeyEvent evt) {
    if(evt.getKeyCode() == KeyEvent.VK_ENTER)
      searchButton.doClick();
  }

  public void keyReleased(KeyEvent evt) {
  }
 
  /**
   * IoC setter
   */
  public void setCadsrModule(CadsrModule module) {
    this.cadsrModule = module;
  }
 
  public void setFreestyleModule(FreestyleModule freestyleModule) {
    this.freestyleModule = freestyleModule;
  }

  public void init(UMLNode node) {
      this.node = node;
  }
  
  public static void main(String[] args) 
  {
        CadsrDialog dialog = new CadsrDialog(CadsrDialog.MODE_REP);

        dialog.setCadsrModule(new CadsrPublicApiModule());

//         Concept con = DomainObjectFactory.newConcept();
//         con.setPreferredName("C16612");
        
//         dialog.startSearchCDEByOCConcept(con);

        dialog.startSearchPreferredRepTerms();
        dialog.setVisible(true);

        
  }
  

}

class SearchResultWrapper {
  private AdminComponent ac;
  private SearchResults sr;
  
  public SearchResultWrapper(Object o)
  {
    if(o instanceof AdminComponent)
      ac = (AdminComponent)o;
    else
      sr = (SearchResults)o;
  }
  
  public AdminComponent getAdminComponent() 
  {
    return ac;
  }
  
  public SearchResults getSearchResults() 
  {
    return sr;
  }
  
  public String getLongName() 
  {
    if(ac != null)
      return ac.getLongName();
    else
      return sr.getLongName();
  }
  
  public String getPreferredName() 
  {
    if(ac != null)
      return ac.getPreferredName();
    else
      return sr.getPreferredName();
  }
  
  public String getPublicId() 
  {
    if(ac != null)
      return ac.getPublicId();
    else
      return new Integer(sr.getPublicID()).toString();
  }
  
  public String getVersion() 
  {
    if (ac != null)
      return "" + ac.getVersion();
    else
      return new Float(sr.getVersion()).toString();
  }
  
  public String getPreferredDefinition() 
  {
    if(ac != null)
      return ac.getPreferredDefinition();
    else
      return sr.getPreferredDefinition();
  }
  
  public String getContextName()
  {
    if(ac != null)
      return ac.getContext().getName();
    else
      return sr.getContextName();
  }

  public String getRegistrationStatus() 
  {
    if(ac != null)
      return "";
    else
      return sr.getRegistrationStatus();
  }

  public String getWorkflowStatus() 
  {
    if(ac != null)
      return ac.getWorkflowStatus();
    else
      return sr.getWorkflowStatus();
  }

}