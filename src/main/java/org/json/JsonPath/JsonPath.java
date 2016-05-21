package org.json.JsonPath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * json的表达式查询支持 仿webmagic的jsonpath表达式 注意： 列表数据必须在表达式后加入"[*]"，否则会将列表数据当字符串处理 例如：
 * 参考：http://goessner.net/articles/JsonPath/
 *
 * @author lcsan
 * @version [版本号, 2016年3月23日]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
@SuppressWarnings("restriction")
public class JsonPath {

	private Object obj;

	private String expr;

	public enum ResultType {
		VALUE, PATH;
	}

	private ResultType resultType = ResultType.VALUE;

	private List<Object> result = new ArrayList<Object>();

	private List<String> subx;

	public JsonPath(JSONObject obj) {
		this.obj = obj;
		result.add(obj);
	}

	public JsonPath(JSONArray obj) {
		this.obj = obj;
		result.add(obj);
	}

	public JsonPath(List<Object> obj) {
		if (obj.isEmpty()) {
			this.obj = null;
		} else {
			this.obj = new JSONArray(obj);
			result.addAll(obj);
		}
	}

	@SuppressWarnings("unchecked")
	public JsonPath(Object obj) {
		if (null != obj) {
			if (obj instanceof String) {
				String o = (String) obj;
				o = o.replaceAll("^[^\\[\\{]+", "").trim();
				if (o.startsWith("{")) {
					this.obj = new JSONObject(o);
					this.result.add(obj);
				} else if (o.startsWith("[")) {
					this.obj = new JSONArray(o);
					this.result.add(obj);
				}
			} else if (obj instanceof List) {
				List<Object> o = (List<Object>) obj;
				this.obj = new JSONArray(o);
				this.result.addAll(o);
				this.result.add(obj);
			} else {
				this.obj = obj;
				this.result.add(obj);
			}
		}
	}

	public JsonPath query(String expr) throws JSONException, ScriptException {
		return query(expr, ResultType.VALUE);
	}

	public JsonPath query(String expr, ResultType arg) throws JSONException, ScriptException {
		this.resultType = null != arg ? arg : ResultType.VALUE;
		this.expr = expr;
		if (null != this.expr && null != this.obj) {
			result.clear();
			trace(normalize(expr), this.obj, "$");
		}
		List<Object> re = new ArrayList<Object>();
		re.addAll(result);
		result.clear();
		if (null != obj) {
			result.add(this.obj);
		}
		return new JsonPath(re);
	}

	/**
	 * 正则提取
	 * 
	 * @param str
	 * @param regex
	 * @return
	 * @see [类、类#方法、类#成员]
	 */
	private List<String> matchAll(String str, String regex) {
		Matcher match = Pattern.compile(regex).matcher(str);
		List<String> list = new ArrayList<String>();
		while (match.find()) {
			list.add(match.group(1));
		}
		return list;
	}

	/**
	 * 表达式处理
	 * 
	 * @param expr
	 * @return
	 * @see [类、类#方法、类#成员]
	 */
	private String normalize(String expr) {
		subx = matchAll(expr, "[\\['](\\??\\(.*?\\))[\\]']");
		for (int i = 0, j = subx.size(); i < j; i++) {
			expr = expr.replace(subx.get(i), "#" + i);
		}
		expr = expr.replaceAll("'?\\.'?|\\['?", ";").replaceAll(";{2,}", ";..;").replaceAll(";$|'?\\]|'$", "");
		List<String> list = matchAll(expr, "(#\\d+)");
		for (int i = 0, j = list.size(); i < j; i++) {
			expr = expr.replace(list.get(i), subx.get(i));
		}
		expr = expr.replaceAll("^\\$;", "");
		return expr;
	}

