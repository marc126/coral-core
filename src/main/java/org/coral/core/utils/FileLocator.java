package org.coral.core.utils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.coral.core.exception.BizException;

/**
 * 配置文件的查找
 * 
 */
public class FileLocator {

	public String getConfFile(String fileName) {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		if (classLoader == null) {
			classLoader = getClass().getClassLoader();
		}
		URL confURL = classLoader.getResource(fileName);
		if (confURL == null)
			confURL = classLoader.getResource("META-INF/" + fileName);
		if (confURL == null) {

			System.err.println(" in classpath can't  locate file: " + fileName);
			return null;
		} else {
			File file1 = new File(confURL.getFile());
			if (file1.isFile()) {
				System.out.println(" locate file: " + confURL.getFile());
				return confURL.getFile();
			} else {
				System.err.println(" it is not a file: " + confURL.getFile());
				return null;
			}
		}
	}

	public InputStream getConfStream(String fileName) {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		if (classLoader == null) {
			classLoader = this.getClass().getClassLoader();
		}
		InputStream stream = classLoader.getResourceAsStream(fileName);
		if (stream == null)
			stream = classLoader.getResourceAsStream("META-INF/" + fileName);
		if (stream == null) {
			System.err.println("PropsUtil error: cann't find config file:-->"
					+ fileName);
		}
		return stream;
	}

	public URL getURL(String location) {
		String fullLocation = location;

		URL url = null;

		try {
			url = new URL(fullLocation);
		} catch (java.net.MalformedURLException e) {
			throw new BizException(e);
		}
		if (url == null) {
			throw new BizException(new Throwable("读取配置文件为空"));
		}

		return url;
	}

	public InputStream loadResource(String location){
		URL url = getURL(location);

		try {
			return url.openStream();
		} catch (java.io.IOException e) {
			throw new BizException(e);
		}
	}
}
