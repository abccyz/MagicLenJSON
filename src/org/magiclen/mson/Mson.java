/*
 *
 * Copyright 2015 magiclen.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.magiclen.mson;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Set;
import org.magiclen.json.JSONArray;
import org.magiclen.json.JSONException;
import org.magiclen.json.JSONObject;
import org.magiclen.json.XML;
import org.magiclen.gson.Gson;
import org.magiclen.gson.JsonSyntaxException;

/**
 * 結合json.org和Gson實作出的Mson，提供較為簡易方便的方式在物件與JSON和XML間進行轉換。
 *
 * @author Magic Len
 * @see MsonToStringListener
 * @see JSONType
 * @see JSONArray
 * @see JSONObject
 */
public final class Mson {

    /**
     * 建立Gson。
     */
    private final static Gson gson = new Gson();

    /**
     * 完整複製JSON物件。
     *
     * @param jsonType 傳入要複製的JSON物件(JSONType)
     * @return 傳回新的JSON物件
     */
    public static JSONType clone(final JSONType jsonType) {
	return clone(jsonType, true);
    }

    /**
     * 複製JSON物件。
     *
     * @param jsonType 傳入要複製的JSON物件(JSONType)
     * @param strict 是否將JSON底下的所有內容都複製一份；若否，則僅複製最上層的JSON，參考到的內容還是相同
     * @return 傳回新的JSON物件
     */
    public static JSONType clone(final JSONType jsonType, final boolean strict) {
	if (jsonType == null) {
	    return null;
	}

	if (strict) {
	    return fromString(jsonType.toString());
	} else {
	    final JSONType.Type type = jsonType.getType();
	    switch (type) {
		case ARRAY:
		    final JSONArray array = (JSONArray) jsonType;
		    final JSONArray newArray = new JSONArray();
		    final int length = array.length();
		    for (int i = 0; i < length; ++i) {
			newArray.put(array.get(i));
		    }
		    return newArray;
		case OBJECT:
		    final JSONObject object = (JSONObject) jsonType;
		    final JSONObject newObject = new JSONObject();
		    final Set<String> keys = object.keySet();
		    for (final String key : keys) {
			newObject.put(key, object.get(key));
		    }
		    return newObject;
	    }

	    //這裡應該不會執行到
	    throw new JSONException("Can't clone!");
	}
    }

    /**
     * 將字串轉成JSONType，失敗會拋出例外。
     *
     * @param jsonString 傳入JSON格式的字串
     * @return 傳回JSON物件
     */
    public static JSONType fromString(final String jsonString) {
	final JSONType.Type[] types = JSONType.Type.values();
	for (final JSONType.Type type : types) {
	    try {
		return fromString(jsonString, type);
	    } catch (final Exception ex) {

	    }
	}
	throw new JSONException("Undefined JSON's type or JSON string format error.");
    }

    /**
     * 將字串轉成JSONType，失敗會拋出例外。
     *
     * @param jsonString 傳入JSON格式的字串
     * @param type 傳入JSON的類型
     * @return 傳回JSON物件
     */
    public static JSONType fromString(final String jsonString, final JSONType.Type type) {
	switch (type) {
	    case ARRAY:
		return new JSONArray(jsonString);
	    case OBJECT:
		return new JSONObject(jsonString);
	}
	throw new JSONException("Undefined JSON's type.");
    }

    /**
     * 將XML轉成JSONType，失敗會拋出例外。
     *
     * @param xml 傳入XML格式的字串
     * @return 傳回JSON物件
     */
    public static JSONType fromXML(String xml) {
	xml = xml.trim();
	final JSONObject obj = XML.toJSONObject("<root>".concat(xml).concat("</root>"));
	try {
	    final JSONObject root = obj.getJSONObject("root");
	    final Set<String> keySet = root.keySet();
	    if (keySet.size() == 1) {
		if (keySet.contains("root")) {
		    final Object in_root = root.get("root");
		    if (in_root instanceof JSONArray) {
			return (JSONArray) in_root;
		    } else {
			return root;
		    }
		} else if (keySet.contains("array")) {
		    return root.getJSONArray("array");
		} else {
		    return obj;
		}
	    } else {
		return obj;
	    }
	} catch (final Exception ex) {
	    return obj;
	}
    }

