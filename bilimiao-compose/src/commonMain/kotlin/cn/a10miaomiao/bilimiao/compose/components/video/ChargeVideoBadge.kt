package cn.a10miaomiao.bilimiao.compose.components.video

import androidx.compose.foundation.background
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bilibili.app.archive.v1.Rights

/**
 * 判断 gRPC 稿件是否为充电专属视频。
 * 与 PiliPlus 的判定口径一致：
 * - 旧版充电专属：rights.ugc_pay == 1
 * - 新版付费/包月充电：rights.arc_pay == 1
 */
fun Rights?.isChargeVideo(): Boolean {
    return this?.let {
        it.ugcPay == 1 || it.arcPay == 1
    } == true
}

/**
 * 充电专属视频标识：
 * 带背景的圆角矩形小卡片，出现在视频标题前方，与标题文字同高。
 */
@Composable
fun ChargeVideoBadge(
    modifier: Modifier = Modifier,
    text: String = "充电专属",
) {
    Text(
        text = text,
        modifier = modifier
            .background(
                // 莫奈取色背景（跟随主题配色），不用红色 error 系
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
}

private const val CHARGE_BADGE_KEY = "chargeBadge"

/**
 * 带"充电专属"内联徽标的标题文字。
 * 徽标作为行内占位符嵌在标题第一行开头，标题换行后从满宽继续，
 * 不会像 Row+Text(weight) 那样把整个标题块整体右移。
 */
@Composable
fun TitleWithChargeBadge(
    title: AnnotatedString,
    isChargeVideo: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val text = remember(title, isChargeVideo) {
        buildAnnotatedString {
            if (isChargeVideo) {
                appendInlineContent(CHARGE_BADGE_KEY, "[充电专属]")
                append(" ")
            }
            append(title)
        }
    }
    // 按"充电专属"真实渲染尺寸生成占位符，避免徽标文字被占位符裁掉
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val badgeStyle = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
    )
    val badgeSize = remember(textMeasurer, badgeStyle) {
        val result = textMeasurer.measure(
            text = AnnotatedString("充电专属"),
            style = badgeStyle,
        )
        val scale = density.density * density.fontScale
        val widthPx = result.size.width + with(density) { 12.dp.toPx() }
        val heightPx = result.size.height + with(density) { 6.dp.toPx() }
        (widthPx / scale).sp to (heightPx / scale).sp
    }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = mapOf(
            CHARGE_BADGE_KEY to InlineTextContent(
                // 占位符尺寸与"充电专属"徽标一致，只占第一行行首
                placeholder = Placeholder(
                    width = badgeSize.first,
                    height = badgeSize.second,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    ChargeVideoBadge()
                }
            },
        ),
    )
}
