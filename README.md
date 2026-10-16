# Transacciones Service — Reversión segura de transacciones

**Autor:** Wilmer Yesid Suarez Morales (`@author YesidSuarez`)
**Rol:** Desarrollador Backend Java
**Tecnologías:** Java 17 · Spring Boot 3.3 · Spring Data JPA · H2 · JUnit 5 · Mockito · Maven

---

## 1. ¿En qué consiste este desarrollo?

Este proyecto resuelve la **Parte 2 — Laboratorio práctico** de la prueba técnica:
*"Reversión segura de transacciones con reglas de negocio y control de concurrencia"*.

Se parte del esqueleto entregado (`Transaccion`, `TransaccionRepository`, `TransaccionService` y
`TransaccionController`) y se implementa el endpoint:

```
PATCH /transacciones/{id}/reversar
```

que cambia el estado de una transacción a `REVERSADA` cumpliendo estas reglas:

| # | Regla | Respuesta si se incumple |
|---|-------|--------------------------|
| 1 | Solo se reversa una transacción cuyo estado actual sea **APROBADA** | `409 Conflict` con mensaje claro |
| 2 | Solo se reversa dentro de las **24 horas** siguientes a `fechaCreacion` | `422 Unprocessable Entity` |
| 3 | Dos reversiones **simultáneas** sobre la misma transacción no pueden tener éxito ambas | `409 Conflict` (bloqueo optimista con `@Version`) |
| — | La transacción no existe | `404 Not Found` |
| ✓ | Reversión válida: estado → `REVERSADA` y se persiste | `204 No Content` |

---

## 2. Diseño y buenas prácticas aplicadas

### Estructura del proyecto

```
src/main/java/com/yesidsuarez/transacciones
├── TransaccionesApplication.java        → Clase principal Spring Boot
├── domain/
│   ├── Transaccion.java                 → Entidad JPA con @Version (bloqueo optimista)
│   └── EstadoTransaccion.java           → Enum de estados
├── repository/
│   └── TransaccionRepository.java       → JpaRepository
├── service/
│   └── TransaccionService.java          → Reglas de negocio de la reversión
├── controller/
│   ├── TransaccionController.java       → Endpoints REST (solo delega)
│   └── TransaccionResponse.java         → DTO de salida (no expone la entidad)
├── exception/
│   ├── NegocioException.java            → Excepción base: cada una sabe su código HTTP
│   ├── TransaccionNoEncontradaException.java          → 404
│   ├── EstadoInvalidoParaReversionException.java      → 409
│   ├── PlazoReversionExpiradoException.java           → 422
│   ├── TransaccionModificadaConcurrentementeException.java → 409
│   ├── ApiErrorResponse.java            → Formato estándar de error
│   └── GlobalExceptionHandler.java      → @RestControllerAdvice centralizado
└── config/
    ├── AppConfig.java                   → Bean Clock (hora inyectable)
    ├── ReversionProperties.java         → Plazo configurable (24h por defecto)
    └── DatosInicialesConfig.java        → Datos de ejemplo para probar
```

### Principios SOLID

- **S — Responsabilidad única:** el controlador solo recibe y delega; el servicio aplica reglas;
  el manejador global traduce errores a HTTP; el repositorio solo accede a datos.
- **O — Abierto/Cerrado:** cada excepción de negocio extiende `NegocioException` y declara su
  propio código HTTP. Agregar una regla nueva no obliga a modificar el `GlobalExceptionHandler`.
- **L — Sustitución de Liskov:** cualquier subclase de `NegocioException` puede usarse donde se
  espera la base y el manejador responde correctamente.
- **I — Segregación de interfaces:** `TransaccionRepository` solo hereda lo que necesita de
  `JpaRepository`; el DTO `TransaccionResponse` expone solo los datos necesarios.
- **D — Inversión de dependencias:** inyección por constructor; el servicio depende de un `Clock`
  inyectado (no de `LocalDateTime.now()`), lo que permite probar la regla de 24 horas con una hora fija.

### ¿Cómo se controla la concurrencia?

La entidad tiene el campo `@Version`. Cuando JPA actualiza la fila ejecuta:

```sql
UPDATE transaccion SET estado = 'REVERSADA', version = 1 WHERE id = ? AND version = 0
```

Si dos solicitudes leen la misma transacción (versión 0) al mismo tiempo, la primera actualiza y
sube la versión a 1; la segunda no encuentra la fila con versión 0 y Spring lanza
`ObjectOptimisticLockingFailureException`. El servicio usa `saveAndFlush` para detectar el
conflicto en ese mismo punto y lo traduce a `TransaccionModificadaConcurrentementeException`
→ **409 Conflict**. Así **nunca** ambas reversiones tienen éxito silenciosamente.

### Formato de error

```json
{
  "timestamp": "2026-10-16T12:00:00.123",
  "status": 409,
  "error": "Conflict",
  "mensaje": "La transacción 3 no puede reversarse porque su estado actual es PENDIENTE. Solo se pueden reversar transacciones en estado APROBADA.",
  "path": "/transacciones/3/reversar"
}
```

---

## 3. Requisitos para ejecutarlo

- **JDK 17 o superior** → verificar con `java -version`
- **Maven 3.8+** → verificar con `mvn -version`
  (o abrir el proyecto en IntelliJ IDEA / Eclipse / VS Code, que traen Maven integrado)

No se necesita instalar base de datos: usa **H2 en memoria**.

---

## 4. Cómo ejecutar el proyecto (paso a paso)

