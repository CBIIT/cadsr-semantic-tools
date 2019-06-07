package gov.nih.nci.ncicb.cadsr.loader.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AddButtonPanel extends JPanel implements PropertyChangeListener, MouseListener, ActionListener
{
	// TODO uncomment to enable concept inheritance feature - VS: Uncommented June 6 2019
	private DropDownButton addButton;
	
	//private JButton addButton; - VS: Commented June 6 2019

  static final String ADD = "Add",
    ADD_INHERITANCE = "Add Inheritance";

  private ConceptEditorPanel conceptEditorPanel;

  public AddButtonPanel(ConceptEditorPanel p) {
    conceptEditorPanel = p;

 // TODO uncomment to enable concept inheritance feature -   VS: Uncommented June 6 2019
    addButton = new DropDownButton(ADD);
    
    //addButton = new JButton(ADD);  - VS: Commented June 6 2019

 // TODO uncomment to enable concept inheritance feature - VS: Uncommented June 6 2019
    JLabel addLabel = new JLabel(ADD),
      addInheritanceLabel = new JLabel(ADD_INHERITANCE);
    
    addButton.addComponent(addLabel);
    addButton.addComponent(addInheritanceLabel);

    addLabel.addMouseListener(this);
    addInheritanceLabel.addMouseListener(this);

    addButton.addActionListener(this);
    
    this.add(addButton);

  }

  public void propertyChange(PropertyChangeEvent e) 
  {
    if(e.getPropertyName().equals(ADD)) {
      addButton.setEnabled((Boolean)e.getNewValue());
    }
  }
  
  public void mouseClicked(MouseEvent e) {
//     cadsrVDPanel.setBackground(Color.WHITE);
//     lvdPanel.setBackground(Color.WHITE);
    //addButton.unfocus();
    if(((JLabel)e.getSource()).getText().equals(ADD))
      conceptEditorPanel.addPressed();
    else if(((JLabel)e.getSource()).getText().equals(ADD_INHERITANCE))
      conceptEditorPanel.addInheritancePressed();
  }

  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {
//     if(((JLabel)e.getSource()).getText().equals(MAP_CADSR_VD))
//       cadsrVDPanel.setBackground(Color.LIGHT_GRAY);
//     else if(((JLabel)e.getSource()).getText().equals(MAP_LOCAL_VD))
//       lvdPanel.setBackground(Color.LIGHT_GRAY);
  }
  public void mouseExited(MouseEvent e) {
//     if(((JLabel)e.getSource()).getText().equals(MAP_CADSR_VD))
//       cadsrVDPanel.setBackground(Color.WHITE);
//     else if(((JLabel)e.getSource()).getText().equals(MAP_LOCAL_VD))
//       lvdPanel.setBackground(Color.WHITE);
  }

  public void actionPerformed(ActionEvent evt) {
    conceptEditorPanel.addPressed();
  }

}
