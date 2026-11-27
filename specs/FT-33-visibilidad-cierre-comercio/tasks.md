# Tareas — FT-33 Visibilidad al cerrar un comercio

- [x] T33.1 Cubrir lista vacía autoritativa y errores de red, y corregir la selección de datos en Inicio (`BuyerViewModelsTest.buyerHome_successfulEmptyCatalogDoesNotRestoreCachedOffers`).
- [x] T33.2 Aplicar invalidación por ID, protección contra respuestas antiguas y reconciliación del estado (`MerchantProfileViewModel.kt`, `MerchantProfileViewModelTest`).
- [x] T33.3 Verificar OpenAPI y compartir la lectura de perfil en curso (`031-edicion-perfil-comercio` solo expone `PUT /api/v1/merchants/profile`; `MerchantProfileViewModel.awaitMerchantProfile()` cubierto en `MerchantProfileViewModelTest`).
- [ ] T33.4 Medir propagación con dos clientes y resolver dependencias de backend (acción del usuario: prueba E2E con dos cuentas/teléfonos según protocolo FT-32; ver plan cierre móvil §5.Z-3).

Evidencia parcial de T33.1 y T33.3 en el checklist. Falta validar con cuentas de prueba la invalidación por comercio y la propagación entre teléfonos.
