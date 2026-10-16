package duoc.cn1.ms_notifications.config;

import duoc.cn1.ms_notifications.event.OrderCreatedEvent;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

	@Value("${rabbitmq.exchange.events}")
	private String eventsExchange;

	@Value("${rabbitmq.routing-key.order-created}")
	private String orderCreatedRoutingKey;

	@Value("${rabbitmq.queues.order-email}")
	private String orderEmailQueue;

	@Value("${rabbitmq.queues.order-audit}")
	private String orderAuditQueue;

	@Value("${rabbitmq.dlq.order-email}")
	private String orderEmailDlq;

	@Value("${rabbitmq.dlq.order-audit}")
	private String orderAuditDlq;

	@Bean
	public TopicExchange eventsExchange() {
		return new TopicExchange(eventsExchange, true, false);
	}

	@Bean
	public DirectExchange dlxExchange() {
		return new DirectExchange("printworks.dlx", true, false);
	}

	@Bean
	public Queue orderEmailQueue() {
		return QueueBuilder.durable(orderEmailQueue)
				.withArgument("x-dead-letter-exchange", "printworks.dlx")
				.withArgument("x-dead-letter-routing-key", orderEmailDlq)
				.build();
	}

	@Bean
	public Queue orderAuditQueue() {
		return QueueBuilder.durable(orderAuditQueue)
				.withArgument("x-dead-letter-exchange", "printworks.dlx")
				.withArgument("x-dead-letter-routing-key", orderAuditDlq)
				.build();
	}

	@Bean
	public Queue orderEmailDlq() {
		return QueueBuilder.durable(orderEmailDlq).build();
	}

	@Bean
	public Queue orderAuditDlq() {
		return QueueBuilder.durable(orderAuditDlq).build();
	}

	@Bean
	public Binding orderEmailBinding() {
		return BindingBuilder.bind(orderEmailQueue())
				.to(eventsExchange())
				.with(orderCreatedRoutingKey);
	}

	@Bean
	public Binding orderAuditBinding() {
		return BindingBuilder.bind(orderAuditQueue())
				.to(eventsExchange())
				.with(orderCreatedRoutingKey);
	}

	@Bean
	public Binding orderEmailDlqBinding() {
		return BindingBuilder.bind(orderEmailDlq())
				.to(dlxExchange())
				.with(orderEmailDlq);
	}

	@Bean
	public Binding orderAuditDlqBinding() {
		return BindingBuilder.bind(orderAuditDlq())
				.to(dlxExchange())
				.with(orderAuditDlq);
	}

	@Bean
	public MessageConverter messageConverter() {
		Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
		converter.setBeanClassLoader(this.getClass().getClassLoader());

		DefaultClassMapper classMapper = new DefaultClassMapper();
		Map<String, Class<?>> idClassMapping = new HashMap<>();
		idClassMapping.put("duoc.cn1.ms_orders.event.OrderCreatedEvent", OrderCreatedEvent.class);
		idClassMapping.put("duoc.cn1.ms_notifications.event.OrderCreatedEvent", OrderCreatedEvent.class);
		classMapper.setIdClassMapping(idClassMapping);
		converter.setClassMapper(classMapper);

		return converter;
	}

	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
																				MessageConverter messageConverter) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(messageConverter);
		factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

		RetryTemplate retryTemplate = new RetryTemplate();
		FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
		backOffPolicy.setBackOffPeriod(2000);
		retryTemplate.setBackOffPolicy(backOffPolicy);

		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts(3);
		retryTemplate.setRetryPolicy(retryPolicy);

		factory.setRetryTemplate(retryTemplate);

		return factory;
	}
}
