# cn1_ms_notifications

Microservicio de notificaciones para PrintWorks.

## Responsabilidades

- Consumir eventos `order.created` publicados por `ms_orders`
- Enviar correos de notificación de nuevos pedidos al administrador
- Adjuntar el PDF del comprobante generado por `ms_orders`
- Registrar auditoría de eventos de pedidos
- Manejo de errores mediante reintentos limitados y Dead Letter Queues (DLQ)

## Stack Tecnológico

- Java 21
- Spring Boot 3.4.11
- Spring AMQP (RabbitMQ)
- Resend Java SDK (envío de correos)
- Maven

## Configuración

### Variables de Entorno

- `RABBITMQ_HOST`: Host de RabbitMQ (default: localhost)
- `RABBITMQ_PORT`: Puerto AMQP (default: 5672)
- `RABBITMQ_USERNAME`: Usuario RabbitMQ (default: guest)
- `RABBITMQ_PASSWORD`: Contraseña RabbitMQ (default: guest)
- `RESEND_API_KEY`: API key de Resend (requerido)
- `RESEND_FROM_EMAIL`: Remitente del correo (default: onboarding@resend.dev)
- `NOTIFICATION_EMAIL`: Destinatario del correo (requerido)

## Colas y Exchanges

### Exchange Principal
- `printworks.events` (TOPIC)

### Colas
- `order.email.queue` - Recibe `order.created` para enviar correo al administrador
- `order.audit.queue` - Recibe `order.created` para auditoría de eventos

### Routing Keys
- `order.created` - Publicado por ms_orders

### DLQ
- `order.email.dlq` - Dead Letter Queue para errores en envío de correo
- `order.audit.dlq` - Dead Letter Queue para errores en auditoría

## Arquitectura

```
ms_orders
    |
    | order.created
    v
printworks.events (TOPIC)
    |
    +--> order.email.queue --> OrderEmailConsumer --> EmailService --> Resend API --> Correo + PDF
    |
    +--> order.audit.queue --> OrderAuditConsumer --> Log
```

## Política de Reintentos

- Máximo 3 intentos de procesamiento por mensaje
- Backoff de 2 segundos entre reintentos
- Después de agotar reintentos, el mensaje se envía a la DLQ
- ACK manual para garantizar procesamiento exitoso
- NACK con `requeue=false` para enviar a DLQ en caso de fallo definitivo

## Transporte del PDF

El PDF se recibe en Base64 dentro del evento `order.created` (generado por `ms_orders`). El microservicio no genera PDF, solo lo adjunta al correo de notificación.

## Ejecución

### Local

```bash
mvn spring-boot:run
```

Con variables de entorno:

```bash
export RESEND_API_KEY=re_xxxxxxxxx
export RESEND_FROM_EMAIL=onboarding@resend.dev
export NOTIFICATION_EMAIL=your-email@example.com
mvn spring-boot:run
```

### Docker

```bash
docker build -t ms-notifications .
docker run -p 8083:8083 \
  -e RABBITMQ_HOST=localhost \
  -e RABBITMQ_PORT=5672 \
  -e RESEND_API_KEY=re_xxxxxxxxx \
  -e RESEND_FROM_EMAIL=onboarding@resend.dev \
  -e NOTIFICATION_EMAIL=your-email@example.com \
  ms-notifications
```

## Pruebas

### Prueba de Integración con ms_orders

1. Levantar RabbitMQ
2. Levantar ms_orders
3. Levantar ms_notifications con variables de entorno de Resend
4. Crear un pedido vía API REST de ms_orders
5. Verificar que el evento `order.created` llega a `order.audit.queue` y `order.email.queue`
6. Verificar los logs de auditoría de OrderAuditConsumer
7. Verificar que se envía el correo con el PDF adjunto al administrador
8. Verificar que el PDF es válido y corresponde al pedido

### Prueba de DLQ

1. Provocar un error controlado en el procesamiento (ej: configuración inválida de Resend)
2. Verificar que se realizan los reintentos configurados
3. Verificar que después de agotar reintentos el mensaje llega a la DLQ correspondiente
4. Verificar desde RabbitMQ Management que el mensaje está en la DLQ

## Dependencias

No depende directamente de `ms_orders`. El contrato es el JSON del evento `OrderCreatedEvent`.
