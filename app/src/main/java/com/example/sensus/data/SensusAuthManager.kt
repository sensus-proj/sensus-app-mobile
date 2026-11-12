package com.example.sensus.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.sensus.domain.model.MerchantStore

private const val MIN_NAME_LEN = 3
private const val MIN_EMAIL_LEN = 5
private const val PHONE_DIGITS_LEN = 10
private const val MIN_PASSWORD_LEN = 6
private const val MAX_BUSINESS_LEN = 150
private const val MIN_ADDR_LEN = 5
private const val MAX_ADDR_LEN = 250
private const val DEFAULT_MERCHANT_RATING = 5.0
private const val MIN_USER_PARTS = 6
private const val STORE_PARTS_INDEX = 6
private const val MIN_STORE_PARTS = 4
private const val INDEX_PHONE = 3
private const val INDEX_PASSWORD = 4
private const val INDEX_MERCHANT_FLAG = 5

data class UserAccount(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val isMerchant: Boolean = false,
    val merchantStore: MerchantStore? = null
)

/**
 * Gestor centralizado de autenticación, sesión y preferencias persistentes en memoria
 * y sincronizado con SharedPreferences para que persista al cerrar y reabrir la app.
 */
object SensusAuthManager {
    private const val PREFS_NAME = "sensus_auth_prefs"
    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private const val KEY_IS_GUEST = "key_is_guest"
    private const val KEY_ACTIVE_USER_EMAIL = "key_active_user_email"
    private const val KEY_LOCATION_GRANTED = "key_location_granted"
    private const val KEY_TERMS_ACCEPTED = "key_terms_accepted"
    private const val KEY_USERS_PREFIX = "user_account_"
    private const val KEY_USERS_LIST = "all_registered_emails"

    private var prefs: SharedPreferences? = null
    private val registeredUsers = mutableMapOf<String, UserAccount>()

    var currentUser by mutableStateOf<UserAccount?>(null)
        private set

    var isGuestUser by mutableStateOf(false)
        private set

    var isSessionActive by mutableStateOf(false)
        private set

    var hasLocationPermission by mutableStateOf(false)
        private set

    var hasAcceptedTerms by mutableStateOf(false)
        private set

    var hasCompletedSplash by mutableStateOf(false)

    fun init(context: Context) {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        seedDefaultAndPersistedUsers(sp)

        hasLocationPermission = sp.getBoolean(KEY_LOCATION_GRANTED, false)
        hasAcceptedTerms = sp.getBoolean(KEY_TERMS_ACCEPTED, false)
        isSessionActive = sp.getBoolean(KEY_IS_LOGGED_IN, false)
        isGuestUser = sp.getBoolean(KEY_IS_GUEST, false)

        val activeEmail = sp.getString(KEY_ACTIVE_USER_EMAIL, null)
        currentUser = resolveInitialUserAccount(registeredUsers, isSessionActive, isGuestUser, activeEmail)
    }

    fun registerUser(name: String, email: String, phone: String, password: String): Pair<Boolean, String?> {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()
        val phoneDigits = phone.trim().filter { it.isDigit() }
        val cleanPassword = password.trim()
        val error = validateRegistration(cleanName, cleanEmail, phoneDigits, cleanPassword, registeredUsers)
        if (error != null) return Pair(false, error)

        val newUser = UserAccount(
            id = "user_${System.currentTimeMillis()}",
            name = cleanName,
            email = cleanEmail,
            phone = phoneDigits,
            password = cleanPassword,
            isMerchant = false
        )
        registeredUsers[cleanEmail] = newUser
        persistNewUserRecord(prefs, KEY_USERS_PREFIX, KEY_USERS_LIST, cleanEmail, newUser)
        activateRegisteredSession(newUser)
        return Pair(true, null)
    }

    fun loginUser(emailOrPhone: String, password: String): Pair<Boolean, String?> {
        val cleanInput = emailOrPhone.trim().lowercase()
        val cleanPassword = password.trim()
        val error = validateLoginInputs(cleanInput, cleanPassword)
        if (error != null) return Pair(false, error)

        val matchedUser = registeredUsers[cleanInput]
            ?: registeredUsers.values.find { it.phone == cleanInput }
            ?: if (cleanInput == "usuario@sensus.app") registeredUsers["usuario@sensus.app"] else null

        val passwordValid = matchedUser != null && isPasswordMatch(matchedUser, cleanPassword)
        return if (!passwordValid) {
            val msg = if (matchedUser == null) {
                "Usuario no encontrado. Verifica tus datos o crea una cuenta"
            } else {
                "Contraseña incorrecta"
            }
            Pair(false, msg)
        } else {
            activateRegisteredSession(matchedUser)
            Pair(true, null)
        }
    }

