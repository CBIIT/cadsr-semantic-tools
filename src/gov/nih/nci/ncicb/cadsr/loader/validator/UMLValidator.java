package gov.nih.nci.ncicb.cadsr.loader.validator;

import java.util.*;

import gov.nih.nci.ncicb.cadsr.domain.*;

import gov.nih.nci.ncicb.cadsr.loader.ElementsLists;

public class UMLValidator implements Validator {

  private ElementsLists elements;
  private List validators;

  public UMLValidator(ElementsLists elements) {
    this.elements = elements;
    
    validators = new ArrayList();
    validators.add(new ConceptCodeValidator(elements));
  }

  /**
   * returns a list of Validation errors.
   */
  public List validate() {
    List errors = new ArrayList();

    for(Iterator it = validators.iterator(); it.hasNext(); ) {
      Validator val = (Validator)it.next();
      errors.addAll(val.validate());
    }

    return errors;
  }

}