/*
 * Copyright (C) 2017 Leidos Biomedical Research, Inc. - All rights reserved.
 */
package gov.nih.nci.ncicb.cadsr.loader.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.border.TitledBorder;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import gov.nih.nci.cadsr.common.Logger;
import gov.nih.nci.ncicb.cadsr.domain.ConceptualDomain;
import gov.nih.nci.ncicb.cadsr.domain.ValueDomain;
import gov.nih.nci.ncicb.cadsr.domain.ValueMeaning;
import gov.nih.nci.ncicb.cadsr.loader.event.ElementChangeEvent;
import gov.nih.nci.ncicb.cadsr.loader.event.ElementChangeListener;
import gov.nih.nci.ncicb.cadsr.loader.ext.CadsrPublicApiModule;
import gov.nih.nci.ncicb.cadsr.loader.ui.ConceptUI.TextFieldLimiter;
import gov.nih.nci.ncicb.cadsr.loader.ui.tree.UMLNode;
import gov.nih.nci.ncicb.cadsr.loader.ui.tree.ValueMeaningNode;
import gov.nih.nci.ncicb.cadsr.loader.ui.util.UIUtil;
import gov.nih.nci.ncicb.cadsr.loader.util.BeansAccessor;
import gov.nih.nci.ncicb.cadsr.loader.util.ConventionUtil;
import gov.nih.nci.ncicb.cadsr.loader.util.PropertyAccessor;
import gov.nih.nci.ncicb.cadsr.loader.util.StringUtil;

@SuppressWarnings("rawtypes")
//SIW-627
public class PublicIdPanel extends JPanel implements Editable, DocumentListener
{
	private static final long serialVersionUID = 1L;
	private org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PublicIdPanel.class.getName());
    //public JTextArea longName = new JTextArea("long name");//FIXME remove
    //public JLabel longNameLabel = new JLabel("long name in here");//FIXME remove
    protected JTextArea longNameArea = new JTextArea(4, 54);
    private JButton cdSearchButton = new JButton("Search");
    public JLabel vmPublicIdJLabel;
    private ValueMeaning vm, tempVM;
    
	private UMLNode node;
    private JPanel publicIdPanel;
    
    private boolean modified = false;
     
    private List<ElementChangeListener> changeListeners = new ArrayList<ElementChangeListener>();
    private List<PropertyChangeListener> propChangeListeners = new ArrayList<PropertyChangeListener>();  
    
    JButton deleteButton;
    
    private CadsrPublicApiModule cadsrPublicApiModule = new CadsrPublicApiModule();
    
    private String pvValue;
    
    public PublicIdPanel(UMLNode node) {
    	this.node = node;
    	initUI() ;
    }
    
    public JButton getDeleteButton() {
        return deleteButton;
    }
    
    public void initUI() 
    {    
    	if(node instanceof ValueMeaningNode) {
    	    longNameArea.setLineWrap(true);
    	    longNameArea.setEditable(false);
    		this.setLayout(new BorderLayout());
    	    this.deleteButton = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("delete-x.gif")));
    	    deleteButton.setBorder(null);
    	    deleteButton.setContentAreaFilled(false);
    	    deleteButton.setOpaque(true);
    	    deleteButton.setToolTipText("Delete this VM ID");
    	    deleteButton.setPreferredSize(new Dimension(28,29));
    	    
        	ValueMeaning vm = (ValueMeaning)node.getUserObject();
    		pvValue = vm.getLongName();
    		logger.debug("PV Value: " + pvValue + ", node.getFullPath(): " + node.getFullPath());
        	Border tb = BorderFactory.createTitledBorder("Map to Value Meaning");
        	//Border tb = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.red), "Map to Value Meaning");//TODO this is for panel testing
        	this.setBorder(tb);   
	        publicIdPanel = new JPanel();
        	//Border blackline = BorderFactory.createLineBorder(Color.black);//TODO for testing
	        //publicIdPanel.setBorder(blackline);
  
	        publicIdPanel.setLayout(new GridBagLayout());
	        if (vm.getVersion() == null) {
	        	vm.setVersion(1f);
	        }
	        
	        //I keep this commented code now for a chance we want VM LongName be scrollable; 
	        //TODO remove when VM LongName format is decided 
