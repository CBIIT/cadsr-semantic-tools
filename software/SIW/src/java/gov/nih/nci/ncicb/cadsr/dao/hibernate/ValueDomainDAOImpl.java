/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package gov.nih.nci.ncicb.cadsr.dao.hibernate;

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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.SimpleExpression;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
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
	//SIW-617
	protected ValueMeaning searchValueMeaningByPublicId(ValueMeaning vm, Session session) {
		ValueMeaning result = null;
        Query query = session.createQuery("from ValueMeaningBean where publicId = :publicId and version = :version");
        query.setString("publicId", vm.getPublicId());
        query.setFloat("version", vm.getVersion());
        result = (ValueMeaning)query.uniqueResult();//could be null
		return result;
	}
	
	//SIW-627 re-factored from create
	protected void saveOrFindPermissibleValue(final ValueDomain vd, ValueMeaning vm, PermissibleValue pv, Session session) {
        Criteria criteria = session.createCriteria(pv.getClass());
        criteria.add(Expression.eq("value", pv.getValue())).createCriteria("valueMeaning").add(Expression.eq("longName", pv.getValueMeaning().getLongName()));

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
	protected void saveVdPvs(final ValueDomain vd, PermissibleValue pv, Session session) {
        ValueDomainPermissibleValue vdPv = new ValueDomainPermissibleValueBean();

        vdPv.setValueDomain(vd);
        vdPv.setPermissibleValue(pv);
        vdPv.setAudit(vd.getAudit());

        session.save(vdPv);
        session.flush();
	}
	
	//SIW-627
	protected ValueMeaning saveValueMeaningWithoutConcept(final ValueDomain vd, ValueMeaning vm, Session session) throws Exception {

        Query query = session.createQuery("from ValueMeaningBean where upper(trim(longName)) = upper(:longName)");
        query.setString("longName", vm.getLongName().trim());
        //FIXME SIW-793 we need to change this code not to throw an error but select VM accordingly
        ValueMeaning byNameVm = (ValueMeaning)query.uniqueResult();

        if (byNameVm == null) {
          vm.setConceptDerivationRule(null);

          session.save(vm);
          session.flush();
        }
        else if ((vm.getPreferredDefinition().equals("No Definition Loaded")) || (byNameVm.getPreferredDefinition().equalsIgnoreCase(vm.getPreferredDefinition())))
        {
          vm = byNameVm;
        }
        else {
          throw new Exception("VM " + byNameVm.getLongName() + " already exists with a different definition, please consider fixing the data before attemtping to reload - 1");
        }
        return vm;
      
	}
	//SIW-627
	/**
	 * 
	 * @param vd not null
	 * @param vm not null
	 * @param pv not null
	 * @param conStr not null
	 * @param session
	 * @throws Exception
	 */
	protected ValueMeaning saveValueMeaningByConceptCodes(final ValueDomain vd, ValueMeaning vm, String conStr, Session session) throws Exception {
		ValueMeaning vmResult;
        Criteria criteria = session.createCriteria(vm.getClass());
        criteria.createCriteria("conceptDerivationRule").add(Expression.eq("codesConcatenation", conStr));
        //FIXME SIW-793
        ValueDomainDAOImpl.this.logger.debug("Search VM by CDR: " + conStr);
        
        ValueMeaning byConceptVm = (ValueMeaning)criteria.uniqueResult();

        if (byConceptVm == null) {
          criteria = session.createCriteria(vm.getClass());
          criteria.add(Expression.eq("longName", vm.getLongName()).ignoreCase());
          //FIXME SIW-793
          ValueMeaning byNameVm = (ValueMeaning)criteria.uniqueResult();
          ConceptDerivationRule conDR;
          if (byNameVm == null) {//this is a new VM
            conDR = vm.getConceptDerivationRule();
            conDR.setAudit(vm.getAudit());
            ValueDomainDAOImpl.this.saveCondr(session, conDR);
            session.flush();
            session.refresh(conDR);
            //Creating a new VM
            session.save(vm);
            session.flush();
            vmResult = vm;
          }
          else { //We found a VM candidate
            if ((byNameVm.getConceptDerivationRule() != null) && (byNameVm.getConceptDerivationRule().getComponentConcepts().size() > 0)) {
              throw new Exception("VM " + byNameVm.getLongName() + " already exists but it's name does not match its concept. Skipping this VM. Please consider manually fixing the data before attemptin to reload.");
            }

            if ((byNameVm.getPreferredDefinition().equalsIgnoreCase(vm.getPreferredDefinition())) || 
            		(byNameVm.getPreferredDefinition().equalsIgnoreCase("No Definition Loaded")) || 
            		(byNameVm.getPreferredDefinition().trim().equalsIgnoreCase(byNameVm.getLongName().trim()))) {
              conDR = vm.getConceptDerivationRule();
              conDR.setAudit(vm.getAudit());
              ValueDomainDAOImpl.this.saveCondr(session, conDR);
              session.flush();
              session.refresh(conDR);

              if ((byNameVm.getPreferredDefinition().trim().equalsIgnoreCase("No Definition Loaded")) || 
            		  (byNameVm.getPreferredDefinition().trim().equalsIgnoreCase(byNameVm.getLongName().trim()))) {
                byNameVm.setPreferredDefinition(vm.getPreferredDefinition());
              }

              byNameVm.setConceptDerivationRule(conDR);
              session.saveOrUpdate(byNameVm);
              vmResult = byNameVm;
            }
            else {
              if (byNameVm.getLongName().equalsIgnoreCase(byNameVm.getPreferredDefinition())) {
                throw new Exception("VM " + byNameVm.getLongName() + " already exists with a different definition, please consider fixing the data before attemtping to reload - 2");
              }
              throw new Exception("VM " + byNameVm.getLongName() + " already exists with a different definition, please consider fixing the data before attemtping to reload - 3");
            }
          }
        }
        else {
        	vmResult = byConceptVm;
        }
        return vmResult;
      
	}
	
	//SIW-627
	protected void buildPermissibleValue(final ValueDomain vd, ValueMeaning vm, PermissibleValue pv) {         
        pv.setValueMeaning(vm);
        pv.setAudit(vd.getAudit());

        pv.setLifecycle(vd.getLifecycle());
	}
	
	//SIW-617
	protected void processPermissibleValues(final ValueDomain vd, List<PermissibleValue> pvs, Session session) {
        for (PermissibleValue pv : pvs) {
        	
            ValueMeaning vm = pv.getValueMeaning();
            pv.setValue(vm.getLongName());//TODO do we need this?
            ValueDomainDAOImpl.this.logger.debug("...current PermissibleValue: " + pv.getValue());
            
            boolean receivedVmId = false;
            try
            {
              if (StringUtils.isNotBlank(vm.getPublicId())) {
                    ValueDomainDAOImpl.this.logger.debug(".......searching VM by Public ID: " + vm.getPublicId());
                    ValueMeaning vmReceived = searchValueMeaningByPublicId(vm, session);
                    receivedVmId = (vmReceived != null);
                    if (receivedVmId) {
                    	vm = vmReceived;
                    }
                    else {
                        ValueDomainDAOImpl.this.logger.error("!!! Could not find VM by ID: " + vm.getPublicId() + ". This PV/VM pair is skipped");
                    	continue;
                    }
              }
              
              if ((vm.getLongName() == null) || (vm.getLongName().trim().length() == 0)) {
                throw new Throwable("VM Name is empty, cannot be saved");
              }
              
              if (! receivedVmId) {
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
	            	  vm = saveValueMeaningByConceptCodes(vd, vm, conSb.toString(), session);
	              }
              }//end of not receivedVmId

              buildPermissibleValue(vd, vm, pv);
              
              //SIW-627 code re-factoring
              saveOrFindPermissibleValue(vd, vm, pv, session);
            } 
            catch (NonUniqueResultException ex) {
              ValueDomainDAOImpl.this.logger.error("Could not load VM : " + vm.getLongName() + " because there are already more than one VM by this name");
              ValueDomainDAOImpl.this.logger.error("Please consider fixing the data before attempting to reload.");
            } catch (Throwable t) {
              ValueDomainDAOImpl.this.logger.error("Could not load VM : " + vm.getLongName() + " -- Please consider loading manually.");
              ValueDomainDAOImpl.this.logger.error("loader reported following error: ");
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