    /**
     * 將JSONType轉成XML字串。
     *
     * @param json 傳入JSONType物件
     * @return 傳回XML格式的字串
     */
    public static String toXML(final JSONType json) {
	return XML.toString(json);
    }

    /**
     * 將JSONArray轉成物件陣列。
     *
     * @param array 傳入JSONArray物件
     * @param type 傳入物件型態(Class)，愈前面的優先嘗試，如果不傳入型態，預設使用Object
     * @return 回傳轉換成的物件，如果轉換失敗，傳回null
     */
    public static Object[] toArray(final JSONArray array, Type... type) {
	if (array == null) {
	    return null;
	}
	if (type == null || type.length == 0) {
	    type = new Type[]{Object.class};
	}
	final int l = array.length();
	final Object[] objects = new Object[l];
	for (int i = 0; i < l; i++) {
	    final Object obj = array.get(i);
	    if (obj == null) {
		return null;
	    }
	    if (obj instanceof JSONObject) {
		final JSONObject jsonObj = (JSONObject) obj;
		boolean success = false;
		for (final Type t : type) {
		    final Object o = toObject(jsonObj, t);
		    if (o != null) {
			objects[i] = o;
			success = true;
			break;
		    }
		}
		if (!success) {
		    return null;
		}
	    } else if (obj instanceof JSONArray) {
		final JSONArray jsonArray = (JSONArray) obj;
		final Object[] arr = toArray(jsonArray, type);
		if (arr == null) {
		    return null;
		}
		objects[i] = arr;
	    } else {
		final Class c = obj.getClass();
		if (c.isArray()) {
		    final JSONArray jsonArray = new JSONArray();
		    final int arrayLength = Array.getLength(jsonArray);
		    for (int k = 0; k < arrayLength; k++) {
			final Object o = Array.get(obj, k);
			jsonArray.put(o);
		    }
		    final Object[] arr = toArray(jsonArray, type);
		    if (arr == null) {
			return null;
		    }
		    objects[i] = arr;
		} else {
		    objects[i] = obj;
		}
	    }
	}

	return objects;
    }

    /**
     * 將JSONObject轉成物件。
     *
     * @param object 傳入JSONObject物件
     * @param type 傳入物件型態(Class)，愈前面的優先嘗試，如果不傳入型態，預設使用Object
     * @return 回傳轉換成的物件，如果轉換失敗，傳回null
     */
    public static Object toObject(final JSONObject object, Type... type) {
	if (object == null) {
	    return null;
	}
	if (type == null || type.length == 0) {
	    type = new Type[]{Object.class};
	}
	for (final Type t : type) {
	    try {
		final Object o = gson.fromJson(object.toString(), t);
		if (o != null) {
		    return o;
		}
	    } catch (final JsonSyntaxException ex) {

	    }
	}
	return null;
    }

    /**
     * 將一般物件轉成JSON物件(JSONType)。
     *
     * @param objects 傳入物件
     * @return 傳回JSON物件，如果轉換失敗，傳回null
     */
    public static JSONType toJSON(final Object... objects) {
	if (objects == null) {
	    return null;
	}
	if (objects.length == 1) {
	    final JSONType json = toJSON(objects[0]);
	    if (json != null) {
		return json;
	    }
	}
	return toJSONArray(objects);
    }