    fun loginAsGuest() {
        val guest = UserAccount(
            id = "guest_${System.currentTimeMillis()}",
            name = "Invitado Sensus",
            email = "invitado@sensus.app",
            phone = "",
            password = "",
            isMerchant = false
        )
        currentUser = guest
        isSessionActive = true
        isGuestUser = true

        prefs?.edit()
            ?.putBoolean(KEY_IS_LOGGED_IN, true)
            ?.putBoolean(KEY_IS_GUEST, true)
            ?.putString(KEY_ACTIVE_USER_EMAIL, guest.email)
            ?.apply()
    }

    fun logout() {
        currentUser = null
        isSessionActive = false
        isGuestUser = false

        prefs?.edit()
            ?.putBoolean(KEY_IS_LOGGED_IN, false)
            ?.remove(KEY_ACTIVE_USER_EMAIL)
            ?.putBoolean(KEY_IS_GUEST, false)
            ?.apply()
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        hasLocationPermission = granted
        prefs?.edit()?.putBoolean(KEY_LOCATION_GRANTED, granted)?.apply()
    }

    fun setTermsAccepted(accepted: Boolean) {
        hasAcceptedTerms = accepted
        prefs?.edit()?.putBoolean(KEY_TERMS_ACCEPTED, accepted)?.apply()
    }

    fun activateMerchant(businessName: String, address: String, postalCode: String): Pair<Boolean, String?> {
        val cleanBusiness = businessName.trim()
        val cleanAddr = address.trim()
        val cleanPostalCode = postalCode.trim()
        val error = validateMerchantInput(cleanBusiness, cleanAddr, cleanPostalCode)
        if (error != null) return Pair(false, error)

        val newStore = MerchantStore(
            name = cleanBusiness,
            category = "Comercio Local",
            isOpen = true,
            rating = DEFAULT_MERCHANT_RATING,
            reviewCount = 1,
            activeOffersCount = 0,
            recoveredMoneyToday = 0.0,
            redeemedTodayCount = 0,
            address = cleanAddr,
            schedule = "Lun - Sáb 09:00 - 20:00",
            phone = "",
            postalCode = cleanPostalCode
        )

        val updatedUser = (
            currentUser ?: UserAccount(
                id = "user_${System.currentTimeMillis()}",
                name = cleanBusiness,
                email = "comercio@sensus.app",
                phone = "",
                password = "••••••••"
            )
            ).copy(isMerchant = true, merchantStore = newStore)

        currentUser = updatedUser
        registeredUsers[updatedUser.email.lowercase()] = updatedUser
        prefs?.edit()
            ?.putString("$KEY_USERS_PREFIX${updatedUser.email.lowercase()}", serializeUserAccount(updatedUser))
            ?.apply()

        return Pair(true, null)
    }

    private fun seedDefaultAndPersistedUsers(sp: SharedPreferences) {
        val defaultUser = UserAccount(
            id = "demo_01",
            name = "Carlos Alberto",
            email = "usuario@sensus.app",
            phone = "5512345678",
            password = "123456",
            isMerchant = false
        )
        registeredUsers[defaultUser.email.lowercase()] = defaultUser

        val emailsSet = sp.getStringSet(KEY_USERS_LIST, emptySet()) ?: emptySet()
        for (email in emailsSet) {
            val serialized = sp.getString("$KEY_USERS_PREFIX${email.lowercase()}", null)
            serialized?.let(::deserializeUserAccount)?.let { user ->
                registeredUsers[user.email.lowercase()] = user
            }
        }
    }

    private fun activateRegisteredSession(user: UserAccount) {
        currentUser = user
        isSessionActive = true
        isGuestUser = false
        prefs?.edit()
            ?.putBoolean(KEY_IS_LOGGED_IN, true)
            ?.putBoolean(KEY_IS_GUEST, false)
            ?.putString(KEY_ACTIVE_USER_EMAIL, user.email.lowercase())
            ?.apply()
    }
}

