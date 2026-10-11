package com.alarm.meluko.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.components.glass
import com.alarm.meluko.ui.theme.DefaultFontFamily

private val BarShape = RoundedCornerShape(32.dp)
private val IndicatorShape = RoundedCornerShape(26.dp)

/**
 * Floating frosted-glass pill. Content scrolls underneath and is blurred
 * through it; the amber indicator slides between tabs on an expressive spring.
 */
@Composable
fun MelukoBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val slideSpring = spring<Dp>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow
    )

    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 36.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(64.dp)
            .glass(BarShape)
    ) {
        val slotWidth = maxWidth / bottomNavItems.size
        val indicatorOffset by animateDpAsState(
            targetValue = slotWidth * selectedTab,
            animationSpec = slideSpring,
            label = "indicatorOffset"
        )

        // Sliding selection indicator
        Box(
            Modifier
                .offset(x = indicatorOffset)
                .width(slotWidth)
                .fillMaxHeight()
                .padding(6.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, IndicatorShape)
        )

        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            bottomNavItems.forEachIndexed { index, item ->
                val isSelected = selectedTab == index
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "navContent$index"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                    label = "navIconScale$index"
                )

                Row(
                    modifier = Modifier
                        .width(slotWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onTabSelected(index)
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                    )
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
                    ) {
                        Text(
                            text = item.label,
                            modifier = Modifier.padding(start = 7.dp),
                            fontFamily = DefaultFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = contentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
