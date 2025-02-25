package com.camagru;

public class PropertyField {
    public String key;
    public Boolean isMandatory;
    private String regex;

    public PropertyField(String key, Boolean isMandatory, String regex) {
        this.key = key;
        this.isMandatory = isMandatory;
        this.regex = regex;
    }

    public PropertyField(String key, Boolean isMandatory) {
        this.key = key;
        this.isMandatory = isMandatory;
        this.regex = null;
    }

    /**
     * Validate a value against the mandatory flag and regex.
     *
     * @param value
     *              A string value.
     * @return A boolean value.
     */
    public Boolean validate(String value) {

        if (!isMandatory && value == null) {
            return true;
        }

        if ((isMandatory && value == null) || (regex != null && !value.matches(regex))) {
            return false;
        } else {
            return true;
        }
    }
}
