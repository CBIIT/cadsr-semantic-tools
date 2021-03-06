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
package gov.nih.nci.ncicb.cadsr.loader.persister;

import gov.nih.nci.ncicb.cadsr.dao.AdminComponentDAO;
import gov.nih.nci.ncicb.cadsr.dao.ConceptDAO;
import gov.nih.nci.ncicb.cadsr.domain.*;

import gov.nih.nci.ncicb.cadsr.loader.ElementsLists;

import org.apache.log4j.Logger;
import gov.nih.nci.ncicb.cadsr.loader.util.PropertyAccessor;
import gov.nih.nci.ncicb.cadsr.loader.util.StringUtil;
import gov.nih.nci.ncicb.cadsr.loader.defaults.UMLDefaults;

import gov.nih.nci.ncicb.cadsr.loader.event.ProgressEvent;
import gov.nih.nci.ncicb.cadsr.loader.event.ProgressListener;
import gov.nih.nci.ncicb.cadsr.loader.ext.EvsModule;
import gov.nih.nci.ncicb.cadsr.loader.ext.EvsResult;

import gov.nih.nci.ncicb.cadsr.loader.util.ConceptUtil;
import gov.nih.nci.ncicb.cadsr.loader.util.DAOAccessor;

import java.util.*;


/**
 *
 * @author <a href="mailto:chris.ludet@oracle.com">Christophe Ludet</a>
 */
public class ConceptPersister implements Persister {

  private static Logger logger = Logger.getLogger(ConceptPersister.class.getName());
  //SIW-407 adding default Definition type see table SBREXT.DEFINITION_TYPES_LOV_EXT 
  public static final String DEFAULT_ALT_DEFINITION_TYPE = "UML Value Definition";
  
  private EvsModule evsModule = new EvsModule();

  private UMLDefaults defaults = UMLDefaults.getInstance();
  private ElementsLists elements = ElementsLists.getInstance();

  private ProgressListener progressListener = null;
  
  private static ConceptDAO conceptDAO;
  private static AdminComponentDAO adminComponentDAO;

  private PersisterUtil persisterUtil;

  public ConceptPersister() {
    initDAOs();
  }

