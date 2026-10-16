package duoc.cn1.ms_notifications.consumer;

import com.rabbitmq.client.Channel;
import duoc.cn1.ms_notifications.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OrderAuditConsumer {

	@RabbitListener(queues = "${rabbitmq.queues.order-audit}")
	public void handleOrderCreated(
			@Payload OrderCreatedEvent event,
			Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

		try {
			log.info("ORDER_CREATED_AUDIT | orderId={} | orderNumber={}", event.getOrderId(), event.getOrderNumber());
			log.info("Cliente: {} | Email: {}", event.getCustomerName(), event.getCustomerEmail());
			log.info("Total: ${} | Estado: {}", event.getTotal(), event.getStatus());
			log.info("Fecha de creación: {}", event.getCreatedAt());

			channel.basicAck(deliveryTag, false);
			log.info("ACK enviado para order.audit.queue - orderId={} orderNumber={}",
					event.getOrderId(), event.getOrderNumber());

		} catch (Exception e) {
			log.error("Error procesando evento de auditoría para orderId={} orderNumber={}: {}",
					event.getOrderId(), event.getOrderNumber(), e.getMessage(), e);
			channel.basicNack(deliveryTag, false, false);
			log.info("NACK enviado para order.audit.queue - mensaje enviado a DLQ - orderId={} orderNumber={}",
					event.getOrderId(), event.getOrderNumber());
		}
	}
}