//		        TextFieldLimiter tf = new TextFieldLimiter(2000);
//		        longName.setDocument(tf);
//		        longName.setFont(new Font("Serif", Font.ITALIC, 16));
//		        longName.setLineWrap(true);
//		        longName.setWrapStyleWord(true);
//		        longName.setEnabled(false);
//		        longName.setText("FIXME: we shall see VM Long name here retrieved from caDSR");
//		        JScrollPane longNameScrollPane = new JScrollPane(longName);
//		        longNameScrollPane
//		          .setVerticalScrollBarPolicy
//		          (JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
//		        longNameScrollPane.setPreferredSize(new Dimension(400, 64));
//		        UIUtil.insertInBag(publicIdPanel, longNameScrollPane, 0, 2, 2, 1);//col 0 row 2 width 2 height 1
	        
	        UIUtil.insertInBag(publicIdPanel, new JLabel("VM Long Name: "), 0, 1);//col 0 row 1       
	        UIUtil.insertInBag(publicIdPanel, longNameArea, 0, 2, 2, 1);
	        JPanel vdCDIdVersionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	        vdCDIdVersionPanel.setBorder(BorderFactory.createLineBorder(Color.blue));
	        vmPublicIdJLabel = new JLabel();
	        vdCDIdVersionPanel.add(vmPublicIdJLabel);
	        vmPublicIdJLabel.setText(ConventionUtil.publicIdVersion(vm));
	        //FIXME API call if empty VM long name
	        longNameArea.setText(StringUtil.isEmpty(vm.getPublicId())? null : vm.getLongName());
	        setLongName(vm);//searching VM name if VM ID is provided
	        vdCDIdVersionPanel.add(cdSearchButton);
	        UIUtil.insertInBag(publicIdPanel, new JLabel("VM ID Version:"), 0, 0);//col 0 row 0
	        UIUtil.insertInBag(publicIdPanel, vdCDIdVersionPanel, 1, 0);//col 1 row 0
	        UIUtil.insertInBag(publicIdPanel, deleteButton, 2, 0);
	        cdSearchButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent evt) {
	            	CadsrDialog cadsrVMDialog = BeansAccessor.getCadsrVMDialog();
	            	cadsrVMDialog.setAlwaysOnTop(true);
	                cadsrVMDialog.setVisible(true);
	                tempVM = (gov.nih.nci.ncicb.cadsr.domain.ValueMeaning)cadsrVMDialog.getAdminComponent();
	                //vmPublicToPrivate
	                if(tempVM == null) return;
	                //initValues();
	                logger.debug("Foung VM in caDSR: " + tempVM);
	                setVmSearchedValues();
	                firePropertyChangeEvent(new PropertyChangeEvent(this, ApplyButtonPanel.SAVE, null, true));
	                fireElementChangeEvent(new ElementChangeEvent(node));
	              }});
	        deleteButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	longNameArea.setText(null);
	            	vmPublicIdJLabel.setText(null);
	            	ValueMeaning vm = (ValueMeaning)node.getUserObject();
	            	vm.setPublicId(null);
	            	vm.setVersion(null);
	            	//this is to enable Apply button
	            	firePropertyChangeEvent(new PropertyChangeEvent(this, ApplyButtonPanel.SAVE, null, true));
	            	fireElementChangeEvent(new ElementChangeEvent(node));
	            }
	        });
	        publicIdPanel.setPreferredSize(publicIdPanel.getPreferredSize());
	        this.add(publicIdPanel);
	        this.setPreferredSize(this.getPreferredSize());
        }  
    }
    
    private void setLongName(ValueMeaning vm2) {
    	if (vm2 == null) return;
    	
    	Map<String, Object> queryFields = new HashMap<String, Object>();
    	queryFields.put("publicId", vm2.getPublicId());
    	queryFields.put("version", vm2.getVersion());
    	try {
    		//Search VM long name if ID is provided, but we have no longName
    		//XMI does not contain LongName
    		if (! StringUtil.isEmpty(vm2.getPublicId())) {
    			Collection<ValueMeaning> res = cadsrPublicApiModule.findValueMeaning(queryFields);
    			if (res.size() > 0) {
	    			for(ValueMeaning vmCurr : res){ 
	    				longNameArea.setText(vmCurr.getLongName());
	    				vm2.setLongName(vmCurr.getLongName());
	    				logger.debug("ValueMeaning is found by public ID: " + vm2);
	    				break;//we take the first one found should be the only one
	    			}
    			}
    			else {
    				//FIXME add error to Error panel; VM ID is wrong
    				logger.error("VM ID provided in XMI is not found in caDSR: " + vm2);
    			}
    		}
    		else {
    			//logger.debug("ValueMeaning public ID is empty: " + vm2);
    		}
		} catch (Exception e) {
			logger.error("CadsrPublicApiModule findValueMeaning error:" + e);
			e.printStackTrace();
		}
	}

	private void setVmSearchedValues(){
        vmPublicIdJLabel.setText(ConventionUtil.publicIdVersion(tempVM));

        if(tempVM != null) {
        	//longName.setText(tempVM.getLongName());
        	longNameArea.setText(tempVM.getLongName());
        }
        else {
        	//longName.setText("Unable to lookup VM Long Name");
        	longNameArea.setText("Unable to lookup VM Long Name");
        }
    }
    
    private void populateTextFields(ValueMeaning vm) {
            if (vm != null) {
	            //longName.setText(vm.getLongName());
	            longNameArea.setText(vm.getLongName());
            }
            else {
            	Logger.error("ValueMeaning is null");//TODO clean up?
            }
	}
    
	public void applyPressed() {   
		ValueMeaning vm = (ValueMeaning)node.getUserObject();
		if (tempVM != null) {
			vm.setPublicId(tempVM.getPublicId());
			vm.setVersion(tempVM.getVersion());
			vm.setLongName(tempVM.getLongName());	
		}
		logger.debug("New VM after Apply Pressed: " + vm + ", node.getFullPath(): " + node.getFullPath());
    }
	
    public void showCADSRSearchDialog(){
	    CadsrDialog cd = BeansAccessor.getCadsrVMDialog();
	    cd.setVisible(true);
	    
	    tempVM = (ValueMeaning)cd.getAdminComponent();
	    if(tempVM != null) {
	    	populateTextFields(tempVM);
	      
	      firePropertyChangeEvent
	        (new PropertyChangeEvent(this, ApplyButtonPanel.SAVE, null, true));
	      
	      modified = true;
	    }
	}   
       
    public void addPropertyChangeListener(PropertyChangeListener l) {
        super.addPropertyChangeListener(l);;
        if (propChangeListeners != null) { propChangeListeners.add(l); }
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
    
    public void insertUpdate(DocumentEvent e) {
        firePropertyChangeEvent(new PropertyChangeEvent(this, ApplyButtonPanel.SAVE, null, true));
    }
    
    public void removeUpdate(DocumentEvent e) {
        firePropertyChangeEvent(new PropertyChangeEvent(this, ApplyButtonPanel.SAVE, null, true));
    }
    
    public void changedUpdate(DocumentEvent e) {
        firePropertyChangeEvent(new PropertyChangeEvent(this, ApplyButtonPanel.SAVE, null, true));
    }
}
