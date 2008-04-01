/*
 * Copyright 2000-2005 Oracle, Inc. This software was developed in conjunction with the National Cancer Institute, and so to the extent government employees are co-authors, any rights in such works shall be subject to Title 17 of the United States Code, section 105.
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
package gov.nih.nci.ncicb.cadsr.loader.parser;

import gov.nih.nci.ncicb.cadsr.domain.*;
import gov.nih.nci.ncicb.cadsr.loader.*;
import gov.nih.nci.ncicb.cadsr.loader.util.*;
import gov.nih.nci.ncicb.cadsr.loader.event.ProgressListener;
import gov.nih.nci.ncicb.cadsr.loader.event.ProgressEvent;

import gov.nih.nci.ncicb.xmiinout.handler.*;
import gov.nih.nci.ncicb.xmiinout.domain.*;

import org.apache.log4j.Logger;

import java.util.*;

/**
 * A writer for XMI files 
 *
 * @author <a href="mailto:chris.ludet@oracle.com">Christophe Ludet</a>
 */
public class XMIWriter2 implements ElementWriter {

  private String output = null;

  private HashMap<String, UMLClass> classMap = new HashMap<String, UMLClass>();
  private HashMap<String, UMLAttribute> attributeMap = new HashMap<String, UMLAttribute>();
  private HashMap<String, UMLAssociation> assocMap = new HashMap<String, UMLAssociation>();
  private HashMap<String, UMLAssociationEnd> assocEndMap = new HashMap<String, UMLAssociationEnd>();

  private ElementsLists cadsrObjects = null;

  private ReviewTracker ownerReviewTracker = ReviewTracker.getInstance(ReviewTrackerType.Owner), 
    curatorReviewTracker = ReviewTracker.getInstance(ReviewTrackerType.Curator);

  private ChangeTracker changeTracker = ChangeTracker.getInstance();

  private ProgressListener progressListener;
  
  private UMLModel model = null;
  private XmiInOutHandler handler = null;

  private Logger logger = Logger.getLogger(XMIWriter2.class.getName());

  public XMIWriter2() {
  }

  public void write(ElementsLists elements) throws ParserException {
    try {
      handler = (XmiInOutHandler)(UserSelections.getInstance().getProperty("XMI_HANDLER"));

      model = handler.getModel();

      this.cadsrObjects = elements;
    
      sendProgressEvent(0, 0, "Parsing Model");
      readModel();
//       doReviewTagLogic();
      sendProgressEvent(0, 0, "Marking Human reviewed");
      markHumanReviewed();
      sendProgressEvent(0, 0, "Updating Elements");
      updateChangedElements();
      sendProgressEvent(0, 0, "ReWriting Model");
      handler.save(output);

    } catch (Exception ex) {
      throw new RuntimeException("Error initializing model", ex);
    }

  }

  public void setOutput(String url) {
    this.output = url;
  }

  public void setProgressListener(ProgressListener listener) {
    progressListener = listener;
  }

  private void readModel() {
    for(UMLPackage pkg : model.getPackages())
      doPackage(pkg);

    int xi = 0;
    for(UMLAssociation assoc : model.getAssociations()) {
        List<UMLAssociationEnd> ends = assoc.getAssociationEnds();
        UMLAssociationEnd aEnd = ends.get(0);
        UMLAssociationEnd bEnd = ends.get(1);
        
        UMLAssociationEnd source = bEnd, target = aEnd;
        // direction B?
        if (bEnd.isNavigable() && !aEnd.isNavigable()) {
            source = aEnd;
            target = bEnd;
        }

        // The assocMap is used in parallel with the Object Class Relationships (OCR). To easily find the
        // corresponding OCR for an Association and the reverse, the key for the map represents the index
        // of the OCR in its list. This works because the same XMI is read/parsed for to create both. In this
        // loop the model.getAssociations() will be in the same order so we can use an artificial index
        // counter.

        String key = String.valueOf(xi); // assoc.getRoleName()+"~"+source.getRoleName()+"~"+target.getRoleName();
        assocMap.put(key,assoc);
        assocEndMap.put(key+"~source",source);
        assocEndMap.put(key+"~target",target);
        
        ++xi;
    }
  }

