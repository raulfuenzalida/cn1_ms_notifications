package duoc.cn1.ms_notifications.consumer;

import com.rabbitmq.client.Channel;
import com.resend.core.exception.ResendException;
import duoc.cn1.ms_notifications.event.OrderCreatedEvent;
import duoc.cn1.ms_notifications.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEmailConsumer {

	private final EmailService emailService;

	@RabbitListener(queues = "${rabbitmq.queues.order-email}")
	public void handleOrderCreated(
			@Payload OrderCreatedEvent event,
			Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

		try {
			log.info("Recibido order.created para enviar email - orderId={} orderNumber={}",
					event.getOrderId(), event.getOrderNumber());

			emailService.sendOrderNotification(event);

			channel.basicAck(deliveryTag, false);
			log.info("ACK enviado para order.email.queue - orderId={} orderNumber={}",
					event.getOrderId(), event.getOrderNumber());

		} catch (ResendException e) {
			log.error("Error enviando email mediante Resend para orderId={} orderNumber={}: {}",
					event.getOrderId(), event.getOrderNumber(), e.getMessage(), e);
			channel.basicNack(deliveryTag, false, false);
			log.info("NACK enviado para order.email.queue - mensaje enviado a DLQ - orderId={} orderNumber={}",
					event.getOrderId(), event.getOrderNumber());
		} catch (Exception e) {
			log.error("Error procesando evento de email para orderId={} orderNumber={}: {}",
					event.getOrderId(), event.getOrderNumber(), e.getMessage(), e);
			channel.basicNack(deliveryTag, false, false);
			log.info("NACK enviado para order.email.queue - mensaje enviado a DLQ - orderId={} orderNumber={}",
					event.getOrderId(), event.getOrderNumber());
		}
	}
}
