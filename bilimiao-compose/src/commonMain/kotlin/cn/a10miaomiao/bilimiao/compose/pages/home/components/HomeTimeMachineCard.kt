package cn.a10miaomiao.bilimiao.compose.pages.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import com.a10miaomiao.bilimiao.comm.entity.region.RegionInfo
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import coil3.compose.AsyncImage
import bilimiao.bilimiao_compose.generated.resources.Res
import bilimiao.bilimiao_compose.generated.resources.ic_region_ad
import bilimiao.bilimiao_compose.generated.resources.ic_region_dh
import bilimiao.bilimiao_compose.generated.resources.ic_region_dsj
import bilimiao.bilimiao_compose.generated.resources.ic_region_dy
import bilimiao.bilimiao_compose.generated.resources.ic_region_fj
import bilimiao.bilimiao_compose.generated.resources.ic_region_fj_domestic
import bilimiao.bilimiao_compose.generated.resources.ic_region_gc
import bilimiao.bilimiao_compose.generated.resources.ic_region_kj
import bilimiao.bilimiao_compose.generated.resources.ic_region_sh
import bilimiao.bilimiao_compose.generated.resources.ic_region_ss
import bilimiao.bilimiao_compose.generated.resources.ic_region_wd
import bilimiao.bilimiao_compose.generated.resources.ic_region_yl
import bilimiao.bilimiao_compose.generated.resources.ic_region_ys
import bilimiao.bilimiao_compose.generated.resources.ic_region_yx
import bilimiao.bilimiao_compose.generated.resources.ic_region_yy
import bilimiao.bilimiao_compose.generated.resources.ic_season_0
import bilimiao.bilimiao_compose.generated.resources.ic_season_1
import bilimiao.bilimiao_compose.generated.resources.ic_season_2
import bilimiao.bilimiao_compose.generated.resources.ic_season_3
import bilimiao.bilimiao_compose.generated.resources.ic_time
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.random.Random

// 大分类图标：统一使用 Material 图标（不再使用彩色位图/网络图）。
// 接口的 icon 字段大多为空，因此按分类名称映射，保证每个大分类都有图标。
private val regionIconMap: Map<String, ImageVector> by lazy {
    mapOf(
        "番剧" to Icons.Filled.Movie,
        "国创" to Icons.Filled.Flag,
        "纪录片" to Icons.Filled.VideoCameraBack,
        "电影" to Icons.Filled.Movie,
        "电视剧" to Icons.Filled.LiveTv,
        "动画" to Icons.Filled.Animation,
        "音乐" to Icons.Filled.MusicNote,
        "舞蹈" to Icons.Filled.SportsGymnastics,
        "游戏" to Icons.Filled.SportsEsports,
        "知识" to Icons.Filled.School,
        "科技" to Icons.Filled.Science,
        "资讯" to Icons.Filled.Newspaper,
        "咨询" to Icons.Filled.Newspaper,
        "运动" to Icons.Filled.DirectionsRun,
        "汽车" to Icons.Filled.LocalTaxi,
        "生活" to Icons.Filled.Yard,
        "美食" to Icons.Filled.Restaurant,
        "动物圈" to Icons.Filled.Pets,
        "鬼畜" to Icons.Filled.SentimentVerySatisfied,
        "时尚" to Icons.Filled.Checkroom,
        "娱乐" to Icons.Filled.Celebration,
        "影视" to Icons.Filled.VideoCameraFront,
        "剧情" to Icons.Filled.SpeakerNotes,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeTimeMachineCard(
    iconModel: Any?,
    cardName: String,
    onClick: () -> Unit,
    segmentedShape: Shape? = null,
    content: @Composable () -> Unit,
) {
    val segmentedShapes = LocalListItemShapes.current
    val hasSegmented = segmentedShapes != null || segmentedShape != null
    // Material 3 Expressive 卡片
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hasSegmented) {
                    // 分段/网格场景：左右边距由网格 contentPadding 提供，
                    // 卡片不再加水平 padding（否则列间距会翻倍）
                    Modifier
                } else {
                    Modifier.padding(5.dp)
                }
            ),
        shape = segmentedShape ?: segmentedShapes?.shape ?: RoundedCornerShape(20.dp),
        color = if (hasSegmented) {
            MaterialTheme.colorScheme.surfaceBright
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        onClick = onClick,
    ) {
        // Surface 的 content 为 Box（叠加）布局，需用 Column 垂直排列标题行与内容区
        Column {
            Row(
                modifier = Modifier.padding(
                    start = 12.dp,
                    top = 10.dp,
                    end = 12.dp,
                    bottom = 10.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (iconModel != null) {
                    when (iconModel) {
                        is ImageVector -> {
                            Icon(
                                imageVector = iconModel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(30.dp)
                                    .padding(end = 4.dp),
                            )
                        }
                        is DrawableResource -> {
                            Image(
                                painter = painterResource(iconModel),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(30.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        else -> {
                            AsyncImage(
                                model = iconModel,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(30.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = cardName,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                    ),
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun HomeTimeMachineTimeCard(
    timeText: String,
    timeSeason: Int,
    onClick: () -> Unit,
) {
    HomeTimeMachineCard(
        iconModel = Icons.Filled.Schedule,
        // 时间线范围（XXXX 至 XXXX）直接作为标题
        cardName = timeText,
        onClick = onClick
    ) {
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeTimeMachineRegionCard(
    region: RegionInfo,
    shape: Shape? = null,
    onClick: (RegionInfo, Int) -> Unit
) {
    // 统一使用 Material 图标：优先按分类名称，其次按接口的 icon key
    val iconModel: Any? = regionIconMap[region.name] ?: regionIconMap[region.icon]

    HomeTimeMachineCard(
        iconModel = iconModel,
        cardName = region.name,
        segmentedShape = shape,
        onClick = {
            // 二级菜单样式：点击大分类直接进入第一个子标签的视频列表
            onClick(region, 0)
        }
    ) {
    }
}
