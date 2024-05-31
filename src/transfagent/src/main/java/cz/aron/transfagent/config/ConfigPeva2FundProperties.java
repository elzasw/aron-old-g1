package cz.aron.transfagent.config;

public class ConfigPeva2FundProperties {
	
	// do edice prida prvni radek z interni zmeny
    private boolean parseInternalChanges;
    
    // do popisu findu prida jazyky
    private boolean languages;
    
    // do popisu fondu prida evidencni jednotky
    private boolean evidenceUnits;
    
    // do popisu fondu prida mista puvodu
    private boolean placesOfOrigin;
    
    // do popisu fondu prida literaturu
    private boolean literature;
    
    // do popisu fondu prida k jakemu datu jsou udaje aktualni
    private boolean toDate;
    
    // do popisu fondu prida stav zachovani
    private boolean preservationStatus;
    
    // nazev fondu slozi z [nazevarchivu],[nazevfondu],[datace]
    private boolean composedFundName;
    
    // nazev fondu slozi z [kratky_nazevarchivu],[nazevfondu],[datace]
    private boolean composedShortFundName;
    
    // do popisu fondu v prehledu prida jako description popis puvodce
    private boolean originatorAsDescription;
    
    // do popisu fondu v prehledu prida jako description poznaku    
    private boolean noteAsDescription;
    
    // do popisu fondu prida poznamku
    private boolean note;
    
    // do popisu fondu prida popis puvodce "O puvodci"
    private boolean originatorNote;
    
    // odstrani prefix nazvu fondu z "O puvodci"
    private boolean originatorNoteRemovePrefix;
    
    // do popisu fondu v prehledu prida jako description prvni radek z interni zmeny
    private boolean parseInternalChangesAsDescription;
    
    // predela \r na \r\n (chyba v la pnp)
    private boolean correctLineSeparators;
    
    // vyplni metraz fondu
    private boolean length;
    
    // vyplni digitalni velikost fondu
    private boolean digitalLength;
    
    // vyplni evidencni status
    private boolean evidenceStatus;
    
    // vyplni pristupnost
    private boolean accessibility;
    
    // vyplni tematicke skupiny
    private boolean thematicEvidenceGroups;
    
    // agregace priloh z pomucek
    private boolean aggregateAttachments;
    
    // vyplni casti ulozene v jinem archivu
    private boolean archiveGroupParts;

    // posle reference na pomucky
    private boolean findingAids;
    
    // posle reference na puvodce
    private boolean originators;
    
    private boolean titlePart = true;
    
    // aktualizace UUID pro testovaci instanci
    private boolean updateUUID = false;
    
    private boolean archdescFlag = false;
    
    // posle odkazy na puvodce z CAM
    private boolean camOriginators = false;

	public boolean isParseInternalChanges() {
		return parseInternalChanges;
	}

	public void setParseInternalChanges(boolean parseInternalChanges) {
		this.parseInternalChanges = parseInternalChanges;
	}

	public boolean isLanguages() {
		return languages;
	}

	public void setLanguages(boolean languages) {
		this.languages = languages;
	}

	public boolean isEvidenceUnits() {
		return evidenceUnits;
	}

	public void setEvidenceUnits(boolean evidenceUnits) {
		this.evidenceUnits = evidenceUnits;
	}

	public boolean isPlacesOfOrigin() {
		return placesOfOrigin;
	}

	public void setPlacesOfOrigin(boolean placesOfOrigin) {
		this.placesOfOrigin = placesOfOrigin;
	}

	public boolean isLiterature() {
		return literature;
	}

	public void setLiterature(boolean literature) {
		this.literature = literature;
	}

	public boolean isToDate() {
		return toDate;
	}

	public void setToDate(boolean toDate) {
		this.toDate = toDate;
	}

	public boolean isPreservationStatus() {
		return preservationStatus;
	}

	public void setPreservationStatus(boolean preservationStatus) {
		this.preservationStatus = preservationStatus;
	}

	public boolean isComposedFundName() {
		return composedFundName;
	}

	public void setComposedFundName(boolean composedFundName) {
		this.composedFundName = composedFundName;
	}

