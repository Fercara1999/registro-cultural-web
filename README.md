# 📚 Mi Registro Cultural — Versión Web

Versión web de la aplicación de registro cultural, migrada desde JavaFX a **Spring Boot + Thymeleaf**.
Accesible desde cualquier dispositivo (móvil, tablet, PC) a través de un enlace.

## 🛠️ Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 3.2 + Spring Data JPA |
| Frontend | Thymeleaf + HTML/CSS/JS vanilla |
| Base de datos | SQLite (archivo `registro_cultural.db`) |
| Gráficos | Chart.js 4 |
| Despliegue | Railway / Render / local |

## 🚀 Ejecutar en local

```bash
git clone https://github.com/Fercara1999/registro-cultural-web
cd registro-cultural-web
mvn spring-boot:run
```

Abre http://localhost:8080 en tu navegador.

## ☁️ Despliegue en Railway (gratis)

1. Entra en [railway.app](https://railway.app) y conecta tu cuenta de GitHub
2. **New Project → Deploy from GitHub repo** → selecciona `registro-cultural-web`
3. Railway detecta el `pom.xml` automáticamente y despliega
4. En **Settings → Networking** genera un dominio público
5. ¡Accede desde el móvil con ese enlace!

> ⚠️ Para persistir la BD en Railway añade un volumen en `/app/registro_cultural.db`
> o migra a PostgreSQL (ver rama `postgres` cuando esté disponible)

## 📱 Funcionalidades

- ✅ Registrar libros, series, películas, teatro y cómics
- ✅ Portada por subida de archivo
- ✅ Valoración con estrellas (0-10)
- ✅ Campos específicos por tipo (autor, temporada, director, tomo único...)
- ✅ Buscar por nombre, tipo y fecha
- ✅ Editar y eliminar registros
- ✅ Estadísticas con gráficos (Chart.js)
- ✅ Sección de películas vistas en el cine
- ✅ Modo oscuro
- ✅ Diseño responsivo (móvil, tablet, PC)
