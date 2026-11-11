# TASKS — FT-25 Red de Amigos por QR y Ofertas Compartidas [BORRADOR]

> Estado: Borrador (bloqueada hasta aprobación de contrato en `sensus-back`, D-6).

- [ ] T25.1 Acordar contrato OpenAPI en `sensus-back` para `/api/v1/friends` y `/api/v1/offers/{id}/share` — verif: revisión de contrato
- [ ] T25.2 Crear modelos `FriendConnection`, `SharedOfferItem` y `FriendsRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T25.3 Crear casos de uso de conexión QR, respuesta de solicitud, eliminación y compartir oferta — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T25.4 Crear `FriendsApiService.kt` y `FriendsRepositoryImpl.kt` retirando `MockDataProvider.sampleFriends` — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T25.5 Integrar QR real + escáner de vecinos en `CommunityScreen.kt` y selector de compartir en `ProductDetailScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T25.6 Añadir tests unitarios, `MockWebServer` y Compose UI con Kover `>= 80%` — verif: `python scripts/check_coverage.py --min 80`
