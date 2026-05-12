# RoBERTa 模型获取指南

## 问题
当前环境的 PyTorch 安装有问题，无法直接导出模型。

## 解决方案

### 方案1：在其他环境导出（推荐）

在另一台有稳定 Python 环境的机器上：

```bash
# 1. 安装依赖
pip install torch transformers onnx onnxruntime numpy

# 2. 运行导出脚本
python roberta_intent_recognizer.py

# 3. 复制生成的文件到 Android 项目
```

### 方案2：使用预导出模型

我可以帮你下载已经导出的模型文件：

**需要的文件：**
1. `chinese-roberta-wwm-ext-int8.onnx` (~100MB)
2. `vocab.txt` (~1MB)
3. `intent_vectors.json` (~50KB)

**放置位置：**
```
app/src/main/assets/roberta/
├── chinese-roberta-wwm-ext-int8.onnx
├── vocab.txt
└── intent_vectors.json
```

### 方案3：跳过 RoBERTa，使用规则引擎

如果模型获取困难，可以暂时使用增强的规则引擎：

```kotlin
// AnalysisViewModel 中回退到规则引擎
val result = localRecognizer.recognize(query)
```

## 当前状态

- ✅ Android 代码已准备就绪
- ✅ RoBERTaIntentRecognizer 已实现
- ✅ SimpleTokenizer 已实现
- ⏳ 等待模型文件

## 建议

由于当前环境的限制，我建议：

1. **短期**：使用规则引擎（已正常工作）
2. **中期**：在其他环境导出模型，然后复制过来
3. **长期**：考虑使用更小的模型（如 DistilBERT，66M 参数）

## 下一步

请选择：
- A. 我帮你准备模型文件下载链接
- B. 先使用规则引擎版本构建 APK
- C. 尝试修复当前环境的 PyTorch
