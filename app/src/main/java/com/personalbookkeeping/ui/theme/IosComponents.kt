package com.personalbookkeeping.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class IosSegmentOption(
    val label: String,
    val testTag: String? = null,
)

@Composable
fun IosSegmentedControl(
    options: List<IosSegmentOption>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val optionModifier = Modifier
                .weight(1f)
                .heightIn(min = 38.dp)
                .then(
                    if (selected) {
                        Modifier.shadow(1.dp, RoundedCornerShape(8.dp))
                    } else {
                        Modifier
                    },
                )
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                )
                .then(
                    option.testTag?.let { Modifier.testTag(it) } ?: Modifier,
                )
                .selectable(
                    selected = selected,
                    onClick = { onSelected(index) },
                    role = Role.Tab,
                )
            Box(optionModifier, contentAlignment = Alignment.Center) {
                Text(
                    option.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
fun IosBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            "‹ 返回",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun IosSettingsGroup(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column { content() }
    }
}

@Composable
fun IosSettingsRow(
    title: String,
    subtitle: String,
    glyph: String,
    tint: Color,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 60.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