	public boolean isOriginatorAsDescription() {
		return originatorAsDescription;
	}

	public void setOriginatorAsDescription(boolean originatorAsDescription) {
		this.originatorAsDescription = originatorAsDescription;
	}

	public boolean isNoteAsDescription() {
		return noteAsDescription;
	}

	public void setNoteAsDescription(boolean noteAsDescription) {
		this.noteAsDescription = noteAsDescription;
	}

	public boolean isParseInternalChangesAsDescription() {
		return parseInternalChangesAsDescription;
	}

	public void setParseInternalChangesAsDescription(boolean parseInternalChangesAsDescription) {
		this.parseInternalChangesAsDescription = parseInternalChangesAsDescription;
	}

	public boolean isCorrectLineSeparators() {
		return correctLineSeparators;
	}

	public void setCorrectLineSeparators(boolean correctLineSeparators) {
		this.correctLineSeparators = correctLineSeparators;
	}

	public boolean isLength() {
		return length;
	}

	public void setLength(boolean length) {
		this.length = length;
	}

	public boolean isEvidenceStatus() {
		return evidenceStatus;
	}

	public void setEvidenceStatus(boolean evidenceStatus) {
		this.evidenceStatus = evidenceStatus;
	}

	public boolean isAccessibility() {
		return accessibility;
	}

	public void setAccessibility(boolean accessibility) {
		this.accessibility = accessibility;
	}

	public boolean isDigitalLength() {
		return digitalLength;
	}

	public void setDigitalLength(boolean digitalLength) {
		this.digitalLength = digitalLength;
	}

	public boolean isThematicEvidenceGroups() {
		return thematicEvidenceGroups;
	}

	public void setThematicEvidenceGroups(boolean thematicEvidenceGroups) {
		this.thematicEvidenceGroups = thematicEvidenceGroups;
	}

	public boolean isAggregateAttachments() {
		return aggregateAttachments;
	}

	public void setAggregateAttachments(boolean aggregateAttachments) {
		this.aggregateAttachments = aggregateAttachments;
	}

	public boolean isArchiveGroupParts() {
		return archiveGroupParts;
	}

	public void setArchiveGroupParts(boolean archiveGroupParts) {
		this.archiveGroupParts = archiveGroupParts;
	}

	public boolean isFindingAids() {
		return findingAids;
	}

	public void setFindingAids(boolean findingAids) {
		this.findingAids = findingAids;
	}

	public boolean isNote() {
		return note;
	}

	public void setNote(boolean note) {
		this.note = note;
	}

	public boolean isOriginators() {
		return originators;
	}

	public void setOriginators(boolean originators) {
		this.originators = originators;
	}

	public boolean isOriginatorNote() {
		return originatorNote;
	}

	public void setOriginatorNote(boolean originatorNote) {
		this.originatorNote = originatorNote;
	}

	public boolean isOriginatorNoteRemovePrefix() {
		return originatorNoteRemovePrefix;
	}

	public void setOriginatorNoteRemovePrefix(boolean originatorNoteRemovePrefix) {
		this.originatorNoteRemovePrefix = originatorNoteRemovePrefix;
	}

	public boolean isTitlePart() {
		return titlePart;
	}

	public void setTitlePart(boolean titlePart) {
		this.titlePart = titlePart;
	}

	public boolean isUpdateUUID() {
		return updateUUID;
	}

	public void setUpdateUUID(boolean updateUUID) {
		this.updateUUID = updateUUID;
	}

	public boolean isComposedShortFundName() {
		return composedShortFundName;
	}

	public void setComposedShortFundName(boolean composedShortFundName) {
		this.composedShortFundName = composedShortFundName;
	}

	public boolean isArchdescFlag() {
		return archdescFlag;
	}

	public void setArchdescFlag(boolean archdescFlag) {
		this.archdescFlag = archdescFlag;
	}

	public boolean isCamOriginators() {
		return camOriginators;
	}

	public void setCamOriginators(boolean camOriginators) {
		this.camOriginators = camOriginators;
	}	

}
