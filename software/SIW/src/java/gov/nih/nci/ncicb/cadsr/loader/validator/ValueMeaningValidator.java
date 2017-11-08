/*
 * Copyright (C) 2017 Leidos Biomedical Research, Inc. - All rights reserved.
 */
package gov.nih.nci.ncicb.cadsr.loader.validator;

import java.util.*;

import gov.nih.nci.ncicb.cadsr.domain.*;

import gov.nih.nci.ncicb.cadsr.loader.ElementsLists;
import gov.nih.nci.ncicb.cadsr.loader.UserSelections;
import gov.nih.nci.ncicb.cadsr.loader.event.ProgressListener;
import gov.nih.nci.ncicb.cadsr.loader.event.ProgressEvent;
import gov.nih.nci.ncicb.cadsr.loader.defaults.UMLDefaults;

import gov.nih.nci.ncicb.cadsr.loader.util.*;

import gov.nih.nci.ncicb.cadsr.loader.ext.*;

import org.apache.log4j.Logger;

/**
 * Validate that Value Meaning ID: <ul>
 * <li>Must already Exist
 * </ul>
 */
//SIW-627
public class ValueMeaningValidator implements Validator, CadsrModuleListener {

  private ElementsLists elements = ElementsLists.getInstance();

  private ValidationItems items = ValidationItems.getInstance();

  private ProgressListener progressListener;

  private CadsrModule cadsrModule;

  private Logger logger = Logger.getLogger(ValueMeaningValidator.class.getName());

  private int mode;

  public ValueMeaningValidator() {

  }

  public void addProgressListener(ProgressListener l) {
    progressListener = l;
  }
  
  private void fireProgressEvent(ProgressEvent evt) {
    if(progressListener != null)
      progressListener.newProgressEvent(evt);
  }

  /**
   * We expect this VM Validator to be called after VD Validator
   * builds a list of Validation errors.
   */
  public ValidationItems validate() {
    
    List<ValueDomain> vds = elements.getElements(DomainObjectFactory.newValueDomain());
    Boolean ignoreVD = (Boolean)UserSelections.getInstance().getProperty("ignore-vd");
    if(ignoreVD == null)
      ignoreVD = false;
    if (ignoreVD) {//we do not validate VM then
    	return items;
    }
    if(vds != null) {
      ProgressEvent evt = new ProgressEvent();
      evt.setMessage("Validating Value Domains ...");
      evt.setGoal(vds.size());
      evt.setStatus(0);
      fireProgressEvent(evt);
      int count = 1;
      for(ValueDomain vd : vds) {
        evt = new ProgressEvent();
        evt.setMessage(" Validating VMs of VD: " + vd.getLongName());
        evt.setStatus(count++);
        fireProgressEvent(evt); 

          try {
              List<PermissibleValue> localPVs = vd.getPermissibleValues();
              
              for (PermissibleValue pv : localPVs) {
            	  ValueMeaning vm = pv.getValueMeaning();
            	  String vmPublicId = vm.getPublicId();
            	  if (! StringUtil.isEmpty(vmPublicId)) {
            		  Float vmVersion = vm.getVersion() != null ? vm.getVersion() : 1.0f;
                      Map<String, Object> queryFields = new HashMap<String, Object>();
                            queryFields.put(CadsrModule.PUBLIC_ID, vm.getPublicId());
                            queryFields.put(CadsrModule.VERSION, vmVersion);
                            Collection<gov.nih.nci.ncicb.cadsr.domain.ValueMeaning> coll = cadsrModule.findValueMeaning(queryFields);
                            if ((coll == null) || (coll.isEmpty())) {
                            	items.addItem(new ValidationError
                                        (PropertyAccessor.getProperty
                                         ("cadsr.vm.id.doesnt.exist", vm.getPublicId() + "v" + vm.getVersion()), vm));
                            }      
            	  }
              }
            
          } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
          } // end of try-catch
        } //end-of-for
    }
    return items;
  }
  
  private boolean contains(String[] defaultList, String valueToBeTested){
      boolean contains = false;  
      for (String uomValue : defaultList)
        if(uomValue.equals(valueToBeTested))
            contains = true;
      return contains;
  }

  public void setCadsrModule(CadsrModule module) {
    this.cadsrModule = module;
  }

  private boolean comparePVLists(List<PermissibleValue> pvList1, List<PermissibleValue> pvList2) 
  {
    if(pvList1 == null || pvList2 == null)
      return false;

    if(pvList1.size() != pvList2.size()) 
      return false;

    if(pvList1.size() == 0 && pvList2.size() == 0)
      return true;

    boolean found = true;
    Iterator<PermissibleValue> it1 = pvList1.iterator();
    //    while(it1.hasNext() && found == true) {
    while(it1.hasNext() && found) {
      found = false;
      PermissibleValue pv1 = it1.next();
      Iterator<PermissibleValue> it2 = pvList2.iterator();
      while(found == false && it2.hasNext()) {
        PermissibleValue pv2 = it2.next();
        if(pv2.getValue().equalsIgnoreCase(pv1.getValue())) {
          found = true;
        }
      }
      if(!found)
        return false;
    }
    // return true if we found the last match 
    return (found == true && !it1.hasNext());
  
}  



}