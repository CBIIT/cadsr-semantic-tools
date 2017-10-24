/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package gov.nih.nci.ncicb.cadsr.dao.hibernate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Projections;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import gov.nih.nci.ncicb.cadsr.dao.DAOCreateException;
import gov.nih.nci.ncicb.cadsr.dao.ValueDomainDAO;
import gov.nih.nci.ncicb.cadsr.domain.ComponentConcept;
import gov.nih.nci.ncicb.cadsr.domain.Concept;
import gov.nih.nci.ncicb.cadsr.domain.ConceptDerivationRule;
import gov.nih.nci.ncicb.cadsr.domain.ConceptualDomain;
import gov.nih.nci.ncicb.cadsr.domain.PermissibleValue;
import gov.nih.nci.ncicb.cadsr.domain.Representation;
import gov.nih.nci.ncicb.cadsr.domain.ValueDomain;
import gov.nih.nci.ncicb.cadsr.domain.ValueDomainPermissibleValue;
import gov.nih.nci.ncicb.cadsr.domain.ValueMeaning;
import gov.nih.nci.ncicb.cadsr.domain.bean.ConceptDerivationRuleBean;
import gov.nih.nci.ncicb.cadsr.domain.bean.ValueDomainPermissibleValueBean;
import gov.nih.nci.ncicb.cadsr.domain.bean.ValueMeaningBean;
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public class ValueDomainDAOImpl extends HibernateDaoSupport implements ValueDomainDAO {
	public List findByNameLike(String name) {
		return getHibernateTemplate().findByNamedQuery("vd.findByNameLike", name);
	}

	public ValueDomain findByName(String name) {
		return ((ValueDomain) getHibernateTemplate().findByNamedQuery("vd.findByName", name).get(0));
	}

	public List<ValueDomain> find(final ValueDomain vd) {
		HibernateCallback callback = new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Criteria criteria = session.createCriteria(vd.getClass());

				AdminComponentQueryBuilder.buildCriteria(criteria, vd);

				return criteria.list();
			}
		};
		return ((List) getHibernateTemplate().execute(callback));
	}

	public List<String> getAllDatatypes() {
		HibernateCallback callback = new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Criteria criteria = session.createCriteria(ValueDomain.class);
				criteria.setProjection(Projections
						.distinct(Projections.projectionList().add(Projections.property("dataType"), "dataType")));

				return criteria.list();
			}
		};
		return ((List) getHibernateTemplate().execute(callback));
	}

	public List<ValueDomain> find(final ValueDomain vd, final List<String> eager) {
		HibernateCallback callback = new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Criteria criteria = session.createCriteria(vd.getClass());

				AdminComponentQueryBuilder.buildCriteria(criteria, vd);

				if (eager != null) {
					for (String s : eager) {
						criteria.setFetchMode(s, FetchMode.JOIN);
					}
				}
				List result = criteria.list();

				if (eager != null) {
					EagerLoadUtil.callSize(result, eager);
				}
				return result;
			}
		};
		return ((List) getHibernateTemplate().execute(callback));
	}

	public List<PermissibleValue> getPermissibleValues(final String pk) {
		HibernateCallback callback = new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Query query = session.getNamedQuery("vd.findByPK");
				query.setString(0, pk);

				ValueDomain vd = (ValueDomain) query.list().get(0);
				List vd_pvs = vd.getValueDomainPermissibleValues();

				List result = new ArrayList();
				for (int i = 0; i < vd_pvs.size(); ++i) {
					result.add(((ValueDomainPermissibleValue) vd_pvs.get(i)).getPermissibleValue());
				}

				return result;
			}
		};
		return ((List) getHibernateTemplate().execute(callback));
	}
	///////////////////////
	/**
	 * 
	 * @param cdr shall not be null concatenated list of concept codes
	 * @param session
	 * @return ValueMeaning or null
	 */
	protected ValueMeaning seachValueMeaningByCdr(final String cdr, final Session session) {
		//SIW-793
		final String sqlVmByLongName = "SELECT {vm.*} "
			+ "FROM SBR.VALUE_MEANINGS vm "
			+ "inner join SBR.AC_STATUS_LOV ws on vm.ASL_NAME = ws.ASL_NAME "
			+ "inner join SBREXT.CON_DERIVATION_RULES_EXT cdr on vm.CONDR_IDSEQ = cdr.CONDR_IDSEQ "
			+ "where cdr.NAME = :condr "
			+ "order by ws.DISPLAY_ORDER, vm.VM_ID DESC";
		logger.debug("Searching VM by CDR: " + cdr);
		
		ValueMeaning valueMeaning = null;
		Query query = session.createSQLQuery(sqlVmByLongName).addEntity("vm", ValueMeaningBean.class);
		List vmList = query.setString("condr", cdr).list();
		if((vmList != null) && (! vmList.isEmpty())) {
			valueMeaning = searchValueMeaningByPublicId((ValueMeaning)vmList.get(0), session);
		}
		return valueMeaning;
	}
	/**
	 * This method search by Long name case insensitive.
	 * 
	 * @param longName shall not be null
	 * @param session
	 * @return ValueMeaning or null
	 */
	protected ValueMeaning seachValueMeaningByLongName(final String longName, final Session session) {
		//SIW-793
		final String sqlVmByLongName = "SELECT {vm.*} "
			+ "FROM SBR.VALUE_MEANINGS vm "
			+ "inner join SBR.AC_STATUS_LOV ws on vm.ASL_NAME = ws.ASL_NAME "
			+ "where upper(vm.LONG_NAME) = :vmLongName "
			+ "order by ws.DISPLAY_ORDER, vm.VM_ID DESC";
		
		ValueMeaning valueMeaning = null;
		Query query = session.createSQLQuery(sqlVmByLongName).addEntity("vm", ValueMeaningBean.class);
		List vmList = query.setString("vmLongName", longName.toUpperCase()).list();
		if((vmList != null) && (! vmList.isEmpty())) {
			logger.info("Found caDSR VM by XMI-provided LongName: " + buildValueMeaningString((ValueMeaning)vmList.get(0)));
			valueMeaning = searchValueMeaningByPublicId((ValueMeaning)vmList.get(0), session);
		}
		return valueMeaning;
	}
	//SIW-617
	/**
	 * 
	 * @param vm not null
	 * @param session
	 * @return VM by ID or null
	 */
	protected ValueMeaning searchValueMeaningByPublicId(final ValueMeaning vm, final Session session) {
		ValueMeaning result = null;
        Query query = session.createQuery("from ValueMeaningBean where publicId = :publicId and version = :version");
        query.setString("publicId", vm.getPublicId());
        query.setFloat("version", vm.getVersion());
        result = (ValueMeaning)query.uniqueResult();//could be null
        logger.info("Found caDSR VM by PublicID: " + buildValueMeaningString(result));
		return result;
	}
	
	//SIW-627 re-factored from create
	protected void saveOrFindPermissibleValue(final ValueDomain vd, ValueMeaning vm, PermissibleValue pv, Session session) {
        Criteria criteria = session.createCriteria(pv.getClass());
        if (StringUtils.isNotBlank(vm.getPublicId())) {
        	Float versionNumber = (vm.getVersion() != null) ? vm.getVersion() : 1f;
        	criteria.add(Expression.eq("value", pv.getValue())).createCriteria("valueMeaning")
        		.add(Expression.eq("publicId", vm.getPublicId())).add(Expression.eq("version", versionNumber));
        }
        else {
        	criteria.add(Expression.eq("value", pv.getValue())).createCriteria("valueMeaning").add(Expression.eq("longName", vm.getLongName()));
        }
        List l = criteria.list();
        if (l.size() == 0) {
          session.save(pv);
          session.flush();
        } else {
          pv = (PermissibleValue)l.get(0);
        }

        saveVdPvs(vd, pv, session);
	}
	
	//SIW-627
	protected void saveVdPvs(final ValueDomain vd, final PermissibleValue pv, final Session session) {
        ValueDomainPermissibleValue vdPv = new ValueDomainPermissibleValueBean();

        vdPv.setValueDomain(vd);
        vdPv.setPermissibleValue(pv);
        vdPv.setAudit(vd.getAudit());

        session.save(vdPv);
        session.flush();
	}
	/**
	 * We call this method for XMI VM without CDR.
	 * 
	 * @param vd
	 * @param vm
	 * @param session
	 * @return ValueMeaning
	 * @throws Exception
	 */
	//SIW-627
	protected ValueMeaning saveValueMeaningWithoutConcept(final ValueDomain vd, ValueMeaning vm, final Session session) throws Exception {
        //SIW-793 this call does not to throw an error. It selects VM WS Status ordered staring with RELEASED, the most recent with the highest Public ID 
        ValueMeaning byNameVm = seachValueMeaningByLongName(vm.getLongName().trim(), session);
        logger.info("Received XMI ValueMeaning to search by LongName with no CDR: " + buildValueMeaningString(vm));
        if (byNameVm == null) {
          vm.setConceptDerivationRule(null);
          session.save(vm);
          session.flush();
          logger.info("ValueMeaning is not found by LongName; a new VM with no CDR is created in caDSR DB: " + buildValueMeaningString(vm));
        }
        else if ((vm.getPreferredDefinition().equals("No Definition Loaded")) || (byNameVm.getPreferredDefinition().equalsIgnoreCase(vm.getPreferredDefinition())))
        {
          vm = byNameVm;
          logger.info("ValueMeaning is found by LongName in caDSR DB to use for PV: " + buildValueMeaningString(vm));
        }
        else {
          throw new Exception("VM " + buildValueMeaningString(byNameVm) + " already exists with a different definition, please consider fixing the data before attemtping to reload - 1");
        }
        return vm;
      
	}
	public static String buildValueMeaningString(final ValueMeaning vm) {
		String tmp;
		if (vm == null) return null;
		return "ValueMeaning ["+ "longName=" + vm.getLongName()
		+ ", preferredDefinition=" + vm.getPreferredDefinition()
		+ (StringUtils.isBlank(tmp = vm.getPreferredName()) ? "" : ", preferredName=" + tmp)
		+ (StringUtils.isBlank(tmp = vm.getPublicId()) ? "" : ", publicId=" + tmp)
		+ (vm.getVersion() == null ? "" : ", version=" + vm.getVersion())
		+ (StringUtils.isBlank(tmp = vm.getWorkflowStatus()) ? "" : ", workflowStatus=" + tmp)
		+ (StringUtils.isBlank(tmp = vm.getOrigin()) ? "" : ", origin=" + tmp)
		+ (StringUtils.isBlank(tmp = vm.getId()) ? "" : ", idseq=" + tmp) 
		+ (StringUtils.isBlank(tmp = vm.getComments()) ? "" : "comments=" + tmp)
		+ (StringUtils.isBlank(tmp = vm.getChangeNote()) ? "" : ", changeNote=" + tmp)
		+ "]";
	}
	//SIW-627
	/**
	 * 
	 * @param vd not null
	 * @param vm not null from XMI
	 * @param pv not null
	 * @param conStr not null
	 * @param session
	 * @throws Exception
	 */
	protected ValueMeaning saveValueMeaningWithConceptCodes(ValueMeaning vm, String conStr, final Session session) throws Exception {
		ValueMeaning vmResult;
        //SIW-793
        ValueMeaning byConceptVm = seachValueMeaningByCdr(conStr, session);
        logger.debug("ValueMeaning from XMI: " + buildValueMeaningString(vm));
        
        if (byConceptVm == null) {
          //SIW-793
          logger.info("ValueMeaning is not found by CDR: " + conStr);
          ValueMeaning byNameVm = seachValueMeaningByLongName(vm.getLongName(), session);
          ConceptDerivationRule conDR;
          if (byNameVm == null) {//this is a new VM
        	logger.info("ValueMeaning not found by CDR: " + conStr + ", and not found by LongName");
            conDR = vm.getConceptDerivationRule();
            conDR.setAudit(vm.getAudit());
            ValueDomainDAOImpl.this.saveCondr(session, conDR);
            session.flush();
            session.refresh(conDR);
            logger.info("Created CDR in caDSR DB: " + conDR);
            //Creating a new VM
            session.save(vm);
            session.flush();
            vmResult = vm;
            logger.info("Created CDR in caDSR DB: " + buildValueMeaningString(vmResult));
          }
          else { //We found a VM candidate
        	logger.info("ValueMeaning not found by CDR: " + conStr + ", but found by LongName: " + vm.getLongName() + byNameVm);
        	
            if ((byNameVm.getConceptDerivationRule() != null) && (byNameVm.getConceptDerivationRule().getComponentConcepts().size() > 0)) {
              throw new Exception("Received XMI VM " + buildValueMeaningString(vm) + " already exists but it's name does not match its concept. Skipping this VM. "
              		+ "Please consider manually fixing the data before attemptin to reload. VM from DB: " + buildValueMeaningString(byNameVm));
            }
            if ((byNameVm.getPreferredDefinition().equalsIgnoreCase(vm.getPreferredDefinition())) || 
            		(byNameVm.getPreferredDefinition().equalsIgnoreCase("No Definition Loaded")) || 
            		(byNameVm.getPreferredDefinition().trim().equalsIgnoreCase(byNameVm.getLongName().trim()))) {//TODO why do we compare caDSR VM LongName with VM PreferredDefinition
              logger.info("caDSR VM found to update CDR: " + buildValueMeaningString(byNameVm));
              conDR = vm.getConceptDerivationRule();
              conDR.setAudit(vm.getAudit());
              ValueDomainDAOImpl.this.saveCondr(session, conDR);
              logger.info("Created CDR in caDSR DB: " + conDR);
              session.flush();
              session.refresh(conDR);

              if ((byNameVm.getPreferredDefinition().trim().equalsIgnoreCase("No Definition Loaded")) || 
            		  (byNameVm.getPreferredDefinition().trim().equalsIgnoreCase(byNameVm.getLongName().trim()))) {
                byNameVm.setPreferredDefinition(vm.getPreferredDefinition());
              }

              byNameVm.setConceptDerivationRule(conDR);
              session.saveOrUpdate(byNameVm);
              vmResult = byNameVm;
              logger.info("caDSR DB VM updated with CDR: " + buildValueMeaningString(byNameVm));
            }
            else {//TODO why do we compare caDSR VM LongName with VM PreferredDefinition
              logger.info("Error with caDSR DB VM: " + buildValueMeaningString(byNameVm));
              if (byNameVm.getLongName().equalsIgnoreCase(byNameVm.getPreferredDefinition())) {
                throw new Exception("VM Long name: " + byNameVm.getLongName() + ", VM PreferredDefinition: " + byNameVm.getPreferredDefinition()
                + " already exists with a different definition, please consider fixing the data before attemtping to reload - 2");
              }
              throw new Exception("VM " + byNameVm.getLongName() + ", VM PreferredDefinition: " + byNameVm.getPreferredDefinition() +
            		  " already exists with a different definition, please consider fixing the data before attemtping to reload - 3");
            }
          }
        }
        else {//we found caDSR VM based on CDR
        	vmResult = byConceptVm;
        	logger.info("Found DB VM by CDR: " + buildValueMeaningString(byConceptVm));
        }
    	logger.debug("Returning DB VM found by CDR: " + buildValueMeaningString(vmResult));

        return vmResult;   
	}
	
	//SIW-627
	protected void buildPermissibleValue(final ValueDomain vd, ValueMeaning vm, PermissibleValue pv) {         
        pv.setValueMeaning(vm);
        pv.setAudit(vd.getAudit());

        pv.setLifecycle(vd.getLifecycle());
	}
	
	//SIW-617
	protected void processPermissibleValues(final ValueDomain vd, List<PermissibleValue> pvs, final Session session) {
		ValueDomainDAOImpl.this.logger.debug("Processing PVs of VD with LongName: " + vd.getLongName());
        for (PermissibleValue pv : pvs) {      	
            ValueMeaning vm = pv.getValueMeaning();
            ValueDomainDAOImpl.this.logger.debug("...Processing PV VM from XMI: " + buildValueMeaningString(vm));
            pv.setValue(vm.getLongName());
            
            boolean receivedVmById = false;
            try
            {
              if (StringUtils.isNotBlank(vm.getPublicId())) {
                    ValueDomainDAOImpl.this.logger.debug(".......searching VM by Public ID: " + vm.getPublicId());
                    ValueMeaning vmReceived = searchValueMeaningByPublicId(vm, session);
                    receivedVmById = (vmReceived != null);
                    if (receivedVmById) {
                    	vm = vmReceived;
                    	logger.info("Found caDSR VM by PublicID: " + buildValueMeaningString(vm));
                    }
                    else {
                        ValueDomainDAOImpl.this.logger.error("!!! Could not find VM by ID: " + vm.getPublicId() + ". This PV/VM pair is skipped");
                    	continue;//SIW-627 description: If the ERROR isn't fixed, and the VM Public ID is still invalid, the system should ignore the PV/VM.
                    }
              }
              
              if ((vm.getLongName() == null) || (vm.getLongName().trim().length() == 0)) {
                throw new Throwable("VM Name is empty, cannot be saved");
              }
              
              if (! receivedVmById) {
            	  if (StringUtils.isBlank(vm.getPreferredDefinition())) {
                      vm.setPreferredDefinition("No Definition Loaded");
            	  }
	              StringBuilder longNameSb = new StringBuilder();
	              StringBuilder conSb = new StringBuilder();
	
	              for (ComponentConcept compCon : vm.getConceptDerivationRule().getComponentConcepts()) {
	                if (longNameSb.length() > 0)
	                  longNameSb.append(" ");
	                if (conSb.length() > 0) {
	                  conSb.append(":");
	                }
	                String s = compCon.getConcept().getLongName();
	                longNameSb.append(s.substring(0, 1).toUpperCase() + s.substring(1));
	
	                s = compCon.getConcept().getPreferredName();
	                conSb.append(s);
	              }
	              
	              boolean vmHasConcept = vm.getConceptDerivationRule().getComponentConcepts().size() > 0;
	
	              if (vmHasConcept) {
	                vm.setLongName(longNameSb.toString());
	              }
	
	              vm.setAudit(vd.getAudit());
	              vm.setContext(vd.getContext());
	              vm.setLifecycle(vd.getLifecycle());
	              if (!(vmHasConcept))
	              {
	            	  //SIW-627 code re-factoring
	            	  vm = saveValueMeaningWithoutConcept(vd, vm, session);
	              }
	              else {
	            	  //SIW-627 code re-factoring
	            	  vm = saveValueMeaningWithConceptCodes(vm, conSb.toString(), session);
	              }
              }//end of not receivedVmId

              buildPermissibleValue(vd, vm, pv);
              
              //SIW-627 code re-factoring
              saveOrFindPermissibleValue(vd, vm, pv, session);
            } 
            catch (NonUniqueResultException ex) {
              ValueDomainDAOImpl.this.logger.error("Could not load VM : " + vm.getLongName() + " because there are already more than one VM by this name");
              ValueDomainDAOImpl.this.logger.error("Please consider fixing the data before attempting to reload.");
              ValueDomainDAOImpl.this.logger.error(ex);
            } catch (Throwable t) {
              ValueDomainDAOImpl.this.logger.error("Could not load VM : " + vm.getLongName() + " -- Please consider loading manually.");
              ValueDomainDAOImpl.this.logger.error(t);
            }
          }		
	}
	
	public ValueDomain create(final ValueDomain vd) throws DAOCreateException {
    HibernateCallback callback = new HibernateCallback()
    {
      public Object doInHibernate(Session session)
        throws HibernateException
      {
        ConceptDerivationRule conDR = vd.getConceptDerivationRule();
        if ((conDR != null) && (conDR.getComponentConcepts().size() > 0)) {
          conDR.setAudit(vd.getAudit());
          ValueDomainDAOImpl.this.saveCondr(session, conDR);
          vd.setConceptDerivationRule(conDR);
          ValueDomainDAOImpl.this.logger.debug("Set VD CDR");
        } else {
          vd.setConceptDerivationRule(null);
        }

        Criteria cdCriteria = session.createCriteria(vd.getConceptualDomain().getClass());
        AdminComponentQueryBuilder.buildCriteria(cdCriteria, vd.getConceptualDomain());
        vd.setConceptualDomain((ConceptualDomain)cdCriteria.uniqueResult());

        if (vd.getRepresentation() != null) {
          Criteria repCriteria = session.createCriteria(vd.getRepresentation().getClass());
          AdminComponentQueryBuilder.buildCriteria(repCriteria, vd.getRepresentation());
          vd.setRepresentation((Representation)repCriteria.uniqueResult());
          ValueDomainDAOImpl.this.logger.debug("Set VD Representation");
        }
        //get PVs before saving VD
        List<PermissibleValue> pvs = vd.getPermissibleValues();
        
        String id = (String)session.save(vd);
        ValueDomainDAOImpl.this.logger.debug("Saved VD: " + vd.getLongName() + ", " + id);
        session.flush();
        session.refresh(vd);
        
        //SIW-627
        ValueDomainDAOImpl.this.logger.debug("Calling processPermissibleValues of VD: " + vd.getLongName());
        processPermissibleValues(vd, pvs, session);

        return vd;
      }
    };
    try
    {
      return ((ValueDomain)getHibernateTemplate().execute(callback));
    } catch (Exception e) {
      e.printStackTrace();
      throw new DAOCreateException(e.getMessage());
    }
  }

	private void saveCondr(Session session, ConceptDerivationRule conDR) throws HibernateException {
		String code = "";
		for (ComponentConcept compCon : conDR.getComponentConcepts()) {
			if (code.length() > 0)
				code = code + ":";
			code = code + compCon.getConcept().getPreferredName();
		}

		((ConceptDerivationRuleBean) conDR).setCodesConcatenation(code);

		if (conDR.getComponentConcepts().size() == 1)
			conDR.setType("Simple Concept");
		else
			conDR.setType("CONCATENATION");
		session.save(conDR);

		for (ComponentConcept compCon : conDR.getComponentConcepts()) {
			List l = session.getNamedQuery("concept.findByPreferredName")
					.setParameter(0, compCon.getConcept().getPreferredName()).list();
			if (l.size() == 0) {
				throw new HibernateException(
						"Cannot find Concept by ConceptCode: " + compCon.getConcept().getPreferredName());
			}
			Concept con = (Concept) l.get(0);
			compCon.setConcept(con);

			session.save(compCon);
			session.flush();
		}
	}
}
