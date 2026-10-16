package duoc.cn1.ms_notifications.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import duoc.cn1.ms_notifications.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

	private final Resend resend;

	@Value("${resend.from}")
	private String fromEmail;

	@Value("${resend.notification-email}")
	private String notificationEmail;

	public void sendOrderNotification(OrderCreatedEvent event) throws ResendException {
		log.info("Preparando email para orderId={} orderNumber={}", event.getOrderId(), event.getOrderNumber());

		String subject = String.format("PrintWorks - Nuevo pedido %s", event.getOrderNumber());
		String fileName = String.format("pedido-%s.pdf", event.getOrderNumber());

		String html = String.format(
				"<h2>PrintWorks</h2>" +
				"<p>Se ha recibido un nuevo pedido.</p>" +
				"<ul>" +
				"<li><strong>Pedido:</strong> %s</li>" +
				"<li><strong>Cliente:</strong> %s</li>" +
				"<li><strong>Correo:</strong> %s</li>" +
				"<li><strong>Total:</strong> $%s</li>" +
				"<li><strong>Estado:</strong> %s</li>" +
				"<li><strong>Fecha:</strong> %s</li>" +
				"</ul>" +
				"<p>Se adjunta el comprobante del pedido.</p>",
				event.getOrderNumber(),
				event.getCustomerName(),
				event.getCustomerEmail(),
				event.getTotal(),
				event.getStatus(),
				event.getCreatedAt()
		);

		Attachment attachment = Attachment.builder()
				.content(event.getPdfBase64())
				.fileName(fileName)
				.build();

		CreateEmailOptions params = CreateEmailOptions.builder()
				.from(fromEmail)
				.to(notificationEmail)
				.subject(subject)
				.html(html)
				.attachments(attachment)
				.build();

		log.info("Enviando email mediante Resend orderId={} orderNumber={}", event.getOrderId(), event.getOrderNumber());
		CreateEmailResponse response = resend.emails().send(params);
		log.info("Email aceptado por Resend orderId={} orderNumber={} resendId={}",
				event.getOrderId(), event.getOrderNumber(), response.getId());
	}
}
