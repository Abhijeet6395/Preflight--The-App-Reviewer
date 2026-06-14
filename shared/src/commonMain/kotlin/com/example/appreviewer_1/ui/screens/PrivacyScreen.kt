package com.example.appreviewer_1.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appreviewer_1.ui.components.GlowCard
import com.example.appreviewer_1.ui.reviewer.ReviewerIntent
import com.example.appreviewer_1.ui.theme.NeonCyan
import com.example.appreviewer_1.ui.theme.Slate

/** A short, plain-language summary of how the app handles data. Kept factual and
 *  in sync with what the code actually does — see PRIVACY.md. */
@Composable
fun PrivacyScreen(onIntent: (ReviewerIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        TextButton(onClick = { onIntent(ReviewerIntent.BackToHome) }) {
            Text("← Back", color = NeonCyan, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Privacy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "What the app does with your data — in plain terms.",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate,
        )

        Spacer(Modifier.height(20.dp))
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            PrivacyPoint(
                "APKs are analyzed on your device",
                "When you scan an APK, it's read on your phone and deleted right after. " +
                    "The file is never uploaded. APK analysis works offline.",
            )
            Spacer(Modifier.height(16.dp))
            PrivacyPoint(
                "No account, no analytics, no ads",
                "The app doesn't ask you to sign in and doesn't track usage.",
            )
            Spacer(Modifier.height(16.dp))
            PrivacyPoint(
                "Going online is limited to GitHub",
                "The only time the app connects to the internet is to read a public " +
                    "repository's manifest from GitHub, and only when you paste a repo link.",
            )
            Spacer(Modifier.height(16.dp))
            PrivacyPoint(
                "Your scan history stays on your device",
                "Past results are saved in the app's private storage and are removed when " +
                    "you uninstall.",
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Open source (MIT) — read or verify the code at\n" +
                    "github.com/Abhijeet6395/Preflight--The-App-Reviewer",
                style = MaterialTheme.typography.bodySmall,
                color = Slate,
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun PrivacyPoint(title: String, body: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = Slate,
        )
    }
}
