/**
 * 
 */
package gov.nih.nci.ncicb.cadsr.domain.bean;
import gov.nih.nci.ncicb.cadsr.domain.ConceptDerivationRule;
import gov.nih.nci.ncicb.cadsr.domain.ValueMeaning;
import java.io.Serializable;

public class ValueMeaningBean extends AdminComponentBean implements ValueMeaning, Serializable {
	private String comments;
	private ConceptDerivationRule conceptDerivationRule;
	private String id;
	protected String publicId;

	public String getId() {
		return this.id;
	}

	public void setId(String newId) {
		this.id = newId;
	}

	public String getComments() {
		return this.comments;
	}

	public ConceptDerivationRule getConceptDerivationRule() {
		return this.conceptDerivationRule;
	}

	public void setConceptDerivationRule(ConceptDerivationRule newConceptDerivationRule) {
		this.conceptDerivationRule = newConceptDerivationRule;
	}

	public void setComments(String newComments) {
		this.comments = newComments;
	}

	public String getPublicId() {
		return publicId;
	}

	public void setPublicId(String publicId) {
		this.publicId = publicId;
	}

	public boolean equals(Object o) {
		if (!(o instanceof ValueMeaning))
			return false;

		ValueMeaning c = (ValueMeaning) o;
		return c.getId().equals(this.id);
	}

	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public String toString() {
		return "ValueMeaningBean [comments=" + getComments() + ", idseq=" + getId() + ", publicId=" + getPublicId()
				+ ", getPreferredName()=" + getPreferredName() + ", getLongName()=" + getLongName() + ", getVersion()="
				+ getVersion() + ", getPreferredDefinition()=" + getPreferredDefinition() + 
				", getWorkflowStatus()=" + getWorkflowStatus()
				+ ", getChangeNote()=" + getChangeNote() + ", getOrigin()=" + getOrigin() + "]";
	}
	
}
