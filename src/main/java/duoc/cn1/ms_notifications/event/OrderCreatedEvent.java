package duoc.cn1.ms_notifications.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

	private Long orderId;
	private String orderNumber;
	private String customerName;
	private String customerEmail;
	private BigDecimal total;
	private String status;
	private LocalDateTime createdAt;
	private String pdfBase64;
}