  private void updateChangedElements()  {
    List<ObjectClass> ocs = cadsrObjects.getElements(DomainObjectFactory.newObjectClass());
    List<DataElement> des = cadsrObjects.getElements(DomainObjectFactory.newDataElement());
    List<ValueDomain> vds = cadsrObjects.getElements(DomainObjectFactory.newValueDomain());
    List<ObjectClassRelationship> ocrs = cadsrObjects.getElements(DomainObjectFactory.newObjectClassRelationship());
    
    int goal = ocs.size() + des.size() + vds.size() + ocrs.size();
    int status = 0;
    sendProgressEvent(status, goal, "");
    
    for(ObjectClass oc : ocs) {
      String fullClassName = null;
      for(AlternateName an : oc.getAlternateNames()) {
        if(an.getType().equals(AlternateName.TYPE_CLASS_FULL_NAME))
          fullClassName = an.getName();
      }

      sendProgressEvent(status++, goal, "Class: " + fullClassName);

      UMLClass clazz = classMap.get(fullClassName);

      String [] conceptCodes = oc.getPreferredName().split(":");

      boolean changed = changeTracker.get(fullClassName);

      // is one of the concepts in this OC changed?
      for(String s : conceptCodes)
        changed = changed | changeTracker.get(s);

      if(changed) {
        // drop all current concept tagged values
        Collection<UMLTaggedValue> allTvs = clazz.getTaggedValues();
        for(UMLTaggedValue tv : allTvs) {
          if(tv.getName().startsWith("ObjectClass") ||
             tv.getName().startsWith("ObjectQualifier"))
            clazz.removeTaggedValue(tv.getName());
        }

        addConceptTvs(clazz, conceptCodes, XMIParser2.TV_TYPE_CLASS);
      }
        
    }

    InheritedAttributeList inheritedList = InheritedAttributeList.getInstance();
    for(DataElement de : des) {
      DataElementConcept dec = de.getDataElementConcept();
      String fullPropName = null;

      for(AlternateName an : de.getAlternateNames()) {
        if(an.getType().equals(AlternateName.TYPE_FULL_NAME))
          fullPropName = an.getName();
      }
      sendProgressEvent(status++, goal, "Attribute: " + fullPropName);

      UMLAttribute att = attributeMap.get(fullPropName);
        
      boolean changed = changeTracker.get(fullPropName);

      String [] conceptCodes = dec.getProperty().getPreferredName().split(":");
      // is one of the concepts in this Property changed?
      for(String s : conceptCodes)
        changed = changed | changeTracker.get(s);

      if(changed) {
        if(!inheritedList.isInherited(de)) {
          // drop all current concept tagged values
          Collection<UMLTaggedValue> allTvs = att.getTaggedValues();
          for(UMLTaggedValue tv : allTvs) {
            if(tv.getName().startsWith("Property") ||
               tv.getName().startsWith("PropertyQualifier"))
              att.removeTaggedValue(tv.getName());
          }
          
          // Map to Existing DE
          if(!StringUtil.isEmpty(de.getPublicId()) && de.getVersion() != null) {
            att.removeTaggedValue(XMIParser2.TV_DE_ID);
            att.removeTaggedValue(XMIParser2.TV_DE_VERSION);
            
            att.addTaggedValue(XMIParser2.TV_DE_ID,
                               de.getPublicId());
            
            att.addTaggedValue(XMIParser2.TV_DE_VERSION,
                               de.getVersion().toString());
            
          } else {
            att.removeTaggedValue(XMIParser2.TV_DE_ID);
            att.removeTaggedValue(XMIParser2.TV_DE_VERSION);
            
            if(!StringUtil.isEmpty(de.getValueDomain().getPublicId()) && de.getValueDomain().getVersion() != null) {
              att.removeTaggedValue(XMIParser2.TV_VD_ID);
              att.removeTaggedValue(XMIParser2.TV_VD_VERSION);
              att.addTaggedValue(XMIParser2.TV_VD_ID,
                                 de.getValueDomain().getPublicId());
              att.addTaggedValue(XMIParser2.TV_VD_VERSION,
                                 de.getValueDomain().getVersion().toString());
            }
            else {
              att.removeTaggedValue(XMIParser2.TV_VD_ID);
              att.removeTaggedValue(XMIParser2.TV_VD_VERSION);
            }
            addConceptTvs(att, conceptCodes, XMIParser2.TV_TYPE_PROPERTY);
          }
        } else { // in case of inherited attribute
          ObjectClass oc = de.getDataElementConcept().getObjectClass();
          String fullClassName = LookupUtil.lookupFullName(oc);
          
          UMLClass clazz = classMap.get(fullClassName);

          String attributeName = LookupUtil.lookupFullName(de);
          attributeName = attributeName.substring(attributeName.lastIndexOf(".") + 1);

          clazz.removeTaggedValue(XMIParser2.TV_INHERITED_DE_ID.replace("{1}", attributeName));
          clazz.removeTaggedValue(XMIParser2.TV_INHERITED_DE_VERSION.replace("{1}", attributeName));
          clazz.removeTaggedValue(XMIParser2.TV_INHERITED_VD_ID.replace("{1}", attributeName));
          clazz.removeTaggedValue(XMIParser2.TV_INHERITED_VD_VERSION.replace("{1}", attributeName));
          
          if(!StringUtil.isEmpty(de.getPublicId())) {
            clazz.addTaggedValue(XMIParser2.TV_INHERITED_DE_ID.replace("{1}", attributeName), de.getPublicId());
            clazz.addTaggedValue(XMIParser2.TV_INHERITED_DE_VERSION.replace("{1}", attributeName), de.getVersion().toString());
          } else if(!StringUtil.isEmpty(de.getValueDomain().getPublicId())) {
            clazz.addTaggedValue(XMIParser2.TV_INHERITED_VD_ID.replace("{1}", attributeName), de.getValueDomain().getPublicId());
            clazz.addTaggedValue(XMIParser2.TV_INHERITED_VD_VERSION.replace("{1}", attributeName), de.getValueDomain().getVersion().toString());
          }
        }

      }
    }

    for(ValueDomain vd : vds) {

      sendProgressEvent(status++, goal, "Value Domain: " + vd.getLongName());
      String fullClassName = "ValueDomains." + LookupUtil.lookupFullName(vd);

      UMLClass clazz = classMap.get(fullClassName);
      
//       boolean vdChanged = changeTracker.get(vd.getLongName());
      boolean vdChanged = changeTracker.get(LookupUtil.lookupFullName(vd));
      
      if(vdChanged){
        clazz.removeTaggedValue(XMIParser2.TV_VD_DEFINITION);
        clazz.removeTaggedValue(XMIParser2.TV_VD_DATATYPE); 
        clazz.removeTaggedValue(XMIParser2.TV_VD_TYPE);
        clazz.removeTaggedValue(XMIParser2.TV_CD_ID);
        clazz.removeTaggedValue(XMIParser2.TV_CD_VERSION);
        clazz.removeTaggedValue(XMIParser2.TV_REP_ID);
        clazz.removeTaggedValue(XMIParser2.TV_REP_VERSION);
        clazz.removeTaggedValue(XMIParser2.TV_VD_ID);
        clazz.removeTaggedValue(XMIParser2.TV_VD_VERSION);
        
        if(!StringUtil.isEmpty(vd.getPublicId()) && vd.getVersion() != null) {
          clazz.addTaggedValue(XMIParser2.TV_VD_ID, vd.getPublicId());
          clazz.addTaggedValue(XMIParser2.TV_VD_VERSION, vd.getVersion().toString());
        } else {
          if(!StringUtil.isEmpty(vd.getPreferredDefinition()))
            clazz.addTaggedValue(XMIParser2.TV_VD_DEFINITION, vd.getPreferredDefinition());
          
          if(!StringUtil.isEmpty(vd.getDataType()))
            clazz.addTaggedValue(XMIParser2.TV_VD_DATATYPE, vd.getDataType());
          
          if(!StringUtil.isEmpty(vd.getVdType()))
            if(!vd.getVdType().equals("null"))
              if(!vd.getVdType().equals(null)){
                clazz.addTaggedValue(XMIParser2.TV_VD_TYPE, vd.getVdType());
              }
            
          if(vd.getConceptualDomain() != null) {
            if(!StringUtil.isEmpty(vd.getConceptualDomain().getPublicId()))
              clazz.addTaggedValue(XMIParser2.TV_CD_ID, vd.getConceptualDomain().getPublicId());
              
            if(vd.getConceptualDomain().getVersion() != null)
              clazz.addTaggedValue(XMIParser2.TV_CD_VERSION, vd.getConceptualDomain().getVersion().toString());
          }            
            
          if(vd.getRepresentation() != null) {
            if(!StringUtil.isEmpty(vd.getRepresentation().getPublicId()))
              clazz.addTaggedValue(XMIParser2.TV_REP_ID, vd.getRepresentation().getPublicId());
              
            if(vd.getRepresentation().getVersion() != null)
              clazz.addTaggedValue(XMIParser2.TV_REP_VERSION, String.valueOf(vd.getRepresentation().getVersion().toString()));
          }
        }
      }        

      for(PermissibleValue pv : vd.getPermissibleValues()) {
        ValueMeaning vm = pv.getValueMeaning();
        String fullPropName = "ValueDomains." + vd.getLongName() + "." + vm.getLongName();
        UMLAttribute att = attributeMap.get(fullPropName);
        boolean changed = changeTracker.get(fullPropName);

        String [] conceptCodes = ConceptUtil.getConceptCodes(vm);
        // is one of the concepts in this VM changed?
        for(String s : conceptCodes)
          changed = changed | changeTracker.get(s);

        if(changed) {
          // drop all current concept tagged values
          Collection<UMLTaggedValue> allTvs = att.getTaggedValues();
          for(UMLTaggedValue tv : allTvs) {
            if(tv.getName().startsWith(XMIParser2.TV_TYPE_VM) ||
               tv.getName().startsWith(XMIParser2.TV_TYPE_VM + "Qualifier"))
            att.removeTaggedValue(tv.getName());
          }

          addConceptTvs(att, conceptCodes, XMIParser2.TV_TYPE_VM);
        }
      }
    }

    for(int xi = 0; xi < ocrs.size(); ++xi) {
      
      // For this to work the OCRS must appear in the same order as the model.getAssociations().
      // This is further noted in the readModel() method with the assocMap.put(...) reference.
      ObjectClassRelationship ocr = ocrs.get(xi);

      ConceptDerivationRule rule = ocr.getConceptDerivationRule();
      ConceptDerivationRule srule = ocr.getSourceRoleConceptDerivationRule();
      ConceptDerivationRule trule = ocr.getTargetRoleConceptDerivationRule();
      
      OCRRoleNameBuilder nameBuilder = new OCRRoleNameBuilder();
      String fullName = nameBuilder.buildRoleName(ocr);
      
      sendProgressEvent(status++, goal, "Relationship: " + fullName);

      // The key assigned to the assocMap entry is the index from the OCRS list. See the readModel()
      // method for more information at the assocMap.put(...)
      String key = String.valueOf(xi); // ocr.getLongName()+"~"+ocr.getSourceRole()+"~"+ocr.getTargetRole();
      UMLAssociation assoc = assocMap.get(key);
      UMLAssociationEnd source = assocEndMap.get(key+"~source");
      UMLAssociationEnd target = assocEndMap.get(key+"~target");


      // Role Level
      if (rule != null ){
        List<ComponentConcept> rConcepts = rule.getComponentConcepts();
        String[] rcodes = new String[rConcepts.size()];
        int i = 0;
        for (ComponentConcept con: rConcepts) {
          rcodes[i++] = con.getConcept().getPreferredName();
        }
        
        boolean changed = changeTracker.get(fullName);
        for(String s : rcodes)
          changed = changed | changeTracker.get(s);
        
        if(changed) {
          dropCurrentAssocTvs(assoc);
          addConceptTvs(assoc, rcodes, XMIParser2.TV_TYPE_ASSOC_ROLE);
        }
      }
      
      // Source Level
      if (srule != null) {
        List<ComponentConcept> sConcepts = srule.getComponentConcepts();
        String[] scodes = new String[sConcepts.size()];
        int i = 0;
        for (ComponentConcept con: sConcepts) {
          scodes[i++] = con.getConcept().getPreferredName();
        }
        boolean changedSource = changeTracker.get(fullName+" Source");
        for(String s : scodes)
          changedSource = changedSource | changeTracker.get(s);
        if(changedSource) {
          dropCurrentAssocTvs(source);
          addConceptTvs(source, scodes, XMIParser2.TV_TYPE_ASSOC_SOURCE);
        }
      }

      // Target Level
      if(trule != null) {
        List<ComponentConcept> tConcepts = trule.getComponentConcepts();
        String[] tcodes = new String[tConcepts.size()];
        int i = 0;
        for (ComponentConcept con: tConcepts) {
          tcodes[i++] = con.getConcept().getPreferredName();
        }

        boolean changedTarget = changeTracker.get(fullName+" Target");
        for(String s : tcodes)
          changedTarget = changedTarget | changeTracker.get(s);

        if(changedTarget) {
          dropCurrentAssocTvs(target);
          addConceptTvs(target, tcodes, XMIParser2.TV_TYPE_ASSOC_TARGET);
        }
      }
      
    }
    
    changeTracker.clear();
  }
  
