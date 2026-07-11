package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.QuranViewModel
import com.example.ui.theme.Localization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val messages by viewModel.aiChatMessages.collectAsStateWithLifecycle()
    val isThinking by viewModel.aiThinking.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll chat to latest messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isAr = appLanguage == "Arabic" || appLanguage == "العربية"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = Color(0xFFD4AF37))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(Localization.translate("ai_scholar", appLanguage), fontWeight = FontWeight.Bold)
                            Text(if (isAr) "إجابات قرآنية موثوقة" else "Trusted Quranic Answers", fontSize = 11.sp, color = Color(0xFFD4AF37))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Localization.translate("back", appLanguage), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAiChat() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = Localization.translate("clear_chat", appLanguage), tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Chat List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    val bubbleColor = if (msg.isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }
                    val textColor = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface
                    val align = if (msg.isUser) Alignment.End else Alignment.Start
                    val bubbleShape = if (msg.isUser) {
                        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
                    } else {
                        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = align
                    ) {
                        Card(
                            shape = bubbleShape,
                            colors = CardDefaults.cardColors(containerColor = bubbleColor),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = msg.text,
                                    color = textColor,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (msg.isUser) {
                                if (isAr) "أنت" else "You"
                            } else {
                                if (isAr) "نور" else "Noor AI"
                            },
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                // Thinking Pulse Loader
                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFD4AF37),
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                Localization.translate("ai_thinking", appLanguage),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Quick suggestion chips
            val suggestions = if (isAr) {
                listOf(
                    "اشرح سورة الكوثر",
                    "اقترح خطة يومية للحفظ",
                    "اشرح فضل آية الكرسي"
                )
            } else {
                listOf(
                    "Explain the word Al-Kawthar",
                    "Suggest a daily memorization plan",
                    "Explain the benefits of Ayah Al-Kursi"
                )
            }

            ScrollableTabRow(
                selectedTabIndex = 0,
                indicator = {},
                divider = {},
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                suggestions.forEach { suggestion ->
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .clickable { viewModel.sendAiMessage(suggestion) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = suggestion,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Input Field Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text(Localization.translate("chat_placeholder", appLanguage)) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendAiMessage(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
