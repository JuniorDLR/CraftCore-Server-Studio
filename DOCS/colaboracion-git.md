# 🤝 Guía de Flujo Git y Colaboración — CraftCore Server Studio

Para garantizar la estabilidad de la rama principal (`main`) y mantener buenas prácticas de ingeniería de software, se ha configurado la siguiente política de protección de ramas en GitHub:

---

## 🔒 Política de Protección de la Rama `main`

1. **Protección contra Push Directo:**
   * La rama `main` está protegida. Ningún colaborador externo puede hacer `git push origin main` directamente.
   * Solo el propietario/administrador del repositorio (`JuniorDLR`) tiene permiso de bypass administrativo para subir cambios directos cuando sea necesario.
2. **Obligatoriedad de Pull Requests (PR):**
   * Todo cambio o nueva funcionalidad proveniente de colaboradores debe llegar mediante un **Pull Request**.
   * El Pull Request requiere al menos **1 aprobación** antes de poder fusionarse (*merge*).
3. **Historial Protegido:**
   * Se ha bloqueado el `git push --force` (nadie puede sobreescribir la historia de commits).
   * Se ha bloqueado la eliminación accidental de la rama `main`.

---

## 🚀 Flujo de Trabajo para el Colaborador

Para que tu compañero de equipo trabaje sin fricción:

### Paso 1: Clonar el Repositorio
```bash
git clone https://github.com/JuniorDLR/CraftCore-Server-Studio.git
cd CraftCore-Server-Studio
```

### Paso 2: Crear una Rama de Trabajo (Feature Branch)
Nunca trabajar sobre `main`. Crear una rama descriptiva:
```bash
git checkout -b feature/nombre-de-la-tarea
# Ejemplo: git checkout -b feature/pantalla-whitelist
```

### Paso 3: Realizar Cambios y Commits
```bash
git add .
git commit -m "feat: implementacion del servicio de consulta REST Mojang"
```

### Paso 4: Subir la Rama a GitHub
```bash
git push -u origin feature/nombre-de-la-tarea
```

### Paso 5: Abrir el Pull Request en GitHub
1. Ir a [github.com/JuniorDLR/CraftCore-Server-Studio](https://github.com/JuniorDLR/CraftCore-Server-Studio).
2. GitHub mostrará un botón amarillo: **"Compare & pull request"**.
3. Llenar la plantilla del PR describiendo qué RA o módulo se avanzó.
4. Tú revisas el código, lo apruebas y lo fusionas (*Merge pull request*) a `main`.
