package uk.co.eskabe.RateServer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Created by Phil on 21/04/2017.
 */
public class JsonSerializerBase {

    private transient int internalNoPrintCount = 0;

    public JsonSerializerBase() {

    }

    private String dump(Object o, int callCount) {
        callCount++;
        StringBuffer tabs = new StringBuffer();
        for (int k = 0; k < callCount; k++) {
            tabs.append("\t");
        }
        StringBuffer buffer = new StringBuffer();
        Class oClass = o.getClass();
        if (oClass.isArray()) {
            buffer.append("\n");
            buffer.append(tabs.toString());
            buffer.append("[");
            int arrayLength = Array.getLength(o);
            for (int i = 0; i < arrayLength; i++) {
                Object value = Array.get(o, i);

                if ( value.getClass().getModifiers() != Modifier.TRANSIENT ) {
                    if (value.getClass().isPrimitive() ||
                            value.getClass() == java.lang.Long.class ||
                            value.getClass() == java.lang.String.class ||
                            value.getClass() == java.lang.Integer.class ||
                            value.getClass() == java.lang.Boolean.class
                            ) {
                        buffer.append("\"" + value + "\"");
                    } else {
                        buffer.append(dump(value, callCount));
                    }
                }

                if (i < (arrayLength - 1))
                    buffer.append(", ");
            }
            buffer.append(tabs.toString());
            buffer.append("]\n");
        } else {
            buffer.append("\n");
            buffer.append(tabs.toString());
            buffer.append("{\n");
            while (oClass != null) {
                // Don't dump the contents of this base class.
                if ( oClass.getName().compareTo("uk.co.eskabe.RateServer.JsonSerializerBase") != 0 ) {
                    Field[] fields = oClass.getDeclaredFields();
                    for (int i = 0; i < fields.length; i++) {
                        String objectname = fields[i].getName();
                        if ( objectname.compareTo("this$0") != 0 ) {
                            fields[i].setAccessible(true);
                            if (i > 0) {
                                buffer.append(", ");
                            }
                            buffer.append(tabs.toString());
                            buffer.append("\"");
                            buffer.append(fields[i].getName());
                            buffer.append("\": ");
                            try {
                                Object value = fields[i].get(o);
                                if ((value != null) && (value.getClass().getModifiers() != Modifier.TRANSIENT)) {
                                    if (value.getClass().isPrimitive() ||
                                            value.getClass() == java.lang.Long.class ||
                                            value.getClass() == java.lang.String.class ||
                                            value.getClass() == java.lang.Integer.class ||
                                            value.getClass() == java.lang.Boolean.class
                                            ) {
                                        buffer.append("\"" + value + "\"");
                                    } else {
                                        buffer.append(dump(value, callCount));
                                    }
                                }
                            } catch (IllegalAccessException e) {
                                buffer.append(e.getMessage());
                            }

                            buffer.append("\n");
                        }
                    }
                }
                oClass = null; //oClass.getSuperclass();
            }
            buffer.append(tabs.toString());
            buffer.append("}\n");
        }
        return buffer.toString();
    }

    public String writeOut() {
        String fancy = dump(this, internalNoPrintCount);
        return fancy.replace("\t", "").replace("\r", "").replace("\n", "");
    }

    private void load( Object loadObject, JSONObject jsonInbound ) throws JsonSerializerException {

        Class oClass = loadObject.getClass();
        if (oClass.isArray()) {
            for (int i = 0; i < Array.getLength(loadObject); i++) {
                Object value = Array.get(loadObject, i);

                if (value.getClass().isPrimitive() ||
                        value.getClass() == java.lang.Long.class ||
                        value.getClass() == java.lang.String.class ||
                        value.getClass() == java.lang.Integer.class ||
                        value.getClass() == java.lang.Boolean.class
                        ) {
                } else {
                }
            }
        } else {
            while (oClass != null) {
                Field[] fields = oClass.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    String fieldName = fields[i].getName();
                    if ( (fieldName.compareTo("this$0") != 0) && (fieldName.compareTo("internalNoPrintCount") != 0) ) {
                        String strNewFieldValue;
                        fields[i].setAccessible(true);
                        try {
                            strNewFieldValue = jsonInbound.get(fieldName).toString();
                        } catch ( NullPointerException npEx ) {
                            throw new JsonSerializerException("Mapping failure on field '" + fieldName + "'");
                        }
                        if (strNewFieldValue == null) {
                            // If we hit this then the object we're serialising into contains an object
                            // which is not named in the inbound JSON object.
                            throw new JsonSerializerException("Parameter mapping failure in load from JSON!");
                        }
                        try {
                            Object value = fields[i].get(loadObject);
                            if (value.getClass() == java.lang.Long.class) {
                                Long newValue = Long.valueOf(strNewFieldValue);
                                fields[i].setLong(loadObject, newValue.longValue());
                            } else if (value.getClass() == java.lang.String.class) {
                                fields[i].set(loadObject, strNewFieldValue);
                            } else if (value.getClass() == java.lang.Integer.class) {
                                Integer newValue = Integer.valueOf(strNewFieldValue);
                                fields[i].setInt(loadObject, newValue.intValue());
                            } else if (value.getClass() == java.lang.Boolean.class) {
                                Boolean newValue = Boolean.valueOf(strNewFieldValue);
                                fields[i].setBoolean(loadObject, newValue.booleanValue());
                            } else {
                                load(value, (JSONObject) jsonInbound.get(fieldName));
                            }
                        } catch (IllegalAccessException e) {
                            System.out.println(e.toString());
                        }
                    }
                }
                oClass = oClass.getSuperclass();
            }
        }
    }

    public void readIn(String inbound) throws JsonSerializerException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject)parser.parse(inbound);
        load( this, jsonObject );
    }
}
