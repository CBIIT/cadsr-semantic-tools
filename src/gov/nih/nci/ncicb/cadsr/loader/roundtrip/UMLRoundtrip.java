/*
 * Copyright 2000-2003 Oracle, Inc. This software was developed in conjunction with the National Cancer Institute, and so to the extent government employees are co-authors, any rights in such works shall be subject to Title 17 of the United States Code, section 105.
 *
 Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the disclaimer of Article 3, below. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 2. The end-user documentation included with the redistribution, if any, must include the following acknowledgment:
 *
 * "This product includes software developed by Oracle, Inc. and the National Cancer Institute."
 *
 * If no such end-user documentation is to be included, this acknowledgment shall appear in the software itself, wherever such third-party acknowledgments normally appear.
 *
 * 3. The names "The National Cancer Institute", "NCI" and "Oracle" must not be used to endorse or promote products derived from this software.
 *
 * 4. This license does not authorize the incorporation of this software into any proprietary programs. This license does not authorize the recipient to use any trademarks owned by either NCI or Oracle, Inc.
 *
 * 5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE, ORACLE, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 */
package gov.nih.nci.ncicb.cadsr.loader.roundtrip;

import gov.nih.nci.ncicb.cadsr.dao.*;
import gov.nih.nci.ncicb.cadsr.domain.*;
import gov.nih.nci.ncicb.cadsr.loader.ElementsLists;
import gov.nih.nci.ncicb.cadsr.spring.*;

import gov.nih.nci.ncicb.cadsr.loader.event.ProgressListener;
import gov.nih.nci.ncicb.cadsr.loader.event.ProgressEvent;


import gov.nih.nci.ncicb.cadsr.loader.defaults.UMLDefaults;
import gov.nih.nci.ncicb.cadsr.loader.util.*;
import gov.nih.nci.ncicb.cadsr.loader.ext.*;

import org.apache.log4j.Logger;

import java.util.*;


/**
 * Roundtrip implementation for UML Models
 *
 * @author <a href="mailto:chris.ludet@oracle.com">Christophe Ludet</a>
 */
public class UMLRoundtrip implements Roundtrip, CadsrModuleListener {

  private static Logger logger = Logger.getLogger(UMLRoundtrip.class.getName());

  protected ElementsLists elements = ElementsLists.getInstance();

  private Map<String, ValueDomain> valueDomains = new HashMap<String, ValueDomain>();

  protected UMLDefaults defaults = UMLDefaults.getInstance();

  private ClassificationScheme projectCs = null;
  private ProgressListener progressListener = null;

  private CadsrModule cadsrModule;

  public UMLRoundtrip() {
  }

  public void setProgressListener(ProgressListener l) {
    progressListener = l;
  }

  public void start() throws RoundtripException {
    List<DataElement> des = elements.getElements(DomainObjectFactory.newDataElement());
    List<ObjectClass> ocs = elements.getElements(DomainObjectFactory.newObjectClass());
    List<ValueDomain> vds = elements.getElements(DomainObjectFactory.newValueDomain());
    List<ClassificationSchemeItem> packages = elements.getElements(DomainObjectFactory.newClassificationSchemeItem());


    ProgressEvent pEvt = new ProgressEvent();
    pEvt.setGoal(des.size() + 2);
    pEvt.setMessage("Looking up Project");
    if(progressListener != null) 
      progressListener.newProgressEvent(pEvt);

    pEvt = new ProgressEvent();
    pEvt.setStatus(1);
    pEvt.setMessage("Looking up CDEs");
    if(progressListener != null) 
      progressListener.newProgressEvent(pEvt);

    
    // cache package / csCsi
    Map<String, ClassSchemeClassSchemeItem> csCsiCache = 
      new HashMap<String, ClassSchemeClassSchemeItem>();

    for(DataElement de : des) {
      ObjectClass oc = de.getDataElementConcept().getObjectClass();

      pEvt.setMessage("Looking up CDEs");
      pEvt.setStatus(pEvt.getStatus() + 1);
      if(progressListener != null) 
        progressListener.newProgressEvent(pEvt);

//       String className = oc.getLongName();
      String className = LookupUtil.lookupFullName(oc);

      int ind = className.lastIndexOf(".");
      if(ind < 0)
        continue;
      String packageName = className.substring(0, ind);
      className = className.substring(ind + 1);

      ClassSchemeClassSchemeItem csCsi = csCsiCache.get(packageName);
      if(csCsi == null) {
        csCsi = lookupCsCsi(packageName);
      }
      
      if(csCsi != null) {  
        AlternateName altName = DomainObjectFactory.newAlternateName();
        altName.setName(de.getDataElementConcept().getLongName());
        altName.setType(AlternateName.TYPE_UML_DE);
        
        try {
          Collection<DataElement> l = cadsrModule.findDEByClassifiedAltName(altName, csCsi);

          
          DataElement newDe = null;
          if(l.size() > 0) 
            newDe = l.iterator().next();
          
          if(newDe != null) {
            logger.debug("Found Matching DE " + altName.getName());
            de.setPublicId(newDe.getPublicId());
            de.setVersion(newDe.getVersion());

            AlternateName gmeAn = null;
            // find GME alt Name
            List<AlternateName> _ans = cadsrModule.getAlternateNames(newDe);
            loopAn:
            for(AlternateName _an : _ans) {
              if(_an.getType().equals(AlternateName.TYPE_GME_XML_LOC_REF)) {
                for(ClassSchemeClassSchemeItem _csCsi : _an.getCsCsis()) {
                  if(_csCsi.getId().equals(csCsi.getId())) {
                    gmeAn = _an;
                    break loopAn;
                  }
                }
              }
            }

            if(gmeAn != null)
              de.addAlternateName(gmeAn);

          } else
            logger.debug("NO DE MATCH " + altName.getName());
        } catch (Exception e){
          e.printStackTrace();
          logger.error("Cannot connect to Cadsr Public API: " + e.getMessage());
        } // end of try-catch
      }
    }

    for(ValueDomain vd : vds) {

    }

    // Do the classes that don't have an attribute
    // Next Version
//     List<DataElement> des = elements.getElements(DomainObjectFactory.newDataElement());
//     for(ObjectClass oc : ocs) {

//     }

  }

  private ClassSchemeClassSchemeItem lookupCsCsi(String packageName) {
    List<ClassSchemeClassSchemeItem> csCsis = projectCs.getCsCsis();
    ClassSchemeClassSchemeItem packageCsCsi = null;

    for(ClassSchemeClassSchemeItem csCsi : csCsis) {
      try {
        if(csCsi.getCsi().getLongName().equals(packageName)
           || (csCsi.getCsi().getComments() != null && csCsi.getCsi().getComments().equals(packageName))
           )
          packageCsCsi = csCsi;
      } catch (NullPointerException e){
        e.printStackTrace();
      } // end of try-catch
    }
    
    return packageCsCsi;

  }

  public void setClassificationScheme(ClassificationScheme cs) {
    this.projectCs = cs;
  }

  /**
   * IoC setter
   */
  public void setCadsrModule(CadsrModule module) {
    cadsrModule = module;
  }

}
