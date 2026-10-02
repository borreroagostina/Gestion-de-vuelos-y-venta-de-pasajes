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

Usá `.env.example` como referencia. Si querés configurar variables locales, creá un archivo `.env` con tus valores.

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
