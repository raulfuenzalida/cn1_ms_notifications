package duoc.cn1.ms_notifications.consumer;

import com.rabbitmq.client.Channel;
import com.resend.core.exception.ResendException;
import duoc.cn1.ms_notifications.event.OrderCreatedEvent;
import duoc.cn1.ms_notifications.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEmailConsumerTest {

	@Mock
	private EmailService emailService;

	@Mock
	private Channel channel;

	private OrderEmailConsumer orderEmailConsumer;

	@BeforeEach
	void setUp() {
		orderEmailConsumer = new OrderEmailConsumer(emailService);
	}

	@Test
	void handleOrderCreated_Success() throws IOException, ResendException {
		OrderCreatedEvent event = OrderCreatedEvent.builder()
				.orderId(1L)
				.orderNumber("PW-000001")
				.customerName("Juan Pérez")
				.customerEmail("juan@email.cl")
				.total(new BigDecimal("25990"))
				.status("CREATED")
				.createdAt(LocalDateTime.now())
				.pdfBase64("base64pdfcontent")
				.build();

		long deliveryTag = 1L;

		orderEmailConsumer.handleOrderCreated(event, channel, deliveryTag);

		verify(emailService, times(1)).sendOrderNotification(event);
		verify(channel, times(1)).basicAck(deliveryTag, false);
		verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
	}

	@Test
	void handleOrderCreated_ServiceException() throws IOException, ResendException {
		OrderCreatedEvent event = OrderCreatedEvent.builder()
				.orderId(1L)
				.orderNumber("PW-000001")
				.customerName("Juan Pérez")
				.customerEmail("juan@email.cl")
				.total(new BigDecimal("25990"))
				.status("CREATED")
				.createdAt(LocalDateTime.now())
				.pdfBase64("base64pdfcontent")
				.build();

		long deliveryTag = 1L;

		doThrow(new RuntimeException("Service error")).when(emailService).sendOrderNotification(any());

		orderEmailConsumer.handleOrderCreated(event, channel, deliveryTag);

		verify(emailService, times(1)).sendOrderNotification(event);
		verify(channel, never()).basicAck(anyLong(), anyBoolean());
		verify(channel, times(1)).basicNack(deliveryTag, false, false);
	}
}
