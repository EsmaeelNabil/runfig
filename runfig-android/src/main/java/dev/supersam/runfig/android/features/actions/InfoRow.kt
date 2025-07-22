package dev.supersam.runfig.android.features.actions

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfoRow(label: String, value: String?, allowCopy: Boolean = true) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(enabled = allowCopy && value != null, onClick = {}, onLongClick = {
                value?.let {
                    clipboardManager.setText(AnnotatedString(it))
                    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
                }
            })
            .padding(vertical = 2.dp)
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(max = 130.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(value ?: "N/A")
    }
    Spacer(modifier = Modifier.height(4.dp))
}
