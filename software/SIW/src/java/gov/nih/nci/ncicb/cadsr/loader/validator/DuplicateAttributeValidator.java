package gov.nih.nci.ncicb.cadsr.loader.validator;
import gov.nih.nci.ncicb.cadsr.domain.*;
import gov.nih.nci.ncicb.cadsr.loader.ElementsLists;
import gov.nih.nci.ncicb.cadsr.loader.UserSelections;
import gov.nih.nci.ncicb.cadsr.loader.event.IdVersionPair;
import gov.nih.nci.ncicb.cadsr.loader.event.LoaderHandler;
import gov.nih.nci.ncicb.cadsr.loader.event.NewAttributeEvent;
import gov.nih.nci.ncicb.cadsr.loader.event.NewClassEvent;
import gov.nih.nci.ncicb.cadsr.loader.event.NewGeneralizationEvent;
import gov.nih.nci.ncicb.cadsr.loader.event.NewPackageEvent;
import gov.nih.nci.ncicb.cadsr.loader.event.ProgressEvent;
import gov.nih.nci.ncicb.cadsr.loader.event.ProgressListener;
import gov.nih.nci.ncicb.cadsr.loader.event.UMLHandler;
import gov.nih.nci.ncicb.cadsr.loader.ext.CadsrModule;
import gov.nih.nci.ncicb.cadsr.loader.parser.ParserException;
import gov.nih.nci.ncicb.cadsr.loader.parser.XMIParser2;
import gov.nih.nci.ncicb.cadsr.loader.util.ConceptUtil;
import gov.nih.nci.ncicb.cadsr.loader.util.InheritedAttributeList;
import gov.nih.nci.ncicb.cadsr.loader.util.LookupUtil;
import gov.nih.nci.ncicb.cadsr.loader.util.PropertyAccessor;
import gov.nih.nci.ncicb.cadsr.loader.util.RunMode;
import gov.nih.nci.ncicb.cadsr.loader.util.StringUtil;
import gov.nih.nci.ncicb.xmiinout.domain.UMLAttribute;
import gov.nih.nci.ncicb.xmiinout.domain.UMLClass;
import gov.nih.nci.ncicb.xmiinout.domain.UMLGeneralizable;
import gov.nih.nci.ncicb.xmiinout.domain.UMLGeneralization;
import gov.nih.nci.ncicb.xmiinout.domain.UMLModel;
import gov.nih.nci.ncicb.xmiinout.domain.UMLPackage;
import gov.nih.nci.ncicb.xmiinout.domain.UMLTaggedValue;
import gov.nih.nci.ncicb.xmiinout.handler.HandlerEnum;
import gov.nih.nci.ncicb.xmiinout.handler.XmiException;
import gov.nih.nci.ncicb.xmiinout.handler.XmiHandlerFactory;
import gov.nih.nci.ncicb.xmiinout.handler.XmiInOutHandler;
import gov.nih.nci.ncicb.xmiinout.util.ModelUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import org.apache.log4j.Logger;

public class DuplicateAttributeValidator implements Validator
{
  private ElementsLists elements = ElementsLists.getInstance();
  
  private ValidationItems items = ValidationItems.getInstance();

  private Logger logger = Logger.getLogger(DuplicateAttributeValidator.class.getName());
  
  private ProgressListener progressListener;
  
  private CadsrModule cadsrModule;
  
  private HashSet<String> vdTags = new HashSet<String>(); 
  int dupeCount = 0;
  
  private UMLHandler listener;
  
  private UserSelections userSelections = UserSelections.getInstance();
  
  private HandlerEnum handlerType;
  private int totalNumberOfElements = 0, currentElementIndex = 0;
  public static final String[] validVdStereotypes = 
		    PropertyAccessor.getProperty("vd.valid.stereotypes").split(",");
  
  private String packageName = "";
  private String className = "";
  
  public static final String TV_PROP_ID = "CADSR_PROP_ID";
  public static final String TV_PROP_VERSION = "CADSR_PROP_VERSION";

  public static final String TV_DE_ID = "CADSR_DE_ID";
  public static final String TV_DE_VERSION = "CADSR_DE_VERSION";

  public static final String TV_VALUE_DOMAIN = "CADSR Local Value Domain";

  public static final String TV_VD_ID = "CADSR_VD_ID";
  public static final String TV_VD_VERSION = "CADSR_VD_VERSION";
  public static final String TV_OC_ID = "CADSR_OC_ID";
  public static final String TV_OC_VERSION = "CADSR_OC_VERSION";


