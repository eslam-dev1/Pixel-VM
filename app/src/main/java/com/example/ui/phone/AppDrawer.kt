package com.example.ui.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SettingsDataStore
import com.example.domain.search.FuzzySearch
import com.example.domain.search.SystemSettingItem
import com.example.domain.search.SystemSettingsRegistry
import com.example.model.AppDefinition
import com.example.os.AppRegistry
import com.example.simulated.contacts.ContactsHelper
import com.example.simulated.contacts.DeviceContact
import com.example.ui.components.SimulatedAppIcon

@Composable
fun AppDrawer(
    onOpenApp: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    selectedIconPack: String? = null,
    searchEngine: String = "google",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    // Contacts permission & query state
    var hasContactsPermission by remember { mutableStateOf(ContactsHelper.hasContactsPermission(context)) }
    var contactsList by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
        if (isGranted) {
            contactsList = ContactsHelper.loadContacts(context)
        }
    }

    LaunchedEffect(hasContactsPermission) {
        if (hasContactsPermission) {
            contactsList = ContactsHelper.loadContacts(context)
        }
    }

    // Resolve localized titles and sort alphabetically
    val appsWithTitles = remember(context) {
        AppRegistry.APPS.map { app ->
            val title = context.getString(app.titleRes)
            app to title
        }.sortedBy { it.second }
    }

    // Fuzzy matching for Applications
    val filteredApps = remember(searchQuery, appsWithTitles) {
        if (searchQuery.isBlank()) {
            appsWithTitles
        } else {
            appsWithTitles.filter { (app, title) ->
                FuzzySearch.matches(searchQuery, title) || FuzzySearch.matches(searchQuery, app.id)
            }
        }
    }

    // Fuzzy matching for Contacts
    val filteredContacts = remember(searchQuery, contactsList) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            contactsList.filter { contact ->
                FuzzySearch.matches(searchQuery, contact.name) ||
                    (contact.phoneNumber != null && contact.phoneNumber.contains(searchQuery.trim()))
            }
        }
    }

    // Fuzzy matching for System Settings deep links
    val filteredSettings = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            SystemSettingsRegistry.SETTINGS.filter { setting ->
                FuzzySearch.matches(searchQuery, setting.title) ||
                    FuzzySearch.matches(searchQuery, setting.subtitle)
            }
        }
    }

    val isSearching = searchQuery.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF17181C).copy(alpha = 0.98f))
            .pointerInput(Unit) {
                var dragY = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragY = 0f },
                    onVerticalDrag = { _, amount -> dragY += amount },
                    onDragEnd = {
                        if (dragY > 50f) {
                            onCloseDrawer()
                        }
                    }
                )
            }
            .padding(top = 36.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Drag handle pill at top
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Universal Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_search_input"),
                placeholder = {
                    Text(
                        text = "Search apps, contacts, settings...",
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search", tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = onCloseDrawer) {
                            Icon(Icons.Default.Close, contentDescription = "Close Drawer", tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF26272D),
                    unfocusedContainerColor = Color(0xFF26272D),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Universal Results List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isSearching) {
                    // Regular App Drawer view
                    item {
                        Text(
                            text = "All Apps (${appsWithTitles.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(filteredApps, key = { it.first.id }) { (app, title) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenApp(app.id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("drawer_item_${app.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SimulatedAppIcon(
                                appId = app.id,
                                size = 48.dp,
                                iconPackPackage = selectedIconPack
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Group 1: Applications
                    if (filteredApps.isNotEmpty()) {
                        item {
                            SearchCategoryHeader(
                                title = "Apps",
                                count = filteredApps.size
                            )
                        }

                        items(filteredApps, key = { "app_${it.first.id}" }) { (app, title) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onOpenApp(app.id) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("drawer_search_app_${app.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SimulatedAppIcon(
                                    appId = app.id,
                                    size = 44.dp,
                                    iconPackPackage = selectedIconPack
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Application",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Group 2: Contacts
                    if (hasContactsPermission) {
                        if (filteredContacts.isNotEmpty()) {
                            item {
                                SearchCategoryHeader(
                                    title = "Contacts",
                                    count = filteredContacts.size
                                )
                            }

                            items(filteredContacts, key = { "contact_${it.id}_${it.phoneNumber}" }) { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            contact.phoneNumber?.let { num ->
                                                ContactsHelper.openDialer(context, num)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("drawer_search_contact_${contact.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        contact.phoneNumber?.let { phone ->
                                            Text(
                                                text = phone,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    contact.phoneNumber?.let {
                                        IconButton(onClick = { ContactsHelper.openDialer(context, it) }) {
                                            Icon(
                                                Icons.Default.Call,
                                                contentDescription = "Call",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Permission prompt card for Contacts
                        item {
                            SearchCategoryHeader(title = "Contacts", count = null)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF26272D)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Search Device Contacts",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Grant permission to find contacts directly in search",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Allow", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Group 3: System Settings
                    if (filteredSettings.isNotEmpty()) {
                        item {
                            SearchCategoryHeader(
                                title = "Settings",
                                count = filteredSettings.size
                            )
                        }

                        items(filteredSettings, key = { "setting_${it.id}" }) { setting ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        launchSystemSetting(context, setting.action)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("drawer_search_setting_${setting.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        setting.icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = setting.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = setting.subtitle,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Group 4: Web Search
                    item {
                        SearchCategoryHeader(title = "Web Search", count = null)
                        val engineName = when (searchEngine.lowercase()) {
                            "duckduckgo" -> "DuckDuckGo"
                            "bing" -> "Bing"
                            else -> "Google"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    launchWebSearch(context, searchEngine, searchQuery)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("drawer_search_web"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.OpenInBrowser,
                                    contentDescription = "Search Web",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Search with $engineName",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "\"$searchQuery\"",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchCategoryHeader(title: String, count: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (count != null) "$title ($count)" else title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 0.5.sp
        )
    }
}

private fun launchSystemSetting(context: Context, action: String) {
    val intent = Intent(action).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (_: Exception) {}
    }
}

private fun launchWebSearch(context: Context, engine: String, query: String) {
    val url = SettingsDataStore.getSearchUrl(engine, query)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {}
}
