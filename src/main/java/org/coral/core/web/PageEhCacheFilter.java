package org.coral.core.web;

import java.io.ByteArrayOutputStream;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;
import net.sf.ehcache.constructs.web.AlreadyCommittedException;
import net.sf.ehcache.constructs.web.AlreadyGzippedException;
import net.sf.ehcache.constructs.web.GenericResponseWrapper;
import net.sf.ehcache.constructs.web.Header;
import net.sf.ehcache.constructs.web.PageInfo;
import net.sf.ehcache.constructs.web.filter.FilterNonReentrantException;
import net.sf.ehcache.constructs.web.filter.SimplePageCachingFilter;

import org.coral.core.utils.UtilString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import com.google.common.net.HttpHeaders;

public class PageEhCacheFilter extends SimplePageCachingFilter {
	public static final String DEFAULT_CACHE_NAME = "PageCache";
	static AntPathMatcher matcher = new AntPathMatcher();
	private static final Logger log = LoggerFactory.getLogger(PageEhCacheFilter.class);
	private final static String FILTER_URL_PATTERNS = "patterns";
	private static String[] cacheURLs;

	@Override
	protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws AlreadyGzippedException, AlreadyCommittedException, FilterNonReentrantException,
			LockTimeoutException, Exception {
		if (cacheURLs == null) {
			String patterns = filterConfig.getInitParameter(FILTER_URL_PATTERNS);
			cacheURLs = UtilString.split(patterns, ",");
		}
		String url = request.getRequestURI();
		boolean flag = false;
		if (cacheURLs != null && cacheURLs.length > 0) {
			for (String cacheURL : cacheURLs) {
				if (matcher.match(cacheURL.trim(), url)) {
					flag = true;
					break;
				}
			}
		}
		// 如果包含我们要缓存的url 就缓存该页面，否则执行正常的页面转向
		if (flag) {
			super.doFilter(request, response, chain);
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	protected String calculateKey(HttpServletRequest httpRequest) {
		StringBuffer stringBuffer = new StringBuffer();
		// 屏蔽掉参数的区别
		stringBuffer.append(httpRequest.getMethod()).append(httpRequest.getRequestURI());// .append(httpRequest.getQueryString());
		return stringBuffer.toString();
	}

	private final VisitLog visitLog = new VisitLog();

	private static class VisitLog extends ThreadLocal<Boolean> {
		@Override
		protected Boolean initialValue() {
			return false;
		}

		public boolean hasVisited() {
			return get();
		}

		public void markAsVisited() {
			set(true);
		}

		public void clear() {
			super.remove();
		}
	}

	@Override
	protected PageInfo buildPageInfo(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain chain) throws Exception {
		// Look up the cached page
		final String key = calculateKey(request);
		PageInfo pageInfo = null;
		try {
			checkNoReentry(request);
			Element element = blockingCache.get(key);
			if (element == null || element.getObjectValue() == null) {
				try {
					// Page is not cached - build the response, cache it, and
					// send to client
					pageInfo = buildPage(request, response, chain);
					if (pageInfo.isOk()) {
						long timeToLiveSeconds = pageInfo.getTimeToLiveSeconds();
						if(timeToLiveSeconds>0){
							blockingCache.put(new Element(key, pageInfo,false,0,(int)timeToLiveSeconds));
						}else{
							blockingCache.put(new Element(key, null));
						}
					} else {
						blockingCache.put(new Element(key, null));
					}
				} catch (final Throwable throwable) {
					// Must unlock the cache if the above fails. Will be logged at Filter
					blockingCache.put(new Element(key, null));
					throw new Exception(throwable);
				}
			} else {
				pageInfo = (PageInfo) element.getObjectValue();
			}
		} catch (LockTimeoutException e) {
			// do not release the lock, because you never acquired it
			throw e;
		} finally {
			// all done building page, reset the re-entrant flag
			visitLog.clear();
		}
		return pageInfo;
	}

	@Override
	protected void checkNoReentry(final HttpServletRequest httpRequest) throws FilterNonReentrantException {
		String filterName = getClass().getName();
		if (visitLog.hasVisited()) {
			throw new FilterNonReentrantException("The request thread is attempting to reenter" + " filter "
					+ filterName + ". URL: " + httpRequest.getRequestURL());
		} else {
			// mark this thread as already visited
			visitLog.markAsVisited();
			if (log.isDebugEnabled()) {
				log.debug("Thread {}  has been marked as visited.", Thread.currentThread().getName());
			}
		}
	}

	@Override
	protected PageInfo buildPage(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws AlreadyGzippedException, Exception {
		// Invoke the next entity in the chain
		final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
		final GenericResponseWrapper wrapper = new GenericResponseWrapper(response, outstr);
		chain.doFilter(request, wrapper);
		wrapper.flush();
		
		long timeToLiveSeconds = 0;//blockingCache.getCacheConfiguration().getTimeToLiveSeconds();
		if(response.containsHeader(HttpHeaders.CACHE_CONTROL)){
			if (wrapper.getAllHeaders() != null && wrapper.getAllHeaders().size()>0) {
	            for(Header h:wrapper.getAllHeaders()){
	            	if(h.getType()==Header.Type.STRING && h.getName().equals(HttpHeaders.CACHE_CONTROL)){
	            		String v = h.getValue().toString();
	            		timeToLiveSeconds = Long.parseLong(v.substring(v.lastIndexOf("=")+1).trim());
	            		break;
	            	}
	            }
	        }
		}
		
		// Return the page info
		return new PageInfo(wrapper.getStatus(), wrapper.getContentType(), wrapper.getCookies(), outstr.toByteArray(),
				true, timeToLiveSeconds, wrapper.getAllHeaders());

	}

}
