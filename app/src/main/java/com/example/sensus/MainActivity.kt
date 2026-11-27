package com.example.sensus

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.sensus.domain.model.FriendInviteCodec
import com.example.sensus.presentation.auth.PasswordResetLink
import com.example.sensus.presentation.common.FriendRequestNotifier
import com.example.sensus.presentation.push.PushLaunchParser
import com.example.sensus.presentation.push.PushOpenRequests
import com.example.sensus.theme.SensusAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var pushOpenRequests: PushOpenRequests

    private var deepLinkResetToken by mutableStateOf<String?>(null)
    private var deepLinkInviteUrl by mutableStateOf<String?>(null)

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        enableEdgeToEdge()

        deepLinkResetToken = parseResetToken(intent)
        deepLinkInviteUrl = parseFriendInviteUrl(intent)
        // FT-39: app cerrada o en segundo plano; el sistema abre la actividad con el bloque `data` como extras.
        if (savedInstanceState == null) handlePushIntent(intent)

        setContent {
            SensusAppTheme(window) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        initialDeepLinkResetToken = deepLinkResetToken,
                        onResetTokenConsumed = { deepLinkResetToken = null },
                        initialDeepLinkInviteUrl = deepLinkInviteUrl,
                        onInviteUrlConsumed = { deepLinkInviteUrl = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val token = parseResetToken(intent)
        if (!token.isNullOrBlank()) {
            deepLinkResetToken = token
        }
        val inviteUrl = parseFriendInviteUrl(intent)
        if (!inviteUrl.isNullOrBlank()) {
            deepLinkInviteUrl = inviteUrl
        }
        handlePushIntent(intent)
    }

    private fun handlePushIntent(intent: Intent?) {
        PushLaunchParser.parse(intent)?.let(pushOpenRequests::submit)
        PushLaunchParser.consume(intent)
    }

    companion object {
        fun parseResetToken(intent: Intent?): String? {
            return intent?.takeIf { it.action == Intent.ACTION_VIEW }?.dataString?.let(PasswordResetLink::parse)
        }

        fun parseFriendInviteUrl(intent: Intent?): String? {
            val raw = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.dataString ?: return null
            if (raw == FriendRequestNotifier.OPEN_REQUESTS_URI) return raw
            return raw.takeIf { FriendInviteCodec.parseInvite(it) != null }
        }
    }
}
