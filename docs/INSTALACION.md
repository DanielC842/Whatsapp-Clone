# Instalación y ejecución de Nexo

Nexo necesita Java 17 o posterior, pero **no hace falta instalarlo a mano**: si `run.bat` / `run.sh`
no encuentran Java 17+, descargan automáticamente un Java portable (Eclipse Temurin) en la carpeta
`.jdk` del proyecto. Solo ocurre la primera vez y requiere internet. Esta guía cubre Windows, Linux y macOS.

## 1. (Opcional) Instalar Java en Windows

Solo si prefieres tener Java instalado en el sistema o el PC no tiene internet al ejecutar la app:

1. Descarga el instalador **Eclipse Temurin JDK 17 (LTS) para Windows x64** desde [Adoptium](https://adoptium.net/temurin/releases/).
2. Ejecuta el instalador y deja habilitadas las opciones para agregar Java al `PATH` y definir `JAVA_HOME`, si aparecen.
3. Abre una nueva ventana de PowerShell o Símbolo del sistema y comprueba la instalación:

   ```powershell
   java -version
   javac -version
   ```

   Ambos comandos deben mostrar la versión 17 o superior. Si Windows no los reconoce, cierra y vuelve a abrir la terminal; si el problema continúa, revisa `PATH` y `JAVA_HOME`.

## 2. Clonar o descargar el proyecto

Instala Git para Windows si aún no está instalado. En PowerShell:

```powershell
git clone https://github.com/DanielC842/Whatsapp-Clone.git
cd Whatsapp-Clone
```

También puedes abrir la página del repositorio en GitHub, elegir **Code → Download ZIP** y extraer el archivo descargado.

## 3. Ejecutar

Desde la carpeta raíz del proyecto:

- **Windows:** ejecuta `run.bat` para abrir el lanzador con las opciones de inicio. Ejecuta `demo.bat` para abrir la demo local con el servidor, Ana y Luis.
- **Linux/macOS:** ejecuta `./run.sh` para abrir el lanzador, o `./run.sh demo` para la demo local:

  ```bash
  chmod +x run.sh
  ./run.sh demo
  ```

El modo demo crea dos ventanas de cliente, una para Ana y otra para Luis. Para iniciar solo el servidor en Linux/macOS, usa `./run.sh server`; en Windows, elige **Solo servidor** desde el lanzador. Para conectarte a otro equipo desde una terminal, ejecuta `run.bat client IP 5050` o `./run.sh client IP 5050`, reemplazando `IP` por la dirección del servidor.

Los scripts compilan el código con `javac` y lo ejecutan; no hace falta Maven ni un IDE.

## 4. Usar Nexo en una red local

1. Conecta todos los equipos a la misma red local.
2. En el equipo servidor, averigua su IPv4 local. En Windows usa `ipconfig`; busca la dirección IPv4 del adaptador conectado (por ejemplo, `192.168.1.25`).
3. Inicia el servidor con **Ser el servidor y entrar** o **Solo servidor** en el lanzador. La ventana del servidor muestra las IP disponibles. Nexo usa el puerto TCP **5050**.
4. En el Firewall de Windows, permite la aplicación Java en redes privadas o crea una regla de entrada para TCP 5050, limitada al perfil **Privado**. La persona administradora del equipo puede necesitar conceder este permiso.
5. En cada cliente, abre **Conectarme a un servidor** e introduce la IP del servidor y el puerto 5050. No uses `localhost` desde otro equipo: esa dirección siempre apunta al propio equipo.
6. Si la conexión falla, verifica que ambos equipos estén en la misma red y que el servidor siga activo.

## Problemas comunes

| Síntoma | Qué revisar |
| --- | --- |
| La descarga automática de Java falla | Revisa la conexión a internet (o un proxy de la universidad) e intenta de nuevo; si persiste, instala Java como en el paso 1. |
| `javac` no se reconoce | Instala Temurin JDK (no solo un runtime), habilita `PATH` y abre una terminal nueva. Comprueba `java -version` y `javac -version`. |
| El puerto 5050 ya está en uso | Cierra otra instancia de Nexo u otra aplicación que use el puerto y vuelve a iniciar el servidor. |
| Los clientes no encuentran el servidor | Usa la IPv4 del equipo servidor, confirma que están en la misma red y permite TCP 5050 en el firewall. |
| El cliente funciona en el servidor, pero no desde otro equipo | No uses `localhost`; revisa la IP del servidor, el perfil de firewall y el aislamiento de clientes del punto de acceso. |
| No se puede ejecutar `run.sh` | En Linux/macOS, ejecuta `chmod +x run.sh` y luego `./run.sh` desde la raíz del proyecto. |
| Aparece un error de compilación | Confirma que tienes JDK 17 o posterior (incluye `javac`) y ejecuta el script desde la raíz del repositorio. |