  public static final String TV_VD_DEFINITION = "CADSR_ValueDomainDefinition";
  public static final String TV_VD_DATATYPE = "CADSR_ValueDomainDatatype";
  public static final String TV_VD_TYPE = "CADSR_ValueDomainType";
  public static final String TV_VD_UOM = "NCI_VD_UOM";
  public static final String TV_VD_DISPLAY_FORMAT = "NCI_VD_DISPLAY_FORMAT";
  public static final String TV_VD_MINIMUM_LENGTH = "NCI_VD_MIN_LENGTH";
  public static final String TV_VD_MAXIMUM_LENGTH = "NCI_VD_MAX_LENGTH";
  public static final String TV_VD_DECIMAL_PLACE = "NCI_VD_DECIMAL_PLACE";
  public static final String TV_VD_HIGH_VALUE = "NCI_VD_HIGH_VALUE";
  public static final String TV_VD_LOW_VALUE = "NCI_VD_LOW_VALUE";
  public static final String TV_CD_ID = "CADSR_ConceptualDomainPublicID";
  public static final String TV_CD_VERSION = "CADSR_ConceptualDomainVersion";
  public static final String TV_REP_ID = "CADSR_RepresentationPublicID";
  public static final String TV_REP_VERSION = "CADSR_RepresentationVersion";

  public static final String TV_INHERITED_DE_ID = "CADSR_Inherited.{1}.DE_ID";
  public static final String TV_INHERITED_DE_VERSION = "CADSR_Inherited.{1}.DE_VERSION";
  public static final String TV_INHERITED_VD_ID = "CADSR_Inherited.{1}.VD_ID";
  public static final String TV_INHERITED_VD_VERSION = "CADSR_Inherited.{1}.VD_VERSION";
  public static final String TV_INHERITED_VALUE_DOMAIN = "CADSR_Inherited.{1}.Local Value Domain";
  
  public static final String TV_GME_NAMESPACE = "NCI_GME_XML_NAMESPACE";
  public static final String TV_GME_XML_ELEMENT = "NCI_GME_XML_ELEMENT";
  public static final String TV_GME_XML_LOC_REFERENCE = "NCI_GME_XML_LOC_REF";
  public static final String TV_GME_SOURCE_XML_LOC_REFERENCE = "NCI_GME_SOURCE_XML_LOC_REF";
  public static final String TV_GME_TARGET_XML_LOC_REFERENCE = "NCI_GME_TARGET_XML_LOC_REF";

  public static final String TV_EXCLUDE_SEMANTIC_INHERITANCE = "NCI_IGNORE_CONCEPT_INHERITANCE"; 
  public static final String TV_EXCLUDE_SEMANTIC_INHERITANCE_REASON = "NCI_REASON_FOR_CONCEPT_EXCLUSION";
  
  /**
   * Tagged Value name for Concept Code
   */
  public static final String TV_CONCEPT_CODE = "ConceptCode";

  /**
   * Tagged Value name for Concept Preferred Name
   */
  public static final String TV_CONCEPT_PREFERRED_NAME = "ConceptPreferredName";

  /**
   * Tagged Value name for Concept Definition
   */
  public static final String TV_CONCEPT_DEFINITION = "ConceptDefinition";

  /**
   * Tagged Value name for Concept Definition Source
   */
  public static final String TV_CONCEPT_DEFINITION_SOURCE = "ConceptDefinitionSource";


  /**
   * Qualifier Tagged Value prepender. 
   */
  public static final String TV_QUALIFIER = "Qualifier";

  /**
   * ObjectClass Tagged Value prepender. 
   */
  public static final String TV_TYPE_CLASS = "ObjectClass";

  /**
   * Property Tagged Value prepender. 
   */
  public static final String TV_TYPE_PROPERTY = "Property";

  /**
   * ValueDomain Tagged Value prepender. 
   */
  public static final String TV_TYPE_VD = "ValueDomain";

  /**
   * Value Meaning Tagged Value prepender. 
   */
  public static final String TV_TYPE_VM = "ValueMeaning";

  /**
   * Association Role Tagged Value prepender. 
   */
  public static final String TV_TYPE_ASSOC_ROLE = "AssociationRole";

  /**
   * Association Source Tagged Value prepender. 
   */
  public static final String TV_TYPE_ASSOC_SOURCE = "AssociationSource";

  /**
   * Association Target Tagged Value prepender. 
   */
  public static final String TV_TYPE_ASSOC_TARGET = "AssociationTarget";
  

  /**
   * Tagged Value name for Documentation
   */
  public static final String TV_DOCUMENTATION = "documentation";
  public static final String TV_DESCRIPTION = "description";
  public static final String TV_CADSR_DESCRIPTION = "CADSR_Description";