  private void dropCurrentAssocTvs(UMLTaggableElement elt) {
      Collection<UMLTaggedValue> allTvs = elt.getTaggedValues();
      for(UMLTaggedValue tv : allTvs) {
        String name = tv.getName();
        if(name.startsWith(XMIParser2.TV_TYPE_ASSOC_ROLE) ||
                name.startsWith(XMIParser2.TV_TYPE_ASSOC_SOURCE)||
                name.startsWith(XMIParser2.TV_TYPE_ASSOC_TARGET)) {
            elt.removeTaggedValue(name);
        }
      }
  }

  private void addConceptTvs(UMLTaggableElement elt, String[] conceptCodes, String type) {
    if(conceptCodes.length == 0)
      return;

    addConceptTv(elt, conceptCodes[conceptCodes.length - 1], type, "", 0);

    for(int i= 1; i < conceptCodes.length; i++) {
      
      addConceptTv(elt, conceptCodes[conceptCodes.length - i - 1], type, XMIParser2.TV_QUALIFIER, i);

    }

  }

  private void addConceptTv(UMLTaggableElement elt, String conceptCode, String type, String pre, int n) {

    Concept con = LookupUtil.lookupConcept(conceptCode);
    if(con == null)
      return;

    String tvName = type + pre + XMIParser2.TV_CONCEPT_CODE + ((n>0)?""+n:"");

    if(con.getPreferredName() != null)
      elt.addTaggedValue(tvName,con.getPreferredName());
    
    tvName = type + pre + XMIParser2.TV_CONCEPT_DEFINITION + ((n>0)?""+n:"");

    if(con.getPreferredDefinition() != null)
      addSplitTaggedValue(elt, tvName, con.getPreferredDefinition(), "_");
//       elt.addTaggedValue(tvName,con.getPreferredDefinition());
    
    tvName = type + pre + XMIParser2.TV_CONCEPT_DEFINITION_SOURCE + ((n>0)?""+n:"");

    if(con.getDefinitionSource() != null)
      elt.addTaggedValue(tvName,con.getDefinitionSource());
    
    tvName = type + pre + XMIParser2.TV_CONCEPT_PREFERRED_NAME + ((n>0)?""+n:"");

    if(con.getLongName() != null)
      elt.addTaggedValue
        (tvName,
         con.getLongName());
    
//    tvName = type + pre + XMIParser2.TV_TYPE_VM + ((n>0)?""+n:"");
//    
//    if(con.getLongName() != null)
//      elt.addTaggedValue
//        (tvName,
//         con.getLongName());
  }
  
