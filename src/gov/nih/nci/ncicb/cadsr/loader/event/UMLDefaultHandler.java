package gov.nih.nci.ncicb.cadsr.loader.event;

import gov.nih.nci.ncicb.cadsr.domain.*;
import gov.nih.nci.ncicb.cadsr.loader.ElementsLists;

import org.apache.log4j.Logger;

import java.util.*;
import gov.nih.nci.ncicb.cadsr.loader.util.*;

import gov.nih.nci.ncicb.cadsr.loader.validator.ValidationError;
import gov.nih.nci.ncicb.cadsr.loader.validator.ValidationItems;

/**
 * This class implements UMLHandler specifically to handle UML events and convert them into caDSR objects.<br/> The handler's responsibility is to transform events received into cadsr domain objects, and store those objects in the Elements List.
 *
 *
 * @author <a href="mailto:ludetc@mail.nih.gov">Christophe Ludet</a>
 */
public class UMLDefaultHandler implements UMLHandler {
  private ElementsLists elements;
  private Logger logger = Logger.getLogger(UMLDefaultHandler.class.getName());
  private List packageList = new ArrayList();
  
  public UMLDefaultHandler(ElementsLists elements) {
    this.elements = elements;
  }

  public void newPackage(NewPackageEvent event) {
    logger.debug("Package: " + event.getName());
  }

  public void newOperation(NewOperationEvent event) {
    logger.debug("Operation: " + event.getClassName() + "." +
                 event.getName());
  }

  public void newClass(NewClassEvent event) {
    logger.debug("Class: " + event.getName());
    
    List concepts = createConcepts(event);

//     verifyConcepts(event.getName(), concepts);

    ObjectClass oc = DomainObjectFactory.newObjectClass();

    verifyConcepts(oc, concepts);

    // store concept codes in preferredName
    oc.setPreferredName(preferredNameFromConcepts(concepts));

    oc.setLongName(event.getName());
    if(event.getDescription() != null && event.getDescription().length() > 0)
      oc.setPreferredDefinition(event.getDescription());
    else 
      oc.setPreferredDefinition("");

    elements.addElement(oc);
    
    ClassificationSchemeItem csi = DomainObjectFactory.newClassificationSchemeItem();
    String csiName = null;
    String pName = event.getPackageName();
//     csi.setComments(pName);
    csi.setName(pName);
    
    if (!packageList.contains(pName)) {
      elements.addElement(csi);
      packageList.add(pName);
    }

    // Store package names
    AdminComponentClassSchemeClassSchemeItem acCsCsi = DomainObjectFactory.newAdminComponentClassSchemeClassSchemeItem();
    ClassSchemeClassSchemeItem csCsi = DomainObjectFactory.newClassSchemeClassSchemeItem();
    csCsi.setCsi(csi);
    acCsCsi.setCsCsi(csCsi);
    List l = new ArrayList();
    l.add(acCsCsi);
    oc.setAcCsCsis(l);

  }

  public void newAttribute(NewAttributeEvent event) {
    logger.debug("Attribute: " + event.getClassName() + "." +
                 event.getName());

    List concepts = createConcepts(event);

//     verifyConcepts(event.getClassName() + "." + event.getName(), concepts);

    Property prop = DomainObjectFactory.newProperty();
    verifyConcepts(prop, concepts);

    // store concept codes in preferredName
    prop.setPreferredName(preferredNameFromConcepts(concepts));

    //     prop.setPreferredName(event.getName());
    prop.setLongName(event.getName());

    String propName = event.getName();

    String s = event.getClassName();
    int ind = s.lastIndexOf(".");
    String className = s.substring(ind + 1);
    String packageName = s.substring(0, ind);

    DataElementConcept dec = DomainObjectFactory.newDataElementConcept();
    dec.setLongName(className + ":" + propName);
    dec.setProperty(prop);

    logger.debug("DEC LONG_NAME: " + dec.getLongName());

    ObjectClass oc = DomainObjectFactory.newObjectClass();
    List ocs = elements.getElements(oc.getClass());

    for (int i = 0; i < ocs.size(); i++) {
      ObjectClass o = (ObjectClass) ocs.get(i);

      if (o.getLongName().equals(event.getClassName())) {
        oc = o;
      }
    }

    dec.setObjectClass(oc);

    DataElement de = DomainObjectFactory.newDataElement();

    de.setLongName(dec.getLongName() + event.getType());
//     de.setPreferredDefinition(event.getDescription());

    logger.debug("DE LONG_NAME: " + de.getLongName());

    de.setDataElementConcept(dec);

    // Store alt Name for DE:
    // packageName.ClassName.PropertyName
    AlternateName fullName = DomainObjectFactory.newAlternateName();
    fullName.setType(AlternateName.TYPE_FULL_NAME);
    fullName.setName(packageName + "." + className + "." + propName);
    de.addAlternateName(fullName);
    
    // Store alt Name for DE:
    // ClassName:PropertyName
    fullName = DomainObjectFactory.newAlternateName();
    fullName.setType(AlternateName.TYPE_UML_DE);
    fullName.setName(className + ":" + propName);
    de.addAlternateName(fullName);

    ValueDomain vd = DomainObjectFactory.newValueDomain();
    vd.setPreferredName(event.getType());
    de.setValueDomain(vd);

    if(!StringUtil.isEmpty(event.getDescription())) {
      Definition altDef = DomainObjectFactory.newDefinition();
      altDef.setType(Definition.TYPE_UML_DE);
      altDef.setDefinition(event.getDescription());
      de.addDefinition(altDef);

      altDef = DomainObjectFactory.newDefinition();
      altDef.setType(Definition.TYPE_UML_DEC);
      altDef.setDefinition(event.getDescription());
      dec.addDefinition(altDef);
    }

    // Add packages to Prop, DE and DEC.
    prop.setAcCsCsis(oc.getAcCsCsis());
    de.setAcCsCsis(oc.getAcCsCsis());
    dec.setAcCsCsis(oc.getAcCsCsis());

    elements.addElement(de);
    elements.addElement(dec);
    elements.addElement(prop);
  }

