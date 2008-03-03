package gov.nih.nci.ncicb.cadsr.loader.validator;

import java.util.*;

import gov.nih.nci.ncicb.cadsr.domain.*;

import gov.nih.nci.ncicb.cadsr.loader.ElementsLists;
import gov.nih.nci.ncicb.cadsr.loader.event.ProgressListener;

import gov.nih.nci.ncicb.cadsr.loader.util.*;

public class DefinitionLengthValidator implements Validator {

  private ElementsLists elements = ElementsLists.getInstance();

  private ValidationItems items = ValidationItems.getInstance();

  public void addProgressListener(ProgressListener l) {

  }


  /**
   * returns a list of Validation errors.
   */
  public ValidationItems validate() {
    List<ObjectClass> ocs = elements.getElements(DomainObjectFactory.newObjectClass());
    if(ocs != null)
      for(ObjectClass oc : ocs) {
        if((StringUtil.isEmpty(oc.getPublicId()) || oc.getVersion() == null) && !StringUtil.isEmpty(oc.getPreferredName())) {
          String[] conceptCodes = oc.getPreferredName().split(":");
          if(conceptCodes.length > 0) {
            Concept[] concepts = new Concept[conceptCodes.length];
            for (int i = 0; i < concepts.length; concepts[i] = LookupUtil
                   .lookupConcept(conceptCodes[i++]));
            
            String def = ConceptUtil.preferredDefinitionFromConcepts(concepts);
            if(def.length() > 2000) {
              items.addItem(new ValidationConceptError(PropertyAccessor.getProperty("class.definition.too.long", oc.getLongName()), oc));
            }
          }
        }
      }

//     List<Property> props = elements.getElements(DomainObjectFactory.newProperty());
//     if(props != null)
//       for(Property prop : ocs) {
//         if(StringUtil.isEmpty(prop.getPublicId()) || prop.getVersion() == null) {
//           String[] conceptCodes = prop.getPreferredName().split(":");
//           Concept[] concepts = new Concept[conceptCodes.length];
//           for (int i = 0; i < concepts.length; concepts[i] = LookupUtil
//                  .lookupConcept(conceptCodes[i++]));

//           String def = ConceptUtil.preferredDefinitionFromConcepts(concepts);
//           if(def.length() > 2000) {
//             items.addItem(new ValidationConceptError(PropertyAccessor.getProperty("property.definition.too.long", prop.getLongName()), prop));
//           }
//         }
//       }

    List<DataElement> des = elements.getElements(DomainObjectFactory.newDataElement());
    if(des != null)
      for(DataElement de : des) {
        DataElementConcept dec = de.getDataElementConcept();
        if(StringUtil.isEmpty(de.getPublicId()) || de.getVersion() == null) {
          String ocDef = "";
          if(!StringUtil.isEmpty(dec.getObjectClass().getPreferredName())) {
            String[] conceptCodes = dec.getObjectClass().getPreferredName().split(":");
            if(conceptCodes.length > 0) {
              Concept[] concepts = new Concept[conceptCodes.length];
              for (int i = 0; i < concepts.length; concepts[i] = LookupUtil
                     .lookupConcept(conceptCodes[i++]));
              
              ocDef = ConceptUtil.preferredDefinitionFromConcepts(concepts);
            }
          }     
          
          String propDef = "";
          if(!StringUtil.isEmpty(dec.getProperty().getPreferredName())) {
            String[] conceptCodes = dec.getProperty().getPreferredName().split(":");
            if(conceptCodes.length > 0) {
              Concept[] concepts = new Concept[conceptCodes.length];
              for (int i = 0; i < concepts.length; concepts[i] = LookupUtil
                     .lookupConcept(conceptCodes[i++]));
              
              propDef = ConceptUtil.preferredDefinitionFromConcepts(concepts);
            }
          }

          ValueDomain vd = LookupUtil.lookupValueDomain(de.getValueDomain());
          String vdDefinition = "";
          if(vd != null && vd.getPreferredDefinition() != null)
            vdDefinition = vd.getPreferredDefinition();

          if(propDef.length() > 2000) {
            items.addItem(new ValidationConceptError(PropertyAccessor.getProperty("property.definition.too.long", dec.getProperty().getLongName()), de));
          }
          if((ocDef + propDef + vdDefinition).length() > 2000) {
            items.addItem(new ValidationConceptError(PropertyAccessor.getProperty("attribute.definition.too.long", dec.getProperty().getLongName()), de));
          }
        }
      }
    
    
    List<ValueMeaning> vms = elements.getElements(DomainObjectFactory.newValueMeaning());
    if(vms != null)
      for(ValueMeaning vm : vms) {
        if(vm.getPreferredDefinition().length() > 2000) {
          items.addItem(new ValidationConceptError(PropertyAccessor.getProperty("vm.definition.too.long", vm.getLongName()), vm));
        }
      }

      if(des != null)
        for(DataElement de : des) {
            List<AlternateName> ans = de.getAlternateNames();
                for(AlternateName an : ans)
                    if(an.getName().length() > 255)
                        items.addItem(new ValidationError(PropertyAccessor.getProperty("alt.name.too.long", an.getName()), an));
            List<Definition> def = de.getDefinitions();
                for(Definition dfn : def)
                    if(dfn.getDefinition().length() > 2000)
                        items.addItem(new ValidationError(PropertyAccessor.getProperty("alt.definition.too.long", dfn.getDefinition()), dfn));
        }

    return items;

  }
}