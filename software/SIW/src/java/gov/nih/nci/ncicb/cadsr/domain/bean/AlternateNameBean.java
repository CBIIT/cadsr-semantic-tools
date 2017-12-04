/**
 * Copyright (C) 2017 Leidos Biomedical Research, Inc. - All rights reserved.
 */
/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package gov.nih.nci.ncicb.cadsr.domain.bean;

import gov.nih.nci.ncicb.cadsr.domain.AlternateName;
import gov.nih.nci.ncicb.cadsr.domain.Audit;
import gov.nih.nci.ncicb.cadsr.domain.ClassSchemeClassSchemeItem;
import gov.nih.nci.ncicb.cadsr.domain.Context;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlternateNameBean implements AlternateName, Serializable {
	private String id;
	private Context context;
	private String name;
	private String type;
	private List<AttributeClassSchemeClassSchemeItemBean> attCsCsis;
	private String acId;
	private Audit audit;
	private String language;

	public AlternateNameBean() {
		this.language = "ENGLISH";
	}

	public String getId() {
		return this.id;
	}

	public Context getContext() {
		return this.context;
	}

	public String getName() {
		return this.name;
	}

	public String getType() {
		return this.type;
	}

	private List<AttributeClassSchemeClassSchemeItemBean> getAttCsCsis() {
		return this.attCsCsis;
	}

	public List<ClassSchemeClassSchemeItem> getCsCsis() {
		Set result = new HashSet();
		for (AttributeClassSchemeClassSchemeItemBean attCsCsi : getAttCsCsis()) {
			result.add(attCsCsi.getCsCsi());
		}
		return new ArrayList(result);
	}

	public void addCsCsi(ClassSchemeClassSchemeItem newCsCsi) {
		AttributeClassSchemeClassSchemeItemBean attCsCsi = new AttributeClassSchemeClassSchemeItemBean();
		attCsCsi.setCsCsi(newCsCsi);
		attCsCsi.setType("DESIGNATION");
		attCsCsi.setAttId(getId());

		if (this.attCsCsis == null) {
			this.attCsCsis = new ArrayList();
		}
		this.attCsCsis.add(attCsCsi);
	}

	private String getAcId() {
		return this.acId;
	}

	public Audit getAudit() {
		return this.audit;
	}

	public String getLanguage() {
		return this.language;
	}

	public void setLanguage(String newLanguage) {
		if (newLanguage != null)
			this.language = newLanguage;
	}

	public void setAudit(Audit newAudit) {
		this.audit = newAudit;
	}

	public void setAcId(String newAcId) {
		this.acId = newAcId;
	}

	public void setAttCsCsis(List newCsCsis) {
		this.attCsCsis = newCsCsis;
	}

	public void setType(String newType) {
		this.type = newType;
	}

	public void setName(String newName) {
		this.name = newName;
	}

	public void setContext(Context newContext) {
		this.context = newContext;
	}

	public void setId(String newId) {
		this.id = newId;
	}
}