package com.covolt.backend.core.model.enums;

public enum CompanyType {
    CORPORATION("Şirket"),
    SCHOOL("Okul"),
    HOSPITAL("Hastane"),
    FACTORY("Fabrika"),
    MUNICIPALITY("Belediye"),
    OTHER("Diğer");

    //TODO : Enumlar isteğe bağlı genişletilecek

    private final String description;

    CompanyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}