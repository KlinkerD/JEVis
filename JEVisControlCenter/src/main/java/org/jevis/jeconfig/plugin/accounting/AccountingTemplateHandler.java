package org.jevis.jeconfig.plugin.accounting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisFile;
import org.jevis.api.JEVisObject;

public class AccountingTemplateHandler {
    public final static String TYPE = "SimpleDataHandler";
    private static final Logger logger = LogManager.getLogger(AccountingTemplateHandler.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private SelectionTemplate selectionTemplate;
    private JEVisObject templateObject;
    private String Title;

    public AccountingTemplateHandler() {
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public SelectionTemplate getSelectionTemplate() {
        if (selectionTemplate == null && templateObject != null) {

        }

        return selectionTemplate;
    }

    public void setSelectionTemplate(SelectionTemplate selectionTemplate) {
        this.selectionTemplate = selectionTemplate;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public void jsonToModel(JsonNode jsonNode) {
        try {
            this.selectionTemplate = this.mapper.treeToValue(jsonNode, SelectionTemplate.class);
        } catch (JsonProcessingException e) {
            logger.error("Could not parse json model", e);
        }
    }

    public JsonNode toJsonNode() {
        ObjectNode dataHandlerNode = JsonNodeFactory.instance.objectNode();

        ArrayNode templateInputsArrayNode = JsonNodeFactory.instance.arrayNode();
        this.selectionTemplate.getSelectedInputs().forEach(templateInput -> {

            ObjectNode inputNode = JsonNodeFactory.instance.objectNode();
            inputNode.put("objectClass", templateInput.getObjectClass());
            inputNode.put("attributeName", templateInput.getAttributeName());
            inputNode.put("variableName", templateInput.getVariableName());
            inputNode.put("variableType", templateInput.getVariableType());
            inputNode.put("filter", templateInput.getFilter());
            inputNode.put("group", templateInput.getGroup());
            inputNode.put("objectID", templateInput.getObjectID());

            templateInputsArrayNode.add(inputNode);
        });

        dataHandlerNode.set("selectedInputs", templateInputsArrayNode);
        dataHandlerNode.set("templateSelection", JsonNodeFactory.instance.numberNode(selectionTemplate.getTemplateSelection()));

        dataHandlerNode.set("type", JsonNodeFactory.instance.textNode(TYPE));

        return dataHandlerNode;

    }

    public JEVisObject getTemplateObject() {
        return templateObject;
    }

    public void setTemplateObject(JEVisObject templateObject) {
        this.templateObject = templateObject;
        try {
            JEVisAttribute templateFileAttribute = templateObject.getAttribute("Template File");
            if (templateFileAttribute.hasSample()) {
                JEVisFile file = templateFileAttribute.getLatestSample().getValueAsFile();
                JsonNode jsonNode = mapper.readTree(file.getBytes());
                jsonToModel(jsonNode);
            }
        } catch (Exception e) {
            logger.error("Could not read template file from object {}", templateObject);
        }
    }
}
