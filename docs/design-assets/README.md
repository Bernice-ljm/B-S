# 中期报告图示资产

`report-figures/` 包含中期设计文档使用的 13 张图，每张同时提供：

- `.svg`：可编辑矢量源图，文字保持可编辑；
- `.png`：Markdown 报告预览；
- `.pdf`：矢量打印版本。

视觉规范沿用审核通过的 `flowchart-sample-v4`：白底、微软雅黑（回退为 Noto Sans CJK SC / Arial）、深色文字、唯一饱和蓝色强调、浅绿/浅红状态色、无渐变和阴影。箭头由直线与三角形组成，不使用可能被转换器错误缩放的 SVG marker；样式直接写在元素上，减少渲染器差异。

运行以下命令可从生成器重新生成全部正式图：

```powershell
python design-assets/generate_report_figures.py
```

正式报告引用 PNG；编辑图示内容时修改生成器并重新生成三种格式，然后执行 `nature-figure` 的 SVG 审计并目视检查最终 PNG。`flowchart-sample-v4.*` 是视觉样图，不直接用于报告正文。

