package org.json.JsonPath;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.junit.Before;
import org.junit.Test;

public class TestXsoup {

	private String xp;

	@Before
	public void initBefore() {
		try {
			xp = Jsoup.connect("http://www.tudou.com/list/ich7.html").timeout(10000).ignoreContentType(true).execute()
					.body();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testXSoup() {
		XSoup xsoup = new XSoup(xp);
		System.out.println(xsoup.query("//title/text()").get());
		System.out.println(xsoup.query("title text()").get());
		System.out.println(xsoup.query("//a/@href").all());
		System.out.println(xsoup.query("a @href").all());
		System.out.println(xsoup.query("//ul[@class=\"info\"]//a/@href").all());
		System.out.println(xsoup.query("//ul[@class='info']//a/@href").all());
		System.out.println(xsoup.query("ul.info a @href").all());
		System.out.println(xsoup.query("ul[class=info] a @href").all());

		System.out.println(xsoup.query("//ul[@class='info']//a[1]/@href").all());
		System.out.println(xsoup.query("ul[class=info] a @href").all());
	}
}
