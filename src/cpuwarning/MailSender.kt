package cpuwarning

import java.util.Date
import java.util.Properties
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.MimeMessage

// 在构造方法中提供第三方邮件服务器认证信息
class MailSender(val me: String, val password: String){
	// 此处以腾讯企业邮箱为例
	fun sendWarning(to: String, subject: String, content: String) {
		val props = Properties();
		// SMTP 邮件服务器名称
		props.put("mail.smtp.host", "smtp.exmail.qq.com")
		//	props.put("mail.smtp.port", "25")
		val session = Session.getInstance(props, null)

		try {
			val msg = MimeMessage(session);
			// 发件人
			msg.setFrom(me);
			// 收件人
			msg.setRecipients(Message.RecipientType.TO, to)
			// 邮件标题
			msg.setSubject(subject)
			// 发送日期
			msg.setSentDate(Date())
			// 正文
			msg.setText(content)
			// SMTP 邮件服务器认证账号&密码
			Transport.send(msg, me, password)
			println("已发送警报邮件至 $to : $subject -> $content")
		} catch (e: Throwable) {
			println("发送警报邮件失败: ${ e.message }")
			e.printStackTrace()
		}
	}

}