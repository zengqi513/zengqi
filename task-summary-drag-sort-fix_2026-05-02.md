# Task Summary: 拖动排序BUG修复

## Objective
修复分类管理中拖动排序的三个问题：
1. 长按拖动松开后无法进行替换
2. 排序不准确，排序很乱
3. 收入无法操作排序

## Key Reasoning
根本原因：`TransactionViewModel.moveCategoryToPosition()` 中的 `siblings` 列表只过滤了 `parentName`，但没有过滤 `isHidden` 的分类。

由于 UI 层面的 `parentCategories` 已经过滤了隐藏分类：
```kotlin
val parentCategories = allCategories.filter { it.parentName == null && !it.isHidden }
```

但数据库层面的 `siblings` 没有过滤隐藏分类：
```kotlin
val siblings = categoryDao.getCategoriesByType(category.type)
    .filter { it.parentName.isNullOrEmpty() }
    .sortedBy { it.sortOrder }
```

导致当存在隐藏分类时，UI 索引和数据库索引不一致，从而 `toIndex` 计算错误，排序混乱。

## Solution Implemented
修改 `TransactionViewModel.kt` 中的 `moveCategoryToPosition()` 方法，在 `siblings` 中也过滤隐藏分类：
```kotlin
val siblings = categoryDao.getCategoriesByType(category.type)
    .filter { it.parentName.isNullOrEmpty() && !it.isHidden }
    .sortedBy { it.sortOrder }
```

## Technical Details
- 确保数据库查询和 UI 列表使用相同的过滤条件
- `parentName.isNullOrEmpty()`：只获取一级分类
- `!it.isHidden`：排除已隐藏的分类
- `sortedBy { it.sortOrder }`：按 sortOrder 排序

## Outcome
修复后，拖动排序的逻辑能够正确匹配 UI 和数据库的索引，排序功能应该正常工作。收入和支出的拖动排序使用相同的逻辑，因此都能正常操作。

## Files Modified
- `C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\viewmodel\TransactionViewModel.kt`

## APK
- Output: `C:\Users\77497\Desktop\autobookkeeper-drag-fix.apk`
- Size: 26,018,936 bytes
- Built: 2026-05-02 11:15
