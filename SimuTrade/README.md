# SimuTrade

> Simulador educativo de bolsa y criptomonedas para Android

SimuTrade es una aplicación Android que permite aprender a invertir en mercados financieros de forma segura, sin dinero real.
El usuario empieza con **100 € virtuales** y puede comprar y vender acciones y criptomonedas con precios reales,
competir en rankings, completar retos diarios y jugar con amigos en ligas privadas.

---

## Descarga

 [Descargar APK](https://github.com/tfg2dam/tfg2dam/releases/latest)

> Requiere Android 7.0 o superior. Activa **"Instalar aplicaciones de fuentes desconocidas"** en los ajustes del móvil.

---

## Capturas de pantalla

| Login | Dashboard | Mercado |
|---|---|---|
| ![Login](screenshots/login.png) | ![Dashboard](screenshots/dashboard.png) | ![Mercado](screenshots/mercado.png) |

| Retos | Amigos | Ligas |
|---|---|---|
| ![Retos](screenshots/retos.png) | ![Amigos](screenshots/amigos.png) | ![Ligas](screenshots/ligas.png) |

---

## Características principales

- **Trading en tiempo real** — precios reales de acciones (Finnhub) y criptomonedas (CoinGecko)
- **Sistema de rangos** — 6 rangos basados exclusivamente en beneficio de trading: Principiante → Bronce → Plata → Oro → Platino → Diamante
- **Ranking global** — leaderboard ordenado por beneficio de trading
- **Retos diarios** — 3 retos aleatorios cada día con racha y recompensas en saldo bonus
- **Sistema de amigos** — búsqueda por código único `#XXXXXXXX`, solicitudes de amistad
- **Ligas privadas** — crea ligas con nombre, invita amigos y compite con ranking propio
- **Modo oscuro** — soporte completo con Material Design 3
- **Google Sign-In** — autenticación con cuenta de Google además de email/contraseña

---

## Stack tecnológico

| Tecnología | Versión | Uso |
|---|---|---|
| Kotlin | 2.0+ | Lenguaje principal |
| Jetpack Compose | BOM 2025.04.01 | Interfaz de usuario |
| Firebase Auth | BOM 33.12.0 | Autenticación |
| Cloud Firestore | BOM 33.12.0 | Base de datos |
| Navigation Compose | 2.8.9 | Navegación entre pantallas |
| Retrofit | 2.11.0 | Peticiones HTTP a APIs |
| OkHttp | 4.12.0 | Cliente HTTP |
| Gson | 2.10.1 | Serialización JSON |
| CoinGecko API | v3 Free | Precios de criptomonedas |
| Finnhub API | v1 | Precios de acciones |
| Material Design 3 | BOM 2025.04.01 | Sistema de diseño |

---

## Requisitos

- Android Studio **Hedgehog** o superior
- Android SDK mínimo: **API 24** (Android 7.0)
- Android SDK objetivo: **API 35** (Android 15)
- JDK 17+

---

## Instalación

### 1. Clonar el repositorio

```bash
git clone https://github.com/tfg2dam/tfg2dam.git
cd tfg2dam/SimuTrade
```

### 2. Compilar y ejecutar

Abre el proyecto en Android Studio, sincroniza Gradle y ejecuta la app en un emulador o dispositivo físico con cuenta de Google configurada.

---

## Estructura del proyecto

```
com.simutrade/
├── MainActivity.kt
├── datos/
│   ├── modelo/
│   │   └── Modelos.kt              # Todas las clases de datos del dominio
│   ├── remoto/
│   │   ├── CoinGeckoApi.kt         # Interfaz Retrofit para CoinGecko
│   │   └── FinnhubApi.kt           # Interfaz Retrofit para Finnhub
│   └── repositorio/
│       ├── RepositorioAutenticacion.kt
│       ├── RepositorioUsuario.kt
│       ├── RepositorioMercado.kt
│       ├── RepositorioAmigos.kt
│       └── RepositorioLigas.kt
├── navegacion/
│   └── NavGraph.kt                 # Grafo de navegación con detección reactiva de sesión
└── ui/
    ├── autenticacion/              # Login y Registro
    ├── main/                       # MainScreen, MainViewModel, PerfilScreen
    ├── dashboard/                  # Panel de control
    ├── mercado/                    # Lista de activos con precios reales
    ├── operaciones/                # Compra y venta de activos
    ├── ranking/                    # Leaderboard global
    ├── retos/                      # Retos diarios con racha
    ├── amigos/                     # Sistema social de amigos
    ├── ligas/                      # Ligas privadas
    ├── usuario/                    # UsuarioViewModel (lógica de trading)
    └── tema/                       # Tema.kt, Tipografia.kt
```

---

## Arquitectura

El proyecto sigue el patrón **MVVM** recomendado por Google para Android con Jetpack Compose:

```
View (Composables)
    ↕
ViewModel (StateFlow)
    ↕
Repository (lógica de negocio)
    ↕
Firebase / APIs externas
```

Cada dominio funcional tiene su propio repositorio:

| Repositorio | Responsabilidad |
|---|---|
| `RepositorioAutenticacion` | Login, registro y Google Sign-In |
| `RepositorioUsuario` | Saldo, cartera, transacciones y retos |
| `RepositorioMercado` | Precios en tiempo real de acciones y criptos |
| `RepositorioAmigos` | Solicitudes de amistad y lista de amigos |
| `RepositorioLigas` | Creación, invitaciones y ranking de ligas |

---

## Modelo de datos (Firestore)

```
Usuarios/{uid}
├── nombre_usuario, email, codigo_usuario
├── saldo, saldo_inicial, saldo_bonus
├── valor_cartera, beneficio
├── mis_ligas: [ligaId, ...]
├── cartera/{idActivo}
├── transacciones/{autoId}
├── retos/datos
├── amigos/{amigoUid}
├── solicitudes/{solicitanteUid}
└── invitacionesLiga/{ligaId}

Ligas/{ligaId}
├── nombre, creado_por, creado_en
└── miembros/{uid}
    └── estado: "pendiente" | "aceptado"
```

> **Nota importante:** El campo `saldo_bonus` almacena las recompensas de retos por separado.
> El beneficio de trading se calcula como `(saldo - saldo_bonus + valor_cartera) - saldo_inicial`,
> garantizando que los retos no afecten al rango ni al leaderboard.

---

## Sistema de rangos

| Rango | Beneficio mínimo de trading |
|---|---|
| Principiante | Sin mínimo |
| Bronce | 0 € |
| Plata | 50 € |
| Oro | 150 € |
| Platino | 300 € |
| Diamante | 500 € |

---

## Sistema de retos diarios

Cada día se generan **3 retos aleatorios** de un pool de 5 tipos:

| Tipo | Descripción | Recompensa |
|---|---|---|
| `operacion` | Realiza una compra o venta hoy | 2 € bonus |
| `diversifica` | Ten al menos 2 activos distintos | 3 € bonus |
| `trader` | Realiza 3 operaciones en el día | 5 € bonus |
| `multimercado` | Ten acciones Y criptos en cartera | 4 € bonus |
| `beneficio` | Mantén beneficio de trading positivo | 6 € bonus |

La racha sube al completar todos los retos del día y se rompe si no se completan al día siguiente. El saldo bonus **no** cuenta para el rango.

---

## Rate limiting de APIs

La API gratuita de CoinGecko tiene un límite de ~30 peticiones/minuto. La app gestiona esto con:

- **Cooldown manual**: 60 segundos entre actualizaciones manuales
- **Autorefresh**: cada 2 minutos en segundo plano
- **Cache local**: si hay un error 429, se mantienen los datos anteriores en pantalla

---

## Documentación

La memoria del proyecto y el diagrama de base de datos están disponibles en la carpeta [`sampledata/`](sampledata/):

- [Memoria del proyecto](sampledata/Memoria_SimuTrade.docx)
- [Diagrama de base de datos](sampledata/BD_SimuTrade.jpeg)

---

## Autores

| Nombre | GitHub |
|---|---|
| Guillermo Sierra Castejón | [@GuillermoSierra](https://github.com/GuillermoSierra) |
| Iván Bertolo García | [@Bertolo23](https://github.com/Bertolo23) |
| Ximena Meyzen Calderón | [@xmeyzzzenx](https://github.com/xmeyzzzenx) |

**Tutora:** Ruth Lospitao Ruiz  
**Centro:** IES Isidra de Guzmán  
**Ciclo:** Desarrollo de Aplicaciones Multiplataforma — Curso 2025/2026

---

## Licencia

Este proyecto ha sido desarrollado como Proyecto Intermodular del Ciclo Formativo de Grado Superior en Desarrollo de Aplicaciones Multiplataforma. Todos los derechos reservados © 2026.