	/**
	 * 遍历查询表达式
	 * 
	 * @param expr
	 * @param val
	 * @param path
	 * @throws JSONException
	 * @throws ScriptException
	 * @see [类、类#方法、类#成员]
	 */
	private void trace(String expr, Object val, String path) throws JSONException, ScriptException {
		if (null != expr && !"".equals(expr)) {
			int idx = expr.indexOf(";");
			String loc = "";
			if (idx > -1) {
				loc = expr.substring(0, idx);
				expr = expr.substring(idx + 1);
			} else if (idx == -1) {
				loc = expr;
				expr = "";
			}
			int lc = 0;
			if (loc.matches("^\\d+$")) {
				lc = Integer.parseInt(loc);
			}

			if (val instanceof JSONObject && null != val && ((JSONObject) val).has(loc)) {
				JSONObject json = (JSONObject) val;
				trace(expr, json.get(loc), path + ";" + loc);
			} else if (val instanceof JSONArray && null != val && loc.matches("^\\d+$")
					&& ((JSONArray) val).length() > lc) {
				JSONArray json = (JSONArray) val;
				trace(expr, json.get(lc), path + ";" + lc);
			} else if ("*".equals(loc)) {
				walk(loc, expr, val, path, 1);
			} else if ("..".equals(loc)) {
				trace(expr, val, path + ";" + loc);
				walk(loc, expr, val, path, 2);
			} else if (loc.matches("^[\\d,]+$")) {
				String[] s = loc.split(",");
				for (String str : s) {
					trace(str + ";" + expr, val, path);
				}
			} else if (loc.matches("^\\(.*?\\)$")) {
				trace(eval(loc.replaceAll("^\\((.*?)\\)$", "$1"), val, path.substring(path.lastIndexOf(";") + 1)) + ";"
						+ expr, val, path);
			} else if (loc.matches("^\\?\\(.*?\\)$")) {
				walk(loc, expr, val, path, 3);
			} else if (loc.matches("^(-?[0-9]*):(-?[0-9]*):?([0-9]*)$")) {
				slice(loc, expr, val, path);
			} else if (val instanceof JSONArray && null != val && !path.endsWith(loc) && !path.endsWith("..")
					&& ((JSONArray) val).length() > lc) {
				walk(loc, loc, val, path, 1);
			}
		} else {
			store(path, val);
		}
	}

	/**
	 * 根据不同表达式变量结果集
	 * 
	 * @param loc
	 * @param expr
	 * @param val
	 * @param path
	 * @param f
	 * @throws JSONException
	 * @throws ScriptException
	 * @see [类、类#方法、类#成员]
	 */
	private <T> void walk(String loc, String expr, T val, String path, int f) throws JSONException, ScriptException {
		if (val instanceof JSONArray) {
			JSONArray jsary = (JSONArray) val;
			for (int i = 0, j = jsary.length(); i < j; i++) {
				switch (f) {
				case 1:
					trace(i + ";" + expr, val, path);
					break;
				case 2:
					Object json = jsary.get(i);
					if (null != json && (json instanceof JSONArray || json instanceof JSONObject)) {
						trace("..;" + expr, json, path + ";" + i);
					}
					break;

				default:
					String a = eval(loc.replaceAll("^\\?\\((.*?)\\)$", "$1"), jsary.get(i), i + "");
					if (null != a && "TRUE".equals(a.toUpperCase()))
						trace(i + ";" + expr, val, path);
					break;
				}
			}
		} else if (val instanceof JSONObject) {
			JSONObject json = (JSONObject) val;
			Iterator<String> it = json.keys();
			while (it.hasNext()) {
				String key = it.next();
				switch (f) {
				case 1:
					trace(key + ";" + expr, val, path);
					break;
				case 2:
					Object js = json.get(key);
					if (null != js && (js instanceof JSONArray || js instanceof JSONObject)) {
						trace("..;" + expr, js, path + ";" + key);
					}
					break;
				default:
					String a = eval(loc.replaceAll("^\\?\\((.*?)\\)$", "$1"), json.get(key), key);
					if (null != a && "TRUE".equals(a.toUpperCase()))
						trace(key + ";" + expr, val, path);
					break;
				}
			}
		}
	}

