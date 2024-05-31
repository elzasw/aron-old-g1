package cz.aron.transfagent.config;

public class ConfigPeva2FindingAidProperties {
	
	// nazev pomucky slozi z [nazevarchivu],[nazevfondu],[datace]
    private boolean composedFindingAidName;
    
    // nazev pomucky slozi z [kratky_nazevarchivu],[nazevfondu],[datace]
    private boolean composedShortFindingAidName;
    
    // neodesila pomucku do arona, pouze ji nacte
    private boolean dontSend;
    
    // odkaz na archivni soubor a instituci vygeneruje do samostatneho partu
    private boolean referencesPart;
    
    // vyplni autory jako odkaz na entitu, jinak vyplnuje textovou hodnotu
    private boolean auhorRef;
    
    // pokud nenajde fond, tak se ho pokusi importovat jako command
    private boolean importMissingFund;

	public boolean isComposedFindingAidName() {
		return composedFindingAidName;
	}

	public void setComposedFindingAidName(boolean composedFindingAidName) {
		this.composedFindingAidName = composedFindingAidName;
	}

	public boolean isDontSend() {
		return dontSend;
	}

	public void setDontSend(boolean dontSend) {
		this.dontSend = dontSend;
	}

	public boolean isReferencesPart() {
		return referencesPart;
	}

	public void setReferencesPart(boolean referencesPart) {
		this.referencesPart = referencesPart;
	}

	public boolean isAuhorRef() {
		return auhorRef;
	}

	public void setAuhorRef(boolean auhorRef) {
		this.auhorRef = auhorRef;
	}

	public boolean isImportMissingFund() {
		return importMissingFund;
	}

	public void setImportMissingFund(boolean importMissingFund) {
		this.importMissingFund = importMissingFund;
	}

	public boolean isComposedShortFindingAidName() {
		return composedShortFindingAidName;
	}

	public void setComposedShortFindingAidName(boolean composedShortFindingAidName) {
		this.composedShortFindingAidName = composedShortFindingAidName;
	}

}
