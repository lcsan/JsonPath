package org.json.JsonPath;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * 基于jsoup的伪xpath表达式支持。 可以直接嵌套css表达式或者直接使用css表达式。
 * 
 * css表达式:http://www.w3school.com.cn/cssref/css_selectors.asp xsoup表达式：
 *
 * @author lcsan
 * @version [版本号, 2016年3月23日]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class XSoup {

	private Object obj;

	private List<Object> result = new ArrayList<Object>();

	@SuppressWarnings("unchecked")
	public XSoup(Object em) {
		if (null == em) {
			return;
		}
		if (em instanceof Element) {
			this.obj = em;
			this.result.add(em);
		} else if (em instanceof Elements) {
			this.obj = em;
			Elements ems = (Elements) em;
			// 解决elements是list结果为1的bug
			for (Element e : ems) {
				this.result.add(e);
			}
		} else if (em instanceof List) {
			this.result.addAll((Collection<? extends Object>) em);
		} else if (em instanceof String) {
			this.obj = Jsoup.parse((String) em);
			this.result.add(obj);
		} else {
			throw new IllegalArgumentException(
					"Parameter object are not extends from the element or elements or Collection");
		}
	}

	/**
	 * 表达式转换
	 * 
	 * @param jq
	 * @return
	 * @see [类、类#方法、类#成员]
	 */
	private String XpathToJquery(String jq) {
		if (null != jq && !"".equals(jq.trim()))

		{
			String end = getMatchStr(
					"/(html\\(\\)|outerhtml\\(\\)|text\\(\\)|alltext\\(\\)|val\\(\\)|regex\\(.*?\\)|replace\\(.*?\\)|concat\\(.*?\\))$",
					jq.toLowerCase());
			String start = jq;
			if (null != end && end.trim().length() > 0) {
				int index = jq.toLowerCase().indexOf(end);
				if (index > -1) {
					start = jq.substring(0, index);
				}
			}
			jq = start.replaceAll("//", " ").replaceAll("/", " > ").replaceAll("(\\S*=)\\s*", "$1")
					.replaceAll("\\[position\\(\\)\\s*=\\s*(\\d+)\\]", ":eq($1)")
					.replaceAll("\\[position\\(\\)\\s*<\\s*(\\d+)\\]", ":lt($1)")
					.replaceAll("\\[position\\(\\)\\s*>\\s*(\\d+)\\]", ":gt($1)")
					.replaceAll("\\[(\\d+)\\]", ":nth-of-type($1)")
					.replaceAll("(?:\\[last\\(\\)\\]|\\[position\\(\\)\\s*=\\s*last\\(\\)\\])", ":last-child")
					.replaceAll("\\[@", "[").trim() + " " + end;
			return jq.replaceAll(">\\s*$", "").trim();
		} else {
			return null;
		}
	}

	/**
	 * 执行Jsoup相关类的select方法
	 * 
	 * @param query
	 * @return
	 * @see [类、类#方法、类#成员]
	 */
	@SuppressWarnings("rawtypes")
	private XSoup select(String query) {
		if (null != query && !"".equals(query)) {
			Class[] parameterTypes = new Class[] { String.class };
			try {
				return new XSoup(invokeMethod("select", parameterTypes, query));
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	/**
	 * 执行obj的相关方法
	 * 
	 * @param methName
	 * @param parameterTypes
	 * @param args
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @see [类、类#方法、类#成员]
	 */
	@SuppressWarnings("rawtypes")
	private Object invokeMethod(String methName, Class[] parameterTypes, Object... args) throws NoSuchMethodException,
			SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method;
		method = obj.getClass().getMethod(methName, parameterTypes);
		return method.invoke(obj, args);
	}

	/**
	 * 获取匹配地址，默认获取第一个地址
	 * 
	 * @param reg
	 * @param data
	 * @return
	 * @see [类、类#方法、类#成员]
	 */
	private String getMatchStr(String reg, String data) {
		String result = "";
		if (null != data) {
			Matcher m = Pattern.compile(reg).matcher(data);
			if (m.find()) {
				result = m.group(1);
			}
		}
		return result;
	}

	/**
	 * 正则提取
	 * 
	 * @param reg
	 * @param data
	 * @return
	 * @see [类、类#方法、类#成员]
	 */
	private List<Object> regex(String reg, Integer... data) {
		List<Object> list = new ArrayList<Object>();
		if (null != obj) {
			Matcher m = Pattern.compile(reg).matcher(obj.toString());
			while (m.find()) {
				int j = m.groupCount();
				if (j > 1) {
					List<String> result = new ArrayList<String>();
					for (int i = 1; i <= j; i++) {
						result.add(m.group(i));
					}
					list.add(result);
				} else {
					list.add(m.group(j));
				}
			}
		}
		if (data.length > 0) {
			List<Object> result = new ArrayList<Object>();
			for (int dt : data) {
				if (dt <= list.size() && dt > 0) {
					result.add(list.get(dt - 1));
				}
			}
			return result;
		} else {
			return list;
		}
	}

	private String concat(String reg, Integer... data) {
		List<Object> list = regex(reg, data);
		StringBuffer buf = new StringBuffer();
		for (Object str : list) {
			buf.append(str);
		}
		return buf.toString();
	}

	private String replace(String regex, String replacement) {
		return obj.toString().replaceAll(regex, replacement);
	}

	private Integer[] parseParam(String arg) {
		List<Integer> list = new ArrayList<Integer>();
		if (null != arg) {
			String[] str = arg.split(",");
			for (String s : str) {
				s = s.trim();
				if (s.matches("^[0-9]+$")) {
					list.add(Integer.parseInt(s));
				}
			}
		}
		return list.toArray(new Integer[] {});
	}

	/**
	 * 特殊获取值扩展
	 * 
	 * @param qy
	 * @return
	 * @see [类、类#方法、类#成员]
	 */
	@SuppressWarnings("rawtypes")
	private Object getValue(String qy) {
		Class[] parameterTypes = new Class[] {};
		try {
			if (qy.startsWith("@")) {
				parameterTypes = new Class[] { String.class };
				return invokeMethod("attr", parameterTypes, qy.substring(1));
			} else if ("html()".equals(qy.toLowerCase())) {
				return invokeMethod("html", parameterTypes);
			} else if ("outerhtml()".equals(qy.toLowerCase())) {
				return invokeMethod("outerHtml", parameterTypes);
			} else if ("alltext()".equals(qy.toLowerCase())) {
				return invokeMethod("text", parameterTypes);
			} else if ("text()".equals(qy.toLowerCase())) {
				if (obj instanceof Element) {
					Element em = (Element) obj;
					// 复制对象，防止指针应用造成对象改变
					Element temp = em.clone();
					Elements ems = em.children();
					// 临时删除子节点，再获取其text
					for (Element element : ems) {
						element.remove();
					}
					Object re = invokeMethod("text", parameterTypes);
					// 还原obj对象
					obj = temp;
					return re;
				}
			} else if ("val()".equals(qy.toLowerCase())) {
				return invokeMethod("val", parameterTypes);
			} else if (qy.startsWith("regex")) {
				String arg1 = getMatchStr("['\"](.*?)['\"](?:,[,0-9]+\\)$|\\)$)", qy);
				String arg2 = getMatchStr("['\"]([,0-9]+)\\)$", qy);
				return regex(arg1, parseParam(arg2));
			} else if (qy.startsWith("replace")) {
				String arg1 = getMatchStr("['\"](.*?)['\"],['\"]", qy);
				String arg2 = getMatchStr("['\"],['\"](.*?)['\"]\\)", qy);
				return replace(arg1, arg2);
			} else if (qy.startsWith("concat")) {
				String arg1 = getMatchStr("['\"](.*?)['\"](?:,[,0-9]+\\)$|\\)$)", qy);
				String arg2 = getMatchStr("['\"]([,0-9]+)\\)", qy);
				return concat(arg1, parseParam(arg2));
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 遍历执行表达式
	 * 
	 * @param qy
	 * @return
	 * @see [类、类#方法、类#成员]
	 */
	@SuppressWarnings("unchecked")
	private List<Object> each(String qy) {
		if (null != obj && obj instanceof Elements) {
			Elements em = (Elements) obj;
			result.clear();
			for (Element element : em) {
				Object a = new XSoup(element).getValue(qy);
				if (null != a) {
					if (a instanceof List) {
						result.addAll((Collection<? extends Object>) a);
					} else {
						result.add(a);
					}
				}
			}
		}
		return result;
	}

	/**
	 * 执行表达式
	 * 
	 * @param query
	 * @return
	 * @see [类、类#方法、类#成员]
	 */
	public XSoup query(String query) {
		query = XpathToJquery(query);
		String sel = query, qy = getMatchStr("(\\S+)$", query);
		boolean flag = false;
		int index = 0;
		if (null != query && null != qy) {
			index = query.lastIndexOf(qy);
			if (index > -1 && null != qy && qy.toLowerCase().matches(
					"^>?(?:@.*?|html\\(\\)|outerhtml\\(\\)|text\\(\\)|alltext\\(\\)|val\\(\\)|regex\\(.*?\\)|replace\\(.*?\\)|concat\\(.*?\\))$")) {
				flag = true;
				qy = qy.replaceAll("^>", "");
				sel = query.substring(0, index).replaceAll(">\\s*$", "").trim();
			}
		}
		if (null != sel) {
			sel = sel.replaceAll("\"|'", "");
		}
		XSoup xs = select(sel);
		if (flag) {
			xs = new XSoup(xs.each(qy));
		}
		return xs;
	}

	public Object get() {
		if (!result.isEmpty()) {
			return result.get(0);
		}
		return null;
	}

	public String getStr() {
		if (!result.isEmpty()) {
			return result.get(0).toString();
		}
		return null;
	}

	public List<Object> all() {
		return result;
	}

	public List<String> allStr() {
		List<String> list = new ArrayList<String>();
		for (Object ob : result) {
			list.add(ob.toString());
		}
		return list;
	}

}