	/**
	 * 执行计步器表达式
	 * 
	 * @param loc
	 * @param expr
	 * @param val
	 * @param path
	 * @throws JSONException
	 * @throws ScriptException
	 * @see [类、类#方法、类#成员]
	 */
	private <T> void slice(String loc, String expr, T val, String path) throws JSONException, ScriptException {
		if (val instanceof JSONArray) {
			JSONArray jsay = (JSONArray) val;
			int len = jsay.length(), start = 0, end = len, step = 1;
			Matcher match = Pattern.compile("^(-?[0-9]*):(-?[0-9]*):?(-?[0-9]*)$").matcher(loc);
			if (match.find()) {
				String s1 = match.group(1);
				if (null != s1 && !"".equals(s1)) {
					start = Integer.parseInt(s1);
				}
				String s2 = match.group(2);
				if (null != s2 && !"".equals(s2)) {
					end = Integer.parseInt(s2);
				}
				String s3 = match.group(3);
				if (null != s3 && !"".equals(s3)) {
					step = Integer.parseInt(s3);
				}
			}
			start = (start < 0) ? Math.max(0, start + len) : Math.min(len, start);
			end = (end < 0) ? Math.max(0, end + len) : Math.min(len, end);
			for (int i = start; i < end; i += step)
				trace(i + ";" + expr, val, path);
		}
	}

	/**
	 * 执行js表达式 建议不要用，性能非常底。可以考虑第三方java表达式扩展工具
	 *
	 * @param loc
	 * @param val
	 * @param path
	 * @return
	 * @throws ScriptException
	 * @see [类、类#方法、类#成员]
	 */
	private <T> String eval(String loc, T val, String path) throws ScriptException {
		if (null != val && (val instanceof JSONArray || val instanceof JSONObject)) {
			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine se = manager.getEngineByName("js");
			String exp = "var _v = " + val.toString() + ";" + loc.replaceAll("@", "_v");
			if (se != null) {
				Object resu = se.eval(exp);
				String str = (null == resu) ? null : resu.toString();
				str = (null != str) ? (str.matches("^\\d+\\.\\d+$") ? str.substring(0, str.indexOf(".")) : str) : null;
				return str;
			}
		}
		return "";
	}

	/**
	 * 收集最终结果
	 * 
	 * @param path
	 * @param val
	 * @see [类、类#方法、类#成员]
	 */
	private void store(String path, Object val) {
		if (null != path && !"".equals(path)) {
			Object obj = ResultType.PATH.equals(resultType) ? asPath(path) : val;
			this.result.add(obj);
		}
	}

	/**
	 * 已查询表达式格式化
	 *
	 * @param path
	 *            已查询表达式
	 * @return
	 */
	private String asPath(String path) {
		String[] x = path.split(";");
		String p = "$";
		for (String str : x) {
			if (!str.matches("^(?:\\$|\\.\\.)$")) {
				p += "." + str;
			}
		}
		return p;
	}

	/**
	 * 获取结果集
	 *
	 * @return
	 */
	public List<Object> all() {
		return new ArrayList<Object>(result);
	}

	/**
	 * 获取字符串结果集
	 *
	 * @return
	 */
	public List<String> allStr() {
		List<String> list = new ArrayList<String>();
		for (Object obj : result) {
			list.add(obj.toString());
		}
		return list;
	}

	/**
	 * 获取字符串结果集
	 *
	 * @return
	 */
	public List<JsonPath> allJsonPath() {
		List<JsonPath> list = new ArrayList<JsonPath>();
		for (Object obj : result) {
			list.add(new JsonPath(obj));
		}
		return list;
	}

	/**
	 * 获取结果
	 *
	 * @return
	 */
	public Object get() {
		if (null != result && !result.isEmpty()) {
			return result.get(0);
		}
		return null;
	}

	/**
	 * 获取字符串结果
	 *
	 * @return
	 */
	public String getStr() {
		if (null != result && !result.isEmpty()) {
			return result.get(0).toString();
		}
		return null;
	}

	/**
	 * 表达式校验
	 *
	 * @param expr
	 *            表达式
	 * @param arg
	 *            结构类型
	 * @return
	 * @throws JSONException
	 * @throws ScriptException
	 */
	public boolean has(String expr) throws JSONException, ScriptException {
		query(expr);
		return !result.isEmpty();
	}

	/**
	 * 表达式校验
	 *
	 * @param expr
	 *            表达式
	 * @param arg
	 *            结构类型
	 * @return
	 * @throws JSONException
	 * @throws ScriptException
	 */
	public boolean has(String expr, ResultType arg) throws JSONException, ScriptException {
		query(expr, arg);
		return !result.isEmpty();
	}

	/**
	 * 表达式校验，执行表达式后再校验 json.$("$..V[*]").match()
	 *
	 * @return
	 */
	public boolean match() {
		return !result.isEmpty();
	}

}