  public void persist() {
    Concept con = DomainObjectFactory.newConcept();
    List<Concept> cons = elements.getElements(con);

    int consSize = cons.size();

    logger.debug("ConceptPersister.persist()");

    sendProgressEvent(0, consSize, "Persisting Concepts");

    int count = 1;
    if (cons != null) {
    	HashMap<Concept, Concept> conceptsToReplace = new HashMap<Concept, Concept>();
      for(Iterator it = cons.iterator(); it.hasNext();) {
        Concept c = (Concept)it.next();
        con.setPreferredName(c.getPreferredName());
        logger.debug("concept name: " + con.getPreferredName());
        sendProgressEvent(count++, consSize, "Concept : " + con.getPreferredName());

        // find out if concept is used by any element. If not, skip this concept. 
        if(!ConceptUtil.isConceptUsed(c)) {
          logger.info(PropertyAccessor.getProperty("concept.never.used", c.getPreferredName()));
          continue;
        }

        List l = conceptDAO.find(con);

        if(l.size() == 0) { // concept does not exist
          // Check if it exists in EVS

          EvsResult evsResult = evsModule.findByConceptCode(c.getPreferredName(), false);
          if(evsResult == null) {
            logger.error(PropertyAccessor.getProperty
                         ("cannot.create.concept.not.in.evs", c.getPreferredName())); 
            throw new PersisterException
              (PropertyAccessor.getProperty
               ("cannot.create.concept.not.in.evs", c.getPreferredName()));
          } else {
            Concept evsConcept = evsResult.getConcept();
            if(evsConcept.getLongName() == null || !evsConcept.getLongName().equals(c.getLongName())) {
              logger.error(PropertyAccessor.getProperty
                           ("cannot.create.concept.in.evs.different.name", c.getPreferredName())); 
              throw new PersisterException
                (PropertyAccessor.getProperty
                 ("cannot.create.concept.in.evs.different.name", c.getPreferredName()));  
            } 
            
            if(//SIW-407 description not to give an error if Pref Def is different
            		((evsConcept.getPreferredDefinition() == null || evsConcept.getPreferredDefinition().equals("")) && !c.getPreferredDefinition().equals(PropertyAccessor.getProperty("default.evs.definition"))) 
                    || 
                    (evsConcept.getPreferredDefinition() != null && !evsConcept.getPreferredDefinition().equals("") && !evsConcept.getPreferredDefinition().equals(c.getPreferredDefinition()))) {
              
              logger.warn("WARNING: " + PropertyAccessor.getProperty
                           ("cannot.create.concept.in.evs.different.definition", c.getPreferredName()) + ", concept: " + toStringConcept(c)); 
//              throw new PersisterException
//                (PropertyAccessor.getProperty
//                 ("cannot.create.concept.in.evs.different.definition", c.getPreferredName()));
            } 
            //SIW-407 
            // create
            c.setVersion(new Float(1.0f));
            c.setContext(defaults.getMainContext());
            c.setWorkflowStatus(AdminComponent.WF_STATUS_RELEASED);
            c.setAudit(defaults.getAudit());
            c.setOrigin(defaults.getOrigin());
            c.setEvsSource(PropertyAccessor.getProperty("default.evsSource"));
            c.setLifecycle(defaults.getLifecycle());
            String evsDefSource = evsConcept.getDefinitionSource();
            String siwDefSource = c.getDefinitionSource();//SIW-407 we want to use Definition Source given in XMI file
            if (StringUtil.isEmpty(siwDefSource)) {
            	siwDefSource = evsDefSource;
            	logger.info("WARNING: Concept Definition Source received in XMI is empty, using EVS Definition Source: " + evsDefSource 
            		+ ", concept: " + toStringConcept(c));
            }
            else if (! evsDefSource.equals(siwDefSource)) {
            	logger.warn("WARNING: EVS Definition Source: " + evsDefSource + " is different than new Concept Definition Source received in XMI: " + siwDefSource 
            		+ ", concept: " + toStringConcept(c));
            }
            c.setDefinitionSource(siwDefSource);

            String evsPrefDef = evsConcept.getPreferredDefinition();
            String siwPrefDef = c.getPreferredDefinition();//SIW-407 we want to use Preferred Definition given in XMI file
            if (StringUtil.isEmpty(siwPrefDef)) {
            	logger.info("WARNING: Preferred Definition received in XMI is empty, using EVS Preferred Definition : " + evsPrefDef 
                		+ ", concept: " + toStringConcept(c));
            	siwPrefDef = evsPrefDef;
            }
            
            StringBuilder builder = new StringBuilder();
  		    for (char currentChar : siwPrefDef.toCharArray()) {
  		    	Character replacementChar = charReplacementMap.get(currentChar);
  		        builder.append(replacementChar != null ? replacementChar : currentChar);
  		    }
  		    c.setPreferredDefinition(builder.toString());

            c.setId(conceptDAO.create(c));
            logger.info(PropertyAccessor.getProperty("created.concept"));
            LogUtil.logAc(c, logger);
          
          }
        } else { // concept exist: See if we need to add alternate def.
          logger.info(PropertyAccessor.getProperty("existed.concept", c.getPreferredName()));


          Concept userConcept = c;
          String newSource = c.getDefinitionSource();
          String newDef = c.getPreferredDefinition();
          String newName = c.getLongName();
          c = (Concept)l.get(0);

          if(!newName.equalsIgnoreCase(c.getLongName())) {
        	  userConcept.setLongName(c.getLongName());
          }
          
          conceptsToReplace.put(c, userConcept);
          
          /*EvsResult evsResult = null;
          // verify that name in input and name in caDSR are the same
          if(!newName.equalsIgnoreCase(c.getLongName())) {
            // the names are not the same. 
            // lookup EVS to see if what's in input is in sync with EVS
            evsResult = evsModule.findByConceptCode(c.getPreferredName(), false);
            
            if(evsResult != null) {
              Concept conRes = evsResult.getConcept();
              if(conRes.getLongName().equals(newName)) {
                // evs return came same as input, so update caDSR
                conceptDAO.updateName(c, newName);
                logger.info("Updated Concept name for concept:  " + c.getPreferredName());
                
                addAlternateName(c, c.getLongName(), AlternateName.PRIOR_PREFERRED_NAME);
              }
            } else { // this concept is not in EVS, the names are not the same. Stop the load.
              logger.error(PropertyAccessor.getProperty("cant.validate.concept", 
                                                        new String[]{c.getPreferredName(), "preferred name"}));
              throw new PersisterException
                (PropertyAccessor.getProperty
                 ("cant.validate.concept", 
                  new String[]{c.getPreferredName(), "preferred name"})
);
            }

          }*/

          if(newSource != null && !newSource.equalsIgnoreCase(c.getDefinitionSource())) { // Add alt def.

            logger.debug("Concept had different definition source. newSource: " + newSource + ' ' + toStringConcept(c));
            Definition def = DomainObjectFactory.newDefinition();
            def.setType(newSource);//SIW-407 This value can be not in the allowed LoV from SBREXT.DEFINITION_TYPES_LOV_EXT
            def.setDefinition(newDef);
            def.setAudit(defaults.getAudit());
            def.setContext(defaults.getContext());
            
            //SIW-407 If we cannot use user provided Definition type
            addConceptAltDefinition(c, def);
          } else {
            // Do nothing, this is the common case where the concept existed and had the same def source.
          }
        }
      }
      
      replaceConcepts(conceptsToReplace, cons);
    }
  }
  //SIW-407 re-factoring and changing
  private void addConceptAltDefinition (Concept c, Definition def) {
      try {
      	adminComponentDAO.addDefinition(c, def);//Trying to use user provided Definition type
      	logger.info(PropertyAccessor.getProperty("added.altDef", new String[]{c.getPreferredName(), def.getDefinition(), "Concept"}));
      }
      catch (Exception e) {
      	try {
          	String conceptAltDefError = "Error adding Alternate Definition: " + toStringDefinition(def) + " to Concept: " + toStringConcept(c) + '\n' + e;
          	logger.error(conceptAltDefError);
          	logger.debug("...Adding Alternate Definition with default type: " + DEFAULT_ALT_DEFINITION_TYPE);
          	def.setType(DEFAULT_ALT_DEFINITION_TYPE);//If we cannot use user provided Definition type we can try default type
          	adminComponentDAO.addDefinition(c, def);
          	logger.info(PropertyAccessor.getProperty("added.altDef", new String[]{c.getPreferredName(), def.getDefinition(), "Concept"}));
          	logger.warn("WARNING: Consider manually changing new Concept Alternate Definition type from default type: " + toStringDefinition(def) + "; Concept: " + toStringConcept(c));
      	}
      	catch (Exception ex) {//Skipping creating Alt Definition when we have an error
          	String conceptAltDefError = "Error adding Alternate Definition: " + toStringDefinition(def) + " to Concept: " + toStringConcept(c) + '\n' + e;
          	logger.error(conceptAltDefError);
          	logger.warn("WARNING: Concept Alternate Definition is not created. Consider manually creating Alternate Definition for Concept: " + toStringConcept(c));	            	
      	}
     }
  }
  private List<Concept> replaceConcepts(HashMap<Concept, Concept> conceptsToReplace, List<Concept> conList) {
	  if (conceptsToReplace != null && conceptsToReplace.size() > 0 && conList != null) {
		  for (Concept con: conceptsToReplace.keySet()) {
			  conList.remove(con);
			  conList.add(conceptsToReplace.get(con));
		  }
	  }
	  
	  return conList;
  }

