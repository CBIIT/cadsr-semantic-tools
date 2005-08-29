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
package gov.nih.nci.ncicb.cadsr.loader.validator;

import java.util.*;

import gov.nih.nci.ncicb.cadsr.domain.*;

import gov.nih.nci.ncicb.cadsr.loader.ElementsLists;

import gov.nih.nci.ncicb.cadsr.loader.util.*;

public class ConceptCodeValidator implements Validator {

  private ElementsLists elements;

  private ValidationItems items = ValidationItems.getInstance();

  public ConceptCodeValidator(ElementsLists elements) {
    this.elements = elements;
  }

  /**
   * returns a list of Validation errors.
   */
  public ValidationItems validate() {
    
    List<ObjectClass> ocs = (List<ObjectClass>)elements.getElements(DomainObjectFactory.newObjectClass().getClass());
    if(ocs != null)
      for(ObjectClass o : ocs) {
        if(StringUtil.isEmpty(o.getPreferredName()))
          items.addItem(new ValidationError("Class: " + o.getLongName() + " has no concept code.", o));
        else {
          checkConcepts(o);
        }
      }    


    List<Property> props = (List<Property>)elements.getElements(DomainObjectFactory.newProperty().getClass());
    if(props != null)
      for(Property o : props ) 
        if(StringUtil.isEmpty(o.getPreferredName()))
          items.addItem(new ValidationError("Attribute: " + o.getLongName() + " has no concept code.", o));
        else {
          checkConcepts(o);
        }
    
//     List<Concept> concepts = (List<Concept>)elements.getElements(DomainObjectFactory.newConcept().getClass());
//     if(concepts != null) {
//       for(Concept o : concepts ) {
//         Concept o = (Concept)it.next();
//         if(o.getPreferredName() == null) {

//         } else {
//           if(o.getLongName() == null)
//             items.addItem(new ValidationError(PropertyAccessor.getProperty("validation.concept.missing.longName", o.getPreferredName()), o));
//           if(o.getPreferredDefinition() == null) {
//             items.addItem(new ValidationError(PropertyAccessor.getProperty("validation.concept.missing.definition", o.getPreferredName()), o));
//           }
//           if(o.getDefinitionSource() == null)
//             items.addItem(new ValidationError(PropertyAccessor.getProperty("validation.concept.missing.source", o.getPreferredName()), o));
//         }
//       }
//     }

    return items;
  }

  private void checkConcepts(AdminComponent ac) {
    String[] conStr = ac.getPreferredName().split(":");
    for(String s : conStr) {
      Concept con = LookupUtil.lookupConcept(s);
      if(con.getLongName() == null)
        items.addItem(new ValidationError(PropertyAccessor.getProperty("validation.concept.missing.longName", con.getPreferredName()), ac));
      if(con.getPreferredDefinition() == null) {
        items.addItem(new ValidationError(PropertyAccessor.getProperty("validation.concept.missing.definition", con.getPreferredName()), ac));
      }
      if(con.getDefinitionSource() == null)
        items.addItem(new ValidationError(PropertyAccessor.getProperty("validation.concept.missing.source", con.getPreferredName()), ac));
    }
  }

}