package duoc.cn1.ms_notifications.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailResponse;
import duoc.cn1.ms_notifications.event.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

	@Mock
	private Resend resend;

	@Mock
	private Emails emails;

	private EmailService emailService;

	@BeforeEach
	void setUp() {
		when(resend.emails()).thenReturn(emails);
		emailService = new EmailService(resend);
		org.springframework.test.util.ReflectionTestUtils.setField(emailService, "fromEmail", "from@test.com");
		org.springframework.test.util.ReflectionTestUtils.setField(emailService, "notificationEmail", "admin@test.com");
	}

	@Test
	void sendOrderNotification_Success() throws ResendException {
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

		CreateEmailResponse mockResponse = new CreateEmailResponse();
		mockResponse.setId("resend-id-123");

		when(emails.send(any())).thenReturn(mockResponse);

		emailService.sendOrderNotification(event);

		verify(emails, times(1)).send(any());
	}

	@Test
	void sendOrderNotification_ResendException() throws ResendException {
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

		when(emails.send(any())).thenThrow(new ResendException("API Error"));

		assertThrows(ResendException.class, () -> emailService.sendOrderNotification(event));

		verify(emails, times(1)).send(any());
	}
}
