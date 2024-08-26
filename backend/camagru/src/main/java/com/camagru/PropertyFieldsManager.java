package com.camagru;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.camagru.request_handlers.Request;

public class PropertyFieldsManager {
    private List<PropertyField> queryFields;
    private List<PropertyField> bodyFields;

    public PropertyFieldsManager(List<PropertyField> queryFields, List<PropertyField> bodyFields) {
        this.queryFields = queryFields;
        this.bodyFields = bodyFields;
    }

    public PropertyFieldsManager(List<PropertyField> bodyFields) {
        this.queryFields = null;
        this.bodyFields = bodyFields;
    }

    public List<String> getWrongFields(JSONObject jsonBody) {
        List<String> wrongFields = new ArrayList<>();
        // Get wrong fields
        for (PropertyField queryFields : queryFields) {
            String field = null;
            try {
                field = jsonBody.getString(queryFields.key);

            } catch (Exception e) {
            }

            if (!queryFields.validate(field)) {
                wrongFields.add(queryFields.key);
            }
        }
        return wrongFields;
    }

    public List<String> validationResult(Request req) throws IOException {

        List<String> wrongFields = new ArrayList<>();

        if (bodyFields != null) {
            JSONObject jsonBody = req.getBodyAsJson();
            for (PropertyField bodyField : bodyFields) {
                String field = null;
                try {
                    field = jsonBody.getString(bodyField.key);

                } catch (Exception e) {
                }

                if (!bodyField.validate(field)) {
                    wrongFields.add(bodyField.key);
                }
            }
        }

        if (queryFields != null) {
            for (PropertyField queryField : queryFields) {
                String field = null;
                try {
                    field = req.getQueryParameter(queryField.key);

                } catch (Exception e) {
                }

                if (!queryField.validate(field)) {
                    wrongFields.add(queryField.key);
                }
            }
        }
        return wrongFields;
    }
}
