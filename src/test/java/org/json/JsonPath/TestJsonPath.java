package org.json.JsonPath;

import java.io.IOException;
import java.util.List;

import javax.script.ScriptException;

import org.json.JSONException;
import org.json.JsonPath.JsonPath.ResultType;
import org.jsoup.Jsoup;
import org.junit.Before;
import org.junit.Test;

public class TestJsonPath {

	private String jsonString;

	@Before
	public void initJsonStr() {
		try {
			jsonString = Jsoup
					.connect(
							"http://www.tudou.com/list/itemData.action?tagType=1&firstTagId=7&areaCode=&tags=&initials=&hotSingerId=&page=2&sort=2&key=")
					.timeout(10000).ignoreContentType(true).execute().body();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testJson() throws JSONException, ScriptException {
		JsonPath json = new JsonPath(jsonString);
		System.out.println(json.query("$..data").get());
		System.out.println(json.query("$..data[*]").getStr());
		System.out.println(json.query("$..data.title", ResultType.VALUE).getStr());
		System.out.println(json.query("$..data.title", ResultType.VALUE).all());
		System.out.println(json.query("$..bigPicUrl", ResultType.VALUE).allStr());
		List<JsonPath> list = json.query("$..data[*]", ResultType.VALUE).allJsonPath();
		for (JsonPath jsonPath : list) {
			System.out.println(jsonPath.get());
			System.out.println(jsonPath.query("$..title").getStr());
			System.out.println(jsonPath.query("$..bigPicUrl").getStr());
		}
	}

}
