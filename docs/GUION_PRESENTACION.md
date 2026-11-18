# Guion de presentación de Nexo (5–7 minutos)

**Objetivo:** mostrar el recorrido de Ana, Luis y Sofía en una aplicación de mensajería cliente-servidor. El modo demo abre a Ana y Luis en dos ventanas; Sofía puede conectarse desde otro PC o desde un tercer cliente.

## Preparación antes de empezar

- Comprueba que Java 17+ esté instalado.
- Inicia `demo.bat` en Windows o `./run.sh demo` en Linux/macOS para abrir el servidor y las ventanas de Ana y Luis.
- Ten lista una tercera ventana cliente para Sofía, en este u otro PC. Si usa otro PC, inicia el servidor en modo **Solo servidor**, anota la IPv4 que muestra y permite TCP 5050 en el firewall.
- Las cuentas de ejemplo son `ana`, `luis` y `sofia`; la contraseña de demostración es `1234`.
- Organiza las ventanas para que el público vea los chats y, si es posible, la consola del servidor.

## Recorrido

### 0:00–0:40 — Presentación

**Decir:** “Nexo es una aplicación de mensajería de escritorio hecha en Java. Veremos cómo tres usuarios conversan mediante un servidor central.”

**Mostrar:** las ventanas de Ana y Luis y la consola del servidor. Presenta a Sofía como tercer participante que se conectará durante la demo.

### 0:40–1:15 — Inicio de sesión

**Mostrar:** las sesiones de Ana y Luis ya abiertas por el modo demo. Inicia sesión como Sofía en la tercera ventana con su cuenta de ejemplo.

**Decir:** “Cada ventana representa un cliente conectado al servidor e identificado con su propia cuenta.”

### 1:15–2:05 — Chat directo y entrega

**Mostrar:** desde Ana, abre el chat directo con Luis y envía: “Hola, ¿listo para la presentación?”. Enseña la llegada en la ventana de Luis y responde desde Luis.

**Decir:** “El servidor recibe el mensaje y lo distribuye a los miembros del chat. La marca indica cuándo fue enviado y entregado.”

### 2:05–2:55 — Escritura y lectura

**Mostrar:** empieza a escribir en Ana sin enviar, para que Luis vea “escribiendo…”. Envía el mensaje, abre el chat en Luis y señala la marca doble azul cuando el estado cambia a leído.

**Decir:** “El cliente comunica el evento de escritura y, al abrir la conversación, envía la confirmación de lectura.”

### 2:55–4:00 — Grupo de tres

**Mostrar:** desde Ana, crea el grupo “Equipo Nexo” con Luis y Sofía. Envía un saludo y confirma que aparece en las tres sesiones. Pide a Sofía que responda.

**Decir:** “El grupo reúne a varios usuarios; los estados de entrega y lectura se calculan considerando a sus participantes.”

### 4:00–4:45 — Imagen

**Mostrar:** adjunta una imagen pequeña en el grupo. Confirma que los otros participantes la reciban y se vea en el chat.

**Decir:** “También se pueden adjuntar imágenes y archivos de hasta 8 MB.”

### 4:45–5:30 — Conexión desde otro PC

**Mostrar:** si Sofía está en un segundo equipo, señala la IP local usada por el cliente y el registro del servidor. Si la demo es en un único equipo, muestra la ventana de Sofía y explica que el cliente se conecta a la misma dirección del servidor usando la IP local.

**Decir:** “Para usar Nexo en red local, los equipos deben estar en la misma red y el firewall debe permitir el puerto TCP 5050.”

### 5:30–6:00 — Cierre y arquitectura

**Mostrar:** señala el servidor y el archivo `nexo-data/nexo.db`. Si queda tiempo, enseña el menú de perfil y la edición del nombre visible o la información.

**Decir:** “La aplicación separa interfaz, protocolo y servidor. Los datos se guardan en el equipo que hace de servidor.”

## Preguntas posibles del profesor

- **¿Qué tecnologías usa?** Java 17, Swing para la interfaz y sockets TCP para comunicar los clientes con el servidor. No hacen falta librerías externas para ejecutarla.
- **¿Dónde quedan los datos?** En el equipo servidor, en `nexo-data/nexo.db`, usando serialización de Java.
- **¿Cómo se marca un mensaje como leído?** El cliente envía `MARK_READ` al abrir el chat; el servidor actualiza quién lo leyó y distribuye `STATUS_UPDATE`.
- **¿Se conecta otro computador?** Sí. Debe usar la IPv4 del servidor, estar en la misma red y tener acceso al puerto TCP 5050.
- **¿Cómo se protegen las contraseñas?** Se almacenan como hash SHA-256 con salt por usuario; no quedan en texto plano.

## Si algo falla durante la demo

- **No conecta:** confirma que el servidor está activo, usa la IP correcta y revisa que el firewall permita TCP 5050.
- **No aparece “escribiendo…” o la marca de lectura:** continúa con el chat y explica el evento que envía el cliente.
- **No se crea el grupo:** reinicia el servidor y usa el chat directo como alternativa mientras explicas el flujo.
- **No está disponible el segundo PC:** usa una tercera ventana local para Sofía y explica cómo cambiar el host a la IP del servidor.
- **No se envía la imagen:** prueba con una imagen PNG, JPG, GIF o BMP inferior a 8 MB.

Usa solo las cuentas de demostración; no muestres contraseñas reales ni datos personales.