    /**
     * 將數個物件轉成JSONArray。
     *
     * @param objects 傳入物件
     * @return 傳回JSONArray，如果轉換失敗，傳回null
     */
    public static JSONArray toJSONArray(final Object... objects) {
	try {
	    final JSONArray array = new JSONArray();
	    for (final Object obj : objects) {
		final JSONObject json = toJSONObject(obj);
		if (json != null) {
		    array.put(json);
		} else {
		    final JSONArray jsonArray = toJSONArray(obj);
		    if (jsonArray != null) {
			final int l = jsonArray.length();
			for (int i = 0; i < l; i++) {
			    array.put(jsonArray.getJSONObject(i));
			}
		    } else {
			return null;
		    }
		}
	    }
	    return array;
	} catch (JSONException ex) {
	    return null;
	}
    }

    /**
     * 將一般物件轉成JSON物件(JSONType)。
     *
     * @param object 傳入一般物件
     * @return 傳回JSON物件
     */
    public static JSONType toJSON(final Object object) {
	final JSONType.Type[] types = JSONType.Type.values();
	for (final JSONType.Type type : types) {
	    final JSONType tmp = toJSON(object, type);
	    if (tmp != null) {
		return tmp;
	    }
	}
	return null;
    }

    /**
     * 將一般物件轉成JSON物件(JSONType)。
     *
     * @param object 傳入一般物件
     * @param type 傳入JSON的類型
     * @return 傳回JSON物件
     */
    public static JSONType toJSON(final Object object, final JSONType.Type type) {
	switch (type) {
	    case ARRAY:
		return toJSONArray(object);
	    case OBJECT:
		return toJSONObject(object);
	}
	throw new JSONException("Undefined JSON's type.");
    }

    /**
     * 將物件轉成JSONArray。
     *
     * @param object 傳入物件
     * @return 傳回JSONArray，如果轉換失敗，傳回null
     */
    public static JSONArray toJSONArray(final Object object) {
	try {
	    return new JSONArray(gson.toJson(object));
	} catch (final JSONException ex) {
	    return null;
	}
    }

    /**
     * 將物件轉成JSONObject。
     *
     * @param object 傳入物件
     * @return 傳回JSONObject，如果轉換失敗，傳回null
     */
    public static JSONObject toJSONObject(final Object object) {
	try {
	    return new JSONObject(gson.toJson(object));
	} catch (final JSONException ex) {
	    return null;
	}
    }

    /**
     * 清空JSON物件(JSONType)。
     *
     * @param jsonType 傳入要清空的JSONType
     */
    public static void clearJSON(final JSONType jsonType) {
	final JSONType.Type type = jsonType.getType();
	switch (type) {
	    case ARRAY:
		clearJSONArray((JSONArray) jsonType);
		break;
	    case OBJECT:
		clearJSONObject((JSONObject) jsonType);
		break;
	}
    }

    /**
     * 清空JSONArray。
     *
     * @param array 傳入要清空的JSONArray
     */
    public static void clearJSONArray(final JSONArray array) {
	final int l = array.length();
	for (int i = l - 1; i >= 0; i--) {
	    array.remove(i);
	}
    }

    /**
     * 清空JSONObject。
     *
     * @param object 傳入要清空的JSONObject
     */
    public static void clearJSONObject(final JSONObject object) {
	final Set<String> keys = object.keySet();
	final String[] keysString = new String[keys.size()];
	keys.toArray(keysString);
	for (final String key : keysString) {
	    object.remove(key);
	}
    }

    /**
     * 將JSON物件(JSONType)轉成字串。
     *
     * @param jsonType 傳入JSON物件
     * @return 傳回字串
     */
    public static String toString(final JSONType jsonType) {
	return toString(jsonType, false);
    }

    /**
     * 將JSON物件(JSONType)轉成字串。
     *
     * @param jsonType 傳入JSON物件
     * @param format 是否要進行編排格式化
     * @return 傳回字串
     */
    public static String toString(final JSONType jsonType, final boolean format) {
	if (format) {
	    try {
		return toString(null, jsonType);
	    } catch (final IOException ex) {
		//應該不會執行到這裡才對
		return null;
	    }
	} else {
	    return jsonType.toString();
	}
    }