  public void newInterface(NewInterfaceEvent event) {
    logger.debug("Interface: " + event.getName());
  }

  public void newStereotype(NewStereotypeEvent event) {
    logger.debug("Stereotype: " + event.getName());
  }

  public void newDataType(NewDataTypeEvent event) {
    logger.debug("DataType: " + event.getName());
  }

  public void newAssociation(NewAssociationEvent event) {
    ObjectClassRelationship ocr = DomainObjectFactory.newObjectClassRelationship();
    ObjectClass oc = DomainObjectFactory.newObjectClass();
    
    List ocs = elements.getElements(oc.getClass());
    logger.debug("direction: " + event.getDirection());

    for(Iterator it = ocs.iterator(); it.hasNext(); ) {
      ObjectClass o = (ObjectClass) it.next();
      
      if (o.getLongName().equals(event.getAClassName())) {
        if (event.getDirection().equals("B")) {
          ocr.setSource(o);
          ocr.setSourceRole(event.getARole());
          ocr.setSourceLowCardinality(event.getALowCardinality());
          ocr.setSourceHighCardinality(event.getAHighCardinality());
        } else {
          ocr.setTarget(o);
          ocr.setTargetRole(event.getARole());
          ocr.setTargetLowCardinality(event.getALowCardinality());
          ocr.setTargetHighCardinality(event.getAHighCardinality());
        }
      } else if (o.getLongName().equals(event.getBClassName())) {
        if (event.getDirection().equals("B")) {
          ocr.setTarget(o);
          ocr.setTargetRole(event.getBRole());
          ocr.setTargetLowCardinality(event.getBLowCardinality());
          ocr.setTargetHighCardinality(event.getBHighCardinality());
        } else {
          ocr.setSource(o);
          ocr.setSourceRole(event.getBRole());
          ocr.setSourceLowCardinality(event.getBLowCardinality());
          ocr.setSourceHighCardinality(event.getBHighCardinality());
        }
      }
    }

    if (event.getDirection().equals("AB")) {
      ocr.setDirection(ObjectClassRelationship.DIRECTION_BOTH);
    } else {
      ocr.setDirection(ObjectClassRelationship.DIRECTION_SINGLE);
    }

    ocr.setLongName(event.getRoleName());
    ocr.setType(ObjectClassRelationship.TYPE_HAS);
    elements.addElement(ocr);

  }

