/**
 * Copyright (C) 2017 Leidos Biomedical Research, Inc. - All rights reserved.
 */
package gov.nih.nci.ncicb.cadsr.domain.bean;

import gov.nih.nci.ncicb.cadsr.domain.Audit;
import gov.nih.nci.ncicb.cadsr.domain.ClassSchemeClassSchemeItem;
import gov.nih.nci.ncicb.cadsr.domain.Context;
import gov.nih.nci.ncicb.cadsr.domain.Definition;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DefinitionBean implements Definition, Serializable {
	private String id;
	private Context context;
	private String definition;
	private Audit audit;
	private String acId;
	private List<AttributeClassSchemeClassSchemeItemBean> attCsCsis;
	private String type;
	private String language;

	public DefinitionBean() {
		this.language = "ENGLISH";
	}

	public String getId() {
		return this.id;
	}

	public Context getContext() {
		return this.context;
	}

	public String getDefinition() {
		return this.definition;
	}

	public Audit getAudit() {
		return this.audit;
	}

	private String getAcId() {
		return this.acId;
	}

	private List<AttributeClassSchemeClassSchemeItemBean> getAttCsCsis() {
		return this.attCsCsis;
	}

	public List<ClassSchemeClassSchemeItem> getCsCsis() {
		HashSet result = new HashSet();
		for (AttributeClassSchemeClassSchemeItemBean attCsCsi : this.attCsCsis) {
			result.add(attCsCsi.getCsCsi());
		}
		return new ArrayList(result);
	}

	public void addCsCsi(ClassSchemeClassSchemeItem newCsCsi) {
		AttributeClassSchemeClassSchemeItemBean attCsCsi = new AttributeClassSchemeClassSchemeItemBean();
		attCsCsi.setCsCsi(newCsCsi);
		attCsCsi.setType("DEFINITION");
		attCsCsi.setAttId(getId());

		if (this.attCsCsis == null) {
			this.attCsCsis = new ArrayList();
		}
		this.attCsCsis.add(attCsCsi);
	}

	public void setAttCsCsis(List newCsCsis) {
		this.attCsCsis = newCsCsis;
	}

	public String getType() {
		return this.type;
	}

	public String getLanguage() {
		return this.language;
	}

	public void setLanguage(String newLanguage) {
		if (newLanguage != null)
			this.language = newLanguage;
	}

	public void setType(String newType) {
		this.type = newType;
	}

	void setAcId(String newAcId) {
		this.acId = newAcId;
	}

	public void setAudit(Audit newAudit) {
		this.audit = newAudit;
	}

	public void setDefinition(String newDefinition) {
		this.definition = newDefinition;
	}

	public void setContext(Context newContext) {
		this.context = newContext;
	}

	public void setId(String newId) {
		this.id = newId;
	}
}