private fun resolveInitialUserAccount(registeredUsers: Map<String, UserAccount>, active: Boolean, guest: Boolean, activeEmail: String?): UserAccount? = when {
    active && !guest && activeEmail != null -> registeredUsers[activeEmail.lowercase()]
    active && guest -> UserAccount("guest", "Invitado Sensus", "invitado@sensus.app", "", "")
    else -> null
}

private fun validateRegistration(name: String, email: String, phoneDigits: String, password: String, existing: Map<String, UserAccount>): String? = when {
    name.length < MIN_NAME_LEN -> "El nombre debe tener al menos $MIN_NAME_LEN caracteres"
    !email.contains("@") || !email.contains(".") || email.length < MIN_EMAIL_LEN ->
        "Ingresa un correo electrónico válido"
    phoneDigits.length != PHONE_DIGITS_LEN -> "El teléfono debe contener $PHONE_DIGITS_LEN dígitos"
    password.length < MIN_PASSWORD_LEN -> "La contraseña debe tener al menos $MIN_PASSWORD_LEN caracteres"
    existing.containsKey(email) -> "Este correo electrónico ya se encuentra registrado"
    else -> null
}

private fun validateLoginInputs(cleanInput: String, cleanPassword: String): String? = when {
    cleanInput.isBlank() -> "Ingresa tu correo o teléfono"
    cleanPassword.isBlank() -> "Ingresa tu contraseña"
    else -> null
}

private fun isPasswordMatch(matchedUser: UserAccount, cleanPassword: String): Boolean = matchedUser.password == cleanPassword ||
    (matchedUser.email == "usuario@sensus.app" && (cleanPassword == "••••••••" || cleanPassword == "123456"))

private fun validateMerchantInput(name: String, address: String, postalCode: String): String? {
    val cpRegex = Regex("^[0-9a-zA-Z\\s\\-]{4,10}$")
    return when {
        name.length < 2 -> "El nombre comercial debe tener al menos 2 caracteres"
        name.length > MAX_BUSINESS_LEN -> "El nombre comercial no puede exceder $MAX_BUSINESS_LEN caracteres"
        address.length < MIN_ADDR_LEN -> "La dirección del comercio debe contener al menos $MIN_ADDR_LEN caracteres"
        address.length > MAX_ADDR_LEN -> "La dirección no puede exceder $MAX_ADDR_LEN caracteres"
        !cpRegex.matches(postalCode) -> "El código postal no tiene un formato válido (entre 4 y 10 caracteres)"
        else -> null
    }
}

private fun persistNewUserRecord(sp: SharedPreferences?, prefix: String, listKey: String, cleanEmail: String, user: UserAccount) {
    sp ?: return
    val emailsSet = (sp.getStringSet(listKey, emptySet()) ?: emptySet()).toMutableSet()
    emailsSet.add(cleanEmail)
    sp.edit()
        .putString("$prefix$cleanEmail", serializeUserAccount(user))
        .putStringSet(listKey, emailsSet)
        .apply()
}

private fun serializeUserAccount(user: UserAccount): String {
    val store = user.merchantStore
    val storeData = if (store != null) {
        "${store.name}||${store.category}||${store.address}||${store.phone}"
    } else {
        ""
    }
    return "${user.id}##${user.name}##${user.email}##${user.phone}##${user.password}##${user.isMerchant}##$storeData"
}

private fun deserializeUserAccount(raw: String): UserAccount? {
    val parts = raw.split("##")
    if (parts.size < MIN_USER_PARTS) return null
    val store = parseStoreParts(parts.getOrNull(STORE_PARTS_INDEX))
    return UserAccount(
        id = parts[0],
        name = parts[1],
        email = parts[2],
        phone = parts[INDEX_PHONE],
        password = parts[INDEX_PASSWORD],
        isMerchant = parts[INDEX_MERCHANT_FLAG].toBoolean(),
        merchantStore = store
    )
}

private fun parseStoreParts(rawStore: String?): MerchantStore? {
    if (rawStore.isNullOrBlank()) return null
    val sParts = rawStore.split("||")
    if (sParts.size < MIN_STORE_PARTS) return null
    return MerchantStore(
        name = sParts[0],
        category = sParts[1],
        address = sParts[2],
        phone = sParts[INDEX_PHONE]
    )
}