  private void markHumanReviewed() throws ParserException {
    try{ 
      List<ObjectClass> ocs = cadsrObjects.getElements(DomainObjectFactory.newObjectClass());
      List<DataElement> des = cadsrObjects.getElements(DomainObjectFactory.newDataElement());
      List<ValueDomain> vds = cadsrObjects.getElements(DomainObjectFactory.newValueDomain());
      List<ObjectClassRelationship> ocrs = cadsrObjects.getElements(DomainObjectFactory.newObjectClassRelationship());
      
      for(ObjectClass oc : ocs) {
        String fullClassName = null;
        for(AlternateName an : oc.getAlternateNames()) {
          if(an.getType().equals(AlternateName.TYPE_CLASS_FULL_NAME))
            fullClassName = an.getName();
        }

        UMLClass clazz = classMap.get(fullClassName);

        Boolean reviewed = ownerReviewTracker.get(fullClassName);
        if(reviewed != null) {
          clazz.removeTaggedValue(XMIParser2.TV_OWNER_REVIEWED);
          clazz.addTaggedValue(XMIParser2.TV_OWNER_REVIEWED,
                                reviewed?"1":"0");
        }

        reviewed = curatorReviewTracker.get(fullClassName);
        if(reviewed != null) {
          clazz.removeTaggedValue(XMIParser2.TV_CURATOR_REVIEWED);
          clazz.addTaggedValue(XMIParser2.TV_CURATOR_REVIEWED,
                                reviewed?"1":"0");
        }
      }

      InheritedAttributeList inheritedList = InheritedAttributeList.getInstance();
      for(DataElement de : des) {
        DataElementConcept dec = de.getDataElementConcept();

        String fullClassName = LookupUtil.lookupFullName(de.getDataElementConcept().getObjectClass());
        String fullPropName = fullClassName + "." + dec.getProperty().getLongName();

        String attributeName = LookupUtil.lookupFullName(de);
        attributeName = attributeName.substring(attributeName.lastIndexOf(".") + 1);

        Boolean reviewed = ownerReviewTracker.get(fullPropName);
        if(reviewed != null) {
          if(!inheritedList.isInherited(de)) {
            UMLAttribute umlAtt = attributeMap.get(fullPropName);
            umlAtt.removeTaggedValue(XMIParser2.TV_OWNER_REVIEWED);
            umlAtt.addTaggedValue(XMIParser2.TV_OWNER_REVIEWED,
                                reviewed?"1":"0");
          } else {
            UMLClass clazz = classMap.get(fullClassName);
            clazz.removeTaggedValue(XMIParser2.TV_INHERITED_OWNER_REVIEWED.replace("{1}", attributeName));
            clazz.addTaggedValue(XMIParser2.TV_INHERITED_OWNER_REVIEWED.replace("{1}", attributeName), reviewed?"1":"0");
          }
        }

        reviewed = curatorReviewTracker.get(fullPropName);
        if(reviewed != null) {
          if(!inheritedList.isInherited(de)) {
            UMLAttribute umlAtt = attributeMap.get(fullPropName);
            umlAtt.removeTaggedValue(XMIParser2.TV_CURATOR_REVIEWED);
            umlAtt.addTaggedValue(XMIParser2.TV_CURATOR_REVIEWED,
                                reviewed?"1":"0");
          } else {
            UMLClass clazz = classMap.get(fullClassName);
            clazz.removeTaggedValue(XMIParser2.TV_INHERITED_CURATOR_REVIEWED.replace("{1}", attributeName));
            clazz.addTaggedValue(XMIParser2.TV_INHERITED_CURATOR_REVIEWED.replace("{1}", attributeName), reviewed?"1":"0");
          }
            
        }

      }

      for(ValueDomain vd : vds) {
        for(PermissibleValue pv : vd.getPermissibleValues()) {
          ValueMeaning vm = pv.getValueMeaning();
          String fullPropName = "ValueDomains." + vd.getLongName() + "." + vm.getLongName();

          Boolean reviewed = ownerReviewTracker.get(fullPropName);
          UMLAttribute umlAtt = null;
          if(reviewed != null) {
            umlAtt = attributeMap.get(fullPropName);
            umlAtt.removeTaggedValue(XMIParser2.TV_OWNER_REVIEWED);
            umlAtt.addTaggedValue(XMIParser2.TV_OWNER_REVIEWED,
                                  reviewed?"1":"0");
          }

          reviewed = curatorReviewTracker.get(fullPropName);
          if(reviewed != null) {
            umlAtt = attributeMap.get(fullPropName);
            umlAtt.removeTaggedValue(XMIParser2.TV_CURATOR_REVIEWED);
            umlAtt.addTaggedValue(XMIParser2.TV_CURATOR_REVIEWED,
                                  reviewed?"1":"0");
          }
        }
      }
      
      for(int xi = 0; xi < ocrs.size(); ++xi) {
          
          // For this to work the OCRS must appear in the same order as the model.getAssociations().
          // This is further noted in the readModel() method with the assocMap.put(...) reference.
          ObjectClassRelationship ocr  = ocrs.get(xi);

          final OCRRoleNameBuilder nameBuilder = new OCRRoleNameBuilder();
          final String fullPropName = nameBuilder.buildRoleName(ocr);
          final String tPropName = fullPropName + " Target";
          final String sPropName = fullPropName + " Source";
          
          // The key assigned to the assocMap entry is the index from the OCRS list. See the readModel()
          // method for more information at the assocMap.put(...)
          final String key = String.valueOf(xi); // ocr.getLongName()+"~"+ocr.getSourceRole()+"~"+ocr.getTargetRole();
          final UMLAssociation assoc = assocMap.get(key);
          final UMLAssociationEnd target = assocEndMap.get(key+"~target");
          final UMLAssociationEnd source = assocEndMap.get(key+"~source");
          
          // ROLE
          Boolean reviewed = ownerReviewTracker.get(fullPropName);
          if(reviewed != null) refreshOwnerTag(assoc, reviewed);
          reviewed = curatorReviewTracker.get(fullPropName);
          if(reviewed != null) refreshCuratorTag(assoc, reviewed);

          // SOURCE
          reviewed = ownerReviewTracker.get(sPropName);
          if(reviewed != null) refreshOwnerTag(source, reviewed);
          reviewed = curatorReviewTracker.get(sPropName);
          if(reviewed != null) refreshCuratorTag(source, reviewed);
          
          // TARGET
          reviewed = ownerReviewTracker.get(tPropName);
          if(reviewed != null) refreshOwnerTag(target, reviewed);
          reviewed = curatorReviewTracker.get(tPropName);
          if(reviewed != null) refreshCuratorTag(target, reviewed);
        }

    } catch (RuntimeException e) {
      throw new ParserException(e);
    }
  }

