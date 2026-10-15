# Capturas en vivo — aviso y procedencia

Respuestas **reales** guardadas el **2026-10-15** desde una sesión propia (`arodriguez53`)
contra `https://api.enzona.net` (solo peticiones **GET**; cabecera `Authorization: Bearer`).
Documentadas y explicadas en
[`docs/reverse-engineering/11-capturas-live.md`](../../../../../docs/reverse-engineering/11-capturas-live.md).

> ⚠ **Datos personales reales**: PAN completos de tarjetas de destinatarios, CI, teléfonos,
> emails, direcciones y nombres de terceros. El repositorio debe seguir siendo **privado**;
> no publiques ni compartas esta carpeta. Las capturas **no** contienen tokens ni
> credenciales: el access token nunca se escribió a disco.

- `_index.json` — índice máquina-legible: archivo → URL exacta, método, estado HTTP, ms,
  bytes y nota.
- `_tabla.md` — la misma tabla agrupada por áreas (el doc 11 tiene la versión enlazada).
- ⚠ **No todos los `.json` son parseables**: algunos conservan cuerpos crudos del backend
  tal como llegaron (p. ej. `transfers-list-limit10-offset0-202-transitorio.json` lleva un
  JSON inválido real, `"avatars":,`). Un test que itere la carpeta debe tolerar esos casos
  o filtrarlos por nombre.
- Los nombres `*-transitorio`, `*-2intento`, `*-timeout` conservan fallos reales
  (timeouts, 5xx, respuestas sucias) porque documentan el comportamiento del backend.
- Un contenido `{"probe_error": "…timed out…"}` significa que la sonda agotó su timeout
  sin obtener respuesta HTTP.
- Los guiones bajos en `user_name` son deliberados: así se llama el parámetro real.

Para regenerar más capturas hace falta una sesión propia en la app (ver `11`, §Cómo se
obtuvieron); los scripts de sondeo **no se versionan** (misma política que las PoC).