  // replaced by type specific review tags
  //   public static final String TV_HUMAN_REVIEWED = "HUMAN_REVIEWED";
  public static final String TV_OWNER_REVIEWED = "OWNER_REVIEWED";
  public static final String TV_CURATOR_REVIEWED = "CURATOR_REVIEWED";

  public static final String TV_INHERITED_OWNER_REVIEWED = "CADSR_Inherited.{1}.OWNER_REVIEWED";
  public static final String TV_INHERITED_CURATOR_REVIEWED = "CADSR_Inherited.{1}.CURATOR_REVIEWED";  
  
  public void addProgressListener(ProgressListener l) {
    progressListener = l;
  }

  private void fireProgressEvent(ProgressEvent evt) {
    if(progressListener != null)
      progressListener.newProgressEvent(evt);
  }
  
  public void setEventHandler(LoaderHandler handler) {
	    this.listener = (UMLHandler) handler;
	    if (progressListener != null) {
	      listener.addProgressListener(progressListener);
	    }
	  }  

  public DuplicateAttributeValidator()
  {
  }
  
  public ValidationItems validate() {
    List<ObjectClass> ocs = elements.getElements(DomainObjectFactory.newObjectClass());
    List<DataElement> des = elements.getElements(DomainObjectFactory.newDataElement());
   
    ProgressEvent evt = new ProgressEvent();
    evt.setMessage("Validating Object Classes ...");
    evt.setGoal(ocs.size());
    evt.setStatus(0);
    fireProgressEvent(evt);
    int count = 1;
    InheritedAttributeList inheritedAttrs = InheritedAttributeList.getInstance();

    
    for(ObjectClass oc : ocs) {
      evt = new ProgressEvent();
      evt.setMessage(" Validating " + oc.getLongName());      
      evt.setStatus(count++);
      fireProgressEvent(evt);
      List<DataElement> matchedDes = new ArrayList<DataElement>();      
      List<ObjectClass> childOCs = inheritedAttrs.getChildrenOc(oc);
      NewGeneralizationEvent event = new NewGeneralizationEvent();
      for(DataElement de : des) {
        for(DataElement de2 : des) {
          if(de != de2) {
        	  if(de.getDataElementConcept().getObjectClass() == oc) {
        		  if(de2.getDataElementConcept().getObjectClass() == oc) {
        			  if(de.getDataElementConcept().getProperty().getLongName().equals(
                              de2.getDataElementConcept().getProperty().getLongName())) {
        				// SIW-794 Allow there to be more than one UML attribute with the same name in a UML Class        				  
        				  		if (matchedDes.contains(de) || matchedDes.contains(de2)) {
        				  			continue;
        				  		} else {
        				  		dupeCount = 0;
        				  	// SIW-794 Allow there to be more than one UML attribute with the same name in a UML Class        				  		
        					  	compareVDTags(de.getDataElementConcept().getProperty().getLongName(), oc.getLongName());
        					  	/*if (concatenateConcepts(de).equals(concatenateConcepts(de2)))
        					  			items.addItem(new ValidationError
		                                  (PropertyAccessor.getProperty
		                                    ("de.same.mapping", de.getDataElementConcept().getProperty().getLongName()),de));*/
        					  	
        					  if (dupeCount > 0) {
		        				  items.addItem(new ValidationWarning
		                                  (PropertyAccessor.getProperty
		                                    ("de.same.attribute", de.getDataElementConcept().getProperty().getLongName()),de));		        				
        					  }
        					// SIW-794 Add both the compared data elements to a list, so that they wont be compared again. 
        					  matchedDes.add(de);
        					  matchedDes.add(de2);
        				  		}
        			  }          
        		  }
        		  else {
        			  if (objectClassInList(de2.getDataElementConcept().getObjectClass(), childOCs)) {
        				  if (!inheritedAttrs.isInherited(de2) && propertiesEqual(de, de2)) {
        					  ValidationError error = new ValidationError
                              (PropertyAccessor.getProperty
                                      ("de.same.inherited.attribute", de2.getDataElementConcept().getLongName(),
                                    		  de.getDataElementConcept().getLongName()),de2);
        					  error.setIncludeInInherited(true);
        					  items.addItem(error);
        				  }
        			  }
        		  }
        	  }
          }
        }
        
    }
 }
 // SIW-794 checking for the list of warning items
  logger.info("Warning items: " + items.getWarnings().size());  
  for (ValidationWarning warn : items.getWarnings()) {
	  logger.info("Warning message: " + warn.getMessage());
  }
  logger.info("Error items: " + items.getErrors().size()); 
  for (ValidationError error : items.getErrors()) {
	  logger.info("Error message: " + error.getMessage());
  }  
  return items;
}
  
  
//SIW-794 compare the Value Domains for two different attributes that have the same attribute name 
  private void compareVDTags(String longName, String objectClassName) {
	  
	  String filename;
			filename = (String) userSelections.getProperty("FILENAME");
			userSelections.setProperty("LongName", longName);
			userSelections.setProperty("ObjectClassName", objectClassName);         
			XmiInOutHandler handler = (XmiInOutHandler)UserSelections.getInstance().getProperty("XMI_HANDLER");
			UMLModel model = handler.getModel();
			try {
				doPackages(model);
			} catch (ParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
  }  
  
  private String concatenateConcepts (DataElement de) {
  String conceptConcat = null;
  if(!StringUtil.isEmpty(de.getDataElementConcept().getProperty().getPublicId())) {
    List<Concept> concepts = cadsrModule.getConcepts(de.getDataElementConcept().getProperty());
    if (cadsrModule.isPublic()) {
  	  Collections.reverse(concepts); //order of concepts returned by the public and private apis is reversed. reverse collection to make them same
    }
    conceptConcat = ConceptUtil.preferredNameFromConcepts(concepts);              
  } else {
    //conceptConcat = de.getDataElementConcept().getProperty().getPreferredName();             
  }
  return conceptConcat;
  }
  
  
  private boolean objectClassInList(ObjectClass oc, List<ObjectClass> ocs) {
	  String ocName = oc.getLongName();
	  if (ocName != null) {
		  for (ObjectClass listOC: ocs) {
			  String listOCName = listOC.getLongName();
			  if (listOCName != null && ocName.equals(listOCName)) {
				  return true;
			  }
		  }
	  }
	  
	  return false;
  }
  
  private boolean propertiesEqual(DataElement de1, DataElement de2) {
	  Property prop1 = getProperty(de1);
	  Property prop2 = getProperty(de2);
	  
	  if (prop1 != null && prop2 != null) {
		  String longName1 = prop1.getLongName();
		  String longName2 = prop2.getLongName();
		  
		  if (longName1 != null && longName2 != null && longName1.equals(longName2)) {
			  return true;
		  }
	  }
	  return false;
  }
  
  private Property getProperty(DataElement de) {
	  DataElementConcept dec = de.getDataElementConcept();
	  if (dec != null) {
		  if (dec.getProperty() != null) {
			  return dec.getProperty();
		  }
	  }
	  
	  return null;
  }
  
//SIW-794 Allow there to be more than one UML attribute with the same name in a UML Class 
  private void doPackages(UMLModel model) throws ParserException{
	  for(UMLPackage pkg : model.getPackages()) {
		  doPackage(pkg);
      }
	  
  }
  
//SIW-794 Allow there to be more than one UML attribute with the same name in a UML Class 
  private void doPackage(UMLPackage pack) throws ParserException {
	    if (packageName.length() == 0) {
	      packageName = pack.getName();
	    }
	    else {
	      packageName += ("." + pack.getName());
	    }

	    NewPackageEvent event = new NewPackageEvent(LookupUtil.getPackageName(pack));
	    UMLTaggedValue tv = pack.getTaggedValue(TV_GME_NAMESPACE);
	    if(tv != null) {
	      event.setGmeNamespace(tv.getValue());
	    }  

	    //listener.newPackage(event);

	    for(UMLPackage subPkg : pack.getPackages()) {
	      String oldPackage = packageName;
	      doPackage(subPkg);
	      packageName = oldPackage;
	    }
	    String ocName = (String)userSelections.getProperty("ObjectClassName");
	    //logger.info("Object Class Name for the attributes: "+ocName);
	    for(UMLClass clazz : pack.getClasses()) {
	    	//logger.info("Matching OC name: "+LookupUtil.getPackageName(clazz.getPackage())+"."+clazz.getName());
	    	if ((LookupUtil.getPackageName(clazz.getPackage())+"."+clazz.getName()).equalsIgnoreCase(ocName))
	    		doClass(clazz);
	    }

	    packageName = "";
	  }
  
//SIW-794 Allow there to be more than one UML attribute with the same name in a UML Class 
  private void doClass(UMLClass clazz) throws ParserException {
	    String pName = LookupUtil.getPackageName(clazz.getPackage());

	    className = clazz.getName();
	    logger.info("Class Name: "+className);
	    String st = clazz.getStereotype();
	    if(st != null) {
	      boolean foundVd = false;
	      for(int i=0; i<validVdStereotypes.length; i++) {
	        if(st.equalsIgnoreCase(validVdStereotypes[i])) foundVd = true;
	      }

	    }
	      
	    if (pName != null) {
	      className = pName + "." + className;
	    }



	    currentElementIndex++;
	    ProgressEvent evt = new ProgressEvent();
	    evt.setMessage("Parsing " + className);
	    evt.setStatus(currentElementIndex);
	    fireProgressEvent(evt);

	    NewClassEvent event = new NewClassEvent(className.trim());
	    event.setPackageName(pName);

	    //setConceptInfo(clazz, event, TV_TYPE_CLASS);

	    if(className.length() != className.trim().length()) {
	        ValidationItems.getInstance()
	          .addItem(new ValidationFatal
	                (PropertyAccessor.getProperty("class.name.spaces" , event.getName()),null));
	        return;
	    }

	    logger.debug("CLASS PACKAGE: " + LookupUtil.getPackageName(clazz.getPackage()));

	    /*if(isClassBanned(className)) {
	      logger.info(PropertyAccessor.getProperty("class.filtered", className));
	      return;
	    }*/
	    
	    if(StringUtil.isEmpty(pName)) 
	    {
	      logger.info(PropertyAccessor.getProperty("class.no.package", className));
	      return;
	    }

 
	    String longName = (String)userSelections.getProperty("LongName");

	    for(UMLAttribute att : clazz.getAttributes()) {
	    	if (att.getName().equalsIgnoreCase(longName)) 
	    			doAttribute(att);
	    		}
	    className = "";

	  }
  
//SIW-794 Allow there to be more than one UML attribute with the same name in a UML Class  
  private void doAttribute(UMLAttribute att) throws ParserException {
	    NewAttributeEvent event = new NewAttributeEvent(att.getName().trim());
	    event.setClassName(className);
	    currentElementIndex++;
	    ProgressEvent evt = new ProgressEvent();
	    evt.setMessage("Parsing " + att.getName());
	    evt.setStatus(currentElementIndex);
	    fireProgressEvent(evt);
	    
	    String vdIdVersion = "";
	    String attributeName = att.getName();
	    if(attributeName.length() != attributeName.trim().length()) {
	        ValidationItems.getInstance()
	            .addItem(new ValidationFatal(PropertyAccessor.getProperty("attribute.name.spaces" , className+"."+attributeName /*event.getName()*/), null));
	        return;
	    }

	    if(att.getDatatype() == null || att.getDatatype().getName() == null) {
	      ValidationItems.getInstance()
	        .addItem(new ValidationFatal
	                 (PropertyAccessor
	                  .getProperty
	                  ("validation.type.missing.for"
	                   , event.getClassName() + "." + event.getName()),
	                  null));
	      return;
	    }

	    // See if datatype is a simple datatype or a value domain.
	    UMLTaggedValue tv = att.getTaggedValue(TV_VALUE_DOMAIN);
	    if(tv != null) {       // Use Value Domain
	      event.setLocalType(tv.getValue());
	    }
	    
	    event.setType(att.getDatatype().getName());


	    tv = att.getTaggedValue(TV_VD_ID);
	    if(tv != null) {
	     vdIdVersion = tv.getValue().trim();
	      event.setTypeId(tv.getValue().trim());
	    }

	    tv = att.getTaggedValue(TV_VD_VERSION);
	    if(tv != null) {
	      try {
	    	  vdIdVersion = vdIdVersion +  new Float(tv.getValue());
	        event.setTypeVersion(new Float(tv.getValue()));
	      } catch (NumberFormatException e){
	        logger.warn("vd version is not a number, ignoring: " + tv.getValue());     
	      } // end of try-catch
	    }
	    
	    if (vdIdVersion.equals("")) {
	    	tv = att.getTaggedValue(TV_VALUE_DOMAIN);
	    	if(tv != null) {	   	    
	   	     	vdIdVersion = tv.getValue().trim();
	   	     }
	    }
	    
	    if (vdTags.contains(vdIdVersion)) {
	    	dupeCount++;
	    } else {
	    	vdTags.add(vdIdVersion);
	    }
	    tv = att.getTaggedValue(TV_GME_XML_LOC_REFERENCE);
	    if(tv != null) {
	      event.setGmeXmlLocRef(tv.getValue());
	    }

	    //setConceptInfo(att, event, TV_TYPE_PROPERTY);

	    //listener.newAttribute(event);
	  }  

}