# Tareas — FT-34 Rendimiento del mapa y ofertas cercanas

- [x] T34.1 Unificar disparadores de búsqueda y cancelar o descartar resultados obsoletos (`BuyerMapViewModel` probado en `BuyerViewModelsTest.kt`).
- [x] T34.2 Agregar lectura compatible en Room, vigencia e invalidación con E/S fuera del hilo principal (`OffersRepositoryOfflineTest`, `MapPreferencesRepositoryTest`, `EncryptedDatabaseIntegrationTest`).
- [ ] T34.3 Separar ubicación y refresco del render y reducir trabajo de overlays con base en las trazas (acción del usuario: capturar trazas de mapa/overlays y aplicar protocolo FT-32 en dispositivo físico; ver plan cierre móvil §5.Z-3).

Evidencia parcial de T34.1 y T34.2 en el checklist. La caché persistente por búsqueda y la reducción de trabajo de overlays siguen pendientes.
