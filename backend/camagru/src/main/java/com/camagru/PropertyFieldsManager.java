package com.camagru;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class PropertyFieldsManager {
    private List<PropertyField> propertyFields;

    public PropertyFieldsManager(List<PropertyField> propertyFields) {
        this.propertyFields = propertyFields;
    }

    public List<String> getWrongFields(JSONObject jsonBody) {
        List<String> wrongFields = new ArrayList<>();
        // Get wrong fields
        for (PropertyField propertyField : propertyFields) {
            String field = null;
            try {
                field = jsonBody.getString(propertyField.key);

            } catch (Exception e) {
            }

            if (!propertyField.validate(field)) {
                wrongFields.add(propertyField.key);
            }
        }
        return wrongFields;
    }
}