  private void addAlternateName(Concept con, String newName, String type) 
  {
    List<String> eager = new ArrayList<String>();

    List<AlternateName> altNames = adminComponentDAO.getAlternateNames(con, eager);
    boolean found = false;
    for(AlternateName an : altNames) {
      if(an.getType().equals(type) && an.getName().equals(newName)) {
        found = true;
        logger.info(PropertyAccessor.getProperty(
                      "existed.altName", newName));
      }
    }
    
    if(!found) {
      AlternateName altName = DomainObjectFactory.newAlternateName();
      altName.setContext(defaults.getContext());
      altName.setAudit(defaults.getAudit());
      altName.setName(newName);
      altName.setType(type);
      altName.setId(adminComponentDAO.addAlternateName(con, altName));
      logger.info(PropertyAccessor.getProperty(
                    "added.altName", 
                    new String[] {
                      altName.getName(),
                      con.getLongName()
                    }));
      
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

  public void setPersisterUtil(PersisterUtil pu) {
    persisterUtil = pu;
  }

  public void setProgressListener(ProgressListener listener) {
    progressListener = listener;
  }

  private void initDAOs()  {
    conceptDAO = DAOAccessor.getConceptDAO();
    adminComponentDAO = DAOAccessor.getAdminComponentDAO();
  }
  protected static String toStringConcept(Concept concept) {
	  return "[Concept: evsSource=" + concept.getEvsSource() 
	  + ", definitionSource=" + concept.getDefinitionSource() 
	  +	  ", publicId=" + concept.getPublicId()
	  + ", preferredName=" + concept.getPreferredName() 
	  + ", longName=" + concept.getLongName() 
	  + ", version=" + concept.getVersion() 
	  + ", preferredDefinition=" + concept.getPreferredDefinition()
	  + ", workflowStatus=" + concept.getWorkflowStatus()
	  + ", origin=" + concept.getOrigin() + "]";
  }
  protected static String toStringDefinition(Definition def) {
	  String context = (def.getContext() != null) ? def.getContext().getName() : null;
	  return "[Definition: definition=" + def.getDefinition() 
	  + ", type=" + def.getType()
	  + ", context=" + context + "]";
  }
}
