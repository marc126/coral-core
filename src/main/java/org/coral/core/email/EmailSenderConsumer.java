package org.coral.core.email;

import java.io.File;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.coral.core.queue.BlockingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class EmailSenderConsumer extends BlockingConsumer {

	private static final Logger log = LoggerFactory.getLogger(EmailSenderConsumer.class);
	@Autowired
	protected JavaMailSender mailSender;
	@Autowired
	protected Configuration freemarkerConfiguration;

	
	/**
	 * 发送简单text邮件，不带附件的
	 * @param from
	 * @param to
	 * @param title
	 * @param content
	 */
	public void sendSimpleMail(String from,String[] to,String title,String content){
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setFrom(from);
		msg.setTo(to);
		msg.setSubject(title);
		msg.setText(content);
		try{
			mailSender.send(msg);
		}catch(Exception e){
			log.error("发送邮件失败！",e);
		}
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
		try{
			MimeMessage msg = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(msg,true,encoding);
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject(title);
			helper.setText(content);
			for(String key:attachments.keySet()){
				helper.addAttachment(key, attachments.get(key));
			}
			mailSender.send(msg);
		}catch(Exception e){
			log.error("发送邮件失败！",e);
		}
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
		try{
			Template template = freemarkerConfiguration.getTemplate(templateName,encoding);
			MimeMessage msg = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(msg,true,encoding);
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject(title);
			helper.setText(FreeMarkerTemplateUtils.processTemplateIntoString(template, context),true);
			for(String key:attachments.keySet()){
				helper.addAttachment(key, attachments.get(key));
			}
			mailSender.send(msg);
		}catch(Exception e){
			log.error("发送邮件失败！",e);
		}
	}
	@Override
	protected void processMessage(Object message) {
		Object[] param = (Object[]) message;
		if(param.length==4){
			sendSimpleMail((String)param[0],(String[])param[1],(String)param[2],(String)param[3]);
		}else if(param.length==6){
			sendMimeMail((String)param[0],(String[])param[1],(String)param[2],(String)param[3],(Map<String,File>)param[4],(String)param[5]);
		}else if(param.length==7){
			sendMimeMail((String)param[0],(String[])param[1],(String)param[2],(String)param[3],(Map<String,Object>)param[4],(Map<String,File>)param[5],(String)param[6]);
		}
	}
	@Override
	protected void clean() {
	}

}