  private void refreshCuratorTag(UMLTaggableElement umlElement, boolean reviewed) {
      umlElement.removeTaggedValue(XMIParser2.TV_CURATOR_REVIEWED);
      umlElement.addTaggedValue(XMIParser2.TV_CURATOR_REVIEWED,
                            reviewed?"1":"0");
  }
  
  private void refreshOwnerTag(UMLTaggableElement umlElement, boolean reviewed) {
      umlElement.removeTaggedValue(XMIParser2.TV_OWNER_REVIEWED);
      umlElement.addTaggedValue(XMIParser2.TV_OWNER_REVIEWED,
                            reviewed?"1":"0");
  }
  
  private void doPackage(UMLPackage pkg) {
    for(UMLClass clazz : pkg.getClasses()) {
      String className = null;
      
      String st = clazz.getStereotype();
      boolean foundVd = false;
      if(st != null)
        for(int i=0; i < XMIParser2.validVdStereotypes.length; i++) {
          if(st.equalsIgnoreCase(XMIParser2.validVdStereotypes[i])) foundVd = true;
        }
      if(foundVd) {
        className = "ValueDomains." + clazz.getName();
      } else {
        className = getPackageName(pkg) + "." + clazz.getName();
      }
      classMap.put(className, clazz);
      for(UMLAttribute att : clazz.getAttributes()) {
        attributeMap.put(className + "." + att.getName(), att);
      }
    }

    for(UMLPackage subPkg : pkg.getPackages()) {
      doPackage(subPkg);
    }

  }

