package gov.nih.nci.ncicb.cadsr.loader.validator;
import gov.nih.nci.ncicb.cadsr.domain.DataElement;
import gov.nih.nci.ncicb.cadsr.domain.DomainObjectFactory;
import gov.nih.nci.ncicb.cadsr.domain.ObjectClass;
import gov.nih.nci.ncicb.cadsr.domain.ValueDomain;
import gov.nih.nci.ncicb.cadsr.domain.Concept;
import gov.nih.nci.ncicb.cadsr.loader.ElementsLists;
import gov.nih.nci.ncicb.cadsr.loader.UserSelections;
import gov.nih.nci.ncicb.cadsr.loader.event.ProgressListener;
import gov.nih.nci.ncicb.cadsr.loader.util.*;
import gov.nih.nci.ncicb.xmiinout.domain.UMLAttribute;
import gov.nih.nci.ncicb.xmiinout.domain.UMLClass;
import gov.nih.nci.ncicb.xmiinout.domain.UMLModel;
import gov.nih.nci.ncicb.xmiinout.domain.UMLPackage;
import gov.nih.nci.ncicb.xmiinout.handler.XmiInOutHandler;
import gov.nih.nci.ncicb.cadsr.loader.ext.CadsrModule;
import gov.nih.nci.ncicb.cadsr.loader.ext.CadsrModuleListener;
import gov.nih.nci.ncicb.cadsr.loader.parser.ParserException;
import gov.nih.nci.ncicb.cadsr.loader.parser.XMIParser2;
import gov.nih.nci.ncicb.cadsr.loader.ui.MapToLVD;

import java.util.*;

import org.apache.log4j.Logger;

public class DuplicateValidator implements Validator, CadsrModuleListener
{
  private ElementsLists elements = ElementsLists.getInstance();

  private ValidationItems items = ValidationItems.getInstance();
  
  private ValueDomain localValueDomain = null;

  private CadsrModule cadsrModule;
  
  //private UMLModel model = null;
  //private XmiInOutHandler handler = null;  
  private HashMap<String, UMLAttribute> attributeMap = new HashMap<String, UMLAttribute>();  
  public DuplicateValidator()
  {
  }
  
  public void addProgressListener(ProgressListener l) {

  }
  
  private Logger logger = Logger.getLogger(DuplicateValidator.class.getName());
  public ValidationItems validate() {
	  
    List<ObjectClass> ocs = elements.getElements(DomainObjectFactory.newObjectClass());
    Map<String, ObjectClass> listed = new HashMap<String, ObjectClass>();
    Map<String, ObjectClass> prefNameList = new HashMap<String, ObjectClass>();
    
    if(ocs != null) {
      for(ObjectClass oc : ocs) {  
        if(oc.getPublicId() != null) {
          if(listed.containsKey(oc.getPublicId()))
            items.addItem(new ValidationError
                          (PropertyAccessor.getProperty
                           ("class.same.mapping", oc.getLongName(),(listed.get(oc.getPublicId())).getLongName()),oc));
          else {
            listed.put(oc.getPublicId(), oc);

            List<Concept> concepts = cadsrModule.getConcepts(oc);
            String prefname = ConceptUtil.preferredNameFromConcepts(concepts);

            if(prefNameList.containsKey(prefname))
              items.addItem(new ValidationError
                            (PropertyAccessor.getProperty
                             ("class.same.mapping", oc.getLongName(),(prefNameList.get(prefname)).getLongName()),oc));
            
            
            else
              // we also need to add the concept lists so it can be validated against OC that are mapped to concepts
              if(!StringUtil.isEmpty(prefname))
                prefNameList.put(prefname, oc);
          }
        }
        else if(!StringUtil.isEmpty(oc.getPreferredName())) {
          if(prefNameList.containsKey(oc.getPreferredName()))
            items.addItem(new ValidationConceptError
                          (PropertyAccessor.getProperty
                           ("class.same.mapping", oc.getLongName(),(prefNameList.get(oc.getPreferredName())).getLongName()),oc));
          else
            prefNameList.put(oc.getPreferredName(), oc);
        }
      } 
    }
    
    InheritedAttributeList inheritedAttrs = InheritedAttributeList.getInstance();
    List<DataElement> des = elements.getElements(DomainObjectFactory.newDataElement());
    if(des != null && ocs != null) {
      for(ObjectClass oc : ocs) {
        Map<String, DataElement> deList = new HashMap<String, DataElement>();
        for(DataElement de : des) {
          if(de.getDataElementConcept().getObjectClass() == oc) {
            String conceptConcat = null;
            if(!StringUtil.isEmpty(de.getDataElementConcept().getProperty().getPublicId())) {
              List<Concept> concepts = cadsrModule.getConcepts(de.getDataElementConcept().getProperty());
              if (cadsrModule.isPublic()) {
            	  Collections.reverse(concepts); //order of concepts returned by the public and private apis is reversed. reverse collection to make them same
              }
              conceptConcat = ConceptUtil.preferredNameFromConcepts(concepts);              
            } else {
              conceptConcat = de.getDataElementConcept().getProperty().getPreferredName();             
            }
            
         // SIW-794 Adding Value domain as criteria
            if (!StringUtil.isEmpty(de.getValueDomain().getPublicId())) {
            	if (!StringUtil.isEmpty(conceptConcat)) {
            		conceptConcat = conceptConcat + ":" +  de.getValueDomain().getPublicId() + ":" + de.getValueDomain().getVersion();
            	}            		
            }             
            // SIW-794 Adding Local Value domain as criteria
            if (DEMappingUtil.isMappedToLVD(de) && DEMappingUtil.getLVDValue(de)!=null) {
            	if (DEMappingUtil.getLVDValue(de).length()>0)
            		conceptConcat = conceptConcat + ":" + DEMappingUtil.getLVDValue(de);            	
            }
                                
            
            if(deList.containsKey(conceptConcat)) {
              ValidationError item = new  ValidationError
                            (PropertyAccessor.getProperty
                             ("de.same.mapping", de.getDataElementConcept().getLongName(),
                              (deList.get(conceptConcat)).getDataElementConcept().getLongName()),de);
              item.setIncludeInInherited(true);
              items.addItem(item);
            } else {
            	// Fix for inherited attributes duplicate concept mapping error - SIW 796
              if(!StringUtil.isEmpty(conceptConcat) && !inheritedAttrs.isInherited(de))
                deList.put(conceptConcat, de);
            }
          }
        }
      }
    }
    return items;
  }

  public void setCadsrModule(CadsrModule module) {
    this.cadsrModule = module;
  }
  
  
}