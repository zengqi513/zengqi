# Task Summary: 上下箭头排序功能

## Objective
将分类管理的长按拖动排序改为上下箭头移动排序，解决拖动排序不准确的问题。

## Key Reasoning
用户反馈拖动排序功能存在以下问题：
1. 长按拖动松开后无法进行替换
2. 排序不准确，排序很乱
3. 收入无法操作排序

尽管已经修复了 siblings 列表没有过滤隐藏分类的问题，但用户仍然不满意拖动排序的使用体验。因此改为更直观的上下箭头点击排序。

## Solution Implemented

### 1. 修改 ParentCategoryItem 组件
移除拖动相关参数，添加上下移动相关参数：
- 移除：`isDragging`, `onDragStart`, `onDrag`, `onDragEnd`
- 新增：`canMoveUp`, `canMoveDown`, `onMoveUp`, `onMoveDown`

### 2. 移除拖动手势检测
- 移除 Card 的 `detectDragGesturesAfterLongPress`
- 移除拖动视觉反馈（offsetY 计算和 translationY）
- 简化 UI，移除"拖动中"提示文本

### 3. 添加上下箭头按钮
在分类名称右侧添加两个 IconButton：
- 上移按钮：使用 `Icons.Default.KeyboardArrowUp`
- 下移按钮：使用 `Icons.Default.KeyboardArrowDown`
- 按钮状态：
  - 第一个分类：canMoveUp = false，箭头禁用
  - 最后一个分类：canMoveDown = false，箭头禁用
  - 中间分类：两个按钮都启用

### 4. 修改调用逻辑
- 移除所有 dragState 相关代码
- 在 itemsIndexed 中计算 `canMoveUp = index > 0` 和 `canMoveDown = index < parentCategories.size - 1`
- 点击箭头时调用 `viewModel.moveCategoryToPosition(parent, index, index ± 1)`

### 5. 更新提示文本
将"长按一级分类可拖动排序"改为"使用上下箭头移动排序"

## Technical Details
- 上下箭头使用 Material Icons 的 `KeyboardArrowUp` 和 `KeyboardArrowDown`
- 禁用状态的箭头透明度降低（`alpha = 0.3f`）
- 点击一次箭头移动一个位置
- 保持原有的 moveCategoryToPosition 逻辑（已修复隐藏分类过滤）

## Outcome
分类排序改为点击操作，更加直观和可靠：
- ✅ 点击上移按钮，分类向上移动一位
- ✅ 点击下移按钮，分类向下移动一位
- ✅ 边界分类的箭头自动禁用
- ✅ 收入和支出分类都能正常排序

## Files Modified
- `C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\ui\CategoryManagementScreen.kt`

## APK
- Output: `C:\Users\77497\Desktop\autobookkeeper-arrow-sort.apk`
- Size: 26,018,936 bytes
- Built: 2026-05-02 11:25
