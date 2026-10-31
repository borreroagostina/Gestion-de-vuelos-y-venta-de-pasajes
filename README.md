# Gestión de vuelos y venta de pasajes

## Stack tecnológico

- Java 25
- Maven Wrapper
- Spring Boot 3.5.0
- Spring Data JPA
- MySQL 8
- Flyway
- JUnit 4
- Spotless
- Checkstyle
- Docker Compose

## Estructura del repositorio

- `src/main/java`: código de la aplicación
- `src/main/resources`: configuración y recursos de Spring Boot
- `src/main/resources/db/migration`: migraciones de Flyway
- `src/test/java`: pruebas automatizadas
- `migraciones/`: scripts SQL de inicialización del contenedor MySQL
- `.github/workflows/ci.yml`: pipeline de CI para GitHub Actions
- `docker-compose.yml`: entorno de base de datos local
- `pom.xml`: configuración de Maven y dependencias

## Estrategia de ramas

Se recomienda seguir una estrategia simple:

- `main`: rama principal estable
- `develop`: integración de nuevas funcionalidades
- `feature/<nombre>`: ramas de trabajo por funcionalidad
- `hotfix/<nombre>`: correcciones urgentes

## Requisitos previos

- Java 25
- Docker Desktop o Docker Engine
- Git

## Puesta en marcha desde cero

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd Gestion-de-vuelos-y-venta-de-pasajes
```

### 2. Levantar la base de datos

```bash
docker compose up -d
```

Esto levanta MySQL 8 con la base `baseAerolinea` y usuario `appAerolinea` usando las credenciales definidas en `docker-compose.yml`.

### 3. Configurar Java

Asegurate de tener JDK 25 instalado y exportado en `JAVA_HOME`.

```bash
export JAVA_HOME=/ruta/hacia/jdk-25
export PATH=$JAVA_HOME/bin:$PATH
```

### 4. Ejecutar la aplicación

```bash
./mvnw spring-boot:run
```

La aplicación arranca y Flyway ejecuta automáticamente las migraciones de `src/main/resources/db/migration`.

## Cuentas internas de prueba

Luego de correr las migraciones (arranque normal de la app), quedan creadas estas cuentas activas:

- Administrador: `admin@aerolinea.local` / `admin123`
- Empleado de mostrador: `empleado@aerolinea.local` / `emp123`

El administrador puede consultar el reporte de ocupación desde **Reporte de ocupación** en su panel (`/admin/reportes/ocupacion`). El reporte permite filtrar por vuelo, clase y rango de fechas de salida; se descarga en formato CSV con el mismo conjunto de filtros.

### 5. Verificar la calidad del código

```bash
./mvnw test
./mvnw spotless:apply
./mvnw verify
```

### 6. Ejecutar la build completa en CI

La pipeline de GitHub Actions ya corre `./mvnw -B verify` en cada push o pull request.

## Migraciones

Las migraciones viven en:

```text
src/main/resources/db/migration
```

Convención:

- `V1__create_schema.sql`
- `V2__add_new_table.sql`
- `V3__insert_seed_data.sql`

Flyway las ejecuta en orden numérico y guarda el historial en la base de datos para evitar ejecuciones duplicadas.

## Variables de entorno

Copiá `.env.example` a `.env` en la raíz del proyecto y mantené las claves reales únicamente en ese archivo local. En PowerShell:

```powershell
Copy-Item .env.example .env
```

Los precios de los vuelos se cargan y cobran en dólares estadounidenses (USD); asegurate de ingresar en USD los importes configurados para Economy y Primera Clase. Para probar Stripe Checkout, creá una cuenta Stripe en modo test y configurá `STRIPE_SECRET_KEY` con una clave secreta `sk_test_...`. No uses una clave publicable (`pk_test_...`) en su lugar: el servidor necesita la clave secreta para crear y consultar sesiones. El checkout redirige a Stripe Checkout alojado; la plataforma no recibe ni almacena datos de tarjeta.

Para recibir webhooks en localhost, instalá Stripe CLI, iniciá sesión y ejecutá:

```bash
stripe listen --forward-to localhost:8080/api/pagos/stripe/webhook
```

La CLI imprime un secreto de webhook `whsec_...`; guardalo como `STRIPE_WEBHOOK_SECRET` en `.env` y reiniciá la aplicación. Dejá `APP_BASE_URL=http://localhost:8080` para los retornos del navegador cuando ejecutes localmente. Mantené Stripe CLI activo mientras probás para recibir las confirmaciones. El retorno del navegador también consulta el estado directamente a Stripe; el webhook permite procesar la confirmación aunque el comprador no vuelva a la aplicación. Las sesiones de Checkout de Stripe tienen un vencimiento mínimo mayor que el bloqueo temporal de asientos; por eso el backend intenta expirar la sesión al vencer la reserva y valida nuevamente el vencimiento antes de consolidar la venta. Si llega tarde una confirmación aprobada, se inicia el flujo de reembolso.

No agregues `.env` al repositorio ni reutilices claves que hayan sido subidas a Git, incluso si son de prueba: revocalas desde Stripe y generá otras nuevas.

Flyway aplica `V11__create_payment_orders_and_email_outbox.sql` para registrar órdenes, transacciones externas y envíos de correo pendientes. El worker reintenta automáticamente los emails fallidos; asegurate de configurar las variables SMTP para que los envíos puedan completarse.

Las pruebas automatizadas usan respuestas simuladas de Stripe y no realizan cobros reales. Para la prueba manual, usá Stripe Checkout en modo test y sus tarjetas de prueba oficiales (por ejemplo, `4242 4242 4242 4242` para aprobación); Stripe CLI permite probar la entrega del webhook localmente. Nunca uses tarjetas reales en modo test.

## Comandos útiles

```bash
./mvnw clean test
./mvnw spotless:apply
./mvnw verify
./mvnw spring-boot:run
docker compose up -d
docker compose down -v
```

## Observaciones

- El proyecto quedó preparado para que cualquier integrante pueda levantarlo sin instalar Maven global.
- La base de datos se inicializa con Docker, y Flyway se encarga de mantener el esquema versionado.
- La validación automática de formato, lint y pruebas queda activada en la pipeline de CI.
