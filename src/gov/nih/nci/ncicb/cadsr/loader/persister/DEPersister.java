package gov.nih.nci.ncicb.cadsr.loader.persister;

import gov.nih.nci.ncicb.cadsr.loader.defaults.UMLDefaults;
import gov.nih.nci.ncicb.cadsr.dao.*;
import gov.nih.nci.ncicb.cadsr.domain.*;
import gov.nih.nci.ncicb.cadsr.loader.ElementsLists;
import gov.nih.nci.ncicb.cadsr.loader.util.PropertyAccessor;

import org.apache.log4j.Logger;

import java.util.*;


public class DEPersister extends UMLPersister {

  private static Logger logger = Logger.getLogger(DEPersister.class.getName());

  public DEPersister(ElementsLists list) {
    this.elements = list;
    defaults = UMLDefaults.getInstance();
  }

  public void persist() throws PersisterException {
    DataElement de = DomainObjectFactory.newDataElement();
    List des = (List) elements.getElements(de.getClass());

    logger.debug("des...");

    if (des != null) {
      for (ListIterator it = des.listIterator(); it.hasNext();) {
	try {
	  de = (DataElement) it.next();

	  de.setContext(defaults.getContext());

	  for (ListIterator lit = elements.getElements(
                 DomainObjectFactory.
                 newDataElementConcept().getClass()).listIterator(); lit.hasNext();) {
            
	    DataElementConcept o = (DataElementConcept) lit.next();
	    if (o.getObjectClass().getConcept().getId()
                .equals(de.getDataElementConcept()
                        .getObjectClass().getConcept().getId())
                && 
                o.getProperty().getConcept().getId()
                .equals(de.getDataElementConcept()
                        .getProperty().getConcept().getId()))
	      de.setDataElementConcept(o);
	  }

          de.setLongName(
            de.getDataElementConcept()
            .getObjectClass().getConcept().getPreferredName()
            + de.getDataElementConcept()
            .getProperty().getConcept().getPreferredName());

	  de.setValueDomain(lookupValueDomain(de.getValueDomain()));

	  List l = dataElementDAO.find(de);

	  if (l.size() == 0) {
            // !!!! TODO
	    de.setPreferredDefinition("???");
	    de.setPreferredName(de.getLongName());

	    // !!!!! TODO -- following will pass constraints
	    if (de.getPreferredName().length() > 30) {
	      de.setPreferredName(
                de.getPreferredName().substring(0,29));
	    }

	    de.setVersion(defaults.getVersion());
	    de.setWorkflowStatus(defaults.getWorkflowStatus());

	    de.setAudit(defaults.getAudit());
	    logger.debug("Creating DE: " + de.getLongName());
	    de.setId(dataElementDAO.create(de));
	    logger.info(PropertyAccessor.getProperty("created.de"));
	  } else {
	    de = (DataElement) l.get(0);
	    logger.info(PropertyAccessor.getProperty("existed.de"));
	  }

	  LogUtil.logAc(de, logger);
	  logger.info(PropertyAccessor.
                      getProperty("vd.preferredName",
                                  de.getValueDomain().getPreferredName()));

	  addProjectCs(de);
	  it.set(de);
	} catch (PersisterException e) {
	  logger.error("Could not persist DE: " + de.getLongName());
	  logger.error(e.getMessage());
	} // end of try-catch
      }
    }

  }


}
