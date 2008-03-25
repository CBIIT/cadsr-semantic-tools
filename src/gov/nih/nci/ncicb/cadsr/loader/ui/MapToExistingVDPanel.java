package gov.nih.nci.ncicb.cadsr.loader.ui;

import gov.nih.nci.ncicb.cadsr.domain.DomainObjectFactory;
import gov.nih.nci.ncicb.cadsr.domain.PermissibleValue;
import gov.nih.nci.ncicb.cadsr.domain.ValueDomain;
import gov.nih.nci.ncicb.cadsr.loader.event.ElementChangeEvent;
import gov.nih.nci.ncicb.cadsr.loader.event.ElementChangeListener;
import gov.nih.nci.ncicb.cadsr.loader.ext.CadsrModule;
import gov.nih.nci.ncicb.cadsr.loader.ext.CadsrModuleListener;
import gov.nih.nci.ncicb.cadsr.loader.ui.tree.UMLNode;
import gov.nih.nci.ncicb.cadsr.loader.ui.util.UIUtil;
import gov.nih.nci.ncicb.cadsr.loader.util.ConventionUtil;
import gov.nih.nci.ncicb.cadsr.loader.util.StringUtil;

import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class MapToExistingVDPanel extends JPanel 
    implements Editable, CadsrModuleListener
{
  private JLabel vdPrefDefTitleLabel = new JLabel("VD Preferred Definition"),
    vdLongNameLabel = new JLabel("VD Long Name"),
    vdDatatypeTitleLabel = new JLabel("VD Datatype"),
    vdCdIdTitleLabel = new JLabel("VD CD PublicId / Version"),
    vdRepIdTitleLabel = new JLabel("Representation Term"),
    vdCdLongNameTitleLabel = new JLabel("VD CD Long Name"),
    vdCreatedByLabel = new JLabel("Created By"),
    vdCreatedDateLabel = new JLabel("Created Date");
  private JLabel vdDatatypeTitleLabelValue = new JLabel(),
    vdLongNameLabelValue = new JLabel(),
    vdTypeTitleLabelValue = new JLabel(),
    vdCdIdTitleLabelValue = new JLabel(),
    vdRepIdTitleLabelValue = new JLabel(),
    vdCdLongNameTitleLabelValue = new JLabel(),
    vdCreatedByLabelValue = new JLabel(),
    vdCreatedDateLabelValue = new JLabel();
  private JTextArea vdPrefDefValueTextField = new JTextArea();
  
  private CadsrDialog cadsrVDDialog;

//   private JScrollPane scrollPane;

  private ValueDomain vd, tempVD;
  private JButton searchButton;
  private JButton lvdCadsrButton;
  private UMLNode umlNode;
  private CadsrModule cadsrModule;

  private boolean isInitialized = false;
  private List<ElementChangeListener> changeListeners = new ArrayList<ElementChangeListener>();
  private List<PropertyChangeListener> propChangeListeners = new ArrayList<PropertyChangeListener>();  
  private Logger logger = Logger.getLogger(MapToExistingVDPanel.class.getName());

  
  public void addPropertyChangeListener(PropertyChangeListener l) {
    super.addPropertyChangeListener(l);;
    propChangeListeners.add(l);
  }
  
  
  public MapToExistingVDPanel() {
    if(!isInitialized)
      initUI();
  }
  
  public void update(ValueDomain vd, UMLNode umlNode) 
  {
    this.vd = vd;
    this.umlNode = umlNode;

    tempVD = DomainObjectFactory.newValueDomain();
    tempVD.setConceptualDomain(vd.getConceptualDomain());
    tempVD.setPreferredDefinition(vd.getPreferredDefinition());
    tempVD.setRepresentation(vd.getRepresentation());
    tempVD.setLongName(vd.getLongName());
    tempVD.setDataType(vd.getDataType());
    tempVD.setVdType(vd.getVdType());
    tempVD.setPublicId(vd.getPublicId());
    tempVD.setVersion(vd.getVersion());
    tempVD.setAudit(vd.getAudit());

    if(!isInitialized)
      initUI();

    initValues();
  }
  
  private void initUI() 
  {
    isInitialized = true;

    this.setLayout(new BorderLayout());
    JPanel mainPanel = new JPanel(new GridBagLayout());

    vdPrefDefValueTextField.setLineWrap(true);
    vdPrefDefValueTextField.setEnabled(false);
    vdPrefDefValueTextField.setWrapStyleWord(true);
    JScrollPane defScrollPane = new JScrollPane(vdPrefDefValueTextField);
    defScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    defScrollPane.setPreferredSize(new Dimension(200, 100));
      
    searchButton = new JButton("Search Value Domains");
    searchButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
        cadsrVDDialog.setAlwaysOnTop(true);
        cadsrVDDialog.setVisible(true);
        tempVD = (ValueDomain)cadsrVDDialog.getAdminComponent();
        setSearchedValues();
        firePropertyChangeEvent(new PropertyChangeEvent(this, ApplyButtonPanel.SAVE, null, true));
    }});

    lvdCadsrButton = new JButton("<html>Compare<br>Values</html>");
    lvdCadsrButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          try {
            List<PermissibleValue> cadsrPVs = cadsrModule.getPermissibleValues(tempVD);
            List<PermissibleValue> localPVs = vd.getPermissibleValues();
            PVCompareDialog pvCompareDialog = new PVCompareDialog(localPVs, cadsrPVs);
            pvCompareDialog.setVisible(true);
            pvCompareDialog.setAlwaysOnTop(true);
          } catch (Exception e) {
            logger.error(e);
          } 
        }});
    
    UIUtil.insertInBag(mainPanel, vdPrefDefTitleLabel, 0, 1);
    UIUtil.insertInBag(mainPanel, defScrollPane, 1, 1, 3, 2); 
    
    UIUtil.insertInBag(mainPanel, lvdCadsrButton, 2, 1, 1, 10);

    UIUtil.insertInBag(mainPanel, vdLongNameLabel, 0, 3);
    UIUtil.insertInBag(mainPanel, vdLongNameLabelValue, 1, 3);

    UIUtil.insertInBag(mainPanel, vdDatatypeTitleLabel, 0, 5);
    UIUtil.insertInBag(mainPanel, vdDatatypeTitleLabelValue, 1, 5);
    
    UIUtil.insertInBag(mainPanel, vdCdIdTitleLabel, 0, 6);
    UIUtil.insertInBag(mainPanel, vdCdIdTitleLabelValue, 1, 6);
    
    UIUtil.insertInBag(mainPanel, vdCdLongNameTitleLabel, 0, 7);
    UIUtil.insertInBag(mainPanel, vdCdLongNameTitleLabelValue, 1, 7);

    UIUtil.insertInBag(mainPanel, vdRepIdTitleLabel, 0, 8);
    UIUtil.insertInBag(mainPanel, vdRepIdTitleLabelValue, 1, 8);

    UIUtil.insertInBag(mainPanel, vdCreatedByLabel, 0, 9);
    UIUtil.insertInBag(mainPanel, vdCreatedByLabelValue, 1, 9);

    UIUtil.insertInBag(mainPanel, vdCreatedDateLabel, 0, 10);
    UIUtil.insertInBag(mainPanel, vdCreatedDateLabelValue, 1, 10);

    JScrollPane mainScrollPane = new JScrollPane(mainPanel);
    mainScrollPane.getVerticalScrollBar().setUnitIncrement(30);

    JPanel searchButtonPanel = new JPanel();
    searchButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT)); 
    searchButtonPanel.add(searchButton);
