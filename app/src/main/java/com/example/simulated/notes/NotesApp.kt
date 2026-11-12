package com.example.simulated.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.NoteDao
import com.example.data.NoteEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NOTE_COLORS = listOf(
    Color(0xFFFFF9C4), // Yellow
    Color(0xFFFFCCBC), // Coral
    Color(0xFFC8E6C9), // Sage Green
    Color(0xFFB3E5FC), // Sky Blue
    Color(0xFFE1BEE7), // Lavender
    Color(0xFFF5F5F5)  // Light Grey
)

@Composable
fun NotesApp(
    noteDao: NoteDao,
    modifier: Modifier = Modifier
) {
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val allNotes by if (searchQuery.isBlank()) {
        noteDao.getAllNotes().collectAsState(initial = emptyList())
    } else {
        noteDao.searchNotes(searchQuery).collectAsState(initial = emptyList())
    }

    val scope = rememberCoroutineScope()

    if (editingNote != null || isCreatingNew) {
        NoteEditorScreen(
            initialNote = editingNote,
            onSave = { title, content, isPinned, colorIndex ->
                scope.launch {
                    val noteToSave = editingNote?.copy(
                        title = title,
                        content = content,
                        isPinned = isPinned,
                        colorIndex = colorIndex,
                        updatedAt = System.currentTimeMillis()
                    ) ?: NoteEntity(
                        title = title,
                        content = content,
                        isPinned = isPinned,
                        colorIndex = colorIndex,
                        updatedAt = System.currentTimeMillis()
                    )
                    noteDao.insertNote(noteToSave)
                    editingNote = null
                    isCreatingNew = false
                }
            },
            onDelete = { note ->
                scope.launch {
                    noteDao.deleteNote(note)
                    editingNote = null
                    isCreatingNew = false
                }
            },
            onBack = {
                editingNote = null
                isCreatingNew = false
            }
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_search_input"),
                    placeholder = { Text("Search notes…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (allNotes.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.no_notes_yet),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.create_first_note),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    val pinnedNotes = allNotes.filter { it.isPinned }
                    val otherNotes = allNotes.filter { !it.isPinned }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (pinnedNotes.isNotEmpty()) {
                            item {
                                Text(
                                    text = "PINNED",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                            }
                            items(pinnedNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { editingNote = note },
                                    onPinToggle = {
                                        scope.launch {
                                            noteDao.updateNote(note.copy(isPinned = !note.isPinned))
                                        }
                                    }
                                )
                            }
                        }

                        if (otherNotes.isNotEmpty()) {
                            if (pinnedNotes.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "OTHERS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                                    )
                                }
                            }
                            items(otherNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { editingNote = note },
                                    onPinToggle = {
                                        scope.launch {
                                            noteDao.updateNote(note.copy(isPinned = !note.isPinned))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { isCreatingNew = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_note_fab"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onPinToggle: () -> Unit
) {
    val bgColor = NOTE_COLORS.getOrElse(note.colorIndex) { NOTE_COLORS.last() }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (note.title.isBlank()) "Untitled" else note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onPinToggle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin Note",
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f)
                    )
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    maxLines = 3,
                    fontSize = 14.sp,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dateFormat.format(Date(note.updatedAt)),
                fontSize = 11.sp,
                color = Color.Black.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun NoteEditorScreen(
    initialNote: NoteEntity?,
    onSave: (title: String, content: String, isPinned: Boolean, colorIndex: Int) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var colorIndex by remember { mutableIntStateOf(initialNote?.colorIndex ?: 0) }

    val bgColor = NOTE_COLORS.getOrElse(colorIndex) { NOTE_COLORS.last() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                onSave(title, content, isPinned, colorIndex)
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black.copy(alpha = 0.8f))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isPinned = !isPinned }) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f)
                    )
                }

                if (initialNote != null) {
                    IconButton(onClick = { onDelete(initialNote) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Black.copy(alpha = 0.5f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Color selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NOTE_COLORS.forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { colorIndex = index }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Title", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = { Text("Note content…", color = Color.Black.copy(alpha = 0.4f)) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("note_content_input"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}
