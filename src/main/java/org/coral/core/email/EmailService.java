package org.coral.core.email;

import java.io.File;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.coral.core.queue.QueueHolder;

public class EmailService{
	public static final String TEMPLATE_NAME="mailTemplate.ftl";
	public static final String DEFAULT_ENCODING="utf-8";
	public static final String QUEUE_NAME="EMAIL_SENDER_QUEUE";

	
	/**
	 * 发送简单text邮件，不带附件的
	 * @param from
	 * @param to
	 * @param title
	 * @param content
	 */
	public void sendSimpleMail(String from,String[] to,String title,String content){
		BlockingQueue<Object[]> queue = QueueHolder.getQueue(QUEUE_NAME);
		queue.offer(new Object[]{from,to,title,content});
	}
	/**
	 * 发送带附件的text邮件
	 * @param from
	 * @param to
	 * @param title
	 * @param content
	 * @param attachments
	 * @param encoding
	 */
	public void sendMimeMail(String from,String[] to,String title,String content,Map<String,File> attachments,String encoding){
		BlockingQueue<Object[]> queue = QueueHolder.getQueue(QUEUE_NAME);
		queue.offer(new Object[]{from,to,title,content,attachments,encoding});
	}
	
	/**
	 * 发送带附件的html邮件
	 * @param from
	 * @param to
	 * @param title
	 * @param templateName
	 * @param context
	 * @param attachments
	 * @param encoding
	 */
	public void sendMimeMail(String from,String[] to,String title,String templateName,Map<String,Object> context,Map<String,File> attachments,String encoding){
		BlockingQueue<Object[]> queue = QueueHolder.getQueue(QUEUE_NAME);
		queue.offer(new Object[]{from,to,title,templateName,context,attachments,encoding});
	}

}