//     searchButtonPanel.add(lvdCadsrButton);

    this.add(mainScrollPane, BorderLayout.CENTER);
    this.add(searchButtonPanel, BorderLayout.SOUTH);
    
  }
  
  private void initValues() 
  {
  
    if(StringUtil.isEmpty(tempVD.getPublicId())) {
        vdPrefDefValueTextField.setText("");
        vdLongNameLabelValue.setText("");
        vdDatatypeTitleLabelValue.setText("");
        vdTypeTitleLabelValue.setText("");
        vdCdIdTitleLabelValue.setText("");
        vdCdLongNameTitleLabelValue.setText("");
        vdRepIdTitleLabelValue.setText("");
        vdCreatedByLabelValue.setText("");
        vdCreatedDateLabelValue.setText("");

        lvdCadsrButton.setVisible(false);
    } else {
        vdPrefDefValueTextField.setText(tempVD.getPreferredDefinition());
        vdLongNameLabelValue.setText(tempVD.getLongName());
        vdDatatypeTitleLabelValue.setText(tempVD.getDataType());
        vdTypeTitleLabelValue.setText(tempVD.getVdType());
        vdCdIdTitleLabelValue.setText(ConventionUtil.publicIdVersion(tempVD.getConceptualDomain()));
        vdCdLongNameTitleLabelValue.setText(tempVD.getConceptualDomain().getLongName());
        vdRepIdTitleLabelValue.setText(ConventionUtil.publicIdVersion(tempVD.getRepresentation()));
        vdCreatedByLabelValue.setText(tempVD.getAudit().getCreatedBy());
        vdCreatedDateLabelValue.setText(getFormatedDate(tempVD.getAudit().getCreationDate()));

        lvdCadsrButton.setVisible(true);
    }
  }
  
  private void setSearchedValues(){
    if(tempVD != null){
      if(tempVD.getConceptualDomain() != null){
        vdPrefDefValueTextField.setText(tempVD.getPreferredDefinition());
        vdCdIdTitleLabelValue.setText(ConventionUtil.publicIdVersion(tempVD.getConceptualDomain()));
        vdCdLongNameTitleLabelValue.setText(tempVD.getConceptualDomain().getLongName());
      }
      if(tempVD.getRepresentation() != null)
        vdRepIdTitleLabelValue.setText(ConventionUtil.publicIdVersion(tempVD.getRepresentation()));
      vdLongNameLabelValue.setText(tempVD.getLongName());
      vdDatatypeTitleLabelValue.setText(tempVD.getDataType());
      vdTypeTitleLabelValue.setText(tempVD.getVdType());
      vdCreatedByLabelValue.setText(tempVD.getAudit().getCreatedBy());
      vdCreatedDateLabelValue.setText(getFormatedDate(tempVD.getAudit().getCreationDate()));

      lvdCadsrButton.setVisible(true);
    } else
      lvdCadsrButton.setVisible(false);
      
    
  }  
  public static void main(String args[]) 
  {
//    JFrame frame = new JFrame();
//    ValueDomainViewPanel vdPanel = new ValueDomainViewPanel();
//    vdPanel.setVisible(true);
//    frame.add(vdPanel);
//    frame.setVisible(true);
//    frame.setSize(450, 350);
  }

    public void applyPressed() {   
        vd.setConceptualDomain(tempVD.getConceptualDomain());
        vd.setRepresentation(tempVD.getRepresentation());
        vd.setLongName(tempVD.getLongName());
        vd.setDataType(tempVD.getDataType());
        vd.setPreferredDefinition(tempVD.getPreferredDefinition());
        vd.setVdType(tempVD.getVdType());
        vd.setPublicId(tempVD.getPublicId());
        vd.setVersion(tempVD.getVersion());
        vd.setAudit(tempVD.getAudit());
        vd.setId(tempVD.getId());
        
        firePropertyChangeEvent(new PropertyChangeEvent(this, ApplyButtonPanel.SAVE, null, false));
        fireElementChangeEvent(new ElementChangeEvent(umlNode));
    }

    public void setCadsrVDDialog(CadsrDialog cadsrVDDialog) {
      this.cadsrVDDialog = cadsrVDDialog;
    }

    public void setCadsrModule(CadsrModule cadsrModule){
      this.cadsrModule = cadsrModule;
    }
    
    public void addElementChangeListener(ElementChangeListener listener){
        changeListeners.add(listener);
    }
    private void fireElementChangeEvent(ElementChangeEvent event) {
      for(ElementChangeListener l : changeListeners)
        l.elementChanged(event);
    }
    private void firePropertyChangeEvent(PropertyChangeEvent evt) {
      for(PropertyChangeListener l : propChangeListeners) 
        l.propertyChange(evt);
    }
    private String getFormatedDate(java.util.Date date){
        return DateFormat.getDateTimeInstance().format(date);

    }
}
