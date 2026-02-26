package br.com.tec.tecmotors.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R

data class ChartBar(
    val label: String,
    val value: Double,
    val valueText: String
)

@Composable
fun MetricBarChart(
    title: String,
    bars: List<ChartBar>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
            )

            if (bars.isEmpty()) {
                Text(
                    text = stringResource(R.string.text_no_chart_data),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                return@Column
            }

            val maxValue = bars.maxOfOrNull { it.value } ?: 0.0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { bar ->
                    val fraction = if (maxValue <= 0.0) 0f else (bar.value / maxValue).toFloat().coerceIn(0f, 1f)
                    val animatedFraction = animateFloatAsState(targetValue = fraction, label = "bar-animation")

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier.height(120.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .height((8f + 112f * animatedFraction.value).dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    )
                            )
                        }
                        Text(text = bar.valueText, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        Text(text = bar.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}
