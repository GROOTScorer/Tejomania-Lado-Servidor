# TejoManía - Servidor

## Detalles
Inicialmente, el servidor se encarga de escuchar los mensajes enviados por los clientes para conectarlos. Al conectarse dos clientes, se iniciará la partida en multijugador y se ejecutará la lógica de juego (movimientos, colisiones, goles, entre otros aspectos relevantes). Cualquier actualización será notificada a los clientes, que verán los cambios en sus pantallas respectivas. Al finalizarse la partida, se mostrará el ganador y luego se cerrará el servidor.

Además de la lógica incluida, se presenta una pantalla de debug visual donde se puede ver la partida desde la perspectiva del servidor.

## Estado
Actualmente, el lado servidor se encuentra operacional y permite el juego en multijugador para equipos que se encuentren en la misma LAN y que puedan comunicarse.

## Desarrolladores
Gael De Luca y Joaquín Pocovi
