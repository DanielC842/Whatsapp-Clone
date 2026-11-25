# Demo: chat por consola (versión simplificada)

Versión mínima del chat, 100 % Java (sin librerías), para mostrar cómo funciona la
comunicación con **sockets**. Son solo tres clases, cada una con su propio `main`:

| Clase | Qué hace |
|---|---|
| `demo.Servidor` | Abre el puerto 5000, atiende a cada usuario en un hilo y reenvía los mensajes. |
| `demo.Usuario1` | Usuario **Ana**: envía lo que escribes en la consola y muestra lo que llega. |
| `demo.Usuario2` | Usuario **Luis**: igual que el anterior. |

## Ejecutar en NetBeans

1. **File → Open Project** y elige la carpeta `demo-consola` del repositorio.
2. Abre `Servidor.java` y haz clic derecho → **Run File** (Shift+F6).
3. Haz lo mismo con `Usuario1.java` y luego con `Usuario2.java`.
   Cada una abre su propia pestaña en la ventana **Output**.
4. Escribe en la pestaña de Usuario1 y pulsa Enter: el mensaje aparece en la de Usuario2
   (y viceversa). La pestaña del Servidor muestra el registro de todo el chat.
5. Escribe `/salir` para cerrar un usuario.

Para detener el servidor, usa el botón rojo de *Stop* en su pestaña de Output.

## Ejecutar sin IDE (opcional)

Desde la carpeta `demo-consola`, en tres terminales:

```bash
javac -d out src/main/java/demo/*.java
java -cp out demo.Servidor
java -cp out demo.Usuario1
java -cp out demo.Usuario2
```

## En dos PCs

Cambia la constante `HOST` de `Usuario1`/`Usuario2` de `"localhost"` a la IP del PC que
ejecuta el `Servidor` (y permite el puerto 5000 en el Firewall).
