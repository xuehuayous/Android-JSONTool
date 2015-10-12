package com.kevin.jsontool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * 版权所有：XXX有限公司</br>
 * 
 * JsonTool </br>
 * 
 * @author zhou.wenkai ,Created on 2015-10-8 09:04:23</br> 
 * 		   Major Function：<b>JSON 操作 工具类</b> </br>
 * 
 *         注:如果您修改了本类请填写以下内容作为记录，如非本人操作劳烦通知，谢谢！！！</br>
 * @author mender，Modified Date Modify Content:
 */
public class JsonTool<T> {
	
	private static boolean DEBUG = false;
	
	/**
	 * 将JSON字符串封装到对象
	 * 
	 * @param jsonStr 待封装的JSON字符串
	 * @param c 待封装的实例字节码
	 * @return T: 封装JSON数据的对象
	 * @version 1.0 
	 * @date 2015-10-9
	 * @Author zhou.wenkai
	 */
	public static <T> T toBean(String jsonStr, Class<T> clazz) {
		try {
			JSONObject job = new JSONObject(jsonStr);
			return parseObject(job, clazz, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 将 对象编码为 JSON格式
	 * 
	 * @param t 待封装的对象
	 * @return String: 封装后JSONObject String格式
	 * @version 1.0 
	 * @date 2015-10-11
	 * @Author zhou.wenkai
	 */
	public static <T> String toJson(T t) {
		if (t == null) {
            return "{}";  
        }
		return objectToJson(t);
	}
	
	/**
	 * JSONObject 封装到 对象实例
	 * 
	 * @param job 待封装的JSONObject
	 * @param c 待封装的实例对象class
	 * @param v	待封装实例的外部类实例对象</br>只有内部类存在,外部类时传递null
	 * @return T:封装数据的实例对象
	 * @version 1.0 
	 * @date 2015-10-9
	 * @Author zhou.wenkai
	 */
	@SuppressWarnings("unchecked")
	private static <T, V> T parseObject(JSONObject job, Class<T> c, V v) {
		T t = null;
		try {
			if(null == v) {
				t = c.newInstance();
			} else {
				Constructor<?> constructor = c.getDeclaredConstructors()[0];
				constructor.setAccessible(true);
				t = (T) constructor.newInstance(v);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			Log.e(JsonTool.class.getSimpleName(),
					c.toString() + " should provide a default constructor " +
							"(a public constructor with no arguments)");
		} catch (Exception e) {
			if(DEBUG)
				e.printStackTrace();
		}
		
		Field[] fields = c.getDeclaredFields();
		for (Field field : fields) {
			field.setAccessible(true);
			Class<?> type = field.getType();
			String name = field.getName();
			
			// if the object don`t has a mapping for name, then continue
			if(!job.has(name)) continue;
			
			String typeName = type.getName();
			if(typeName.equals("java.lang.String")) {
				try {
					String value = job.getString(name);
					if (value != null && value.equals("null")) {
						value = "";
					}
					field.set(t, value);
				} catch (Exception e) {
					if(DEBUG)
						e.printStackTrace();
					try {
						field.set(t, "");
					} catch (Exception e1) {
						if(DEBUG)
							e1.printStackTrace();
					}
				}
			} else if(typeName.equals("int") ||
					typeName.equals("java.lang.Integer")) {
				try {
					field.set(t, job.getInt(name));
				} catch (Exception e) {
					if(DEBUG)
						e.printStackTrace();
				}
			} else if(typeName.equals("boolean") ||
					typeName.equals("java.lang.Boolean")) {
				try {
					field.set(t, job.getBoolean(name));
				} catch (Exception e) {
					if(DEBUG)
						e.printStackTrace();
				}
			} else if(typeName.equals("float") ||
					typeName.equals("java.lang.Float")) {
				try {
					field.set(t, Float.valueOf(job.getString(name)));
				} catch (Exception e) {
					if(DEBUG)
						e.printStackTrace();
				}
			} else if(typeName.equals("double") || 
					typeName.equals("java.lang.Double")) {
				try {
					field.set(t, job.getDouble(name));
				} catch (Exception e) {
					if(DEBUG)
						e.printStackTrace();
				}
			} else if(typeName.equals("java.util.List") ||
					typeName.equals("java.util.ArrayList")){
				try {
					Object obj = job.get(name);
					Type genericType = field.getGenericType();
					String className = genericType.toString().replace("<", "")
							.replace(type.getName(), "").replace(">", "");
					Class<?> clazz = Class.forName(className);
					if(obj instanceof JSONArray) {
						ArrayList<?> objList = parseArray((JSONArray)obj, clazz, t);
						field.set(t, objList);
					}
				} catch (Exception e) {
					if(DEBUG)
						e.printStackTrace();
				}
			} else {
				try {
					Object obj = job.get(name);
					Class<?> clazz = Class.forName(typeName);
					if(obj instanceof JSONObject) {
						Object parseJson = parseObject((JSONObject)obj, clazz, t);
						field.set(t, parseJson);
					}
				} catch (Exception e) {
					if(DEBUG)
						e.printStackTrace();
				}
				
			}
		}

		return t;
	}
	
	/**
	 * 将 JSONArray 封装到 ArrayList 对象
	 * 
	 * @param array 待封装的JSONArray
	 * @param c 待封装实体字节码
	 * @param v 待封装实例的外部类实例对象</br>只有内部类存在,外部类时传递null
	 * @return ArrayList<T>: 封装后的实体集合
	 * @version 1.0 
	 * @date 2015-10-8
	 */
	@SuppressWarnings("unchecked")
	private static <T, V> ArrayList<T> parseArray(JSONArray array, Class<T> c, V v) {
		ArrayList<T> list = new ArrayList<T>(array.length());
		try {
			for (int i = 0; i < array.length(); i++) {
				if(array.get(i) instanceof JSONObject) {
					T t = parseObject(array.getJSONObject(i), c, v);
					list.add(t);
				} else {
					list.add((T) array.get(i));
				}
				
			}
		} catch (Exception e) {
			if(DEBUG)
				e.printStackTrace();
		}
		return list;
	}
	
	/**
	 * 将 对象编码为 JSON格式
	 * 
	 * @param t 待封装的对象
	 * @return String: 封装后JSONObject String格式
	 * @version 1.0 
	 * @date 2015-10-11
	 * @Author zhou.wenkai
	 */
	private static <T> String objectToJson(T t) {
		
		Field[] fields = t.getClass().getDeclaredFields();
		StringBuilder sb = new StringBuilder(fields.length << 4);
		sb.append("{");
		
		for (Field field : fields) {
			field.setAccessible(true);
			Class<?> type = field.getType();
			String name = field.getName();
			
			// 'this$Number' 是内部类的外部类引用(指针)字段
			if(name.contains("this$")) continue;
			
			String typeName = type.getName();
			if(typeName.equals("java.lang.String")) {
				try {
					sb.append("\""+name+"\":");
					sb.append(stringToJson((String)field.get(t)));
					sb.append(",");
				} catch (Exception e) {
					if(DEBUG)
						e.printStackTrace();
				}
			} else if(typeName.equals("boolean") ||
					typeName.equals("java.lang.Boolean") ||
					typeName.equals("int") ||
					typeName.equals("java.lang.Integer") ||
					typeName.equals("float") ||
					typeName.equals("java.lang.Float") ||
					typeName.equals("double") || 
					typeName.equals("java.lang.Double")) {
				try {
					sb.append("\""+name+"\":");
					sb.append(field.get(t));
					sb.append(",");
				} catch (Exception e) {
					if(DEBUG)
						e.printStackTrace();
				}
			} else if(typeName.equals("java.util.List") ||
					typeName.equals("java.util.ArrayList")){
				try {
					List<?> objList = (List<?>) field.get(t);
					if(null != objList && objList.size() > 0) {
						sb.append("\""+name+"\":");
						sb.append("[");
						String toJson = listToJson((List<?>) field.get(t));
						sb.append(toJson);
						sb.setCharAt(sb.length()-1, ']');
						sb.append(",");
					}
				} catch (Exception e) {
					if(DEBUG)
						e.printStackTrace();
				}
			} else {
				try {
					sb.append("\""+name+"\":");
					sb.append("{");
					sb.append(objectToJson(field.get(t)));
					sb.setCharAt(sb.length()-1, '}');
					sb.append(",");
				} catch (Exception e) {
					if(DEBUG)
						e.printStackTrace();
				}
			}
			
		}
		if(sb.length() == 1) {
			sb.append("}");
		}
		sb.setCharAt(sb.length()-1, '}');
	    return sb.toString();  
	}
	
	/**
	 * 将 List 对象编码为 JSON格式
	 * 
	 * @param objList 待封装的对象集合
	 * @return String:封装后JSONArray String格式
	 * @version 1.0 
	 * @date 2015-10-11
	 * @Author zhou.wenkai
	 */
	private static<T> String listToJson(List<T> objList) {
		final StringBuilder sb = new StringBuilder();
		for (T t : objList) {
			if(t instanceof String) {
				sb.append(stringToJson((String) t));
				sb.append(",");
			} else if(t instanceof Boolean || 
					t instanceof Integer ||
					t instanceof Float ||
					t instanceof Double) {
				sb.append(t);
				sb.append(",");
			} else {
				sb.append(objectToJson(t));
				sb.append(",");
			}
		}
		return sb.toString();
	}

	/**
	 * 将 String 对象编码为 JSON格式，只需处理好特殊字符
	 * 
	 * @param s String 对象
	 * @return String:JSON格式 
	 * @version 1.0 
	 * @date 2015-10-11
	 * @Author zhou.wenkai
	 */
	private static String stringToJson(final String str) {
		if(str == null || str.length() == 0) {
			return "\"\"";
		}
		final StringBuilder sb = new StringBuilder(str.length() + 2 << 4);
		sb.append('\"');
		for (int i = 0; i < str.length(); i++) {
			final char c = str.charAt(i);

			sb.append(c == '\"' ? "\\\"" : c == '\\' ? "\\\\"
					: c == '/' ? "\\/" : c == '\b' ? "\\b" : c == '\f' ? "\\f"
							: c == '\n' ? "\\n" : c == '\r' ? "\\r"
									: c == '\t' ? "\\t" : c);
		}
		sb.append('\"');
		return sb.toString();
	}

}