  public void newGeneralization(NewGeneralizationEvent event) {
    ObjectClassRelationship ocr = DomainObjectFactory.newObjectClassRelationship();
    ObjectClass oc = DomainObjectFactory.newObjectClass();

    List ocs = elements.getElements(oc.getClass());

    for(Iterator it = ocs.iterator(); it.hasNext(); ) {
      ObjectClass o = (ObjectClass) it.next();
      
      if (o.getLongName().equals(event.getParentClassName())) {
        ocr.setTarget(o);
      } else if (o.getLongName().equals(event.getChildClassName())) {
        ocr.setSource(o);
      }
      
    }
    ocr.setType(ObjectClassRelationship.TYPE_IS);

    // Inherit all attributes
    // Find all DECs:
    ObjectClass parentOc = ocr.getTarget(),
      childOc = ocr.getSource();

    List newElts = new ArrayList();
    List des = elements.getElements(DomainObjectFactory.newDataElement().getClass());
    if(des != null)
      for(Iterator it = des.iterator(); it.hasNext(); ) {
        DataElement de = (DataElement)it.next();
        DataElementConcept dec = de.getDataElementConcept();
        if(dec.getObjectClass() == parentOc) {
          // We found property belonging to parent
          // Duplicate it for child.
          Property newProp = DomainObjectFactory.newProperty();
          newProp.setLongName(dec.getProperty().getLongName());
          newProp.setPreferredName(dec.getProperty().getPreferredName());


          DataElementConcept newDec = DomainObjectFactory.newDataElementConcept();
          newDec.setProperty(dec.getProperty());
          newDec.setObjectClass(childOc);
          for(Iterator it2 = dec.getDefinitions().iterator();
              it2.hasNext();) {
            Definition def = (Definition)it2.next();
            newDec.addDefinition(def);
          }
          
          String propName = newDec.getProperty().getLongName();
          
          String className = childOc.getLongName();
          int ind = className.lastIndexOf(".");
          className = className.substring(ind + 1);
          newDec.setLongName(className + ":" + propName);		
          DataElement newDe = DomainObjectFactory.newDataElement();
          newDe.setDataElementConcept(newDec);
          newDe.setValueDomain(de.getValueDomain());
          newDe.setLongName(newDec.getLongName() + de.getValueDomain().getPreferredName());

          for(Iterator it2 = de.getDefinitions().iterator();
              it2.hasNext();) {
            Definition def = (Definition)it2.next();
            newDe.addDefinition(def);
          }

          for(Iterator it2 = de.getAlternateNames().iterator(); it2.hasNext();) {
            AlternateName an = (AlternateName)it2.next();
            newDe.addAlternateName(an);
          }

          newDe.setAcCsCsis(parentOc.getAcCsCsis());
          newDec.setAcCsCsis(parentOc.getAcCsCsis());
          newProp.setAcCsCsis(parentOc.getAcCsCsis());

          newElts.add(newProp);
          newElts.add(newDe);
          newElts.add(newDec);
        }
      }
    
    for(Iterator it = newElts.iterator(); it.hasNext();
        elements.addElement(it.next()));
    
    
    elements.addElement(ocr);

    logger.debug("Generalization: ");
    logger.debug("Source:");
    logger.debug("-- " + ocr.getSource().getLongName());
    logger.debug("Target: ");
    logger.debug("-- " + ocr.getTarget().getLongName());
  }

  private Concept newConcept(NewConceptEvent event) {
    Concept concept = DomainObjectFactory.newConcept();

    concept.setPreferredName(event.getConceptCode());
    concept.setPreferredDefinition(event.getConceptDefinition());
    concept.setDefinitionSource(event.getConceptDefinitionSource());
    concept.setLongName(event.getConceptPreferredName());

    elements.addElement(concept);
    return concept;
  }

  private List createConcepts(NewConceptualEvent event) {
    List concepts = new ArrayList();
    List conEvs = event.getConcepts();
    for(Iterator it = conEvs.iterator(); it.hasNext(); ) {
      NewConceptEvent conEv = (NewConceptEvent)it.next();
      concepts.add(newConcept(conEv));
    }

    return concepts;
  }

  private String preferredNameFromConcepts(List concepts) {
    StringBuffer sb = new StringBuffer();
    for(Iterator it = concepts.iterator(); it.hasNext(); ) {
      Concept con = (Concept)it.next();
      if(sb.length() > 0)
        sb.insert(0, "-");
      sb.insert(0, con.getPreferredName());
    }
    return sb.toString();
  }

  private void verifyConcepts(AdminComponent cause, List concepts) {
    for(Iterator it = concepts.iterator(); it.hasNext(); ) {
      Concept concept = (Concept)it.next();
      if(StringUtil.isEmpty(concept.getPreferredName())) {
        ValidationItems.getInstance()
          .addItem(new ValidationError(
                                       PropertyAccessor.getProperty("validation.concept.missing.for", cause.getLongName()),
                                       cause));
//         elements.addElement(new ConceptError(
//                        ConceptError.SEVERITY_ERROR,
//                        PropertyAccessor.getProperty("validation.concept.missing.for", eltName)));
      }
    }
  }

}