```bash
# 1. Clonar el repositorio
git clone https://github.com/<TU_USUARIO>/transacciones-service.git
cd transacciones-service

# 2. Compilar y ejecutar todas las pruebas
mvn clean test

# 3. Levantar la aplicación
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

### Datos de ejemplo cargados al iniciar

| id | Estado | Creada hace | Resultado esperado al reversar |
|----|--------|-------------|-------------------------------|
| 1 | APROBADA | 2 horas | ✅ `204 No Content` |
| 2 | APROBADA | 30 horas | ❌ `422 Unprocessable Entity` (plazo vencido) |
| 3 | PENDIENTE | 1 hora | ❌ `409 Conflict` (estado inválido) |
| 4 | RECHAZADA | 3 horas | ❌ `409 Conflict` (estado inválido) |
| 5 | APROBADA | 23 horas | ✅ `204 No Content` (útil para probar concurrencia) |

### Probar con curl (o Postman)

```bash
# Ver todas las transacciones
curl http://localhost:8080/transacciones

# Caso exitoso -> 204
curl -i -X PATCH http://localhost:8080/transacciones/1/reversar

# Verificar que quedó REVERSADA
curl http://localhost:8080/transacciones/1

# Reversar otra vez la misma -> 409 (ya no está APROBADA)
curl -i -X PATCH http://localhost:8080/transacciones/1/reversar

# Plazo vencido -> 422
curl -i -X PATCH http://localhost:8080/transacciones/2/reversar

# Estado PENDIENTE -> 409
curl -i -X PATCH http://localhost:8080/transacciones/3/reversar

# No existe -> 404
curl -i -X PATCH http://localhost:8080/transacciones/999/reversar

# Concurrencia: dos solicitudes simultáneas -> una 204 y la otra 409
curl -s -o /dev/null -w "%{http_code}\n" -X PATCH http://localhost:8080/transacciones/5/reversar & \
curl -s -o /dev/null -w "%{http_code}\n" -X PATCH http://localhost:8080/transacciones/5/reversar & wait
```

> En Windows (PowerShell) use `curl.exe` en lugar de `curl`, o importe las peticiones en Postman.

### Consola de base de datos H2

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:transaccionesdb` · Usuario: `sa` · Contraseña: *(vacía)*

---

## 5. Pruebas automatizadas

```bash
mvn test
```

| Clase de prueba | Tipo | Qué valida |
|-----------------|------|------------|
| `TransaccionServiceTest` | Unitaria (Mockito) | **Regla de 24 horas** (dentro del plazo, límite exacto, fuera del plazo), **conflicto de estado** para CREADA/PENDIENTE/RECHAZADA/REVERSADA, transacción inexistente y traducción del bloqueo optimista |
| `TransaccionControllerTest` | Web (`@WebMvcTest`) | Que cada excepción se responda con el código HTTP correcto (204, 404, 409, 422) |
| `ReversionConcurrenteIntegrationTest` | Integración (Spring + JPA + H2) | Que `@Version` rechace una copia desactualizada y que, con **dos hilos simultáneos**, solo una reversión tenga éxito |

La regla de 24 horas se prueba con un `Clock` fijo, así el resultado no depende de la hora en que
se ejecute la prueba.

---

## 6. Configuración

El plazo de reversión es configurable en `src/main/resources/application.yml`:

```yaml
transacciones:
  reversion:
    plazo-maximo: 24h
```

---

## 7. Cómo subir este proyecto a GitHub

### Opción A — Desde la página web de GitHub (sin usar comandos)

1. Ingrese a <https://github.com> e inicie sesión.
2. Arriba a la derecha, clic en **"+"** → **"New repository"**.
3. En **Repository name** escriba `transacciones-service`.
4. (Opcional) En **Description**: *Reversión segura de transacciones con Spring Boot*.
5. Seleccione **Public** (para que el evaluador lo pueda ver) o **Private** e invítelo después.
6. **NO** marque "Add a README file", ni ".gitignore", ni licencia (el proyecto ya los trae).
7. Clic en **"Create repository"**.
8. En la página del repositorio vacío, clic en el enlace **"uploading an existing file"**.
9. Descomprima el `.zip` del proyecto en su computador. Abra la carpeta `transacciones-service`,
   seleccione **todo su contenido** (`src`, `pom.xml`, `README.md`, `.gitignore`) y arrástrelo a la página.
   - Arrastre el contenido de la carpeta, no la carpeta misma, para que `pom.xml` quede en la raíz.
   - Si su explorador no muestra `.gitignore` (archivo oculto): en Windows, *Vista → Elementos ocultos*;
     en Mac, `Cmd + Shift + .`
10. Espere a que termine la carga. Abajo, en **"Commit changes"**, escriba el mensaje:
    `Implementa reversión segura de transacciones`.
11. Clic en **"Commit changes"**.
12. Verifique que se vean las carpetas y que el README aparezca formateado en la página principal.
13. Copie la URL del repositorio (`https://github.com/<TU_USUARIO>/transacciones-service`) y envíela al evaluador.

### Opción B — Con Git desde la terminal

Después de crear el repositorio vacío (pasos 1 a 7 de la opción A):

```bash
cd transacciones-service
git init
git config user.name  "Wilmer Yesid Suarez Morales"
git config user.email "<SU_CORREO_DE_GITHUB>"
git add .
git commit -m "Implementa reversión segura de transacciones"
git branch -M main
git remote add origin https://github.com/<TU_USUARIO>/transacciones-service.git
git push -u origin main
```

> GitHub ya no acepta la contraseña de la cuenta en `git push`. Si la pide, use un
> **Personal Access Token**: *Settings → Developer settings → Personal access tokens → Tokens (classic)
> → Generate new token*, marque el permiso `repo` y pegue el token como contraseña.

---

## 8. Autor

**Wilmer Yesid Suarez Morales** — Desarrollador Backend Java
Todas las clases del proyecto incluyen la etiqueta `@author YesidSuarez`.