    /**
     * 將JSON物件(JSONType)轉成格式化字串，或輸出至串流中。
     *
     * @param outputStream 傳入輸出串流，若為null，直接傳回字串
     * @param jsonType 傳入JSON物件
     * @return 直接傳回字串；若是使用串流方式，傳回null
     * @throws java.io.IOException
     */
    public static String toString(final OutputStream outputStream, final JSONType jsonType) throws IOException {
	return toString(outputStream, jsonType, 1, null);
    }

    /**
     * 將JSON物件(JSONType)轉成格式化字串，或輸出至串流中。
     *
     * @param outputStream 傳入輸出串流，若為null，直接傳回字串
     * @param jsonType 傳入JSON物件
     * @param listener 傳入監聽者
     * @return 直接傳回字串；若是使用串流方式，傳回null
     * @throws java.io.IOException
     */
    public static String toString(final OutputStream outputStream, final JSONType jsonType, final MsonToStringListener listener) throws IOException {
	return toString(outputStream, jsonType, 1, listener);
    }

    /**
     * 將物件轉成字串，或輸出至串流中。
     *
     * @param outputStream 傳入輸出串流，若為null，直接傳回字串
     * @param object 傳入物件
     * @param tab 傳入要用Tab縮排的次數
     * @param listener 傳入監聽者
     * @return 直接傳回字串；若是使用串流方式，傳回null
     * @throws 若IO存取有問題，將拋出例外
     */
    private static String toString(final OutputStream outputStream, final Object object, final int tab, final MsonToStringListener listener) throws IOException {
	if (listener != null) {
	    listener.doing(object, true, tab);
	}
	final boolean toStream = outputStream != null;
	final StringBuilder sb = toStream ? null : new StringBuilder();
	if (object instanceof JSONArray) {
	    if (toStream) {
		outputStream.write(new byte[]{91, 10}); // [\n
	    } else {
		sb.append("[\n");
	    }
	    final JSONArray array = (JSONArray) object;
	    final int l = array.length();
	    for (int k = 0; k < l; k++) {
		final Object obj = array.get(k);
		if (toStream) {
		    for (int i = 0; i < tab; i++) {
			outputStream.write(9); // \t
		    }
		} else {
		    for (int i = 0; i < tab; i++) {
			sb.append("\t");
		    }
		}
		if (toStream) {
		    toString(outputStream, obj, tab + 1, listener);
		} else {
		    sb.append(toString(null, obj, tab + 1, listener));
		}
		if (k != l - 1) {
		    if (toStream) {
			outputStream.write(44); // ,
		    } else {
			sb.append(",");
		    }
		}
		if (toStream) {
		    outputStream.write(10); // \n
		} else {
		    sb.append("\n");
		}
	    }
	    if (toStream) {
		for (int i = 1; i < tab; i++) {
		    outputStream.write(9); // \t
		}
		outputStream.write(93); // ]
	    } else {
		for (int i = 1; i < tab; i++) {
		    sb.append("\t");
		}
		sb.append("]");
	    }
	} else if (object instanceof JSONObject) {
	    if (toStream) {
		outputStream.write(new byte[]{123, 10}); // {\n
	    } else {
		sb.append("{\n");
	    }
	    final JSONObject obj = (JSONObject) object;
	    final Set<String> keys = obj.keySet();
	    final int l = keys.size();
	    int k = 0;
	    for (final String key : keys) {
		final String adjustKey = key.replaceAll("\"", "＼＼magiclen＼＼ｑ").replaceAll("\n", "＼＼magiclen＼＼n").replaceAll("\t", "＼＼magiclen＼＼t").replaceAll("\\\\", "＼＼magiclen＼＼＼＼").replaceAll("＼＼magiclen＼＼＼＼", "\\\\\\\\").replaceAll("＼＼magiclen＼＼ｑ", "\\\\\"").replaceAll("＼＼magiclen＼＼n", "\\\\n").replaceAll("＼＼magiclen＼＼t", "\\\\t");
		if (toStream) {
		    for (int i = 0; i < tab; i++) {
			outputStream.write(9); // \t
		    }
		    outputStream.write(34); // \"
		    outputStream.write(adjustKey.getBytes("UTF-8"));
		    outputStream.write(34); // \"
		    outputStream.write(new byte[]{32, 58, 32}); //  : 
		    toString(outputStream, obj.get(key), tab + 1, listener);
		} else {
		    for (int i = 0; i < tab; i++) {
			sb.append("\t");
		    }
		    sb.append("\"").append(adjustKey).append("\"").append(" : ").append(toString(null, obj.get(key), tab + 1, listener));
		}
		if (k != l - 1) {
		    if (toStream) {
			outputStream.write(44); // ,
		    } else {
			sb.append(",");
		    }
		}
		if (toStream) {
		    outputStream.write(10); // \n
		} else {
		    sb.append("\n");
		}
		k++;
	    }
	    if (toStream) {
		for (int i = 1; i < tab; i++) {
		    outputStream.write(9); // \t
		}
		outputStream.write(125); // }
	    } else {
		for (int i = 1; i < tab; i++) {
		    sb.append("\t");
		}
		sb.append("}");
	    }
	} else if (object instanceof String) {
	    final String adjustObjectString = object.toString().replaceAll("\"", "＼＼magiclen＼＼ｑ").replaceAll("\n", "＼＼magiclen＼＼n").replaceAll("\t", "＼＼magiclen＼＼t").replaceAll("\\\\", "＼＼magiclen＼＼＼＼").replaceAll("＼＼magiclen＼＼＼＼", "\\\\\\\\").replaceAll("＼＼magiclen＼＼ｑ", "\\\\\"").replaceAll("＼＼magiclen＼＼n", "\\\\n").replaceAll("＼＼magiclen＼＼t", "\\\\t");
	    if (toStream) {
		outputStream.write(34); // \"
		outputStream.write(adjustObjectString.getBytes("UTF-8"));
		outputStream.write(34); // \"
	    } else {
		sb.append("\"").append(adjustObjectString).append("\"");
	    }
	} else if (object != null) {
	    final Class c = object.getClass();
	    if (c.isArray()) {
		if (toStream) {
		    outputStream.write(new byte[]{91, 10}); // [\n
		} else {
		    sb.append("[\n");
		}
		final int l = Array.getLength(object);
		for (int k = 0; k < l; k++) {
		    final Object obj = Array.get(object, k);
		    if (toStream) {
			for (int i = 0; i < tab; i++) {
			    outputStream.write(9); // \t
			}
		    } else {
			for (int i = 0; i < tab; i++) {
			    sb.append("\t");
			}
		    }
		    if (toStream) {
			toString(outputStream, obj, tab + 1, listener);
		    } else {
			sb.append(toString(null, obj, tab + 1, listener));
		    }
		    if (k != l - 1) {
			if (toStream) {
			    outputStream.write(44); // ,
			} else {
			    sb.append(",");
			}
		    }
		    if (toStream) {
			outputStream.write(10); // \n
		    } else {
			sb.append("\n");
		    }
		}
		if (toStream) {
		    for (int i = 1; i < tab; i++) {
			outputStream.write(9); // \t
		    }
		    outputStream.write(93); // ]
		} else {
		    for (int i = 1; i < tab; i++) {
			sb.append("\t");
		    }
		    sb.append("]");
		}
	    } else {
		final String objectString = object.toString();
		if (toStream) {
		    outputStream.write(objectString.getBytes("UTF-8"));
		} else {
		    sb.append(objectString);
		}
	    }
	} else {
	    final String objectString = "null";
	    if (toStream) {
		outputStream.write(objectString.getBytes("UTF-8"));
	    } else {
		sb.append(objectString);
	    }
	}
	if (listener != null) {
	    listener.doing(object, false, tab);
	}
	if (toStream) {
	    return null;
	} else {
	    return sb.toString();
	}
    }
}