  protected void sendProgressEvent(int status, int goal, String message) {
    if(progressListener != null) {
      ProgressEvent pEvent = new ProgressEvent();
      pEvent.setMessage(message);
      pEvent.setStatus(status);
      pEvent.setGoal(goal);
      
      progressListener.newProgressEvent(pEvent);
    }
  }
 
  private String getPackageName(UMLPackage pkg) {
    StringBuffer pack = new StringBuffer();
    String s = null;
    do {
      s = null;
      if(pkg != null) {
        s = pkg.getName(); 
        if(s.indexOf(" ") == -1) {
          if(pack.length() > 0)
            pack.insert(0, '.');
          pack.insert(0, s);
        }
        pkg = pkg.getParent();
      }
    } while (s != null);
    
    return pack.toString();
  }
 

  /**
   * This method moved to DefinitionSplitter. Remove in next version. 
   * @deprecated
   */
  private void addSplitTaggedValue(UMLTaggableElement elt, String tag, String value, String separator) 
  {  

    final int MAX_TV_SIZE = 255;

    if(value.length() > MAX_TV_SIZE) {
      int nbOfTags = (int)(Math.ceil((double)value.length() / (double)MAX_TV_SIZE));

      for(int i = 0; i < nbOfTags; i++) {
        String thisTag = (i==0)?tag:tag + separator + (i+1);

        int index = i*MAX_TV_SIZE;
        
        String thisValue = (index + MAX_TV_SIZE > value.length())?value.substring(index):value.substring(index, index + MAX_TV_SIZE);
        
        elt.addTaggedValue(thisTag, thisValue);
      }
      


    } else {
      elt.addTaggedValue(tag, value);
    }
  }

}
