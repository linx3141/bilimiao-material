# Third-Party Notices

本应用包含以下第三方代码的改编与移植：

## KernelSU Manager（GNU GPL v3.0）

- 项目地址：https://github.com/tiann/KernelSU
- 许可证：GNU General Public License v3.0（完整文本见 [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html)）
- 使用内容：
  - 莫奈取色（Monet）配色逻辑（`rememberKernelSUColorScheme`、`PaletteStyle`/`ColorSpec` 的
    `supportsSpec2025` 与 `effectiveFor` 处理），改编为
    `bilimiao-compose/.../MonetTheme.kt`
  - Material 配色页面的完整 UI（主题预览卡片 `ThemePreviewCard`、色块选择
    `ColorButtonMaterial`、模式切换、配色风格/色彩规范选择），改编为
    `bilimiao-compose/.../pages/setting/components/MonetThemeComponents.kt` 与
    `bilimiao-compose/.../pages/setting/ThemeSettingPage.kt`
  - 预测性返回手势开关的应用逻辑（`KernelSUApplication` 中对
    `enableOnBackInvokedCallback` 的 HiddenApiBypass 豁免与反射设置），改编于
    `app/src/main/java/com/a10miaomiao/bilimiao/Bilimiao.kt`

改编代码依据 GPL-3.0 协议发布；本应用整体许可证与 KernelSU 无关，使用上述
改编部分的用户应同时遵守 GPL-3.0 的要求。
